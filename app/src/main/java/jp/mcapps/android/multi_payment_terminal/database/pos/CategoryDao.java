package jp.mcapps.android.multi_payment_terminal.database.pos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;


@Dao
public abstract class CategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insertCategories(List<CategoryData> categories);

    // レコードを全て削除する
    @Query("delete from pos_categories")
    public abstract void deleteCategories();

    // 対象の世代のレコードを全て削除する
    @Query("delete from pos_categories where generation_id = :generation_id")
    public abstract void deleteCategoriesByGenerationId(int generation_id);

    // 世代IDを書き換える
    @Query("UPDATE pos_categories SET generation_id = :generation_to WHERE generation_id = :generation_from")
    public abstract void swapCategoriesGenerationId(int generation_from, int generation_to);

    //初期画面で選択できるカテゴリを取得
    @Query("select * from pos_categories where parent_id is null and generation_id = 1")
    public abstract List<CategoryData> getCategories();

    //サブカテゴリを取得する
    @Query("select * from pos_categories where parent_id = :parent_id and generation_id = 1")
    public abstract List<CategoryData> getCategoriesByParentId(long parent_id);
}
