package com.ixaris.commons.jooq.persistence;

import static com.ixaris.commons.async.lib.idempotency.Intent.INTENT;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;

import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.jooq.exception.DataChangedException;

import com.ixaris.commons.misc.lib.exception.ExceptionUtil;
import com.ixaris.commons.persistence.lib.Transaction;
import com.ixaris.commons.persistence.lib.TransactionalPersistenceContext;
import com.ixaris.commons.persistence.lib.exception.DuplicateEntryException;
import com.ixaris.commons.persistence.lib.exception.OptimisticLockException;

public interface TransactionalDSLContext extends DSLContext, TransactionalPersistenceContext {
    
    ThreadLocal<TransactionalDSLContext> JOOQ_TX = new ThreadLocal<>();
    
    static TransactionalDSLContext create(final Transaction tx, final DSLContext ctx) {
        return TransactionalDSLContextFactory.create(tx, ctx);
    }
    
    static RuntimeException handle(final DataAccessException e) {
        if (isOptimisticLockException(e)) {
            throw new OptimisticLockException(e);
        } else if (isDuplicateEntryException(e)) {
            throw new DuplicateEntryException(String.format("Duplicate entry for intent [%s]", INTENT.get()), e);
        } else {
            final Throwable cause = e.getCause();
            if ((cause == null) || (cause instanceof SQLException)) {
                throw e;
            } else {
                throw ExceptionUtil.sneakyThrow(cause);
            }
        }
    }
    
    static boolean isDuplicateEntryException(final DataAccessException e) {
        final Throwable cause = e.getCause();
        if (cause instanceof SQLIntegrityConstraintViolationException) {
            return cause.getMessage().startsWith("Duplicate entry");
        } else {
            return false;
        }
    }
    
    static boolean isOptimisticLockException(final DataAccessException e) {
        return e instanceof DataChangedException;
    }
    
}
