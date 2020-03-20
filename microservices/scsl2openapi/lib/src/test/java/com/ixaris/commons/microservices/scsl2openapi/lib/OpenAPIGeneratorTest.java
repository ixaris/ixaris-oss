package com.ixaris.commons.microservices.scsl2openapi.lib;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.ixaris.commons.microservices.scsl2openapi.lib.OpenAPIGenerator.ApiAndWebHook;
import com.ixaris.commons.microservices.web.swagger.exposed.ExposedServicesSpec;

import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;

public class OpenAPIGeneratorTest {
    
    private static final String EXPOSED_SERVICES_FILE_NAME = "exposed_services.txt";
    
    @Test
    public void testGenerationWithSampleContract() throws IOException {
        
        final ExposedServicesSpec exposedServicesSpec;
        try (final FileInputStream fis = new FileInputStream(Paths.get("src", "test", "resources", EXPOSED_SERVICES_FILE_NAME).toFile())) {
            exposedServicesSpec = ExposedServicesSpec.fromInputStream(fis);
        }
        
        final Contact contact = new Contact()
            .name("Ixaris Systems Ltd.")
            .url("http://www.ixaris.com/samples")
            .email("samples@ixaris.com");
        
        final Info apiInfo = new Info()
            .title("Microservices Samples Edge API")
            .description("Microservices Samples Edge API")
            .version("V1.1")
            .contact(contact);
        
        final Info webHookInfo = new Info()
            .title("Microservices Samples Edge Web Hook")
            .description("Microservices Samples Edge Web Hook")
            .version("V1.0")
            .contact(contact);
        
        // Generate swagger definition from the defined scsl and reading files from the newly created class loader
        final ApiAndWebHook apiAndWebHook = OpenAPIGenerator.generateApiAndWebHook(exposedServicesSpec, apiInfo, webHookInfo);
        
        final OpenAPIParser parser = new OpenAPIParser();
        OpenAPI pregeneratedApi = parser
            .readContents(
                String.join("\n", Files.readAllLines(Paths.get("src", "test", "resources", "api.yaml"))),
                Collections.emptyList(),
                null)
            .getOpenAPI();
        OpenAPI pregeneratedWebhook = parser
            .readContents(
                String.join("\n", Files.readAllLines(Paths.get("src", "test", "resources", "webhook.yaml"))),
                Collections.emptyList(),
                null)
            .getOpenAPI();
        
        //        final File apiYamlFileInJar = new File("src/test/resources", "_api.yaml");
        //        Yaml.pretty().writeValue(apiYamlFileInJar, apiAndWebHook.api);
        //        
        //        final File webHookYamlFileInJar = new File("src/test/resources", "_webhook.yaml");
        //        Yaml.pretty().writeValue(webHookYamlFileInJar, apiAndWebHook.webHook);
        
        Assertions.assertThat(apiAndWebHook.api).isEqualTo(pregeneratedApi);
        Assertions.assertThat(apiAndWebHook.webHook).isEqualTo(pregeneratedWebhook);
    }
    
}
