package com.ixaris.commons.microservices.lib.service.support;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.ixaris.commons.microservices.lib.service.ServiceProviderSkeleton;
import com.ixaris.commons.microservices.lib.service.ServiceSkeleton;
import com.ixaris.commons.microservices.lib.service.annotations.ServiceKey;

public final class DefaultServiceKeys implements ServiceKeys {
    
    @Override
    public Set<String> get(final Class<? extends ServiceSkeleton> serviceSkeletonType,
                           final ServiceSkeleton serviceSkeleton) {
        final Class<? extends ServiceSkeleton> implType = serviceSkeleton.getClass();
        if (!serviceSkeletonType.isAssignableFrom(implType)) {
            throw new IllegalArgumentException();
        }
        if (ServiceProviderSkeleton.class.isAssignableFrom(serviceSkeletonType)) {
            final Set<String> keys = new HashSet<>(getKeysFromProperties(implType.getPackage().getName()));
            keys.addAll(getKeysFromAnnotation(implType));
            if (keys.isEmpty()) {
                throw new IllegalStateException(String.format("No key found for [%s]", implType.getName()));
            }
            return keys;
        } else {
            throw new IllegalStateException("[" + serviceSkeletonType + "] is not a provider type");
        }
    }
    
    private static List<String> getKeysFromAnnotation(Class<? extends ServiceSkeleton> implType) {
        return Arrays.asList(Optional.ofNullable(implType.getAnnotation(ServiceKey.class))
            .map(ServiceKey::value)
            .orElse(NO_ANNOTATION));
    }
    
    private static final Map<String, Set<String>> KEYS = new HashMap<>();
    private static final String[] NO_ANNOTATION = new String[0];
    
    static {
        try {
            final Enumeration<URL> resources = Thread
                .currentThread()
                .getContextClassLoader()
                .getResources("service_keys.properties");
            while (resources.hasMoreElements()) {
                final URL url = resources.nextElement();
                try (final BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream(), UTF_8))) {
                    String line = br.readLine();
                    while (line != null) {
                        line = line.trim();
                        if (!line.isEmpty()) {
                            final String[] parts = line.split("=", 2);
                            KEYS.compute(parts[0],
                                (k, v) -> {
                                    final List<String> keys = Arrays.asList(parts[1].split(",", -1));
                                    if (v == null) {
                                        return new HashSet<>(keys);
                                    } else {
                                        v.addAll(keys);
                                        return v;
                                    }
                                });
                        }
                        line = br.readLine();
                    }
                }
            }
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }
    
    private static Set<String> getKeysFromProperties(final String packageName) {
        Set<String> keys = KEYS.get(packageName + ".*");
        if (keys != null) {
            return keys;
        }
        
        // legacy logic
        keys = KEYS.get("SERVICE_KEY" + packageName);
        if (keys != null) {
            return keys;
        }
        
        final String[] split = packageName.split("\\.", -1);
        String pn = packageName;
        for (int i = split.length - 1; i >= 0; i--) {
            pn = (i > 0) ? pn.substring(0, pn.length() - split[i].length() - 1) : "";
            keys = KEYS.get(pn + ".*");
            if (keys != null) {
                return keys;
            }
            
            // legacy logic
            keys = KEYS.get("SERVICE_KEY" + pn);
            if (keys != null) {
                return keys;
            }
        }
        
        return Collections.emptySet();
    }
    
}
