package jp.mcapps.android.multi_payment_terminal.database.pos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public abstract class TenantDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insertTenants(TenantData... tenants);

    // 対象の世代のレコードを全て削除する
    @Query("DELETE FROM pos_tenants WHERE generation_id = :generation_id")
    public abstract void deleteTenantsByGenerationId(int generation_id);

    // 世代IDを書き換える
    @Query("UPDATE pos_tenants SET generation_id = :generation_to WHERE generation_id = :generation_from")
    public abstract void swapTenantsGenerationId(int generation_from, int generation_to);

    // 現在アクティブなテナントを取得する
    @Query("SELECT * FROM pos_tenants WHERE generation_id = 1")
    public abstract TenantData[] getTenants();

    public TenantData getTenant() {
        TenantData[] items = getTenants();
        if (0 < items.length) {
            return items[0];
        }
        return null;
    }
}
