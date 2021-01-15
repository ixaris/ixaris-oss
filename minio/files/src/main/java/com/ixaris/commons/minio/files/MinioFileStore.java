package com.ixaris.commons.minio.files;

import static com.ixaris.commons.async.lib.AsyncExecutor.execAndRelay;
import static com.ixaris.commons.async.lib.AsyncIterator.closeableNext;
import static io.minio.ErrorCode.NO_SUCH_KEY;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParserException;

import com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream;
import com.google.protobuf.ByteString;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.AsyncIterator;
import com.ixaris.commons.files.lib.FileNotFoundInStoreException;
import com.ixaris.commons.files.lib.FileObject;
import com.ixaris.commons.files.lib.FileStore;

import io.minio.MinioClient;
import io.minio.Result;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import io.minio.messages.Item;

/**
 * A {@link MinioClient Minio} implementation of file data. Communicates to S3-compatible APIs to store files outside of
 * this service. See the readme in the module for full details.
 */
public final class MinioFileStore implements FileStore {
    
    private static final Logger LOG = LoggerFactory.getLogger(MinioFileStore.class);
    
    private final String bucket;
    private final Executor executor;
    private final MinioClient client;
    
    private volatile boolean bucketCreated = false;
    
    public MinioFileStore(final String bucket, final Executor executor, final MinioClient minioClient) {
        this.bucket = bucket;
        this.executor = executor;
        this.client = minioClient;
        
        try {
            createBucket();
        } catch (final IllegalStateException e) {
            LOG.error("Error while creating bucket " + bucket, e);
        }
    }
    
    @Override
    public Async<Void> saveFile(final String name, final File file) {
        return execAndRelay(executor, () -> {
            try {
                saveStream(file.getName(), new FileInputStream(file), file.length());
            } catch (final FileNotFoundException e) {
                throw new IllegalArgumentException(e);
            }
            return (Void) null;
        });
    }
    
    @Override
    public Async<Void> saveBytes(final String name, final ByteString byteString) {
        return execAndRelay(executor, () -> {
            final ByteBuffer byteBuffer = byteString.asReadOnlyByteBuffer();
            saveStream(name, new ByteBufferBackedInputStream(byteBuffer), byteString.size());
            return (Void) null;
        });
    }
    
    @Override
    public Async<ByteString> loadBytes(final String name) {
        return execAndRelay(executor, () -> {
            try {
                return ByteString.readFrom(loadStream(name));
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
        });
    }
    
    @Override
    public <T> AsyncIterator<T> loadStreamChunks(final String name, final IteratorFactory<T> factory) {
        return new AsyncIterator<T>() {
            
            private InputStream is;
            private Iterator<T> iterator;
            
            @Override
            public Async<T> next() throws NoMoreElementsException {
                return execAndRelay(executor, () -> {
                    if (is == null) {
                        if (!bucketCreated) {
                            createBucket();
                        }
                        is = loadStream(name);
                        iterator = factory.create(is);
                    }
                    return closeableNext(iterator, is);
                });
            }
            
        };
    }
    
    @Override
    public Async<Set<FileObject>> list(final String listPrefix) {
        return execAndRelay(executor, () -> {
            if (!bucketCreated) {
                createBucket();
            }
            final Iterable<Result<Item>> results = client.listObjects(bucket, listPrefix, false);
            
            return StreamSupport.stream(results.spliterator(), false)
                .map(result -> {
                    try {
                        return new FileObject(result.get().objectName(), result.get().isDir());
                    } catch (final MinioException | GeneralSecurityException | XmlPullParserException | IOException e) {
                        throw new IllegalStateException(e);
                    }
                })
                .collect(Collectors.toSet());
        });
    }
    
    @Override
    public Async<Void> copy(final String from, final String to) {
        return execAndRelay(executor, () -> {
            copy(from, to, false);
            return (Void) null;
        });
    }
    
    @Override
    public Async<Void> move(final String from, final String to) {
        return execAndRelay(executor, () -> {
            copy(from, to, true);
            return (Void) null;
        });
    }
    
    private void saveStream(final String name, final InputStream inputStream, long size) {
        if (!bucketCreated) {
            createBucket();
        }
        try {
            // the default is application/octet-stream; we don't care what we upload
            if (size < 0L) {
                client.putObject(bucket, name, inputStream, null, null, null, "application/octet-stream");
            } else {
                client.putObject(bucket, name, inputStream, size, null, null, "application/octet-stream");
            }
        } catch (final MinioException | GeneralSecurityException | XmlPullParserException | IOException e) {
            throw new IllegalStateException(e);
        }
    }
    
    private InputStream loadStream(final String name) {
        if (!bucketCreated) {
            createBucket();
        }
        try {
            return client.getObject(bucket, name);
        } catch (final ErrorResponseException e) {
            return convertErrorResponse(name, e);
        } catch (final MinioException | GeneralSecurityException | XmlPullParserException | IOException e) {
            throw new IllegalStateException(e);
        }
    }
    
    private void copy(final String from, final String to, final boolean deleteOriginal) {
        if (!bucketCreated) {
            createBucket();
        }
        try {
            client.copyObject(bucket, from, bucket, to);
            if (deleteOriginal) {
                client.removeObject(bucket, from);
            }
        } catch (final ErrorResponseException e) {
            convertErrorResponse(from, e);
        } catch (final MinioException | GeneralSecurityException | XmlPullParserException | IOException e) {
            throw new IllegalStateException(e);
        }
    }
    
    private InputStream convertErrorResponse(final String fileName, final ErrorResponseException e) {
        if (NO_SUCH_KEY.equals(e.errorResponse().errorCode())) {
            throw new FileNotFoundInStoreException(fileName, e);
        }
        throw new IllegalStateException(e);
    }
    
    private synchronized void createBucket() {
        if (!bucketCreated) {
            try {
                if (!client.bucketExists(bucket)) {
                    client.makeBucket(bucket);
                }
                bucketCreated = true;
            } catch (final MinioException | GeneralSecurityException | XmlPullParserException | IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }
    
}
