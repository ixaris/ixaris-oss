package com.ixaris.commons.persistence.test;

import static com.ixaris.commons.async.lib.CompletionStageUtil.block;
import static com.ixaris.commons.multitenancy.lib.MultiTenancy.TENANT;

import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import com.ixaris.commons.async.lib.idempotency.Intent;
import com.ixaris.commons.misc.lib.function.RunnableThrows;
import com.ixaris.commons.misc.lib.id.UniqueIdGenerator;
import com.ixaris.commons.multitenancy.test.TestTenants;
import com.ixaris.commons.persistence.lib.AsyncPersistenceProvider;
import com.ixaris.commons.persistence.lib.exception.DuplicateIntentException;
import com.ixaris.commons.persistence.lib.exception.EntityNotFoundException;

/**
 * @author <a href="mailto:aldrin.seychell@ixaris.com">aldrin.seychell</a>
 */
public abstract class AbstractProcessedIntentsTest {
    
    private AsyncPersistenceProvider provider;
    
    @Before
    public void setup() {
        initialiseTenant(TestTenants.DEFAULT);
        provider = createProvider();
    }
    
    protected abstract void initialiseTenant(String tenant);
    
    protected abstract void exec(RunnableThrows<? extends Exception> runnable) throws Exception; // NOSONAR test code
    
    protected abstract AsyncPersistenceProvider createProvider();
    
    @Test
    public void create_IntentDoesNotExist_Created() throws Exception {
        aroundAsync(() -> {
            final Intent intent = new Intent(UniqueIdGenerator.generate(), "op", 0L);
            block(provider.transaction(() -> {
                provider.getProcessedIntents().create(intent);
                return null;
            }));
            
            final Boolean b = block(provider.transaction(() -> provider.getProcessedIntents().exists(intent)));
            Assertions.assertThat(b).isTrue();
            
            final Optional<Intent> fetch = block(provider.transaction(() -> provider.getProcessedIntents().fetch(intent)));
            Assertions.assertThat(fetch).isNotEmpty().contains(intent);
        });
    }
    
    @Test
    public void create_IntentAlreadyExists_ThrowsException() throws Exception {
        aroundAsync(() -> {
            final Intent intent = new Intent(UniqueIdGenerator.generate(), "op", 0L);
            
            block(provider.transaction(() -> {
                provider.getProcessedIntents().create(intent);
                return null;
            }));
            
            Assertions
                .assertThatThrownBy(() -> block(provider.transaction(() -> {
                    provider.getProcessedIntents().create(intent);
                    return null;
                })))
                .isInstanceOf(DuplicateIntentException.class)
                .hasMessageContaining("Duplicate intent");
        });
    }
    
    @Test
    public void fetch_IntentDoesNotExist_ReturnsEmptyIntent() throws Exception {
        aroundAsync(() -> {
            final Intent intent = new Intent(UniqueIdGenerator.generate(), "op", 0L);
            final Optional<Intent> o = block(provider.transaction(() -> provider.getProcessedIntents().fetch(intent)));
            Assertions.assertThat(o).isEmpty();
        });
    }
    
    @Test
    public void fetch_IntentExists_ReturnsExistingIntent() throws Exception {
        aroundAsync(() -> {
            final Intent intent = new Intent(UniqueIdGenerator.generate(), "op", 0L);
            block(provider.transaction(() -> {
                provider.getProcessedIntents().create(intent);
                return null;
            }));
            final Optional<Intent> o = block(provider.transaction(() -> provider.getProcessedIntents().fetch(intent)));
            Assertions.assertThat(o).isNotEmpty().contains(intent);
        });
    }
    
    @Test
    public void exists_IntentDoesNotExist_ReturnsFalse() throws Exception {
        aroundAsync(() -> {
            final Intent intent = new Intent(UniqueIdGenerator.generate(), "op", 0L);
            final Boolean o = block(provider.transaction(() -> provider.getProcessedIntents().exists(intent)));
            Assertions.assertThat(o).isFalse();
        });
    }
    
    @Test
    public void exists_IntentExists_ReturnsTrue() throws Exception {
        aroundAsync(() -> {
            final Intent intent = new Intent(UniqueIdGenerator.generate(), "op", 0L);
            block(provider.transaction(() -> {
                provider.getProcessedIntents().create(intent);
                return null;
            }));
            final Boolean b = block(provider.transaction(() -> provider.getProcessedIntents().exists(intent)));
            Assertions.assertThat(b).isTrue();
        });
    }
    
    @Test
    public void delete_IntentDoesNotExist_ThrowsException() throws Exception {
        aroundAsync(() -> {
            final Intent intent = new Intent(UniqueIdGenerator.generate(), "op", 0L);
            Assertions
                .assertThatThrownBy(() -> block(provider.transaction(() -> {
                    provider.getProcessedIntents().delete(intent);
                    return null;
                })))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Entity with id [" + intent + "] does not exist");
        });
    }
    
    @Test
    public void delete_IntentExists_IsDeleted() throws Exception {
        aroundAsync(() -> {
            final Intent intent = new Intent(UniqueIdGenerator.generate(), "op", 0L);
            block(provider.transaction(() -> {
                provider.getProcessedIntents().create(intent);
                return null;
            }));
            block(provider.transaction(() -> {
                provider.getProcessedIntents().delete(intent);
                return null;
            }));
            
            final Boolean b = block(provider.transaction(() -> provider.getProcessedIntents().exists(intent)));
            Assertions.assertThat(b).isFalse();
        });
    }
    
    private void aroundAsync(final RunnableThrows<? extends Exception> runnable) throws Exception {
        TENANT.exec(TestTenants.DEFAULT, () -> exec(runnable));
    }
}
