package com.ixaris.commons.multitenancy.lib;

import static com.ixaris.commons.async.lib.Async.result;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.ixaris.commons.async.lib.Async;

public class TenantLifecycleParticipantTest {
    
    private static final class TestParticipant implements TenantLifecycleParticipant {
        
        private final int index;
        private boolean failPreActivate;
        private boolean failActivate;
        private boolean failDeactivate;
        private boolean failPostDeactivate;
        
        private final AtomicBoolean donePreActivate = new AtomicBoolean();
        private final AtomicBoolean doneActivate = new AtomicBoolean();
        private final AtomicBoolean doneDeactivate = new AtomicBoolean();
        private final AtomicBoolean donePostDeactivate = new AtomicBoolean();
        
        private final AtomicInteger failPreActivateCount = new AtomicInteger();
        private final AtomicInteger failActivateCount = new AtomicInteger();
        private final AtomicInteger failDeactivateCount = new AtomicInteger();
        private final AtomicInteger failPostDeactivateCount = new AtomicInteger();
        
        TestParticipant(final int index) {
            this.index = index;
        }
        
        @Override
        public String getName() {
            return "TestParticipant" + index;
        }
        
        @Override
        public Async<Void> preActivate(final String tenantId) {
            if (failPreActivate) {
                failPreActivateCount.incrementAndGet();
                throw new IllegalStateException();
            }
            donePreActivate.set(true);
            return result();
        }
        
        @Override
        public Async<Void> activate(final String tenantId) {
            if (failActivate) {
                failActivateCount.incrementAndGet();
                throw new IllegalStateException();
            }
            doneActivate.set(true);
            return result();
        }
        
        @Override
        public Async<Void> deactivate(final String tenantId) {
            if (failDeactivate) {
                failDeactivateCount.incrementAndGet();
                throw new IllegalStateException();
            }
            doneDeactivate.set(true);
            return result();
        }
        
        @Override
        public Async<Void> postDeactivate(final String tenantId) {
            if (failPostDeactivate) {
                failPostDeactivateCount.incrementAndGet();
                throw new IllegalStateException();
            }
            donePostDeactivate.set(true);
            return result();
        }
        
    }
    
    @Test
    public void testTwoParticipants_tenantAddedAndRemovedSuccessfully() throws TenantInactiveException {
        final MultiTenancy multiTenancy = new MultiTenancy(1, 1, TimeUnit.SECONDS);
        try {
            multiTenancy.start();
            
            final TestParticipant p1 = new TestParticipant(1);
            final TestParticipant p2 = new TestParticipant(2);
            
            multiTenancy.registerTenantLifecycleParticipant(p1);
            multiTenancy.registerTenantLifecycleParticipant(p2);
            
            multiTenancy.addTenant(TestTenants.DEFAULT);
            await().atMost(1, TimeUnit.SECONDS).until(() -> p1.doneActivate.get() && p2.doneActivate.get());
            multiTenancy.verifyTenantIsActive(TestTenants.DEFAULT);
            
            assertThat(p1.donePreActivate).isTrue();
            assertThat(p1.doneActivate).isTrue();
            assertThat(p2.donePreActivate).isTrue();
            assertThat(p2.doneActivate).isTrue();
            
            multiTenancy.removeTenant(TestTenants.DEFAULT);
            await().atMost(1, TimeUnit.SECONDS).until(() -> p1.doneDeactivate.get() && p2.doneDeactivate.get());
            assertThatThrownBy(() -> multiTenancy.verifyTenantIsActive(TestTenants.DEFAULT))
                .isInstanceOf(TenantInactiveException.class)
                .hasMessage("Tenant [default] is inactive (UNKNOWN)");
            assertThat(p1.doneDeactivate).isTrue();
            assertThat(p1.donePostDeactivate).isTrue();
            assertThat(p2.doneDeactivate).isTrue();
            assertThat(p2.donePostDeactivate).isTrue();
        } finally {
            multiTenancy.stop();
        }
    }
    
