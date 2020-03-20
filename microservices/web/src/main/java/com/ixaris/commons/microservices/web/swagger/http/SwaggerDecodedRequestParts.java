package com.ixaris.commons.microservices.web.swagger.http;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;

import com.ixaris.commons.microservices.lib.common.ServicePathHolder;
import com.ixaris.commons.microservices.lib.common.exception.ClientInvalidRequestException;
import com.ixaris.commons.microservices.web.HttpMethod;
import com.ixaris.commons.microservices.web.HttpRequest;
import com.ixaris.commons.misc.lib.object.Tuple2;

/**
 * @author <a href="mailto:aldrin.seychell@ixaris.com">aldrin.seychell</a>
 */
public final class SwaggerDecodedRequestParts {
    
    public interface SpiChecker {
        
        boolean isSpi(String serviceName);
        
    }
    
    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    
    private final String serviceName;
    private final String serviceKey;
    private final String method;
    private final ServicePathHolder path;
    private final String payload;
    
    public SwaggerDecodedRequestParts(final HttpRequest<?> request, final String prefix, final SpiChecker spiChecker) {
        // GET: /service_name[/../..] <- method = get
        // POST: /service_name[/../..]/method
        // POST: /service_name[/../..]/_/method <- create
        String url = request.getPath().substring(prefix.length());
        final String[] nameAndPath = url.split("/", 3);
        if (nameAndPath.length < 2) {
            throw new ClientInvalidRequestException("Invalid " + url + ": missing service name");
        }
        serviceName = nameAndPath[1].trim();
        if (serviceName.isEmpty()) {
            throw new ClientInvalidRequestException("Invalid " + url + ": empty service name");
        }
        url = nameAndPath.length == 3 ? nameAndPath[2] : null;
        if (spiChecker.isSpi(serviceName)) {
            final String[] keyAndPath = url != null ? url.split("/", 2) : EMPTY_STRING_ARRAY;
            if (keyAndPath.length < 1) {
                throw new ClientInvalidRequestException("Invalid " + url + ": missing service key");
            }
            serviceKey = keyAndPath[0].trim();
            if (serviceKey.isEmpty()) {
                throw new ClientInvalidRequestException("Invalid " + url + ": empty service key");
            }
            url = keyAndPath.length == 2 ? keyAndPath[1] : null;
        } else {
            serviceKey = null;
        }
        
        if (request.getMethod() == HttpMethod.GET) {
            method = "get";
            path = url == null ? ServicePathHolder.empty() : ServicePathHolder.of(Arrays.asList(url.split("/", -1)));
            try {
                payload = request.getUri().getRawQuery() == null
                    ? null
                    : URLDecoder.decode(request.getUri().getRawQuery(), request.getCharacterEncoding().toString());
            } catch (final UnsupportedEncodingException e) {
                throw new IllegalStateException(e);
            }
        } else {
            final ServicePathHolder pathAndMethod = url == null
                ? ServicePathHolder.empty() : ServicePathHolder.parse(url);
            if (pathAndMethod.isEmpty()) {
                throw new ClientInvalidRequestException("Invalid " + url + ": missing");
            }
            final Tuple2<String, ServicePathHolder> methodAndPath = pathAndMethod.pop();
            method = methodAndPath.get1().trim();
            if (method.isEmpty() || method.equals("_")) {
                throw new ClientInvalidRequestException("Invalid " + url + ": empty or _ method");
            }
            path = methodAndPath.get2();
            payload = request.getBody(String.class).orElse(null);
        }
    }
    
    public String getServiceName() {
        return serviceName;
    }
    
    public String getServiceKey() {
        return serviceKey;
    }
    
    public ServicePathHolder getPath() {
        return path;
    }
    
    public String getMethod() {
        return method;
    }
    
    public String getPayload() {
        return payload;
    }
    
}
