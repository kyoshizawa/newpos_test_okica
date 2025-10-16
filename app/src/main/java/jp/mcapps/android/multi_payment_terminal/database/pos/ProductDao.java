package jp.mcapps.android.multi_payment_terminal.database.pos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;


@Dao
public abstract class ProductDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insertProducts(List<ProductData> products);

    // レコードを全て削除する
    @Query("delete from pos_products")
    public abstract void deleteProducts();

    // 対象の世代のレコードを全て削除する
    @Query("delete from pos_products where generation_id = :generation_id")
    public abstract void deleteProductsByGenerationId(int generation_id);

    // 世代IDを書き換える
    @Query("UPDATE pos_products SET generation_id = :generation_to WHERE generation_id = :generation_from")
    public abstract void swapProductsGenerationId(int generation_from, int generation_to);

    //全ての商品を取得する
    @Query("select * from pos_products where generation_id = 1")
    public abstract List<ProductData> getProducts();

    //カテゴリに属していない商品のみを取得
    @Query("select * from pos_products where product_category_id is null and generation_id = 1")
    public abstract List<ProductData> getProductsWithOutCategory();

    //カテゴリに紐づく商品の一覧を取得
    @Query("select * from pos_products where product_category_id = :category_id and generation_id = 1")
    public abstract List<ProductData> getProductsByCategoryId(long category_id);

    //商品コードから商品を取得
    @Query("select * from pos_products where product_code = :code and generation_id = 1")
    public abstract List<ProductData> getProductsByCode(String code);
}
