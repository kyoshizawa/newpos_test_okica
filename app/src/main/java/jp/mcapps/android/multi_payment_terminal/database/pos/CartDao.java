package jp.mcapps.android.multi_payment_terminal.database.pos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public abstract class CartDao {
    //全件取得（表示順: 新しく追加された順）
    @Query("select * from pos_carts order by created_at desc")
    public abstract List<CartData> getAllProduct();

    //product_codeに紐づくカートの内容を取得
    @Query("select * from pos_carts where product_code = :code")
    public abstract List<CartData> getProductByProductCode(String code);

    // barcode_textに紐づくカートの内容を取得
    @Query("select * from pos_carts where barcode_text = :code")
    public abstract List<CartData> getProductByBarcode(String code);

    //カート挿入
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    public abstract void insertCartData(CartData cartData);

    // idを指定して、カウントを任意の値に変更する
    @Query("UPDATE pos_carts SET count = :newCount WHERE id = :id")
    public abstract void updateCountById(int id, int newCount);

    // idを指定して、単価を任意の値に変更する
    @Query("UPDATE pos_carts SET custom_unit_price = :newPrice, is_custom_price = 1 WHERE id = :id")
    public abstract void updateUnitPriceById(int id, int newPrice);

    // idを指定して、単価を標準単価に戻す
    @Query("UPDATE pos_carts SET is_custom_price = 0 WHERE id = :id")
    public abstract void resetUnitPriceById(int id);

    //全件削除
    @Query("delete from pos_carts")
    public abstract void deleteAll();

    // idを指定して、商品を削除する
    @Query("delete from pos_carts where id = :id")
    public abstract void deleteProduct(int id);
}
