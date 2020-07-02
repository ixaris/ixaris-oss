package com.ixaris.commons.jooq.persistence;

import static com.ixaris.commons.jooq.persistence.TransactionalDSLContext.JOOQ_TX;

import org.jooq.Condition;
import org.jooq.Query;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOnConditionStep;
import org.jooq.impl.DSL;

public final class JooqHelper {
    
    @FunctionalInterface
    public interface DeleteFromStep {
        
        DeleteWhereStep from(TableOnConditionStep<Record> join);
        
    }
    
    @FunctionalInterface
    public interface DeleteWhereStep {
        
        Query where(Condition condition);
        
    }
    
    /**
     * Workaround for supporting mysql delete with join syntax
     */
    public static DeleteFromStep delete(final Table<?>... tables) {
        return join -> condition -> JOOQ_TX.get().query("DELETE {0} FROM {1} WHERE {2}", DSL.list(tables), join, condition);
    }
    
    public static <T> Condition eqNullable(final TableField<?, T> field, final T value) {
        return (value != null) ? field.eq(value) : field.isNull();
    }
    
    private JooqHelper() {}
    
}
