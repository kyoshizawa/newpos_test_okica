package jp.mcapps.android.multi_payment_terminal.model.pos;

import android.util.Log;
import android.util.Pair;

import androidx.core.util.Consumer;

import com.google.common.base.Strings;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.database.LocalDatabase;
import jp.mcapps.android.multi_payment_terminal.database.pos.CartDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.CategoryDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.CategoryData;
import jp.mcapps.android.multi_payment_terminal.database.pos.GenerationIDs;
import jp.mcapps.android.multi_payment_terminal.database.pos.IncludedTaxTypes;
import jp.mcapps.android.multi_payment_terminal.database.pos.ProductDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.ProductData;
import jp.mcapps.android.multi_payment_terminal.database.pos.ProductTaxTypes;
import jp.mcapps.android.multi_payment_terminal.database.pos.ReducedTaxTypes;
import jp.mcapps.android.multi_payment_terminal.database.pos.ServiceFunctionDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.ServiceFunctionData;
import jp.mcapps.android.multi_payment_terminal.database.pos.TaxRates;
import jp.mcapps.android.multi_payment_terminal.database.pos.TenantDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.TenantData;
import jp.mcapps.android.multi_payment_terminal.database.pos.TerminalDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.TerminalData;
import jp.mcapps.android.multi_payment_terminal.error.DomainErrors;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.McPosCenterApi;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.McPosCenterApiImpl;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.TerminalInfo;
import jp.mcapps.android.multi_payment_terminal.webapi.paypf_pos.PaypfPosApi;
import jp.mcapps.android.multi_payment_terminal.webapi.paypf_pos.PaypfPosApiImpl;
import jp.mcapps.android.multi_payment_terminal.webapi.paypf_pos.PaypfStatusException;
import jp.mcapps.android.multi_payment_terminal.webapi.paypf_pos.data.AuthTest;
import jp.mcapps.android.multi_payment_terminal.webapi.paypf_pos.data.ListTenantProduct;
import jp.mcapps.android.multi_payment_terminal.webapi.paypf_pos.data.ListTenantProductCategory;
import jp.mcapps.android.multi_payment_terminal.webapi.paypf_pos.type.ProductCategory;
import jp.mcapps.android.multi_payment_terminal.webapi.paypf_pos.type.Tenant;
import jp.mcapps.android.multi_payment_terminal.webapi.paypf_pos.type.TenantProduct;
import timber.log.Timber;

public class ProductRepository {

    public ProductRepository() {
        _terminalDao = LocalDatabase.getInstance().terminalDao();
        _tenantDao = LocalDatabase.getInstance().tenantDao();
        _productDao = LocalDatabase.getInstance().productDao();
        _categoryDao = LocalDatabase.getInstance().categoryDao();
        _serviceFunctionDao = LocalDatabase.getInstance().serviceFunctionDao();
        _cartDao = LocalDatabase.getInstance().cartDao();
    }

    // network
    private final McPosCenterApi _apiClient = new McPosCenterApiImpl();
    private final PaypfPosApi _posApiClient = PaypfPosApiImpl.getInstance();

    // dao
    private final TerminalDao _terminalDao;
    private final TenantDao _tenantDao;
    private final ProductDao _productDao;
    private final CategoryDao _categoryDao;
    private final ServiceFunctionDao _serviceFunctionDao;
    private final CartDao _cartDao;

    private final int DEFAULT_FETCH_SIZE = 1000;

    // 商品マスタを更新する
    public void refreshProducts() throws IOException, DomainErrors.Exception, PaypfStatusException {
        LocalDatabase db = LocalDatabase.getInstance();

        // 残骸が残っていると嫌なので、ダウンロード済みレコードをクリアする
        db.runInTransaction(() -> doCleanup(GenerationIDs.DOWNLOADING.value));

        // 端末情報を取得する
        final TerminalData terminal = fetchTerminal();
        saveTerminal(terminal);

        // POSサービス機能を取得する (PCI環境)
        final ServiceFunctionData serviceFunction = fetchServiceFunction();
        saveServiceFunction(serviceFunction);

        // POSサービスインスタンスIDが必要
        final String serviceInstanceID = terminal.service_instance_pos;
        if (Strings.isNullOrEmpty(serviceInstanceID)) {
            DomainErrors.POS_SERVICE_INSTANCE_IS_NOT_ASSIGNED.raise("サービスインスタンスIDが空白です. 端末をPOSサービスに割り当ててください.");
        }

        // 店舗情報を取得する
        final TenantData tenant = fetchTenant(serviceInstanceID, serviceFunction.customer_code);
        saveTenant(tenant);
        AppPreference.posTenantSave(tenant);

        // 商品カテゴリ情報を取得する
        fetchCategories(serviceInstanceID, tenant.tenant_id, this::saveCategories);

        // 商品情報を取得する
        fetchProducts(serviceInstanceID, tenant.tenant_id, this::saveProducts);

        // ダウンロードしたコンテンツを有効化する
        db.runInTransaction(() -> {
            // 現在アクティブなレコードを全て削除
            doCleanup(GenerationIDs.CURRENTLY_ACTIVE.value);
            // ここでダウンロードしたレコードをアクティブにする
            doActivate();
            // カート内データは削除
            cleanupCart();
        });
    }

