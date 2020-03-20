package com.ixaris.commons.swaggergenerator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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

import com.ixaris.commons.microservices.web.swagger.exposed.ExposedServicesSpec;
import com.ixaris.commons.swaggergenerator.swagger.SwaggerGenerator;
import com.ixaris.commons.swaggergenerator.swagger.SwaggerGenerator.ApiAndWebHook;

import io.swagger.models.Contact;
import io.swagger.models.Info;
import io.swagger.util.Json;
import io.swagger.util.Yaml;

/**
 * Maven plugin that generates the swagger.json and swagger.yaml based on the exposed SCSL files
 *
 * @author <a href="mailto:aldrin.seychell@ixaris.com">aldrin.seychell</a>
 */
@Mojo(name = "swaggen", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.COMPILE, threadSafe = true)
public class SwaggerGeneratorMojo extends AbstractMojo {
    
    private static final String EXPOSED_SERVICES_FILE_NAME = "exposed_services.txt";
    
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;
    
    @Parameter(defaultValue = "${basedir}/src/main/resources/" + EXPOSED_SERVICES_FILE_NAME)
    private File exposedServicesFile;
    
    @Component
    private MavenProjectHelper mavenProjectHelper;
    
    public void execute() throws MojoExecutionException {
        try {
            // Create a class loader of all dependencies in the project
            final Set<String> classpathElementsSet = new HashSet<>(project.getRuntimeClasspathElements());
            classpathElementsSet.addAll(project.getCompileClasspathElements());
            classpathElementsSet.addAll(project.getTestClasspathElements());
            
            final List<String> classpathElements = new ArrayList<>(classpathElementsSet);
            Collections.sort(classpathElements);
            
            final List<URL> classpath = new ArrayList<>();
            for (final String classpathElement : classpathElements) {
                classpath.add(new URL("file:" + (classpathElement.endsWith(".jar") ? classpathElement : classpathElement + File.separator)));
            }
            
            @SuppressWarnings("ClassLoaderInstantiation")
            final ClassLoader classLoader = new URLClassLoader(classpath.toArray(new URL[classpath.size()]),
                Thread.currentThread().getContextClassLoader());
            
            // Set the new class loader as the context class loader of the current thread
            Thread.currentThread().setContextClassLoader(classLoader);
            
            if (exposedServicesFile == null) {
                throw new MojoExecutionException("Exposed Service file is null. Make sure you have a file called " + EXPOSED_SERVICES_FILE_NAME);
            }
            
            final ExposedServicesSpec exposedServicesSpec;
            
            if (exposedServicesFile.exists()) {
                exposedServicesSpec = ExposedServicesSpec.fromInputStream(new FileInputStream(exposedServicesFile));
            } else {
                throw new MojoExecutionException("Unable to find file: " + exposedServicesFile.getAbsolutePath());
            }
            
            final Properties properties = project.getProperties();
            
            final Contact contact = new Contact()
                .name(properties.getProperty("swagger.contact.name", "Ixaris Systems Ltd."))
                .url(properties.getProperty("swagger.contact.url", "http://www.ixaris.com"));
            
            if (properties.containsKey("swagger.contact.email")) {
                contact.email(properties.getProperty("swagger.contact.email", "ope@ixaris.com"));
            }
            
            final Info apiInfo = new Info()
                .title(properties.getProperty("swagger.title", "Open Payments Cloud APIs"))
                .description(properties.getProperty("swagger.description", "Open Payments Cloud APIs"))
                .version(properties.getProperty("swagger.version", "V1.0"))
                .contact(contact);
            // .license(new License().name("Apache 2.0").url("http://www.apache.org")); // TODO: what should this be is
            // this correct?
            
            final String apiHost = properties.getProperty("swagger.host", "localhost:8324");
            final String apiBasePath = properties.getProperty("swagger.basePath", "/api");
            
            final Info webHookInfo = new Info()
                .title(properties.getProperty("swagger.webhook.title", "Open Payments Cloud Web Hooks"))
                .description(properties.getProperty("swagger.webhook.description", "Open Payments Cloud Web Hooks"))
                .version(properties.getProperty("swagger.webhook.version", "V1.0"))
                .contact(contact);
            // .license(new License().name("Apache 2.0").url("http://www.apache.org")); // TODO: what should this be is
            // this correct?
            
            final String webHookHost = properties.getProperty("swagger.webhook.host", "localhost:8325");
            final String webHookBasePath = properties.getProperty("swagger.webhook.basePath", "/webhook");
            
            // Generate swagger definition from the defined scsl and reading files from the newly created class loader
            final ApiAndWebHook apiAndWebHook = SwaggerGenerator.generateApiAndWebHook(exposedServicesSpec,
                apiInfo,
                apiHost,
                apiBasePath,
                webHookInfo,
                webHookHost,
                webHookBasePath);
            
            // Output the swagger definition as both YAML and JSON
            final File apiYamlFile = new File(project.getBuild().getDirectory() + File.separator + "swagger.yaml");
            Yaml.pretty().writeValue(apiYamlFile, apiAndWebHook.api);
            mavenProjectHelper.attachArtifact(project, "yaml", "swagger", apiYamlFile);
            
            // Output the swagger definition as both YAML and JSON
            final File webHookYamlFile = new File(project.getBuild().getDirectory() + File.separator + "swagger_webhook.yaml");
            Yaml.pretty().writeValue(webHookYamlFile, apiAndWebHook.webHook);
            mavenProjectHelper.attachArtifact(project, "yaml", "swagger_webhook", webHookYamlFile);
            
            final File apiJsonFile = new File(project.getBuild().getDirectory() + File.separator + "swagger.json");
            Json.pretty().writeValue(apiJsonFile, apiAndWebHook.api);
            mavenProjectHelper.attachArtifact(project, "json", "swagger", apiJsonFile);
            
            final File webHookJsonFile = new File(project.getBuild().getDirectory() + File.separator + "swagger_webhook.json");
            Json.pretty().writeValue(webHookJsonFile, apiAndWebHook.webHook);
            mavenProjectHelper.attachArtifact(project, "json", "swagger_webhook", webHookJsonFile);
            
            final String generatedSourcesPath = project.getBuild().getDirectory() + File.separator + "generated-sources" + File.separator;
            new File(generatedSourcesPath).mkdirs();
            
            final Resource resource = new Resource();
            resource.setDirectory(generatedSourcesPath);
            project.getBuild().getResources().add(resource);
            
            final File apiYamlFileInJar = new File(generatedSourcesPath + "swagger.yaml");
            Yaml.pretty().writeValue(apiYamlFileInJar, apiAndWebHook.api);
            
            final File webHookYamlFileInJar = new File(generatedSourcesPath + "swagger_webhook.yaml");
            Yaml.pretty().writeValue(webHookYamlFileInJar, apiAndWebHook.webHook);
            
            final File apiJsonFileInJar = new File(generatedSourcesPath + "swagger.json");
            Json.pretty().writeValue(apiJsonFileInJar, apiAndWebHook.api);
            
            final File webHookJsonFileInJar = new File(generatedSourcesPath + "swagger_webhook.json");
            Json.pretty().writeValue(webHookJsonFileInJar, apiAndWebHook.webHook);
            
        } catch (final IOException | DependencyResolutionRequiredException | RuntimeException e) {
            throw new MojoExecutionException("Error: " + e.getMessage(), e);
        }
    }
}
