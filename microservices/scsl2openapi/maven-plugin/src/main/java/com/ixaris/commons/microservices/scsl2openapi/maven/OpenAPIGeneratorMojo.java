package com.ixaris.commons.microservices.scsl2openapi.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

import com.fasterxml.jackson.databind.ObjectWriter;

import com.ixaris.commons.microservices.scsl2openapi.lib.OpenAPIGenerator;
import com.ixaris.commons.microservices.scsl2openapi.lib.OpenAPIGenerator.ApiAndWebHook;
import com.ixaris.commons.microservices.web.swagger.exposed.ExposedServicesSpec;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;

/**
 * Maven plugin that generates json and yaml for the api and webhooks based on the exposed SCSL files
 */
@Mojo(name = "generate",
      defaultPhase = LifecyclePhase.GENERATE_RESOURCES,
      requiresDependencyResolution = ResolutionScope.COMPILE,
      threadSafe = true)
public final class OpenAPIGeneratorMojo extends AbstractMojo {
    
    private static final String EXPOSED_SERVICES_FILE_NAME = "exposed_services.txt";
    
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;
    
    @Parameter
    private File exposedServicesFile;
    
    @Parameter(required = true,
               readonly = true,
               defaultValue = "${project.build.directory}/generated-sources/openapi/resources")
    private File outputDirectory;
    
    @Parameter
    private String prefix;
    
    @Component
    private MavenProjectHelper mavenProjectHelper;
    
    @SuppressWarnings({ "findsecbugs:PATH_TRAVERSAL_IN", "findbugs:RV_RETURN_VALUE_IGNORED_BAD_PRACTICE" })
    public void execute() throws MojoExecutionException {
        try {
            final String prefixToUse = Optional.ofNullable(prefix).map(p -> p + '_').orElse("");
            exposedServicesFile = exposedServicesFile != null
                ? exposedServicesFile
                : new File(project.getBasedir(), "src/main/resources/" + prefixToUse + EXPOSED_SERVICES_FILE_NAME);
            if (!exposedServicesFile.exists()) {
                throw new IllegalStateException(
                    "Scsl2OpenApiPlugin : Could not find " + exposedServicesFile + " in src/main/resources");
            }
            
            final Properties properties = project.getProperties();
            
            final Contact contact = new Contact()
                .name(properties.getProperty("openapi.contact.name", "Ixaris Systems Ltd."))
                .url(properties.getProperty("openapi.contact.url", "http://www.ixaris.com"));
            
            if (properties.containsKey("openapi.contact.email")) {
                contact.email(properties.getProperty("openapi.contact.email"));
            }
            
            final Info apiInfo = new Info()
                .title(properties.getProperty("openapi.api.title", "API"))
                .description(properties.getProperty("openapi.api.description", "API"))
                .version(properties.getProperty("openapi.version", "V1.0"))
                .contact(contact);
            // .license(new License().name("Apache 2.0").url("http://www.apache.org")); // TODO: what should this be is
            // this correct?
            
            final Info webHookInfo = new Info()
                .title(properties.getProperty("openapi.webhook.title", "Web Hook"))
                .description(properties.getProperty("openapi.webhook.description", "Web Hook"))
                .version(properties.getProperty("openapi.version", "V1.0"))
                .contact(contact);
            // .license(new License().name("Apache 2.0").url("http://www.apache.org")); // TODO: what should this be is
            // this correct?
            
            // Create a class loader of all dependencies in the project
            final Set<String> classpathElementsSet = new HashSet<>(project.getRuntimeClasspathElements());
            classpathElementsSet.addAll(project.getCompileClasspathElements());
            classpathElementsSet.addAll(project.getTestClasspathElements());
            
            final List<String> classpathElements = new ArrayList<>(classpathElementsSet);
            Collections.sort(classpathElements);
            
            final List<URL> classpath = new ArrayList<>();
            for (final String classpathElement : classpathElements) {
                classpath.add(new URL("file:" + (classpathElement.endsWith(".jar") ? classpathElement : (classpathElement + File.separator))));
            }
            
            final ClassLoader cl = Thread.currentThread().getContextClassLoader();
            final ApiAndWebHook apiAndWebHook;
            try (
                @SuppressWarnings("ClassLoaderInstantiation")
                final URLClassLoader classLoader = AccessController.doPrivileged((PrivilegedAction<URLClassLoader>) () -> new URLClassLoader(classpath.toArray(new URL[0]), cl))) {
                // Set the new class loader as the context class loader of the current thread
                Thread.currentThread().setContextClassLoader(classLoader);
                
                final ExposedServicesSpec exposedServicesSpec;
                try (final FileInputStream fis = new FileInputStream(exposedServicesFile)) {
                    exposedServicesSpec = ExposedServicesSpec.fromInputStream(fis);
                }
                
                // Generate swagger definitions, reading files from the newly created class loader
                apiAndWebHook = OpenAPIGenerator.generateApiAndWebHook(
                    exposedServicesSpec, apiInfo, webHookInfo);
            } finally {
                Thread.currentThread().setContextClassLoader(cl);
            }
            
            outputDirectory.mkdirs();
            
            final Resource resource = new Resource();
            resource.setDirectory(outputDirectory.getAbsolutePath());
            project.getBuild().getResources().add(resource);
            
            final ObjectWriter yamlObjectWriter = Yaml.pretty();
            final ObjectWriter jsonObjectWriter = Json.pretty();
            
            final File apiYamlFileInJar = new File(outputDirectory, prefixToUse + "api.yaml");
            yamlObjectWriter.writeValue(apiYamlFileInJar, apiAndWebHook.api);
            
            final File webHookYamlFileInJar = new File(outputDirectory, prefixToUse + "webhook.yaml");
            yamlObjectWriter.writeValue(webHookYamlFileInJar, apiAndWebHook.webHook);
            
            final File apiJsonFileInJar = new File(outputDirectory, prefixToUse + "api.json");
            jsonObjectWriter.writeValue(apiJsonFileInJar, apiAndWebHook.api);
            
            final File webHookJsonFileInJar = new File(outputDirectory, prefixToUse + "webhook.json");
            jsonObjectWriter.writeValue(webHookJsonFileInJar, apiAndWebHook.webHook);
            
        } catch (final IOException | DependencyResolutionRequiredException | RuntimeException e) {
            throw new MojoExecutionException("Error: " + e.getMessage(), e);
        }
    }
}