    // 端末情報を取得する
    TerminalData fetchTerminal() throws IOException {
        // call api
        final AuthTest.Response resp = _posApiClient.authTest();

        // 変換
        TerminalData item = new TerminalData();
        item.terminal_id = resp.sub;
        item.terminal_no = resp.terminal_no;
        item.customer_id = resp.customer_id;
        item.service_instance_abt = resp.service_instance_abt;
        item.service_instance_pos = resp.service_instance_pos;
        item.created_at = new Date();

        return item;
    }

    // 端末情報を保存する
    void saveTerminal(TerminalData item) {
        item.generation_id = GenerationIDs.DOWNLOADING.value;
        _terminalDao.insertTerminals(item);
    }

    // 店舗情報を取得する
    TenantData fetchTenant(String serviceInstanceID, String customerCode) throws IOException, PaypfStatusException {
        // call api
        final Tenant resp = _posApiClient.getTenantByCustomerCode(serviceInstanceID, customerCode);

        // 変換
        TenantData item = new TenantData();
        item.service_instance_id = serviceInstanceID;
        item.tenant_id = resp.id;
        item.tenant_code = resp.tenant_code;
        item.merchant_id = resp.merchant_id;
        item.customer_code = resp.customer_code;
        item.name = resp.name;
        item.name_kana = resp.name_kana;
        item.zipcode = resp.zipcode;
        item.pref_cd = resp.pref_cd;
        item.city = resp.city;
        item.address_line1 = resp.address_line1;
        item.address_line2 = resp.address_line2;
        item.address_line3 = resp.address_line3;
        item.kana_city = resp.kana_city;
        item.address_kana_line1 = resp.address_kana_line1;
        item.address_kana_line2 = resp.address_kana_line2;
        item.address_kana_line3 = resp.address_kana_line3;
        item.phone_number = resp.phone_number;
        item.fax = resp.fax;
        item.houjin_bangou = resp.houjin_bangou;
        item.alphabet_name = resp.alphabet_name;
        if(resp.parentInfo != null) {
            // 親情報がある場合はセット
            item.parent_name = resp.parentInfo.name;
        }
        item.created_at = new Date();

        return item;
    }

    // 店舗情報を保存する
    void saveTenant(TenantData item) {
        item.generation_id = GenerationIDs.DOWNLOADING.value;
        _tenantDao.insertTenants(item);
    }

    void fetchCategories(String serviceInstanceID, long tenantID, Consumer<CategoryData[]> thunk) throws IOException, PaypfStatusException {
        boolean continued = true;
        for (int offset = 0; continued; offset += DEFAULT_FETCH_SIZE) {
            final Pair<CategoryData[], Boolean> resp = fetchCategories(serviceInstanceID, tenantID, offset);
            thunk.accept(resp.first);
            continued = resp.second;
        }
    }

    // 商品カテゴリ情報を取得する
    Pair<CategoryData[], Boolean> fetchCategories(String serviceInstanceID, long tenantID, int offset) throws IOException, PaypfStatusException {
        // call api
        final ListTenantProductCategory.ResponseData data = _posApiClient.listTenantProductCategories(serviceInstanceID, tenantID, DEFAULT_FETCH_SIZE, offset);

        // 変換
        final List<CategoryData> list = new ArrayList<>();
        for (ProductCategory it: data.items) {
            CategoryData item = new CategoryData();

            item.category_id = it.id;
            item.service_instance_id = serviceInstanceID;
            item.name = it.name;
            item.name_kana = it.name_kana;
            item.name_short = it.name_short;
            // item.status = it.status; // TODO ... 一旦不要そうなので無視
            item.parent_id = it.parent_id;
            item.created_at = new Date();

            list.add(item);
        }

        CategoryData[] items = list.toArray(new CategoryData[0]);
        boolean hasNext = (offset + items.length) < data.total_count;

        return new Pair<>(items, hasNext);
    }

    // 商品カテゴリ情報を保存する
    void saveCategories(CategoryData[] items) {
        for (CategoryData it: items) {
            it.generation_id = GenerationIDs.DOWNLOADING.value;
        }
        _categoryDao.insertCategories(Arrays.asList(items));
    }

    void fetchProducts(String serviceInstanceID, long tenantID, Consumer<ProductData[]> thunk) throws IOException, PaypfStatusException {
        boolean continued = true;
        for (int offset = 0; continued; offset += DEFAULT_FETCH_SIZE) {
            final Pair<ProductData[], Boolean> resp = fetchProducts(serviceInstanceID, tenantID, offset);
            thunk.accept(resp.first);
            continued = resp.second;
        }
    }

