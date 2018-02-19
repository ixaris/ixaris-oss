package com.ixaris.commons.async.reactive;

public interface PublisherSupportFactory {
    
    <T> PublisherSupport<T> create();
    
}
