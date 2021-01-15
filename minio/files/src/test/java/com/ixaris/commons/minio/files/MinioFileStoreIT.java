package com.ixaris.commons.minio.files;

import static com.ixaris.commons.async.lib.CompletionStageUtil.join;
import static com.ixaris.commons.multitenancy.lib.MultiTenancy.TENANT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import com.google.protobuf.ByteString;

import com.ixaris.commons.async.lib.AsyncIterator;
import com.ixaris.commons.async.lib.AsyncIterator.NoMoreElementsException;
import com.ixaris.commons.files.lib.FileObject;
import com.ixaris.commons.minio.files.MinioFileStoreIT.MinioConfig;
import com.ixaris.commons.minio.test.MinioContainer;
import com.ixaris.commons.multitenancy.lib.MultiTenancy;
import com.ixaris.commons.multitenancy.lib.TenantLifecycleParticipant;
import com.ixaris.commons.multitenancy.test.TestTenants;

import io.findify.s3mock.S3Mock;
import io.minio.MinioClient;
import io.minio.errors.MinioException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class, classes = MinioConfig.class)
public final class MinioFileStoreIT {
    
    //    @ClassRule
    public static MinioContainer minio = new MinioContainer();
    
    @Autowired
    private S3Mock api;
    
    @ImportResource("classpath*:spring/*.xml")
    @Configuration
    public static class MinioConfig {
        
        @Bean
        public MinioClient minioClient() {
            try {
                return new MinioClient("http://" + minio.getHost() + ":" + minio.getFirstMappedPort(), MinioContainer.ACCESS_KEY, MinioContainer.SECRET_KEY);
            } catch (final MinioException e) {
                throw new IllegalStateException(e);
            }
        }
        
        @Bean(destroyMethod = "stop")
        public S3Mock getS3Mock() {
            final S3Mock api = new S3Mock.Builder().withPort(minio.getFirstMappedPort()).withInMemoryBackend().build();
            api.start();
            return api;
        }
        
        @Bean
        public ApplicationListener<ContextRefreshedEvent> startCluster(final Set<TenantLifecycleParticipant> participants, final MultiTenancy multiTenancy) {
            return e -> multiTenancy.addTenant(TestTenants.DEFAULT);
        }
        
    }
    
    private static final String PREFIX = "prefix";
    
    @Autowired
    public MinioMultiTenantFileStore fileStore;
    
    @Test
    public void list_shouldFindNoFiles() {
        final Set<FileObject> fileObjects = join(TENANT.exec(TestTenants.DEFAULT, () -> fileStore.list(PREFIX)));
        Assertions.assertThat(fileObjects).isEmpty();
    }
    
    @Test
    public void list_shouldStoreAndRetrieve() {
        TENANT.exec(TestTenants.DEFAULT, () -> {
            final String data = "TEST DATA";
            join(fileStore.saveBytes("testFile", ByteString.copyFromUtf8(data)));
            assertEquals(data, join(fileStore.loadBytes("testFile")).toStringUtf8());
        });
    }
    
    @Test
    @Ignore("Only works with minio container. Unignore when we manage to run testcontainers on jenkins")
    public void copy_shouldStoreAndRetrieve() {
        TENANT.exec(TestTenants.DEFAULT, () -> {
            final String data = "TEST DATA";
            join(fileStore.saveBytes("testFile", ByteString.copyFromUtf8(data)));
            join(fileStore.move("testFile", "testFile2"));
            assertEquals(data, join(fileStore.loadBytes("testFile2")).toStringUtf8());
            join(fileStore.copy("testFile2", "testFile3"));
            assertEquals(data, join(fileStore.loadBytes("testFile3")).toStringUtf8());
            assertEquals(data, join(fileStore.loadBytes("testFile2")).toStringUtf8());
        });
    }
    
    @Test
    public void loadChunks() {
        TENANT.exec(TestTenants.DEFAULT, () -> {
            final StringBuilder sb = new StringBuilder();
            for (int x = 0; x < 1000; x++) {
                sb.append("TEST DATA\n");
            }
            join(fileStore.saveBytes("testChunksFile", ByteString.copyFromUtf8(sb.toString())));
            final AsyncIterator<String> i = fileStore.loadStreamChunks("testChunksFile", is -> {
                final BufferedReader r = new BufferedReader(new InputStreamReader(is));
                
                return new Iterator<String>() {
                    
                    String next = advance();
                    
                    @Override
                    public boolean hasNext() {
                        return next != null;
                    }
                    
                    @Override
                    public String next() {
                        if (next != null) {
                            final String n = next;
                            next = advance();
                            return n;
                        } else {
                            throw new NoSuchElementException();
                        }
                    }
                    
                    private String advance() {
                        try {
                            return r.readLine();
                        } catch (final EOFException e) {
                            try {
                                r.close();
                            } catch (final IOException ee) {
                                // ignore
                            }
                            return null;
                        } catch (final IOException e) {
                            throw new IllegalStateException(e);
                        }
                    }
                    
                };
            });
            
            int count = 0;
            try {
                while (count <= 1000) {
                    assertEquals("TEST DATA", join(i.next()));
                    count++;
                }
                fail();
            } catch (final NoMoreElementsException e) {
                assertEquals(1000, count);
            }
        });
    }
    
}
