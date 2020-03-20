package com.ixaris.commons.microservices.scslparser;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.AbstractConstruct;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.Tag;

import com.ixaris.commons.microservices.scslparser.model.ScslDefinition;
import com.ixaris.commons.microservices.scslparser.model.exception.ScslParseException;
import com.ixaris.commons.microservices.scslparser.model.exception.ScslRequiredFieldNotFoundException;

/**
 * @author <a href="mailto:ian.grima@ixaris.com">ian.grima</a>
 * @author <a href="mailto:brian.vella@ixaris.com">brian.vella</a>
 */
public class ScslParser {
    
    private static final Logger LOG = LoggerFactory.getLogger(ScslParser.class);
    public static final String SCSLVERSION_1_0 = "#%SCSL 1.0";
    public static final String SCSLVERSION_FRAGMENT = " Fragment";
    
    @FunctionalInterface
    public interface InputStreamResolver {
        
        /**
         * Given a location (may be referenced inside SCSL files) resolves an input stream for that resource or null of not found
         *
         * @param location
         * @return
         */
        InputStream resolveLocationToInputStream(String location);
    }
    
    /**
     * Parse a SCSL file by specifying a location and an {@link InputStreamResolver} to handle !include sections inside the SCSL file.
     *
     * @param location Location of the SCSL file to parse
     * @param inputStreamResolver An input stream resolver that given a location (may be referenced inside SCSL files) resolves an input stream
     *     for that resource.
     * @return Parsed {@link ScslDefinition}
     */
    public static ScslDefinition parse(final String location, final InputStreamResolver inputStreamResolver) {
        try {
            
            final AtomicReference<Yaml> ref = new AtomicReference<>();
            final Yaml yaml = new Yaml(new Constructor() {
                
                {
                    this.yamlConstructors.put(new Tag("!include"), new ConstructImport());
                }
                
                class ConstructImport extends AbstractConstruct {
                    
                    public Object construct(final Node node) {
                        final String val = (String) constructScalar((ScalarNode) node);
                        final InputStream is = resolveInputStream(val, inputStreamResolver);
                        validateScslHeader(is, true);
                        
                        final Yaml yaml = ref.get();
                        
                        @SuppressWarnings("unchecked")
                        final Map<String, Object> yamlTree = (Map<String, Object>) yaml.load(is);
                        LOG.debug(yaml.dump(yamlTree));
                        
                        return yamlTree;
                    }
                }
            });
            ref.set(yaml);
            
            final InputStream is = resolveInputStream(location, inputStreamResolver);
            validateScslHeader(is, false);
            
            @SuppressWarnings("unchecked")
            final Map<String, Object> yamlTree = (Map<String, Object>) yaml.load(is);
            LOG.debug(yaml.dump(yamlTree));
            
            final ScslDefinition definition = new ScslDefinition();
            return definition.parse(yamlTree);
            
        } catch (final ScslParseException | ScslRequiredFieldNotFoundException e) {
            // Simply re-throw any exceptions handled by us already
            throw e;
        } catch (final RuntimeException e) {
            // Wrap any other exceptions
            throw new ScslParseException(e);
        }
    }
    
    private static InputStream resolveInputStream(final String location, final InputStreamResolver inputStreamResolver) {
        final InputStream is = inputStreamResolver.resolveLocationToInputStream(location);
        if (is == null) {
            throw new IllegalStateException("Cannot resolve location [" + location + "]");
        }
        return is;
    }
    
    private static void validateScslHeader(final InputStream is, final boolean fragment) {
        
        try {
            byte[] c = new byte[10];
            is.read(c, 0, 10);
            if (!Objects.equals(new String(c), SCSLVERSION_1_0)) {
                throw new ScslParseException("File should start with #%SCSL 1.0");
            }
            
            if (fragment) {
                is.read(c, 0, 9);
                if (!Objects.equals(new String(c, 0, 9), SCSLVERSION_FRAGMENT)) {
                    throw new ScslParseException("File should start with #%SCSL 1.0 Fragment");
                }
            }
        } catch (final IOException e) {
            throw new ScslParseException(e);
        }
    }
    
    private ScslParser() {}
    
}
