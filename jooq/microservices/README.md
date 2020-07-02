# Jooq Microservices Bindings

This library provides the jooq implementation of at Least Once Event Publishing.

A microservice which needs guaranteed event delivery typically auto-wires a 
`JooqEventPublisherFactory` and wraps injected publisher:

```java
@Autowired
public UpstreamJooqRepositoryImpl(final JooqAsyncPersistenceProvider jooqAsyncPersistenceProvider,
                                  final JooqEventPublisherFactory jooqEventPublisherFactory,
                                  final UpstreamResourceHandler.Watch upstreamPublisher) {
    this.jooqAsyncPersistenceProvider = jooqAsyncPersistenceProvider;
    this.upstreamEventPublisher = jooqEventPublisherFactory.create(upstreamPublisher);
}
```  
  
The microservice then publishes events in the following way:

```java
persistenceProvider.transaction(INTENT.get(), () -> {
    // side effects
    upstreamEventPublisher.publish(ServiceEventHeader.from(header), UpstreamItemEvent.newBuilder().setUpstreamItem(upstreamItem).build());
    return result;
});
```
  
To use this feature, microservice must provide the required schema by including the provided migration 
steps to their own migration scripts.
