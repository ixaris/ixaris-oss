## Running multiple services during a test
Our microservice infrastructure allows you to run multiple services at once
during a test. In order to do this, you will need to perform the following
steps for every extra service that you need to include in your test.

- Enable database migrations for the schema of that service. This can be done
by registering a bean of type `com.ixaris.commons.persistence.lib.datasource.
DataSourceUnit` specifying the database unit name of your additional service.
For example:
```java
// Make sure that Spring picks up this class in its context!
@Configuration
public class AdditionalDataSourceUnit {

    @Bean
    public static DataSourceUnit additionalDataSourceUnit() {
        // This is the key of your additional service
        return new DataSourceUnit("additional");
    }
}
```

- Make sure your additional service uses the correct data source unit when
executing its code. This can be done by overriding the `aroundAsync` method
in all of your service's skeleton implementations. The method needs to be
overridden as follows:
```java
@Override
public <T, E extends Exception> T aroundAsync(final CallableThrows<T, E> callable) throws E {
    // "additional" is the key of your additional service
    return AbstractMultiTenantDataSource.DATASOURCE_UNIT.exec("additional", callable);
}
```

Finally, make sure to include a scope test dependency from your main module
onto any additional service that you require during your test.

### Why is this necessary?
Each service, by default, will only execute the migrations of the database unit
whose name coincides with the value of the `spring.application.name` property.
In our tests, we set this manually. However, if you need to run multiple migration
units, this will not suffice. As such, you will also need to register an additional
bean in your Spring context (as detailed above) so that when the service starts,
it will find this bean in its context, and automatically also apply the migrations
of that databae unit.

Each service, by default, will also execute all of its code in the context of the
database unit whose name coincides with the value of the `spring.application.name`
property. This means that if there are multiple services loaded in the same test,
they will all use the same database unit (as specified by the property). The
`aroundAsync` hook allows us to override the `AsyncLocal` specifying the value of
the database unit with our own.