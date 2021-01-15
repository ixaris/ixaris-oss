package com.ixaris.commons.files.test;

import static com.ixaris.commons.async.lib.Async.result;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.ByteString;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.AsyncIterator;
import com.ixaris.commons.async.lib.AsyncIterator.NoMoreElementsException;
import com.ixaris.commons.files.lib.FileObject;
import com.ixaris.commons.files.lib.FileStore;
import com.ixaris.commons.multitenancy.lib.MultiTenancy;
import com.ixaris.commons.multitenancy.lib.collection.MultiTenantMap;

/**
 * In memory implementation of {@link FileStore}, intended to be used for tests. (Yes, I get it. It's in memory. It's
 * not a data handler. The tests still need to work with something though!)
 *
 * <p>TODO: eventually this needs to be the version we autowire in microservices-test for all tests to use. At the
 * moment, we just fail the loading of the bean and keep going.
 */
@Component
public final class InMemoryFileStore implements FileStore {
    
    private static final Logger LOG = LoggerFactory.getLogger(InMemoryFileStore.class);
    
    private final MultiTenantMap<String, ByteString, HashMap<String, ByteString>> store;
    
    public InMemoryFileStore(final MultiTenancy multiTenancy) {
        store = new MultiTenantMap<>(multiTenancy, HashMap::new);
    }
    
    @Override
    public Async<Void> saveFile(final String name, final File file) {
        try {
            saveBytes(name, ByteString.readFrom(new FileInputStream(file)));
            return result();
        } catch (final IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
    
    @Override
    public Async<Void> saveBytes(final String name, final ByteString byteString) {
        store.put(name, byteString);
        return result();
    }
    
    @Override
    public Async<ByteString> loadBytes(final String name) {
        return result(store.get(name));
    }
    
    @Override
    public <T> AsyncIterator<T> loadStreamChunks(final String name, final IteratorFactory<T> factory) {
        final InputStream is = store.get(name).newInput();
        final Iterator<T> iterator = factory.create(is);
        return () -> {
            if (iterator.hasNext()) {
                return result(iterator.next());
            } else {
                try {
                    is.close();
                } catch (final IOException e) {
                    LOG.warn("Error while closing inputstream for {}", name, e);
                }
                throw new NoMoreElementsException();
            }
        };
    }
    
    @Override
    public Async<Set<FileObject>> list(final String prefix) {
        return result(
            store.keySet()
                .stream()
                .filter(object -> object.startsWith(prefix))
                .map(object -> new FileObject(object, false))
                .collect(Collectors.toSet()));
    }
    
    @Override
    public Async<Void> copy(final String from, final String to) {
        Optional.ofNullable(store.get(from)).ifPresent(v -> store.put(to, v));
        return result();
    }
    
    @Override
    public Async<Void> move(final String from, final String to) {
        Optional.ofNullable(store.remove(from)).ifPresent(v -> store.put(to, v));
        return result();
    }
    
    public Map<String, ByteString> getFilesList() {
        return ImmutableMap.copyOf(store);
    }
    
}