    @Test
    public void testThreeParticipantsWithErrors_tenantLifecyclePhaseRetriesAndReportParticipantsInError() throws TenantInactiveException {
        final MultiTenancy multiTenancy = new MultiTenancy(1, 1, TimeUnit.SECONDS);
        try {
            multiTenancy.start();
            final TestParticipant p1 = new TestParticipant(1);
            final TestParticipant p2 = new TestParticipant(2);
            final TestParticipant p3 = new TestParticipant(3);
            
            multiTenancy.registerTenantLifecycleParticipant(p1);
            multiTenancy.registerTenantLifecycleParticipant(p2);
            multiTenancy.registerTenantLifecycleParticipant(p3);
            
            assertThatThrownBy(() -> multiTenancy.verifyTenantIsActive(TestTenants.DEFAULT))
                .isInstanceOf(TenantInactiveException.class)
                .hasMessage("Tenant [default] is inactive (UNKNOWN)");
            
            p2.failPreActivate = true;
            p3.failActivate = true;
            multiTenancy.addTenant(TestTenants.DEFAULT);
            await().atMost(1, TimeUnit.SECONDS).until(() -> p2.failPreActivateCount.get() > 0);
            assertThatThrownBy(() -> multiTenancy.verifyTenantIsActive(TestTenants.DEFAULT))
                .isInstanceOf(TenantInactiveException.class)
                .hasMessage("Tenant [default] is inactive (PRE_ACTIVATE) TestParticipant2 in error");
            assertThat(p1.donePreActivate).isTrue();
            assertThat(p3.donePreActivate).isTrue();
            
            p2.failPreActivate = false;
            await().atMost(3, TimeUnit.SECONDS).until(p2.donePreActivate::get);
            await().atMost(3, TimeUnit.SECONDS).until(() -> p3.failActivateCount.get() > 0);
            assertThatThrownBy(() -> multiTenancy.verifyTenantIsActive(TestTenants.DEFAULT))
                .isInstanceOf(TenantInactiveException.class)
                .hasMessage("Tenant [default] is inactive (ACTIVATE) TestParticipant3 in error");
            assertThat(p2.donePreActivate).isTrue();
            
            p3.failActivate = false;
            await().atMost(3, TimeUnit.SECONDS).until(p3.doneActivate::get);
            multiTenancy.verifyTenantIsActive(TestTenants.DEFAULT);
            assertThat(p1.doneActivate).isTrue();
            assertThat(p2.doneActivate).isTrue();
            assertThat(p3.doneActivate).isTrue();
            
            p2.failDeactivate = true;
            p3.failPostDeactivate = true;
            multiTenancy.removeTenant(TestTenants.DEFAULT);
            await().atMost(1, TimeUnit.SECONDS).until(() -> p2.failDeactivateCount.get() > 0);
            assertThatThrownBy(() -> multiTenancy.verifyTenantIsActive(TestTenants.DEFAULT))
                .isInstanceOf(TenantInactiveException.class)
                .hasMessage("Tenant [default] is inactive (DEACTIVATE) TestParticipant2 in error");
            assertThat(p1.doneDeactivate).isTrue();
            assertThat(p3.doneDeactivate).isTrue();
            
            p2.failDeactivate = false;
            await().atMost(3, TimeUnit.SECONDS).until(p2.doneDeactivate::get);
            await().atMost(3, TimeUnit.SECONDS).until(() -> p3.failPostDeactivateCount.get() > 0);
            assertThatThrownBy(() -> multiTenancy.verifyTenantIsActive(TestTenants.DEFAULT))
                .isInstanceOf(TenantInactiveException.class)
                .hasMessage("Tenant [default] is inactive (POST_DEACTIVATE) TestParticipant3 in error");
            assertThat(p2.doneDeactivate).isTrue();
            
            p3.failPostDeactivate = false;
            await().atMost(3, TimeUnit.SECONDS).until(p3.donePostDeactivate::get);
            assertThatThrownBy(() -> multiTenancy.verifyTenantIsActive(TestTenants.DEFAULT))
                .isInstanceOf(TenantInactiveException.class)
                .hasMessage("Tenant [default] is inactive (UNKNOWN)");
            assertThat(p1.donePostDeactivate).isTrue();
            assertThat(p2.donePostDeactivate).isTrue();
            assertThat(p3.donePostDeactivate).isTrue();
        } finally {
            multiTenancy.stop();
        }
    }
    
}
