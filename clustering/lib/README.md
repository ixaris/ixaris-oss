# Clustering Library

The clustering library provides bindings for a cluster to register nodes with endpoint
information for clients to discover and connect, as well as bindings for defining cluster 
routes for partitioning and events for broadcasting. 

This library is typically used by another layer of libraries and not typically used directly 
by client code.

## Cluster Discovery

Clients can discover clusters and information on their nodes through the `ClusterDiscovery`
interface, by registering listeners. There is no way of querying the cluster topology.
Instead, the first time a listener is registered, it is immediately called with the
current topology, and then subsequently called for every topology change. It is the 
responsibility of the client to store (cache) the current topology state. 

Cluster info on the node includes attributes used to connect to the node, as well as the 
set of managed partitions such that clients proactively try to send requests to the correct
node.
 
## Cluster Registry

For services, the cluster registry autoregisters the current node and provides a way to 
merge the node's attributes. It also provides events to inform the node of changes to 
leadership and assigned partitions.

### Routing

Services may define cluster routes, which are then used to route requests to the correct 
node for partitioning:

```java
@Singleton
public final class CountersHelper {
    
    private final ClusterRegistry clusterRegistry;
    private final ClusterRouteHandler<GetCounter, GetCounterResult> getCounterRouteHandler;
    
    public CountersHelper(final ClusterRegistry clusterRegistry) {
        this.clusterRegistry = clusterRegistry;
        
        getCounterRouteHandler = new ClusterRouteHandler<GetCounter, GetCounterResult>() {
            
            @Override
            public String getKey() {
                return "counters_get";
            }
            
            @Override
            public Async<GetCounterResult> handle(final long id, final String key, final GetCounter request) {
                // handle internally
                return result(GetCounterResult.getDefaultInstance());
            }
            
        };
        
        clusterRegistry.register(getCounterRouteHandler);
    }
    
    @PreDestroy
    public void shutdown() {
        clusterRegistry.deregister(getCounterRouteHandler);
    }
    
}
``` 

### Broadcasting

Services may also define cluster events for broadcasting, typically used for evicting stale data
across the cluster:

```java
@Singleton
public final class LocalCacheWithClusterInvalidate implements Cache, ClusterBroadcastHandler<InvalidateCache> {
    
    private final ClusterRegistry clusterRegistry;
    
    public LocalConfigCacheWithClusterInvalidateProvider(final ClusterRegistry clusterRegistry) {
        this.clusterRegistry = clusterRegistry;
    }
    
    @PostConstruct
    public void startup() {
        clusterRegistry.register(this);
    }
    
    @PreDestroy
    public void shutdown() {
        clusterRegistry.deregister(this);
    }
    
    @Override
    public String getKey() {
        return "cache_invalidate";
    }
    
    @Override
    public Async<Boolean> handle(final InvalidateCache message) {
        internalInvalidate(def);
        return result(true);
    }
    
    @Override
    public void invalidate() {
        internalInvalidate(def);
        clusterRegistry.broadcast(this, InvalidateCache.newBuilder().set(???).build());
    }
    
    private void internalInvalidate() {
        // invalidate
    }
    
    ...
    
}
```