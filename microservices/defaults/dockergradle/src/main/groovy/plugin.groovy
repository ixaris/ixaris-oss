import org.gradle.api.Plugin
import org.gradle.api.Project

/*


commonsDocker {
    dockerRegistryUrl = 'local'
    dockerNamespace = 'samples'
    appName = 'app'
}

*/

class IxarisCommonsMicroservicesDefaultsDockerPluginExtension {

    String namespace
    String serviceName
    String profile

}

class IxarisCommonsMicroservicesDefaultsDockerPlugin implements Plugin<Project>  {

    void apply(final Project project) {
        final IxarisCommonsMicroservicesDefaultsDockerPluginExtension extension = project.extensions.create('commonsDocker', IxarisCommonsMicroservicesDefaultsDockerPluginExtension)
        project.afterEvaluate {
            project.plugins.apply('com.google.cloud.tools.jib') // including this ixaris plugin implies we're pulling in jib
            
            def jibExtension = project.extensions.getByName("jib")
            // we could also do jibExtension.from.image = ... and so on
            jibExtension.with {
                to {
                    image = (extension.namespace ?: 'local/services') + '/' + project.name
                    tags = ['latest']
                    allowInsecureRegistries = true
                }
                container {
                    appRoot = '/opt/springboot'
                    workingDirectory = '/opt/springboot'
                    jvmFlags = ['-noverify', 
                                '-XX:NativeMemoryTracking=summary',
                                '-Dspring.application.name=' + extension.serviceName,
                                '-Dspring.profiles.active=' + extension.profile ?: 'dev',
                                '-Dnewrelic.config.app_name=' + extension.serviceName,
                                '-Dorg.springframework.boot.logging.LoggingSystem=com.ixaris.commons.microservices.defaults.app.Log4J2System',
                                '-Dspring.cloud.config.failFast=true',
                                '-Dspring.cloud.config.retry.initialInterval=10000',
                                '-Dspring.cloud.config.retry.maxInterval=300000',
                                '-Dspring.cloud.config.retry.maxAttempts=30',
                                '-Dhealth.config.enabled=false',
                                '-Dspring.main.allow-bean-definition-overriding=true']
                    mainClass = 'com.ixaris.commons.microservices.defaults.app.Application'
                }
            }
        }
    }
    
}

// if plugin is applied as script, run immediately
new IxarisCommonsMicroservicesDefaultsDockerPlugin().apply(project)
