//package com.ixaris.commons.multitenancy.test;
//
//import java.util.Arrays;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import com.ixaris.commons.multitenancy.lib.MultiTenancy;
//import com.ixaris.commons.multitenancy.spring.MultiTenancyTenants;
//
///**
// * @author benjie.gatt
// */
//@Configuration
//@SuppressWarnings("squid:S1118")
//public class MultiTenancyTestTenants {
//    
//    private static final Logger LOG = LoggerFactory.getLogger(MultiTenancyTestTenants.class);
//    
//    @Bean
//    public static MultiTenancyTenants multiTenancyTenants() {
//        // note: the name of the method is important as it overrides the MultiTenancyTenants bean that is normally
//        // created (and would be empty in this case)
//        // there should be a way to do this via loading the right property sources - future devs (maybe even me) who
//        // look at this code in utter disgust can try to do that...
//        final String[] tenants = { MultiTenancy.SYSTEM_TENANT, TestTenants.DEFAULT, TestTenants.LEFT, TestTenants.RIGHT };
//        LOG.info("Preparing test configuration for multitenancy. Default is {} tenants ({})", tenants.length, tenants);
//        final MultiTenancyTenants multiTenancyTenants = new MultiTenancyTenants();
//        multiTenancyTenants.setTenants(Arrays.asList(tenants));
//        
//        return multiTenancyTenants;
//    }
//    
//}
