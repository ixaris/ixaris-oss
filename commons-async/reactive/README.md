# Asynchronous Reactive 

This library provides helpers around reactive streams

## Publisher Support

The `PublisherSupportFactory` interface provides a way to create a factory of `PublisherSupport` instances.
The `PublisherSupport` interface provides an implementation to help create reactive streams publishers without
tying implementations to a specific mechanism or superclass for handling subscribers, threads, etc. 
Instead, these concerns are covered by this interface.
