package com.ixaris.commons.microservices.web.dynamic;

import com.ixaris.commons.microservices.lib.common.ServicePathHolder;

public final class DecodedUrl {
    
    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    
    public final String serviceName;
    public final String serviceKey;
    public final ServicePathHolder path;
    
    public DecodedUrl(final String pathInfo, final boolean withServiceKey) {
        String url = pathInfo;
        // expect (empty-string)/serviceName/serviceKey/[../..]
        final String[] nameAndPath = url != null ? url.split("/", 3) : EMPTY_STRING_ARRAY;
        if ((nameAndPath.length < 2) || nameAndPath[1].trim().isEmpty()) {
            throw new IllegalStateException("Invalid " + url + ": missing or empty service name");
        }
        serviceName = nameAndPath[1];
        url = nameAndPath.length == 3 ? nameAndPath[2] : null;
        if (withServiceKey) {
            final String[] keyAndPath = url != null ? url.split("/", 2) : EMPTY_STRING_ARRAY;
            if (keyAndPath.length < 1) {
                throw new IllegalStateException("Invalid " + url + ": missing service key");
            }
            serviceKey = keyAndPath[0];
            url = keyAndPath.length == 2 ? keyAndPath[1] : null;
        } else {
            serviceKey = null;
        }
        path = url == null ? ServicePathHolder.empty() : ServicePathHolder.parse(url);
    }
    
}
