import java.util.regex.Matcher
import java.util.regex.Pattern
import javax.inject.Inject
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.PathSensitivity

/*

configure like this:

apply plugin: 'com.ixaris.commons.jooq'

ixarisJooq {
    // if true: includes = lib_.*, excludes = , testIncludes = .*, testExcludes = lib_.*|schema_version|flyway_.*
    // if false: includes = .*, excludes = lib_.*|schema_version|flyway_.*
    library = false
}

*/

class IxarisCommonsJooqPluginExtension {

    boolean library

}

class IxarisCommonsJooqPlugin implements Plugin<Project> {

    private final String jooqVersion
    private final String mysqlConnectorVersion

    @Inject
    IxarisCommonsJooqPlugin() {
        final Properties props = new Properties()
        props.load(getClass().getResourceAsStream("jooq-gradle.properties"))
        this.jooqVersion = props.jooqVersion
        this.mysqlConnectorVersion = props.mysqlConnectorVersion
    }

    IxarisCommonsJooqPlugin(final String jooqVersion, final String mysqlConnectorVersion) {
        this.jooqVersion = jooqVersion
        this.mysqlConnectorVersion = mysqlConnectorVersion
    }

    void apply(final Project project) {
        final IxarisCommonsJooqPluginExtension extension = project.extensions.create('ixarisJooq', IxarisCommonsJooqPluginExtension)
        project.afterEvaluate {

            final String dbDriver = 'com.mysql.cj.jdbc.Driver'
            final String dbUrl = 'jdbc:mysql://' + System.getProperty('MYSQL_URL', 'localhost:13306')
            final String dbParams = '?autoReconnect=true&verifyServerCertificate=false&useUniCode=true&characterEncoding=UTF-8&useSSL=false'
            final String dbUser = 'root'
            final String dbPassword = 'root'
            final String dbSchema = 'jqg-' + project.name.substring(0, Math.min(project.name.length(), 60))
            final boolean isLibrary = extension.library

            project.plugins.apply('nu.studer.jooq')

            // undo the effect of JooqPlugin.forceJooqVersionAndEdition which runs when we first apply the plugin
            // which uses the default version compiled in the plugin with no way to owerwrite
            project.configurations.all {
                final def rules = getPrivateField(resolutionStrategy.getClass().getSuperclass(), "dependencySubstitutions", resolutionStrategy).substitutionRules
                rules.remove(getPrivateField(LinkedHashMap.class, "tail", getPrivateField(HashSet.class, "map", rules)).key)
            }
            project.dependencies.add('jooqRuntime', "mysql:mysql-connector-java:$mysqlConnectorVersion")
            project.configurations.getByName('jooqRuntime').resolutionStrategy.eachDependency { details ->
                def requested = details.requested
                if (requested.group == 'org.jooq' && requested.name.startsWith('jooq')) {
                    details.useTarget("$requested.group:$requested.name:$jooqVersion")
                }
            }
            final def jooqExtension = project.extensions.getByName('jooq')// as JooqExtension

            project.plugins.apply('org.flywaydb.flyway')
            project.configurations.create('flywayMigration')
            project.dependencies.add('flywayMigration', "mysql:mysql-connector-java:$mysqlConnectorVersion")
            final def flywayExtension = project.extensions.getByName('flyway')// as FlywayExtension

            flywayExtension.with {
                driver = dbDriver
                url = dbUrl + dbParams
                user = dbUser
                password = dbPassword

                configurations = ['flywayMigration']
            }

            jooqExtension.with {
                version = jooqVersion
                edition = 'OSS'
                with {
                    main(project.sourceSets.main) {
                        jdbc {
                            driver = dbDriver
                            url = dbUrl + dbParams
                            user = dbUser
                            password = dbPassword
                        }
                        generator {
                            name = 'org.jooq.codegen.JavaGenerator'
                            strategy {
                                name = 'org.jooq.codegen.DefaultGeneratorStrategy'
                            }
                            database {
                                name = 'org.jooq.meta.mysql.MySQLDatabase'
                                inputSchema = dbSchema
                                outputSchemaToDefault = true
                                includes = isLibrary ? 'lib_[^.]*|.*?\\.lib_.*\\..*' : '.*'
                                excludes = isLibrary ? 'schema_version|flyway_.*|__.*' : 'lib_.*|schema_version|flyway_.*|__.*'
                                includeExcludeColumns = true
                                overridePrimaryKeys = '.*pk_.*'
                                recordTimestampFields = 'last_updated'
                                recordVersionFields = 'update_version'
                                forcedTypes {
                                    forcedType {
                                        userType = 'java.lang.Character'
                                        binding = 'com.ixaris.commons.jooq.persistence.CharacterConverter'
                                        includeTypes = /(?i)CHAR\(1\)/
                                    }
                                    forcedType {
                                        name = 'BOOLEAN'
                                        includeTypes = /(?i)TINYINT\s*UNSIGNED(\(\d*\))?/
                                    }
                                    forcedType {
                                        name = 'INTEGER'
                                        includeTypes = /(?i)SMALLINT(\s*UNSIGNED)?(\(\d*\))?/
                                    }
                                    forcedType {
                                        userType = 'java.lang.String'
                                        binding = 'com.ixaris.commons.jooq.persistence.JsonConverter'
                                        includeTypes = /(?i)JSON/
                                    }                                }
                            }
                            target {
                                // append group to name and replace tokens to get package name
                                // e.g. com.ixaris.oss:ix-commons-logging.lib becomes
                                // com.ixaris.commons.logging.lib.jooq
                                packageName = (project.group + '.' + project.name.replaceAll('-', '.')).replaceAll(/\.oss\.ix\./, '.') + '.jooq'

                                directory = 'build/generated/sources/jooq/java/main'
                            }
                        }
                    }

                    test(project.sourceSets.test) {
                        def mainConfig = jooqExtension.configs.get('main').configuration
                        jdbc = mainConfig.jdbc
                        generator {
                            name = mainConfig.generator.name
                            strategy = mainConfig.generator.strategy
                            database {
                                name = mainConfig.generator.database.name
                                inputSchema = dbSchema + '_test'
                                outputSchemaToDefault = true
                                includes = '.*'
                                excludes = 'lib_.*|schema_version|flyway_.*|__.*'
                                includeExcludeColumns = true
                                overridePrimaryKeys = mainConfig.generator.database.overridePrimaryKeys
                                recordTimestampFields = mainConfig.generator.database.recordTimestampFields
                                recordVersionFields = mainConfig.generator.database.recordVersionFields
                                forcedTypes = mainConfig.generator.database.forcedTypes
                            }
                            target {
                                packageName = mainConfig.generator.target.packageName.replaceAll(/jooq$/, 'test.jooq')
                                directory = 'build/generated/sources/jooq/java/test'
                            }
                        }
                    }
                }
            }

            project.tasks.named('generateMainJooqSchemaSource').configure {
                outputs.cacheIf { true }
                inputs
                    .files(project.fileTree(isLibrary ? 'src/main/resources/lib/migration' : 'src/main/resources/db/migration'))
                    .withPropertyName('migrations')
                    .withPathSensitivity(PathSensitivity.RELATIVE)
                    .skipWhenEmpty()

                doFirst {
                    ant.sql(
                        driver: dbDriver,
                        url: dbUrl + dbParams,
                        userid: dbUser,
                        password: dbPassword,
                        classpath: project.configurations.flywayMigration.asPath,
                        "CREATE DATABASE IF NOT EXISTS `${dbSchema}`"
                    )

                    cleanAndMigrate(
                        locations: ['filesystem:' + project.projectDir + (isLibrary ? '/src/main/resources/lib/migration' : '/src/main/resources/db/migration')],
                        schemas: [dbSchema],
                        project
                    )
                }

                doLast {
                    ant.sql(
                        driver: dbDriver,
                        url: dbUrl + dbParams,
                        userid: dbUser,
                        password: dbPassword,
                        classpath: project.configurations.flywayMigration.asPath,
                        "DROP DATABASE IF EXISTS `${dbSchema}`"
                    )
                }
            }

            project.tasks.named('generateTestJooqSchemaSource').configure {
                outputs.cacheIf { true }
                inputs
                    .files(project.fileTree('src/test/resources/db/migration'))
                    .withPropertyName('migrations')
                    .withPathSensitivity(PathSensitivity.RELATIVE)
                    .skipWhenEmpty()

                doFirst {
                    ant.sql(
                        driver: dbDriver,
                        url: dbUrl + dbParams,
                        userid: dbUser,
                        password: dbPassword,
                        classpath: project.configurations.flywayMigration.asPath,
                        "CREATE DATABASE IF NOT EXISTS `${dbSchema + '_test'}`"
                    )

                    cleanAndMigrate(
                        locations: ['filesystem:' + project.projectDir + '/src/test/resources/db/migration'],
                        schemas: [dbSchema + '_test'],
                        project
                    )
                }

                doLast{
                    ant.sql(
                        driver: dbDriver,
                        url: dbUrl + dbParams,
                        userid: dbUser,
                        password: dbPassword,
                        classpath: project.configurations.flywayMigration.asPath,
                        "DROP DATABASE IF EXISTS `${dbSchema + '_test'}`"
                    )
                }
            }
        }
    }

    private static getPrivateField(final Class<?> type, final String fieldName, final Object target) {
        final def field = type.getDeclaredField(fieldName)
        field.setAccessible(true)
        return field.get(target)
    }

    private void cleanAndMigrate(final Map params, final Project project) {
        final def cleanTask = project.tasks.flywayClean
        cleanTask.with {
            locations = params.locations
            schemas = params.schemas
        }
        final def migrateTask = project.tasks.flywayMigrate
        migrateTask.with {
            locations = params.locations
            schemas = params.schemas
        }

        // since we are running a task from another task, we need to replace the thread context class loader 
        // with that task's class loader for correct class loading
        final ClassLoader cl = Thread.currentThread().getContextClassLoader()
        try {
            Thread.currentThread().setContextClassLoader(cleanTask.getClass().getClassLoader())
            cleanTask.runTask()
            Thread.currentThread().setContextClassLoader(migrateTask.getClass().getClassLoader())
            migrateTask.runTask()
        } finally {
            Thread.currentThread().setContextClassLoader(cl)
        }
    }

}

// if plugin is applied as script, run immediately
new IxarisCommonsJooqPlugin(project.ext.jooqVersion, project.ext.mysqlConnectorVersion).apply(project)
