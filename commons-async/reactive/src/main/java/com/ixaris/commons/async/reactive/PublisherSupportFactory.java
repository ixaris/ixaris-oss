package com.ixaris.commons.async.reactive;

/**
 * Factory to create {@link PublisherSupport} instances
 */
public interface PublisherSupportFactory {
    
    <T> PublisherSupport<T> create();
    
}
