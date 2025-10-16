package jp.mcapps.android.multi_payment_terminal;

import static jp.mcapps.android.multi_payment_terminal.database.LocalDatabase.MIGRATION_1_2;
import static jp.mcapps.android.multi_payment_terminal.database.LocalDatabase.MIGRATION_2_3;
import static jp.mcapps.android.multi_payment_terminal.database.LocalDatabase.MIGRATION_3_4;
import static jp.mcapps.android.multi_payment_terminal.database.LocalDatabase.MIGRATION_4_5;

import androidx.room.Room;
import androidx.room.migration.Migration;
import androidx.room.testing.MigrationTestHelper;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import jp.mcapps.android.multi_payment_terminal.database.LocalDatabase;

@RunWith(AndroidJUnit4.class)
public class MigrationTest {
    private static final String TEST_DB = "migration-test";

    @Rule
    public MigrationTestHelper helper;

    public MigrationTest() {
        helper = new MigrationTestHelper(InstrumentationRegistry.getInstrumentation(),
                LocalDatabase.class.getCanonicalName(),
                new FrameworkSQLiteOpenHelperFactory());
    }

    @Test
    public void migrateAll() throws IOException {
        // Create earliest version of the database.
        SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 1);
        db.close();

        // Open latest version of the database. Room validates the schema
        // once all migrations execute.
        LocalDatabase appDb = Room.databaseBuilder(
                        InstrumentationRegistry.getInstrumentation().getTargetContext(),
                        LocalDatabase.class,
                        TEST_DB)
                .addMigrations(ALL_MIGRATIONS).build();
        appDb.getOpenHelper().getWritableDatabase();
        appDb.close();
    }

    // Array of all migrations.
    private static final Migration[] ALL_MIGRATIONS = new Migration[]{
            MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5};
}
