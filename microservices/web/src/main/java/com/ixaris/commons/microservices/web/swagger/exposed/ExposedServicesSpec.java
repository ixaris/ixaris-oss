package com.ixaris.commons.microservices.web.swagger.exposed;

import static com.ixaris.commons.misc.lib.object.Tuple.tuple;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;

import com.ixaris.commons.microservices.scslparser.ScslParser;
import com.ixaris.commons.microservices.scslparser.model.ScslDefinition;
import com.ixaris.commons.misc.lib.object.Tuple2;

/**
 * A parsed version of the Exposed Services file to simplify the interpretation of what services/methods are exposed
 *
 * @author <a href="mailto:aldrin.seychell@ixaris.com">aldrin.seychell</a>
 */
public class ExposedServicesSpec {
    
    private static final String EXPOSE_TAGS_PREFIX = "expose_tags=";
    private static final String CREATE_TAGS_PREFIX = "create_tags=";
    private static final String EXPOSE_SPIS_PREFIX = "expose_spis=";
    private static final String HEADERS = "headers=";
    private static final String SECURED_HEADERS = "secured_headers=";
    
    private static final ScslMethodFilter ALLOW_ALL_METHOD_FILTER = (tags, security) -> true;
    private static final ScslCreateMethodFilter CREATE_METHOD_FILTER = (name, methodTags) -> name.equalsIgnoreCase("createProxy");
    
    /**
     * (SCSL definition, Friendly Name). This is a list not a map so that the order is maintained from the
     * exposed_services file
     */
    private final List<Tuple2<ScslDefinition, String>> exposedScslFiles = new ArrayList<>();
    
    private Set<String> exposedMethodTags = new HashSet<>();
    private String createMethodTag = null;
    private boolean exposeSpis = false;
    private Map<String, String> headers = Collections.emptyMap();
    private Map<String, String> securedHeaders = Collections.emptyMap();
    
    public static ExposedServicesSpec fromInputStream(final InputStream is) {
        
        final ExposedServicesSpec exposedServicesSpec = new ExposedServicesSpec();
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        
        try (final BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            br
                .lines()
                .filter(l -> !l.trim().isEmpty() && !l.startsWith("#"))
                .forEach(line -> parseExposedServicesLine(exposedServicesSpec, contextClassLoader, line));
        } catch (final IOException e) {
            throw new IllegalStateException("Unable to read exposed services file", e);
        }
        
        return exposedServicesSpec;
    }
    
    private static void parseExposedServicesLine(final ExposedServicesSpec exposedServicesSpec, final ClassLoader contextClassLoader, final String line) {
        if (line.startsWith(EXPOSE_TAGS_PREFIX)) {
            final String tagsToExpose = line.substring(EXPOSE_TAGS_PREFIX.length());
            exposedServicesSpec.setExposedMethodTags(Sets.newHashSet(tagsToExpose.split(",", -1)));
        } else if (line.startsWith(CREATE_TAGS_PREFIX)) {
            exposedServicesSpec.setCreateMethodTag(line.substring(CREATE_TAGS_PREFIX.length()));
        } else if (line.startsWith(EXPOSE_SPIS_PREFIX)) {
            exposedServicesSpec.setExposeSpis(Boolean.valueOf(line.substring(EXPOSE_SPIS_PREFIX.length())));
        } else if (line.startsWith(HEADERS)) {
            exposedServicesSpec.setHeaders(
                Arrays.stream(line.substring(HEADERS.length()).split(",", -1))
                    .map(s -> s.split(":", 2))
                    .collect(Collectors.toMap(a -> a[0], a -> a[1])));
        } else if (line.startsWith(SECURED_HEADERS)) {
            exposedServicesSpec.setSecuredHeaders(
                Arrays.stream(line.substring(SECURED_HEADERS.length()).split(",", -1))
                    .map(s -> s.split(":", 2))
                    .collect(Collectors.toMap(a -> a[0], a -> a[1])));
        } else {
            final String[] splitLine = line.split("=", 2);
            final String scslLocation;
            String friendlyName = null;
            
            // Support for friendly names based on scsl file name
            // e.g. paylets_managed_cards_api.scsl=mc would expose all methods of paylets_managed_cards_api.scsl as
            // "/mc"
            if (splitLine.length == 2) {
                scslLocation = splitLine[0];
                friendlyName = splitLine[1];
            } else {
                scslLocation = line;
            }
            
            final ScslDefinition scslDefinition = ScslParser.parse(scslLocation, contextClassLoader::getResourceAsStream);
            if (friendlyName == null) {
                friendlyName = scslDefinition.getName();
            }
            
            exposedServicesSpec.exposedScslFiles.add(tuple(scslDefinition, friendlyName));
        }
    }
    
    public List<Tuple2<ScslDefinition, String>> getExposedScslFiles() {
        return Collections.unmodifiableList(exposedScslFiles);
    }
    
    public ScslMethodFilter getExposedMethodFilter() {
        if (getExposedMethodTags().isEmpty()) {
            return ALLOW_ALL_METHOD_FILTER;
        } else {
            return (tags, security) -> tags.containsAll(getExposedMethodTags());
        }
    }
    
    public ScslCreateMethodFilter getCreateMethodFilter() {
        if (getCreateMethodTag() == null) {
            return CREATE_METHOD_FILTER;
        } else {
            return (name, methodTags) -> methodTags.contains(getCreateMethodTag());
        }
    }
    
    public Set<String> getExposedMethodTags() {
        return Collections.unmodifiableSet(exposedMethodTags);
    }
    
    private void setExposedMethodTags(final Set<String> exposedMethodTags) {
        this.exposedMethodTags = exposedMethodTags;
    }
    
    public String getCreateMethodTag() {
        return createMethodTag;
    }
    
    private void setCreateMethodTag(final String createMethodTag) {
        this.createMethodTag = createMethodTag;
    }
    
    public boolean isExposeSpis() {
        return exposeSpis;
    }
    
    public void setExposeSpis(final boolean exposeSpis) {
        this.exposeSpis = exposeSpis;
    }
    
    public Map<String, String> getHeaders() {
        return headers;
    }
    
    public void setHeaders(final Map<String, String> headers) {
        this.headers = headers;
    }
    
    public Map<String, String> getSecuredHeaders() {
        return securedHeaders;
    }
    
    public void setSecuredHeaders(final Map<String, String> securedHeaders) {
        this.securedHeaders = securedHeaders;
    }
    
}
