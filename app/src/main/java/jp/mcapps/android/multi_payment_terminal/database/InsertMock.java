package jp.mcapps.android.multi_payment_terminal.database;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jp.mcapps.android.multi_payment_terminal.BuildConfig;
import jp.mcapps.android.multi_payment_terminal.database.pos.CategoryData;
import jp.mcapps.android.multi_payment_terminal.database.pos.GenerationIDs;
import jp.mcapps.android.multi_payment_terminal.database.pos.ProductData;
import timber.log.Timber;

// 試験用データの投入用クラス
// 本番では使用されない
public class InsertMock {

    // POS 商品データとカテゴリデータを格納
    public static void CreateProductCategoryMock(){
        // デバッグビルドじゃなかったら動作しない
        if (!BuildConfig.DEBUG) {
            return;
        }

        // 既に商品が格納済みなら動作しない
        List<ProductData> _products = DBManager.getProductDao().getProducts();
        if (_products.size() > 0){
            return;
        }
        Timber.d("デバッグモード且つ商品が格納されていないため、Mockを格納");

        // 念の為、すべてのデータを削除
        DBManager.getProductDao().deleteProducts();
        DBManager.getCategoryDao().deleteCategories();

        //
        // 商品カテゴリデータ
        //

        List<CategoryData> _categories = new ArrayList<>(); // insert用データリスト

        CategoryData category1 = new CategoryData();
        category1.category_id = 1L;
        category1.service_instance_id = "service1";
        category1.name = "食料品";
        category1.name_kana = "ｼｮｸﾘｮｳﾋﾝ";
        category1.name_short = "食品";
        category1.status = 1; // 有効
        category1.parent_id = null;
        category1.created_at = new Date();
        _categories.add(category1);
        Timber.d("category1追加：%s", String.valueOf(_categories));

        CategoryData category2 = new CategoryData();
        category2.category_id = 2L;
        category2.service_instance_id = "service1";
        category2.name = "貸会議室・屋外コート";
        category2.name_kana = "ｶｼｶｲｷﾞｼﾂｵｸｶﾞｲｺｰﾄ";
        category2.name_short = "貸場所";
        category2.status = 1; // 有効
        category2.parent_id = null;
        category2.created_at = new Date();
        _categories.add(category2);
        Timber.d("category2追加：%s", String.valueOf(_categories));

        CategoryData category3 = new CategoryData();
        category3.category_id = 3L;
        category3.service_instance_id = "service1";
        category3.name = "食良品：惣菜";
        category3.name_kana = "ｼｮｸﾋﾝｿｳｻﾞｲ";
        category3.name_short = "ソウザイ";
        category3.status = 1; // 有効
        category3.parent_id = 1L;
        category3.created_at = new Date();
        _categories.add(category3);
        Timber.d("category3追加：%s", String.valueOf(_categories));

        CategoryData category4 = new CategoryData();
        category4.category_id = 4L;
        category4.service_instance_id = "service1";
        category4.name = "食品:野菜";
        category4.name_kana = "ｼｮｸﾋﾝﾔｻｲ";
        category4.name_short = "ﾔｻｲ";
        category4.status = 1; // 有効
        category4.parent_id = 1L;
        category4.created_at = new Date();
        _categories.add(category4);
        Timber.d("category4追加：%s", String.valueOf(_categories));

        CategoryData category5 = new CategoryData();
        category5.category_id = 5L;
        category5.service_instance_id = "service1";
        category5.name = "野菜:赤";
        category5.name_kana = "ﾔｻｲｱｶ";
        category5.name_short = "ｱｶ";
        category5.status = 1; // 有効
        category5.parent_id = 4L;
        category5.created_at = new Date();
        _categories.add(category5);
        Timber.d("category5追加：%s", String.valueOf(_categories));

        CategoryData category6 = new CategoryData();
        category6.category_id = 6L;
        category6.service_instance_id = "service1";
        category6.name = "野菜:緑";
        category6.name_kana = "ﾔｻｲﾐﾄﾞﾘ";
        category6.name_short = "ﾐﾄﾞﾘ";
        category6.status = 1; // 有効
        category6.parent_id = 4L;
        category6.created_at = new Date();
        _categories.add(category6);
        Timber.d("category6追加：%s", String.valueOf(_categories));

        DBManager.getCategoryDao().insertCategories(_categories);
        DBManager.getCategoryDao().swapCategoriesGenerationId(
                GenerationIDs.DOWNLOADING.value,
                GenerationIDs.CURRENTLY_ACTIVE.value);

        //
        // 商品データ
        //

        List<ProductData> _productDataList = new ArrayList<>(); // insert用データリスト

        // もっく１
        ProductData productData = new ProductData();
        productData.product_id = 1L;
        productData.service_instance_id = "service1";
        productData.product_code = "4910010000012";
        productData.name = "【軽減外税】りんご ３個入り";
        productData.name_kana = "ﾘﾝｺﾞ 3ｺｲﾘ";
        productData.name_short = "りんご";
        productData.standard_unit_price = 780;
        productData.tax_type = 1; // 課税
        productData.reduce_tax_type = 3; // 軽減税率
        productData.included_tax_type = 2; // 外税
        productData.sale_start_at = new Date();
        productData.sale_end_at = new Date();
        productData.status = 1; // 有効
        productData.remarks = "備考";
        productData.product_category_id = 1L;
        productData.created_at = new Date();
        _productDataList.add(productData);

        // もっく２
        productData = new ProductData();
        productData.product_id = 2L;
        productData.service_instance_id = "service1";
        productData.product_code = "4910010000029";
        productData.name = "【軽減外税】スタミナ弁当";
        productData.name_kana = "ｽﾀﾐﾅﾍﾞﾝﾄｳ";
        productData.name_short = "弁当";
        productData.standard_unit_price = 480;
        productData.tax_type = 1; // 課税
        productData.reduce_tax_type = 3; // 軽減税率
        productData.included_tax_type = 2; // 外税
        productData.sale_start_at = new Date();
        productData.sale_end_at = new Date();
        productData.status = 1; // 有効
        productData.remarks = "備考";
        productData.product_category_id = 1L;
        productData.created_at = new Date();
        _productDataList.add(productData);

        // もっく３
        productData = new ProductData();
        productData.product_id = 3L;
        productData.service_instance_id = "service1";
        productData.product_code = "4910010000036";
        productData.name = "【軽減内税】すたみな太郎弁当";
        productData.name_kana = "ｽﾀﾐﾅﾀﾛｳﾍﾞﾝﾄｳ";
        productData.name_short = "太弁当";
        productData.standard_unit_price = 510;
        productData.tax_type = 1; // 課税
        productData.reduce_tax_type = 3; // 軽減税率
        productData.included_tax_type = 1; // 内税
        productData.sale_start_at = new Date();
        productData.sale_end_at = new Date();
        productData.status = 1; // 有効
        productData.remarks = "備考";
        productData.product_category_id = 1L;
        productData.created_at = new Date();
        _productDataList.add(productData);

        // もっく４
        productData = new ProductData();
        productData.product_id = 4L;
        productData.service_instance_id = "service1";
        productData.product_code = "4910010000043";
        productData.name = "【軽減内税】すたみな太郎大盛弁当";
        productData.name_kana = "ｽﾀﾐﾅﾀﾛｳｵｵﾓﾘﾍﾞﾝﾄｳ";
        productData.name_short = "太盛弁当";
        productData.standard_unit_price = 640;
        productData.tax_type = 1; // 課税
        productData.reduce_tax_type = 3; // 軽減税率
        productData.included_tax_type = 1; // 内税
        productData.sale_start_at = new Date();
        productData.sale_end_at = new Date();
        productData.status = 1; // 有効
        productData.remarks = "備考";
        productData.product_category_id = 1L;
        productData.created_at = new Date();
        _productDataList.add(productData);

        // もっく５
        productData = new ProductData();
        productData.product_id = 5L;
        productData.service_instance_id = "service1";
        productData.product_code = "4910020000019";
        productData.name = "【一般内税】３階　会議室１";
        productData.name_kana = "3ｶｲ ｶｲｷﾞｼﾂ1";
        productData.name_short = "３０１";
        productData.standard_unit_price = 500;
        productData.tax_type = 1; // 課税
        productData.reduce_tax_type = 2; // 一般税率
        productData.included_tax_type = 1; // 内税
        productData.sale_start_at = new Date();
        productData.sale_end_at = new Date();
        productData.status = 1; // 有効
        productData.remarks = "備考";
        productData.product_category_id = 2L;
        productData.created_at = new Date();
        _productDataList.add(productData);

        // もっく６
        productData = new ProductData();
        productData.product_id = 6L;
        productData.service_instance_id = "service1";
        productData.product_code = "4910020000026";
        productData.name = "【一般内税】３階　会議室２";
        productData.name_kana = "3ｶｲ ｶｲｷﾞｼﾂ2";
        productData.name_short = "３０２";
        productData.standard_unit_price = 1000;
        productData.tax_type = 1; // 課税
        productData.reduce_tax_type = 2; // 一般税率
        productData.included_tax_type = 1; // 内税
        productData.sale_start_at = new Date();
        productData.sale_end_at = new Date();
        productData.status = 1; // 有効
        productData.remarks = "備考";
        productData.product_category_id = 2L;
        productData.created_at = new Date();
        _productDataList.add(productData);

        // もっく７
        productData = new ProductData();
        productData.product_id = 7L;
        productData.service_instance_id = "service1";
        productData.product_code = "4910020000033";
        productData.name = "【一般外税】屋外テニスコート";
        productData.name_kana = "ｵｸｶﾞｲﾃﾆｽｺｰﾄ";
        productData.name_short = "テニス";
        productData.standard_unit_price = 1000;
        productData.tax_type = 1; // 課税
        productData.reduce_tax_type = 2; // 一般税率
        productData.included_tax_type = 2; // 外税
        productData.sale_start_at = new Date();
        productData.sale_end_at = new Date();
        productData.status = 1; // 有効
        productData.remarks = "備考";
        productData.product_category_id = 2L;
        productData.created_at = new Date();
        _productDataList.add(productData);

        // もっく８
        productData = new ProductData();
        productData.product_id = 8L;
        productData.service_instance_id = "service1";
        productData.product_code = "4910000000015";
        productData.name = "【軽減内税】ざびえる１２個入り";
        productData.name_kana = "ｻﾞﾋﾞｴﾙ12ｺｲﾘ";
        productData.name_short = "ざびえる";
        productData.standard_unit_price = 1080;
        productData.tax_type = 1; // 課税
        productData.reduce_tax_type = 3; // 軽減税率
        productData.included_tax_type = 1; // 内税
        productData.sale_start_at = new Date();
        productData.sale_end_at = new Date();
        productData.status = 1; // 有効
        productData.remarks = "備考";
        productData.product_category_id = null;
        productData.created_at = new Date();
        _productDataList.add(productData);

        // もっく９
        productData = new ProductData();
        productData.product_id = 9L;
        productData.service_instance_id = "service1";
        productData.product_code = "4910000000022";
        productData.name = "【軽減内税】瑠異沙１２個入り";
        productData.name_kana = "ﾙｲｻ12ｺｲﾘ";
        productData.name_short = "瑠異沙";
        productData.standard_unit_price = 1188;
        productData.tax_type = 1; // 課税
        productData.reduce_tax_type = 3; // 軽減税率
        productData.included_tax_type = 1; // 内税
        productData.sale_start_at = new Date();
        productData.sale_end_at = new Date();
        productData.status = 1; // 有効
        productData.remarks = "備考";
        productData.product_category_id = null;
        productData.created_at = new Date();
        _productDataList.add(productData);

        // もっく１０
        productData = new ProductData();
        productData.product_id = 10L;
        productData.service_instance_id = "service1";
        productData.product_code = "4910000000039";
        productData.name = "【一般内税】一の井出７２０ｍｌ";
        productData.name_kana = "ｲﾁﾉｲﾃ720mlﾞ";
        productData.name_short = "一の井出";
        productData.standard_unit_price = 1511;
        productData.tax_type = 1; // 課税
        productData.reduce_tax_type = 2; // 一般税率
        productData.included_tax_type = 1; // 内税
        productData.sale_start_at = new Date();
        productData.sale_end_at = new Date();
        productData.status = 1; // 有効
        productData.remarks = "備考";
        productData.product_category_id = null;
        productData.created_at = new Date();
        _productDataList.add(productData);

        // もっく１１
        productData = new ProductData();
        productData.product_id = 11L;
        productData.service_instance_id = "service1";
        productData.product_code = "4910000000046";
        productData.name = "【軽減外税】ハーシーチョコシロップボトル６２３ｇ";
        productData.name_kana = "ﾊｰｼｰﾁｮｺｼﾛｯﾌﾟﾎﾞﾄﾙ623g";
        productData.name_short = "チョコシロ";
        productData.standard_unit_price = 1011;
        productData.tax_type = 1; // 課税
        productData.reduce_tax_type = 3; // 軽減税率
        productData.included_tax_type = 2; // 外税
        productData.sale_start_at = new Date();
        productData.sale_end_at = new Date();
        productData.status = 1; // 有効
        productData.remarks = "備考";
        productData.product_category_id = null;
        productData.created_at = new Date();
        _productDataList.add(productData);

        // もっく１２
        productData = new ProductData();
        productData.product_id = 12L;
        productData.service_instance_id = "service1";
        productData.product_code = "4910000000053";
        productData.name = "【一般外税】ジャックダニエルバーボンウイスキー７００ｍｌ";
        productData.name_kana = "ｼﾞｬｯｸﾀﾞﾆｴﾙﾊﾞｰﾎﾞﾝｳｲｽｷｰ700ml";
        productData.name_short = "ウイスキー";
        productData.standard_unit_price = 2322;
        productData.tax_type = 1; // 課税
        productData.reduce_tax_type = 2; // 一般税率
        productData.included_tax_type = 2; // 外税
        productData.sale_start_at = new Date();
        productData.sale_end_at = new Date();
        productData.status = 1; // 有効
        productData.remarks = "備考";
        productData.product_category_id = null;
        productData.created_at = new Date();
        _productDataList.add(productData);

        // もっく１３
        productData = new ProductData();
        productData.product_id = 13L;
        productData.service_instance_id = "service1";
        productData.product_code = "4910000000060";
        productData.name = "【非課税】社用車借用";
        productData.name_kana = "ｼｬﾖｳｼｬｼｬｸﾖｳ";
        productData.name_short = "社用車";
        productData.standard_unit_price = 110;
        productData.tax_type = 2; // 非課税
        productData.reduce_tax_type = 1; // 非課税
        productData.included_tax_type = 0; // 未設定
        productData.sale_start_at = new Date();
        productData.sale_end_at = new Date();
        productData.status = 1; // 有効
        productData.remarks = "備考";
        productData.product_category_id = null;
        productData.created_at = new Date();
        _productDataList.add(productData);

        productData = new ProductData();
        productData.product_id = 14L;
        productData.service_instance_id = "service1";
        productData.product_code = "23123101203";
        productData.name = "【非課税】いちご";
        productData.name_kana = "ｲﾁｺﾞ";
        productData.name_short = "ｲﾁｺﾞ";
        productData.standard_unit_price = 110;
        productData.tax_type = 2; // 非課税
        productData.reduce_tax_type = 1; // 非課税
        productData.included_tax_type = 0; // 未設定
        productData.sale_start_at = new Date();
        productData.sale_end_at = new Date();
        productData.status = 1; // 有効
        productData.remarks = "備考";
        productData.product_category_id = 5L;
        productData.created_at = new Date();
        _productDataList.add(productData);

        productData = new ProductData();
        productData.product_id = 14L;
        productData.service_instance_id = "service1";
        productData.product_code = "23123101203";
        productData.name = "【非課税】リンゴ";
        productData.name_kana = "ﾘﾝｺﾞ";
        productData.name_short = "ﾘﾝｺﾞ";
        productData.standard_unit_price = 120;
        productData.tax_type = 2; // 非課税
        productData.reduce_tax_type = 1; // 非課税
        productData.included_tax_type = 0; // 未設定
        productData.sale_start_at = new Date();
        productData.sale_end_at = new Date();
        productData.status = 1; // 有効
        productData.remarks = "備考";
        productData.product_category_id = 5L;
        productData.created_at = new Date();
        _productDataList.add(productData);

        DBManager.getProductDao().insertProducts(_productDataList);
        DBManager.getProductDao().swapProductsGenerationId(
                GenerationIDs.DOWNLOADING.value,
                GenerationIDs.CURRENTLY_ACTIVE.value);
    }
}
