package com.ixaris.commons.minio.files;

import static com.ixaris.commons.async.lib.Async.result;

import java.io.File;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.google.protobuf.ByteString;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.AsyncIterator;
import com.ixaris.commons.async.lib.executor.AsyncExecutorWrapper;
import com.ixaris.commons.async.lib.thread.NamedThreadFactory;
import com.ixaris.commons.files.lib.FileObject;
import com.ixaris.commons.files.lib.FileStore;
import com.ixaris.commons.misc.lib.object.Wrapper;
import com.ixaris.commons.multitenancy.lib.object.AbstractEagerMultiTenantObject;

import io.minio.MinioClient;

/**
 * Ensures that any started tenant has a bucket in Minio.
 */
public final class MinioMultiTenantFileStore extends AbstractEagerMultiTenantObject<MinioFileStore, Void> implements FileStore {
    
    private static final int CORE_THREAD_POOL_SIZE = 2;
    private static final int MAX_THREAD_POOL_SIZE = 32;
    
    private final Executor executor;
    private final MinioClient minioClient;
    
    public MinioMultiTenantFileStore(final Executor executor, final MinioClient minioClient) {
        super(MinioMultiTenantFileStore.class.getSimpleName());
        this.executor = Wrapper.isWrappedBy(executor, AsyncExecutorWrapper.class)
            ? executor : new AsyncExecutorWrapper<>(true, executor);
        this.minioClient = minioClient;
    }
    
    public MinioMultiTenantFileStore(final MinioClient minioClient) {
        this(
            new AsyncExecutorWrapper<>(true, new ThreadPoolExecutor(
                CORE_THREAD_POOL_SIZE,
                MAX_THREAD_POOL_SIZE,
                2L,
                TimeUnit.MINUTES,
                new LinkedBlockingQueue<>(),
                new NamedThreadFactory("MinioMultiTenantFileStore-"))),
            minioClient);
    }
    
    @Override
    public Async<Void> preActivate(final String tenantId) {
        addTenant(tenantId, null);
        return result();
    }
    
    @Override
    public Async<Void> activate(final String tenantId) {
        return result();
    }
    
    @Override
    public Async<Void> deactivate(final String tenantId) {
        return result();
    }
    
    public Async<Void> postDeactivate(final String tenantId) {
        removeTenant(tenantId);
        return result();
    }
    
    @Override
    protected MinioFileStore create(final Void create, final String tenantId) {
        return new MinioFileStore(tenantId, executor, minioClient);
    }
    
    @Override
    protected void destroy(final MinioFileStore instance, final String tenantId) {
        // nothing to destroy
    }
    
    @Override
    public Async<Void> saveFile(final String name, final File file) {
        return get().saveFile(name, file);
    }
    
    @Override
    public Async<Void> saveBytes(final String name, final ByteString byteString) {
        return get().saveBytes(name, byteString);
    }
    
    @Override
    public Async<ByteString> loadBytes(final String name) {
        return get().loadBytes(name);
    }
    
    @Override
    public <T> AsyncIterator<T> loadStreamChunks(final String name, final IteratorFactory<T> factory) {
        return get().loadStreamChunks(name, factory);
    }
    
    @Override
    public Async<Set<FileObject>> list(final String prefix) {
        return get().list(prefix);
    }
    
    @Override
    public Async<Void> copy(final String from, final String to) {
        return get().copy(from, to);
    }
    
    @Override
    public Async<Void> move(final String from, final String to) {
        return get().move(from, to);
    }
    
}
