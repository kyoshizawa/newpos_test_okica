package jp.mcapps.android.multi_payment_terminal.database;

import android.database.sqlite.SQLiteException;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.database.history.aggregate.AggregateDao;
import jp.mcapps.android.multi_payment_terminal.database.history.aggregate.AggregateData;
import jp.mcapps.android.multi_payment_terminal.database.history.slip.SlipDao;
import jp.mcapps.android.multi_payment_terminal.database.history.slip.SlipData;
import jp.mcapps.android.multi_payment_terminal.database.history.uri.UriDao;
import jp.mcapps.android.multi_payment_terminal.database.history.uri.UriData;
import timber.log.Timber;

//CHG-S BMT S.Oyama 2024/11/26 フタバ双方向向け改修
//CHG-S BMT S.Oyama 2024/09/18 フタバ双方向向け改修
//@Database(entities = {SlipData.class, UriData.class, AggregateData.class}, version = 5, exportSchema = false)
//@Database(entities = {SlipData.class, UriData.class, AggregateData.class}, version = 6, exportSchema = false)
@Database(entities = {SlipData.class, UriData.class, AggregateData.class}, version = 7, exportSchema = false)
//CHG-E BMT S.Oyama 2024/09/18 フタバ双方向向け改修
//CHG-E BMT S.Oyama 2024/11/26 フタバ双方向向け改修
@TypeConverters({Converters.class})
public abstract class DemoDatabase extends RoomDatabase {
    public abstract SlipDao slipDao();
    public abstract UriDao uriDao();
    public abstract AggregateDao aggregateDao();

    private static DemoDatabase _demoDatabase;

    public static DemoDatabase getInstance() {
        if (_demoDatabase == null) {
            synchronized (LocalDatabase.class) {
                MainApplication.getInstance().getApplicationContext().deleteDatabase("demo_mode");
                if (_demoDatabase == null) {
                    _demoDatabase = Room.databaseBuilder(
                            MainApplication.getInstance().getApplicationContext(),
                            DemoDatabase.class,
                            "demo_mode")
                            .addMigrations(MIGRATION_1_2)
                            .addMigrations(MIGRATION_2_3)
                            .addMigrations(MIGRATION_3_4)
                            .addMigrations(MIGRATION_4_5)
                            //ADD-S BMT S.Oyama 2024/09/18 フタバ双方向向け改修
                            .addMigrations(MIGRATION_5_6)
                            //ADD-E BMT S.Oyama 2024/09/18 フタバ双方向向け改修
                            //ADD-S BMT S.Oyama 2024/11/26 フタバ双方向向け改修
                            .addMigrations(MIGRATION_6_7)
                            //ADD-E BMT S.Oyama 2024/11/26 フタバ双方向向け改修
                            .build();
                }
            }
        }
        return _demoDatabase;
    }

    public static void closeDatabase() {
        if(_demoDatabase != null) {
            if (_demoDatabase.isOpen()) {
                _demoDatabase.close();
                //インスタンス初期化
                _demoDatabase = null;
                Timber.d("Demo Database closed");
            }
        }
    }

    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE history_uri ADD COLUMN wallet TEXT");
        }
    };

    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            try {
                database.execSQL("ALTER TABLE history_slip ADD COLUMN credit_kid TEXT");
                database.execSQL("ALTER TABLE history_slip ADD COLUMN trans_complete_amount INTEGER");
            } catch (SQLiteException ex) {
                Timber.e(ex);
            }
        }
    };

    private static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            Timber.i("Demo DB migration ver3->4");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN printing_authid TEXT");
        }
    };

    private static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            Timber.i("Demo DB migration ver4->5");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN watari_point INTEGER");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN watari_sum_point INTEGER");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN watari_calidity_period TEXT");
        }
    };

    //ADD-S BMT S.Oyama 2024/09/18 フタバ双方向向け改修
    private static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            Timber.i("Demo DB migration ver5->6");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  trans_type_name TEXT");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  status TEXT");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  slip_kind INTEGER");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  pay_separation INTEGER");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  card_id_card_company TEXT");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  card_exp_date_merchant TEXT");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  card_exp_date_card_company TEXT");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  auth_id_str TEXT");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  cat_dual_type INTEGER");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  card_seq_no TEXT");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  atc TEXT");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  rw_id TEXT");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  sprw_id TEXT");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  off_on_type TEXT");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  card_type TEXT");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  card_id TEXT");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  point_yuko_msg TEXT");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  point_marchant INTEGER");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  point_total TEXT");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  point_exp_date TEXT");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  point_exp INTEGER");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  waon_trans_type_code INTEGER");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  card_slip_no INTEGER");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  lid INTEGER");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  service_name TEXT");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  card_trans_number_str TEXT");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  pay_id INTEGER");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  ic_no TEXT");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  old_ic_no TEXT");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  terminal_no TEXT");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  terminal_seq_no TEXT");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  terminal_id TEXT");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  edy_seq_no TEXT");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  input_kingaku INTEGER");
        }
    };
    //ADD-E BMT S.Oyama 2024/09/18 フタバ双方向向け改修

    //ADD-S BMT S.Oyama 2024/11/26 フタバ双方向向け改修
    private static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            Timber.i("Demo DB migration ver6->7");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN prepaid_add_point INTEGER");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN prepaid_total_point INTEGER");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN prepaid_next_expired TEXT");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN prepaid_next_expired_point INTEGER");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN prepaid_service_name TEXT");
        }
    };
    //ADD-E BMT S.Oyama 2024/11/26 フタバ双方向向け改修
}
