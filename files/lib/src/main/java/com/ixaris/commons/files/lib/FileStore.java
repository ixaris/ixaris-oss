package com.ixaris.commons.files.lib;

import java.io.File;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Set;

import com.google.protobuf.ByteString;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.AsyncIterator;

/**
 * A generic interface to allow for saving and loading of files to a data storage. Allows services to be able to persist
 * files outside of the container they run in, allowing the containers to be destroyed with no consequences.
 */
public interface FileStore {
    
    @FunctionalInterface
    interface IteratorFactory<T> {
        
        Iterator<T> create(final InputStream is);
        
    }
    
    Async<Void> saveFile(String name, File file);
    
    Async<Void> saveBytes(String name, ByteString byteString);
    
    Async<ByteString> loadBytes(String name);
    
    <T> AsyncIterator<T> loadStreamChunks(String name, IteratorFactory<T> factory);
    
    /**
     * Lists the objects matching the given prefix. Note that the prefix is not trimmed from the names of matching
     * objects (i.e. {@link FileObject#getName()} will return the prefix as well).
     */
    Async<Set<FileObject>> list(String prefix);
    
    Async<Void> copy(String from, String to);
    
    Async<Void> move(String from, String to);
    
}
