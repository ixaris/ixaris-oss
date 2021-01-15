package com.ixaris.commons.zookeeper.multitenancy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ZookeeperConfiguration {
    
    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(value = "local", matchIfMissing = true, havingValue = "false")
    public ZookeeperClient zookeeperClientConfiguration(@Value("${zookeeper.url:zookeeper-service:2181}") final String zookeeperUrl) {
        final ZookeeperClientConfiguration configuration = new ZookeeperClientConfiguration(zookeeperUrl);
        return new ZookeeperClient(configuration);
    }
    
}
