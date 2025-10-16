package jp.mcapps.android.multi_payment_terminal.database.pos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public abstract class ServiceFunctionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insertServiceFunctions(ServiceFunctionData... functions);

    // 対象の世代のレコードを全て削除する
    @Query("DELETE FROM pos_service_functions WHERE generation_id = :generation_id")
    public abstract void deleteServiceFunctionsByGenerationId(int generation_id);

    // 世代IDを書き換える
    @Query("UPDATE pos_service_functions SET generation_id = :generation_to WHERE generation_id = :generation_from")
    public abstract void swapServiceFunctionsGenerationId(int generation_from, int generation_to);

    // 現在アクティブなサービス機能を取得する
    @Query("SELECT * FROM pos_service_functions WHERE generation_id = 1")
    public abstract ServiceFunctionData[] getServiceFunctions();

    public ServiceFunctionData getServiceFunction() {
        ServiceFunctionData[] items = getServiceFunctions();
        if (0 < items.length) {
            return items[0];
        }
        return null;
    }
}