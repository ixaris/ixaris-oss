package com.ixaris.commons.multitenancy.web;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.ixaris.commons.multitenancy.lib.MultiTenancy;
import com.ixaris.commons.multitenancy.test.TestTenants;

@RunWith(MockitoJUnitRunner.class)
public class MultiTenantFilterTest {
    
    @Mock
    private HttpServletRequest request;
    
    private MultiTenancy multiTenancy;
    
    @Before
    public void start() {
        multiTenancy = new MultiTenancy();
        multiTenancy.start();
    }
    
    @After
    public void stop() {
        multiTenancy.stop();
    }
    
    /**
     * Need to make sure that next element in filter chain after a {@link MultiTenantFilter} needs to have the correct active tenant.
     *
     * <p>Tests that when the tenant header is not set (and there is only a single tenant loaded) we take that "default" tenant
     *
     * @throws IOException
     * @throws ServletException
     */
    @Test
    public void testTenantHeaderNotSet() throws IOException, ServletException {
        multiTenancy.addTenant(TestTenants.DEFAULT);
        
        final MultiTenantFilter sut = new MultiTenantFilter(multiTenancy);
        
        sut.doFilter(request, null, (request, response) -> {
            // Make sure the next element in chain has correct active tenant
            Assert.assertEquals(TestTenants.DEFAULT, MultiTenancy.getCurrentTenant());
        });
    }
    
    /**
     * Need to make sure that next element in filter chain after a {@link MultiTenantFilter} needs to have the correct active tenant.
     *
     * <p>Tests that when the tenant header set.
     *
     * @throws IOException
     * @throws ServletException
     */
    @Test
    public void testTenantHeaderSet() throws IOException, ServletException {
        multiTenancy.addTenant(TestTenants.DEFAULT);
        multiTenancy.addTenant(TestTenants.LEFT);
        multiTenancy.addTenant(TestTenants.RIGHT);
        
        Mockito.when(request.getHeader("X-Tenant")).thenReturn(TestTenants.DEFAULT);
        
        final MultiTenantFilter sut = new MultiTenantFilter(multiTenancy);
        
        sut.doFilter(request, null, (request, response) -> {
            // Make sure the next element in chain has correct active tenant
            Assert.assertEquals(TestTenants.DEFAULT, MultiTenancy.getCurrentTenant());
        });
    }
    
    // TODO: missing error path tests!
    
}
