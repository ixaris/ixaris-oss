package com.ixaris.commons.microservices.web.swagger.exposed;

import java.util.List;

/**
 * A filter/predicate to indicate whether a SCSL method should be treated as a "create" method. Create methods in a
 * Swagger API expect the last part of the path is an "_" which should then be translated to a unique ID by the
 * receiving endpoint (e.g. appgateway)
 *
 * @author <a href="mailto:aldrin.seychell@ixaris.com">aldrin.seychell</a>
 */
@FunctionalInterface
public interface ScslCreateMethodFilter {
    
    boolean isCreate(String name, List<String> methodTags);
    
}
