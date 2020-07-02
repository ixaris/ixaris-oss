package com.ixaris.commons.jooq.microservices;

import static com.ixaris.commons.jooq.microservices.AtLeastOnceHandleEventType.PROP_EVENTHANDLE_REFRESH_INTERVAL;
import static com.ixaris.commons.jooq.microservices.AtLeastOncePublishEventType.PROP_EVENTPUBLISH_REFRESH_INTERVAL;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import com.ixaris.commons.clustering.lib.service.ClusterRegistry;
import com.ixaris.commons.jooq.persistence.JooqAsyncPersistenceProvider;
import com.ixaris.commons.microservices.lib.client.support.ServiceClientSupport;
import com.ixaris.commons.microservices.lib.service.support.ServiceSupport;
import com.ixaris.commons.misc.lib.defaults.Defaults;
import com.ixaris.commons.multitenancy.lib.MultiTenancy;
import com.ixaris.commons.multitenancy.lib.async.MultiTenantAtLeastOnceProcessorFactory;
import com.ixaris.commons.multitenancy.lib.data.DataUnit;

/**
 * Provides a factory which wraps a ServiceEventPublishers in a AtLeastOnceEventPublisher backed by JOOQ
 *
 * @author marcel
 */
@Configuration
public class JooqConfiguration {
    
    @Bean(destroyMethod = "stop")
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static AtLeastOncePublishEventType atLeastOncePublishEventType(final MultiTenancy multiTenancy,
                                                                          final JooqAsyncPersistenceProvider db,
                                                                          final ClusterRegistry clusterRegistry,
                                                                          final ServiceSupport serviceSupport,
                                                                          final MultiTenantAtLeastOnceProcessorFactory atLeastOnceProcessorFactory,
                                                                          final Optional<Set<AtLeastOncePublishEventDataUnit>> dataUnits,
                                                                          final Environment env) {
        final Set<String> processedUnits = dataUnits
            .map(ds -> ds.stream().map(DataUnit::get).collect(Collectors.toSet()))
            .orElse(Collections.emptySet());
        final AtLeastOncePublishEventType processor = new AtLeastOncePublishEventType(db,
            clusterRegistry,
            serviceSupport,
            atLeastOnceProcessorFactory,
            getPublishRefreshInterval(env),
            processedUnits);
        processor.start();
        multiTenancy.registerTenantLifecycleParticipant(processor);
        return processor;
    }
    
    @Bean(destroyMethod = "stop")
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static AtLeastOnceHandleEventType atLeastOnceHandleEventType(final MultiTenancy multiTenancy,
                                                                        final JooqAsyncPersistenceProvider db,
                                                                        final ClusterRegistry clusterRegistry,
                                                                        final ServiceClientSupport serviceClientSupport,
                                                                        final MultiTenantAtLeastOnceProcessorFactory atLeastOnceProcessorFactory,
                                                                        final Optional<Set<AtLeastOnceHandleEventDataUnit>> dataUnits,
                                                                        final Environment env) {
        final Set<String> processedUnits = dataUnits
            .map(ds -> ds.stream().map(DataUnit::getUnit).collect(Collectors.toSet()))
            .orElse(Collections.emptySet());
        final AtLeastOnceHandleEventType processor = new AtLeastOnceHandleEventType(db,
            clusterRegistry,
            serviceClientSupport,
            atLeastOnceProcessorFactory,
            getHandleRefreshInterval(env),
            processedUnits);
        processor.start();
        multiTenancy.registerTenantLifecycleParticipant(processor);
        return processor;
    }
    
    private static long getPublishRefreshInterval(final Environment env) {
        return Defaults.getOrDefault(PROP_EVENTPUBLISH_REFRESH_INTERVAL,
            env.getProperty(PROP_EVENTPUBLISH_REFRESH_INTERVAL, Long.class),
            AtLeastOncePublishEventType.REFRESH_INTERVAL);
    }
    
    private static long getHandleRefreshInterval(final Environment env) {
        return Defaults.getOrDefault(PROP_EVENTHANDLE_REFRESH_INTERVAL,
            env.getProperty(PROP_EVENTHANDLE_REFRESH_INTERVAL, Long.class),
            AtLeastOnceHandleEventType.REFRESH_INTERVAL);
    }
    
}
