package com.ixaris.commons.multitenancy.lib;

import static com.ixaris.commons.async.lib.Async.result;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ixaris.commons.async.lib.Async;

public class MultiTenancyRetryTest {
    
    private MultiTenancy multiTenancy;
    
    @BeforeEach
    public void start() {
        multiTenancy = new MultiTenancy(100, 100, TimeUnit.MILLISECONDS);
        multiTenancy.start();
    }
    
    @AfterEach
    public void stop() {
        multiTenancy.stop();
    }
    
    @Test
    public void testActivateRetry() {
        final Lifecycle lifecycle = new Lifecycle();
        final Lifecycle2 lifecycle2 = new Lifecycle2();
        final TestListener listener = new TestListener();
        
        multiTenancy.registerTenantLifecycleParticipant(lifecycle);
        multiTenancy.registerTenantLifecycleParticipant(lifecycle2);
        multiTenancy.addTenantLifecycleListener(listener);
        
        lifecycle.fail = true;
        
        multiTenancy.addTenant(TestTenants.DEFAULT);
        
        Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> lifecycle.paCount.get() > 0);
        
        Assertions.assertThat(multiTenancy.getActiveTenants().size()).isEqualTo(0);
        Assertions.assertThat(listener.getActivated().size()).isEqualTo(0);
        
        lifecycle.fail = false;
        
        Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> lifecycle.aCount.get() > 0);
        
        Assertions.assertThat(multiTenancy.getActiveTenants().size()).isEqualTo(1);
        Assertions.assertThat(listener.getActivated().size()).isEqualTo(1);
    }
    
    private static final class Lifecycle implements TenantLifecycleParticipant {
        
        private final AtomicInteger paCount = new AtomicInteger();
        private final AtomicInteger aCount = new AtomicInteger();
        private final AtomicInteger dCount = new AtomicInteger();
        private final AtomicInteger pdCount = new AtomicInteger();
        private volatile boolean fail = false;
        
        @Override
        public String getName() {
            return "test";
        }
        
        @Override
        public Async<Void> preActivate(final String tenantId) {
            paCount.incrementAndGet();
            return result();
        }
        
        @Override
        public Async<Void> activate(final String tenantId) {
            if (fail) {
                throw new IllegalStateException("Instructed to fail");
            } else {
                aCount.incrementAndGet();
                return result();
            }
        }
        
        @Override
        public Async<Void> deactivate(final String tenantId) {
            dCount.incrementAndGet();
            return result();
        }
        
        @Override
        public Async<Void> postDeactivate(final String tenantId) {
            pdCount.incrementAndGet();
            return result();
        }
        
    }
    
    private static final class Lifecycle2 implements TenantLifecycleParticipant {
        
        @Override
        public String getName() {
            return "test2";
        }
        
        @Override
        public Async<Void> preActivate(final String tenantId) {
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
        
        @Override
        public Async<Void> postDeactivate(final String tenantId) {
            return result();
        }
        
    }
    
}
