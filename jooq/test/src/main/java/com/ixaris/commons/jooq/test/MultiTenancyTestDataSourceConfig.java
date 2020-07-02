package com.ixaris.commons.jooq.test;

import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ixaris.commons.persistence.lib.datasource.MultiTenancyDataSourceConfig;
import com.ixaris.commons.persistence.lib.datasource.MultiTenancyDataSourceDefaultsConfig;
import com.ixaris.commons.persistence.lib.datasource.RelationalDbProperties;

@Configuration
@SuppressWarnings("squid:S1118")
public class MultiTenancyTestDataSourceConfig {
    
    @Bean
    public static MultiTenancyDataSourceConfig multiTenancyDataSourceConfig() {
        final RelationalDbProperties mysql = new RelationalDbProperties();
        mysql.setUrl(getUrl());
        mysql.setUser(getUser());
        mysql.setPassword(getPassword());
        
        final MultiTenancyDataSourceDefaultsConfig defaults = new MultiTenancyDataSourceDefaultsConfig();
        defaults.setMysql(mysql);
        
        final MultiTenancyDataSourceConfig config = new MultiTenancyDataSourceConfig();
        config.setDefaults(defaults);
        return config;
    }
    
    private static String getPassword() {
        return Optional.ofNullable(System.getProperty("test.db.password")).orElse("root");
    }
    
    private static String getUser() {
        return Optional.ofNullable(System.getProperty("test.db.user")).orElse("root");
    }
    
    private static String getUrl() {
        return Optional.ofNullable(System.getProperty("test.db.url")).orElse("jdbc:mysql://"
            + Optional.ofNullable(System.getProperty("mysql.url")).orElse("localhost:13306")
            + "?autoReconnect=true&verifyServerCertificate=false&useSSL=true&useUniCode=true&characterEncoding=UTF-8");
    }
    
}