    // 商品情報を取得する
    Pair<ProductData[], Boolean> fetchProducts(String serviceInstanceID, long tenantID, int offset) throws IOException, PaypfStatusException {
        // call api
        final ListTenantProduct.ResponseData data = _posApiClient.listTenantProducts(serviceInstanceID, tenantID, DEFAULT_FETCH_SIZE, offset);

        // 変換
        final List<ProductData> list = new ArrayList<>();
        for (TenantProduct it: data.items) {

            final ProductData item = new ProductData();
            item.product_id = it.id;
            item.service_instance_id = serviceInstanceID;
            item.product_code = it.product_code;
            item.name = it.name;
            item.name_kana = it.name_kana;
            item.name_short = it.name_short;
            item.standard_unit_price = it.standard_unit_price;
            item.tax_type = ProductTaxTypes.fromKey(it.tax_type).value;
            item.reduce_tax_type = ReducedTaxTypes.fromKey(it.reduced_tax_type).value;
            item.included_tax_type = IncludedTaxTypes.fromKey(it.included_tax_type).value;
            item.sale_start_at = it.sale_start_at;
            item.sale_end_at = it.sale_end_at;
            // item.status = it.status; // TODO ... 一旦不要そうなので無視
            item.remarks = it.remarks;
            item.product_category_id = it.product_category_id;
            item.created_at = new Date();

            list.add(item);
        }

        ProductData[] items = list.toArray(new ProductData[0]);
        boolean hasNext = (offset + items.length) < data.total_count;

        return new Pair<>(items, hasNext);
    }

    // 商品情報を保存する
    void saveProducts(ProductData[] items) {
        for (ProductData it: items) {
            it.generation_id = GenerationIDs.DOWNLOADING.value;
        }
        _productDao.insertProducts(Arrays.asList(items));
    }

    // POSサービス機能を取得する
    ServiceFunctionData fetchServiceFunction() throws IOException, DomainErrors.Exception, PaypfStatusException {

        // POST /Term/GetInfo
        final TerminalInfo.Response resp = _apiClient.getTerminalInfo();

        if (resp.result) {
            // OK
            final TerminalInfo.PosServiceFunc posServiceFunc = resp.posServiceFunc;
            if (posServiceFunc == null) {
                DomainErrors.FAILED_PRECONDITION.raise("POSサービス機能がNULL"); // POSの設定がない？？
            }

            // 受信データを詰め込む
            final ServiceFunctionData item = new ServiceFunctionData();
            item.customer_code = resp.supplierCd;
            item.is_product_category = posServiceFunc.isProductCategory;
            item.is_pos_receipt = posServiceFunc.isPosReceipt;
            item.is_manual_amount = posServiceFunc.isManualAmount;
            item.slip_title = posServiceFunc.slipTitle;
            item.tax_rounding = posServiceFunc.taxRounding;
            for (TerminalInfo.TaxRate it: posServiceFunc.taxList) {
                Log.d("TAX----" , String.valueOf(it.tax));
                if (it.taxClass == TaxRates.STANDARD_TAX_RATE.value) {
                    item.standard_tax_rate = String.valueOf(it.tax);
                    Log.d("TAXIN----" , String.valueOf(item.standard_tax_rate));
                }
                if (it.taxClass == TaxRates.REDUCED_TAX_RATE.value) {
                    item.reduced_tax_rate = String.valueOf(it.tax);
                    Log.d("TAXIN----" , String.valueOf(item.reduced_tax_rate));
                }
            }
            item.receipt_count = posServiceFunc.receiptCounts;

            return item;
        }

        // ERROR
        throw new PaypfStatusException(DomainErrors.INTERNAL.code, "error code: " + resp.errorCode);
    }

    // POSサービス機能を保存する
    void saveServiceFunction(ServiceFunctionData item) {
        item.generation_id = GenerationIDs.DOWNLOADING.value;
        Log.d("TAX INCLU----", String.valueOf(item.reduced_tax_rate));
        Log.d("TAX INCLU----", String.valueOf(item.standard_tax_rate));
        _serviceFunctionDao.insertServiceFunctions(item);
    }

    // カート内データを削除する
    void cleanupCart() {
        _cartDao.deleteAll();
    }

    // 対象の世代のレコードを削除する
    void doCleanup(int generationID) {
        _terminalDao.deleteTerminalsByGenerationId(generationID);
        _tenantDao.deleteTenantsByGenerationId(generationID);
        _productDao.deleteProductsByGenerationId(generationID);
        _categoryDao.deleteCategoriesByGenerationId(generationID);
        _serviceFunctionDao.deleteServiceFunctionsByGenerationId(generationID);
    }

    // ダウンロードしたレコードを有効にする
    void doActivate() {
        int srcID = GenerationIDs.DOWNLOADING.value;
        int dstID = GenerationIDs.CURRENTLY_ACTIVE.value;
        _terminalDao.swapTerminalsGenerationId(srcID, dstID);
        _tenantDao.swapTenantsGenerationId(srcID, dstID);
        _productDao.swapProductsGenerationId(srcID, dstID);
        _categoryDao.swapCategoriesGenerationId(srcID, dstID);
        _serviceFunctionDao.swapServiceFunctionsGenerationId(srcID, dstID);
    }
}
