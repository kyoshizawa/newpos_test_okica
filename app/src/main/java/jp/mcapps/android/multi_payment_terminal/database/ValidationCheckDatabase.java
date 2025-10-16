package jp.mcapps.android.multi_payment_terminal.database;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.database.validation_check.ValidationCheckHistoryData;
import jp.mcapps.android.multi_payment_terminal.database.validation_check.ValidationCheckDao;
import jp.mcapps.android.multi_payment_terminal.database.validation_check.ValidationCheckPaymentData;
import timber.log.Timber;

@Database(entities = {ValidationCheckHistoryData.class, ValidationCheckPaymentData.class}, version = 1, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class ValidationCheckDatabase extends RoomDatabase {
    public abstract ValidationCheckDao validationCheckDao();

    private static ValidationCheckDatabase _instance;

    public static ValidationCheckDatabase getInstance() {
        if (_instance == null) {
            synchronized (ValidationCheckDatabase.class) {
                if (_instance == null) {
                    _instance = Room.databaseBuilder(
                            MainApplication.getInstance().getApplicationContext(),
                            ValidationCheckDatabase.class,
                            "validation_check")
                            .build();
                }
            }
        }
        return _instance;
    }

    public static void closeDatabase() {
        if(_instance != null) {
            if (_instance.isOpen()) {
                deleteOldRecords();
                _instance.close();
                //インスタンス初期化
                _instance = null;
                Timber.d("Validation Check Database closed");
            }
        }
    }

    private static void deleteOldRecords() {
        //トランザクション
        _instance.runInTransaction(() -> {
            _instance.validationCheckDao().deletePosSentPayment(); //POS送信済の売上データが残っていれば削除 ダミーデータ用

            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MONTH, -1); //1ヶ月分まで保持
            _instance.validationCheckDao().deleteOldHistory(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.JAPANESE).format(calendar.getTime()));
        });
    }
}
