package com.ixaris.commons.multitenancy.web;

import static com.ixaris.commons.multitenancy.lib.MultiTenancy.TENANT;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.ixaris.commons.multitenancy.lib.MultiTenancy;

/**
 * Applies multitenant filtering to edge services. Request passed through this filter will be performed by the associated tenant. Applies a
 * Mapped Diagnostics Filter to wrap around all instances of log entries to add currently running tenant
 *
 * @author <a href="mailto:matthias.portelli@ixaris.com">matthias.portelli</a>
 * @author <a href="mailto:aaron.axisa@ixaris.com">aaron.axisa</a>
 */
public final class MultiTenantFilter implements Filter {
    
    public static final String TENANT_HEADER = "X-Tenant";
    
    private static final Logger LOG = LoggerFactory.getLogger(MultiTenantFilter.class);
    private static final String MDC_FILTER_KEY_TENANT = "tenant";
    
    private final MultiTenancy multiTenancy;
    
    public MultiTenantFilter(final MultiTenancy multiTenancy) {
        this.multiTenancy = multiTenancy;
    }
    
    @Override
    public final void init(final FilterConfig filterConfig) {
        // no init required
    }
    
    /*
     * (non-Javadoc)
     * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)
     * Determine correct tenant and process request within the scope of said tenant.
     */
    @Override
    public final void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
        try {
            String tenant;
            if (request instanceof HttpServletRequest) {
                tenant = ((HttpServletRequest) request).getHeader(TENANT_HEADER);
            } else {
                throw new IllegalStateException("Could not extract headers from servlet request: request not an http servlet request but ["
                    + request.getClass()
                    + "]");
            }
            
            final String finalTenant = tenant == null ? multiTenancy.getDefaultTenant() : tenant;
            
            TENANT.exec(finalTenant, () -> {
                MDC.put(MDC_FILTER_KEY_TENANT, finalTenant);
                try {
                    chain.doFilter(request, response);
                } catch (ServletException | IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    MDC.remove(MDC_FILTER_KEY_TENANT);
                }
            });
        } catch (final IllegalStateException e) {
            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            LOG.error("Error while checking the tenant: ", e);
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            // should not happen
            throw new IllegalStateException(e);
        }
    }
    
    @Override
    public final void destroy() {}
    
}
