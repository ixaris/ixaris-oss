package com.ixaris.commons.dimensions.counters;

import static com.ixaris.commons.dimensions.counters.AtLeastOnceApplyCounterEventType.PROP_COUNTEREVENTAPPLY_REFRESH_INTERVAL;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import com.ixaris.commons.clustering.lib.service.ClusterRegistry;
import com.ixaris.commons.dimensions.counters.cache.ClusterShardedCounterCacheProvider;
import com.ixaris.commons.dimensions.counters.cache.CounterCacheProvider;
import com.ixaris.commons.jooq.persistence.JooqAsyncPersistenceProvider;
import com.ixaris.commons.misc.lib.defaults.Defaults;
import com.ixaris.commons.multitenancy.lib.MultiTenancy;
import com.ixaris.commons.multitenancy.lib.async.MultiTenantAtLeastOnceProcessorFactory;
import com.ixaris.commons.multitenancy.lib.data.DataUnit;

/**
 * Provides a factory which wraps a ServiceEventPublishers in a AtLeastOnceEventPublisher backed by JOOQ
 *
 * @author brian.vella
 */
@Configuration
public class CounterEventApplyConfig {
    
    @Bean(destroyMethod = "stop")
    public static AtLeastOnceApplyCounterEventType atLeastOnceCounterEventApplyProcessor(final JooqAsyncPersistenceProvider db,
                                                                                         final CounterCacheProvider cache,
                                                                                         final ClusterRegistry clusterRegistry,
                                                                                         final MultiTenancy multiTenancy,
                                                                                         final Environment env,
                                                                                         final MultiTenantAtLeastOnceProcessorFactory atLeastOnceProcessorFactory,
                                                                                         final Optional<Set<AtLeastOnceApplyCounterEventDataUnit>> units) {
        
        final AtLeastOnceApplyCounterEventType processor = new AtLeastOnceApplyCounterEventType(db,
            cache,
            clusterRegistry,
            atLeastOnceProcessorFactory,
            getRefreshInterval(env),
            units.map(ds -> ds.stream().map(DataUnit::get).collect(Collectors.toSet())).orElse(Collections.emptySet()));
        processor.start();
        multiTenancy.registerTenantLifecycleParticipant(processor);
        return processor;
    }
    
    @Bean
    public static CounterCacheProvider counterCacheProvider(final MultiTenancy multiTenancy, final ClusterRegistry clusterRegistry) {
        return new ClusterShardedCounterCacheProvider(multiTenancy, clusterRegistry);
    }
    
    private static long getRefreshInterval(final Environment env) {
        return Defaults.getOrDefault(PROP_COUNTEREVENTAPPLY_REFRESH_INTERVAL,
            env.getProperty(PROP_COUNTEREVENTAPPLY_REFRESH_INTERVAL, Long.class),
            AtLeastOnceApplyCounterEventType.REFRESH_INTERVAL);
    }
    
}
