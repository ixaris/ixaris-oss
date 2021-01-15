package com.ixaris.commons.minio.files;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ixaris.commons.multitenancy.lib.MultiTenancy;

import io.minio.MinioClient;

/**
 * All the configuration required to be able to persist files via a {@link MinioFileStore}. Loads the secrets required
 * and feeds them to the {@link MinioClient} powering the library.
 */
@Configuration
public class MinioConfiguration {
    
    @Bean
    @ConditionalOnBean(MinioClient.class)
    public MinioMultiTenantFileStore minioMultiTenantFileStore(final MultiTenancy multiTenancy, final MinioClient minioClient) {
        final MinioMultiTenantFileStore store = new MinioMultiTenantFileStore(minioClient);
        multiTenancy.registerTenantLifecycleParticipant(store);
        return store;
    }
    
}
