# Microservice Testing Library

## Mocks

Testing services in isolation means other services used need to be mocked.

The expected successful, conflict or failure responses can be mocked. Note that the returned protobuf objects must
conform to the contract's validation specification, as such validations are carried out by the library even for mock 
responses.

Additionally expected events can be published, as well as a utility that can return a promise that is fulfilled when 
the subscriber acknowledges the event.

Finally, events published by the service under test can be subscribed to and verified. 

### Mocking Expected Response
 
```java
public class SomeTest {
    
    private SomeServiceSkeleton someServiceSkeleton;
    private SomeServiceSkeleton.SomeResourceHandler.Watch someServicePublisher;
    
    @Autowired
    private ServiceSupport serviceSupport;
    
    @Autowired
    private ServiceUnderTestStub stub;
    
    @Before
    public void setup() {
        someServiceSkeleton = Mockito.mock(SomeServiceSkeleton.class, SkeletonResourceMock.createMockResourceAnswer());
        serviceSupport.init(someServiceSkeleton);
    }
    
    @Test
    public void test() {
        SkeletonResourceMock
            .doAnswer((path, header, request) -> {
                // path holds the path parameters. 
                // Add here the logic to create the expected response from the path, header and request            
                return ServicePromise.fulfilled(Ok.ok(result));
            })
            .when(someServiceSkeleton.resource().id(anyLong()))
            .operation(any(), any());
        
        // perform operation under test
        
        SkeletonResourceMock.verify().called(someServiceSkeleton.resource().id(1L)).operation(any(), any());
    }
    
}   
```

### Mocking Expected Event
 
```java
public class SomeTest {
    
    private SomeServiceSkeleton someServiceSkeleton;
    private SomeServiceSkeleton.SomeResourceHandler.Watch someServicePublisher;
    
    @Autowired
    private ServiceClientEventAckTrackingProcessorFactory eventAckTracking;
    
    @Autowired
    private ServiceSupport serviceSupport;
    
    @Autowired
    private ServiceUnderTestStub stub;

    @Before
    public void setup() {
        someServiceSkeleton = Mockito.mock(SomeServiceSkeleton.class, SkeletonResourceMock.createMockResourceAnswer());
        serviceSupport.init(someServiceSkeleton);
        
        someServicePublisher = ServiceSkeletonProxy.get(SomeServiceSkeleton.class, "").getPublisher(SomeServiceSkeleton.SomeResourceHandler.Watch.class);
    }
    
    @Test
    public void test() throws InterruptedException {
        // determine tenant and context
        final ServiceEventHeader<Context> header = new ServiceEventHeader<Context>(intentId, tenant, context);
        
        // build and publish event
        final Promise<EventAck> promise = someServicePublisher.publish(header, event);
        
        // wait for publisher acknowledgement
        promise.await();
        // wait for subscriber acknowledgement
        eventAckTracking.getPromise(header, someServicePublisher).await();
        
        // proceed with test
    }
    
}   
```        

### Verifying published events

```java
public class SomeTest {
    
    @Autowired
    private ServiceUnderTestStub stub;

    private MockEventListener<Context, SomeEvent> someEventListener;
    private ServiceEventSubscription someEventSubscription;
    
    @Before
    public void before() {
        someEventListener = new MockEventListener<>();
        someEventSubscription = stub.watch(someEventListener);
    }
    
    @After
    public void tearDown() {
        someEventSubscription.cancel();
    }
    
    @Test
    public void test() throws InterruptedException {
        // determine tenant and context
        final ServiceOperationHeader<Context> header = new ServiceOperationHeader<Context>(intentId, tenant, context);

        stub.someOperationThatPublishedAnEvent(header);
        
        // verify event published
        final Tuple2<ServiceEventHeader<Context>, SomeEvent> event = listener.expectOne(header);
    }
    
}   
```      