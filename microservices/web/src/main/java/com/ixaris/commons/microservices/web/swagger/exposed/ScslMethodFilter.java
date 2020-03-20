package com.ixaris.commons.microservices.web.swagger.exposed;

import java.util.List;

/**
 * A filter/predicate to indicate whether a SCSL method should be exposed as an endpoint or not.
 *
 * @author <a href="mailto:aldrin.seychell@ixaris.com">aldrin.seychell</a>
 */
@FunctionalInterface
public interface ScslMethodFilter {
    
    boolean shouldProcess(List<String> tags, String security);
    
}
