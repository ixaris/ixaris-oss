package db.migration.jooq_persistence;

import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V3__TestJavaMigration extends BaseJavaMigration {
    
    @Override
    public void migrate(final Context context) throws Exception {
        try (Statement update = context.getConnection().createStatement()) {
            update.execute("INSERT INTO test_table VALUES ('migration', " + System.currentTimeMillis() + ", 'X', true, '{}')");
        }
    }
    
}
