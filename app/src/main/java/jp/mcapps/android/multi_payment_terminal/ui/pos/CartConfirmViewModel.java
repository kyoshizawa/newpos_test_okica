package jp.mcapps.android.multi_payment_terminal.ui.pos;

import android.annotation.SuppressLint;
import android.widget.Toast;

import androidx.annotation.RawRes;
import androidx.core.util.Pair;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.common.base.Strings;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.CommonClickEvent;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.Result;
import jp.mcapps.android.multi_payment_terminal.data.Amount;
import jp.mcapps.android.multi_payment_terminal.database.DBManager;
import jp.mcapps.android.multi_payment_terminal.database.pos.CartDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.CartData;
import jp.mcapps.android.multi_payment_terminal.database.pos.IncludedTaxTypes;
import jp.mcapps.android.multi_payment_terminal.database.pos.ProductDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.ProductData;
import jp.mcapps.android.multi_payment_terminal.database.pos.ProductTaxTypes;
import jp.mcapps.android.multi_payment_terminal.database.pos.ReducedTaxTypes;
import jp.mcapps.android.multi_payment_terminal.database.pos.ServiceFunctionDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.ServiceFunctionData;
import jp.mcapps.android.multi_payment_terminal.database.pos.TaxCalcBuilder;
import jp.mcapps.android.multi_payment_terminal.database.pos.TaxCalcDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.TaxCalcData;
import jp.mcapps.android.multi_payment_terminal.error.DomainErrors;
import jp.mcapps.android.multi_payment_terminal.model.SoundManager;
import jp.mcapps.android.multi_payment_terminal.model.pos.CartRepository;
import jp.mcapps.android.multi_payment_terminal.util.JSON;
import timber.log.Timber;

public class CartConfirmViewModel extends ViewModel {
    private final long _delayMillis = 1000;
    private long _pushedMillis = 0;
    private final CartRepository _cartRepository = new CartRepository();
    private final CartDao cartDao = DBManager.getCartDao();
    private final ProductDao productDao = DBManager.getProductDao();
    private final ServiceFunctionDao serviceFunctionDao = DBManager.getServiceFunctionDao();

    private final SoundManager _soundManager = SoundManager.getInstance();

    private final MutableLiveData<List<CartModel>> data = new MutableLiveData<>();
    public LiveData<List<CartModel>> getData() {
        return data;
    }

    private final MutableLiveData<String> countAmount = new MutableLiveData<>("-");
    public LiveData<String> getCountAmount() {
        return countAmount;
    }

    private final MutableLiveData<String> priceAmount = new MutableLiveData<>("-");
    public LiveData<String> getPriceAmount() {
        return priceAmount;
    }

    private final MutableLiveData<String> errorMessage = new MutableLiveData<>("");
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    public void setErrorMessage(String message) {
        errorMessage.setValue(message);
    }

    List<CartData> getAllCartProduct() {
        return cartDao.getAllProduct();
    }

    private TaxCalcData taxCalcData = new TaxCalcData();

    public TaxCalcData getTaxCalcData(){
        return taxCalcData;
    }

    private ProductTaxTypes  _productTaxType = ProductTaxTypes.TAX;

    public ProductTaxTypes getProductTaxType() {
        return _productTaxType;
    }

    public void setProductTaxType(ProductTaxTypes productTaxType) {
        this._productTaxType = productTaxType;
    }

    private ReducedTaxTypes  reducedTaxType=ReducedTaxTypes.GENERAL;

    public ReducedTaxTypes getReducedTaxType() {
        return reducedTaxType;
    }

    public void setReducedTaxType(ReducedTaxTypes reducedTaxType) {
        this.reducedTaxType = reducedTaxType;
    }

    private IncludedTaxTypes includedTaxType = IncludedTaxTypes.EXCLUDED;

    public IncludedTaxTypes getIncludedTaxType() {
        return includedTaxType;
    }

    public void setIncludedTaxType(IncludedTaxTypes includedTaxType) {
        this.includedTaxType = includedTaxType;
    }

    // 初回取得
    @SuppressLint("CheckResult")
    public void fetchData() {
        Observable.fromCallable(() -> {
                    // データを非同期に取得するメソッドを呼び出す
                    List<CartData> cartData = getAllCartProduct();
                    ServiceFunctionData serviceFunc = serviceFunctionDao.getServiceFunction();
                    return new Pair<>(cartData, serviceFunc);
                })
                .subscribeOn(Schedulers.single())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {
                            // データ取得が成功した場合の処理
                            List<CartModel> _cartModelList = convertDisplayModel(result.first);
                            data.setValue(_cartModelList);
                            calculateCartResult(result);
                            checkPaymentOverdue(result.first);
                        },
                        error -> {
                            // エラーハンドリングの処理
                            Timber.e(error);
                            Timber.e("「処理失敗しました」表示");
                            Toast.makeText(MainApplication.getInstance(), "処理失敗しました", Toast.LENGTH_LONG).show();
                        }
                );
    }

    public Observable<List<CartModel>> initFetchData() {
        return Observable.fromCallable(() -> {
                List<CartData> result = getAllCartProduct();
                List<CartModel> _cartModelList = convertDisplayModel(result);
                return _cartModelList;

                })  // データを非同期に取得するメソッドを呼び出す
                .subscribeOn(Schedulers.single())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public void deleteAllCart() {
        CommonClickEvent.RecordClickOperation("全て削除", true);
        // 連続タップ防止
        long timeMillis = System.currentTimeMillis();
        if (timeMillis - _pushedMillis < _delayMillis)
        {
            Timber.e("１秒以内に連続タップ発生");
            return;
        }
        _pushedMillis = timeMillis;
        //Timber.d("push time millis:%s",_pushedMillis);

        Observable.fromCallable(() -> {
                DBManager.getCartDao().deleteAll();
                return true;
            })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                    result -> {
                       fetchData();
                    },
                    error -> {
                        Timber.e(error);
                        Timber.e("「処理失敗しました」表示");
                        Toast.makeText(MainApplication.getInstance(), "処理失敗しました", Toast.LENGTH_LONG).show();
                    }
            );

    }

    /**
     * 指定した商品コードをカートに追加する
     *
     * @param productCode 商品コード
     */
    public Disposable setProduct(String productCode) {
        return Observable.fromCallable(() -> {
                    // 商品をカートに追加
                    Result<CartData, DomainErrors> result = _cartRepository.insertProduct(productCode);
                    if (result.isOk()) {
                        makeSound(R.raw.pos_scan);
                        return true;
                    } else {
                        String msg;
                        switch (result.err) {
                            case NOT_FOUND:
                                // 読み取った商品コードがマスタにない
                                msg = "商品が見つかりませんでした";
                                errorMessage.postValue(msg);    // 非同期に設定
                                Timber.w(msg + ":" + productCode);
                                makeSound(R.raw.pos_ng);
                                break;

                            case OUT_OF_RANGE:
                                // カートに入れられる上限に達している
                                msg = "999以上は設定できません";
                                errorMessage.postValue(msg);    // 非同期に設定
                                Timber.w(msg + ":" + productCode);
                                makeSound(R.raw.pos_ng);
                                break;
                        }
                        return false;
                    }
                }).
                subscribeOn(Schedulers.io()).
                observeOn(AndroidSchedulers.mainThread()).
                subscribe(
                        result -> {
                            Timber.d("setProduct:succeed");
                            fetchData();
                        },
                        error -> {
                            Timber.e(error);
                            Timber.e("「処理失敗しました」表示");
                            Toast.makeText(MainApplication.getInstance(), "処理失敗しました", Toast.LENGTH_LONG).show();
                        }
                );
    }

    /**
     * 指定した商品コードをカートに追加する
     * @param productCode 商品コード
     */
    private void xxx_setProduct(String productCode) {
        Observable.fromCallable(() -> {
                    ProductData productData = null;

                    // DBから当該商品IDを探す
                    List<ProductData> products = productDao.getProductsByCode(productCode);
                    if(products.size() > 0){
                        productData = products.get(0);
                    }

                    if(productData != null) {
                        // 商品をカートに追加
                        List<CartData> carts = cartDao.getProductByProductCode(productCode);
                        if(carts.size() > 0) {
                            CartData cart = carts.get(0);
                            try {
                                // 商品の加算
                                cart.Increment();
                                cartDao.updateCountById(cart.id, cart.count);
                            } catch (DomainErrors.Exception ex) {
                                // カートに入れられる上限に達している
                                String msg = "999以上は設定できません";
                                errorMessage.postValue(msg);    // 非同期に設定
                                Timber.w(msg+ ":" + cart.count);
                                makeSound(R.raw.pos_ng);
                                return false;
                            }
                        } else {
                            // 商品の新規追加
                            CartData cart = new CartData(productData);
                            cartDao.insertCartData(cart);
                        }
                        makeSound(R.raw.pos_scan);
                        return true;
                    } else {
                        // 読み取った商品コードがマスタにない
                        String msg = "商品が見つかりませんでした";
                        errorMessage.postValue(msg);    // 非同期に設定
                        Timber.w(msg + ":" + productCode);
                        makeSound(R.raw.pos_ng);
                        return false;
                    }
            })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                    result -> {
                        Timber.d("setProduct:succeed");
                        fetchData();
                    },
                    error -> {
                        Timber.e(error);
                        Timber.e("「処理失敗しました」表示");
                        Toast.makeText(MainApplication.getInstance(), "処理失敗しました", Toast.LENGTH_LONG).show();
                    }
            );
    }

    private void makeSound(@RawRes int soundId) {
        // 決済音の音量を設定
        float volume = AppPreference.getSoundPaymentVolume() / 10f;
        _soundManager.load(MainApplication.getInstance(), soundId, 1);

        _soundManager.setOnLoadCompleteListener((soundPool, sampleId, status) -> {
                _soundManager.play(sampleId, volume, volume, 1, 0, 1);
        });
    }

    // RecyclerViewに表示するためのListに変換するメソッド
    private List<CartModel> convertDisplayModel(List<CartData> dataList){
        SimpleDateFormat dueDateFormat = new SimpleDateFormat("M/d", Locale.JAPANESE);

        // データの結合処理を行う
        List<CartModel> _cartModelList = new ArrayList<>();

        // 商品が存在するカテゴリーを表示用リストに詰めるのと合計金額の精算
        for (CartData item : dataList) {
            CartModel cartModel = new CartModel();
            cartModel.id = item.id;
            cartModel.name = item.getDisplayProductName();
            cartModel.code = item.product_code;
            if (item.is_custom_price) {
                cartModel.unitPrice = item.custom_unit_price;
            } else {
                cartModel.unitPrice = item.standard_unit_price;
            }
            cartModel.displayUnitPrice = formatNumberAsCurrency(cartModel.unitPrice);
            cartModel.count = item.count;
            cartModel.isCustomPrice = item.is_custom_price;
            cartModel.isCountEditable = Strings.isNullOrEmpty(item.barcode_type); // バーコードの場合は個数変更不可
            cartModel.isPriceEditable = Strings.isNullOrEmpty(item.barcode_type); // バーコードの場合は単価変更不可
            _cartModelList.add(cartModel);

            // コンビニ収納用バーコード or 地方統一税QR の場合は支払期限を表示
            if (item.barcode_type != null) {
                final List<Pair<Date, Integer>> dueDate = new ArrayList<>();
                if (item.payment_due_date != null) {
                    dueDate.add(new Pair<>(item.payment_due_date, 1));
                }
                if (item.filing_due_date != null) {
                    dueDate.add(new Pair<>(item.filing_due_date, 2));
                }
                Collections.sort(dueDate, (a, b) -> b.first.compareTo(a.first));
                if (!dueDate.isEmpty()) {
                    // 大きい方の期限を表示
                    Pair<Date, Integer> pair = dueDate.get(0);
                    String prefix = pair.second == 1 ? "支払期限" : "納期限";
                    String code = prefix + "(" + dueDateFormat.format(pair.first) + ")";
                    cartModel.code = code;
                }
                for (Pair<Date, Integer> pair : dueDate) {
                    // 期限切れの通知を表示
                    String notice = "";
                    if (pair.second == 1 && item.is_payment_overdue) {
                        notice = "支払期限切れ";
                    } else if (pair.second == 2 && item.is_filing_overdue) {
                        notice = "納期限切れ";
                    }
                    if (!notice.isEmpty()) {
                        cartModel.notice = notice;
                        break;
                    }
                }
            }
            // 名前の部分に手動明細なら(<税区分> <内税 or 外税>)を追記
            else if (item.is_manual) {

                if (item.tax_type == ProductTaxTypes.EXEMPTION.value) {
                    cartModel.code = "手動明細(非課税)";
                    continue;
                }

                if (item.included_tax_type == IncludedTaxTypes.INCLUDED.value) {
                    // 内税
                    Timber.d("%s: 内税", item.product_name);
                    switch (ReducedTaxTypes.valueOf(item.reduce_tax_type)) {
                        case REDUCED:
                            // 軽減税率(内税)
                            cartModel.code = "手動明細(軽減税率 内税)";
                            continue;

                        case GENERAL:
                        default:
                            // 一般課税(内税)
                            cartModel.code = "手動明細(標準税率 内税)";
                    }
                } else {
                    // 外税
                    Timber.d("%s: 外税", item.product_name);
                    switch (ReducedTaxTypes.valueOf(item.reduce_tax_type)) {
                        case REDUCED:
                            // 軽減税率(外税)
                            cartModel.code = "手動明細(軽減税率 外税)";
                            continue;

                        case GENERAL:
                        default:
                            // 一般課税(外税)
                            cartModel.code = "手動明細(標準税率 外税)";
                    }
                }

            }
        }

        return _cartModelList;

    }

    private void calculateCartResult(Pair<List<CartData>, ServiceFunctionData> data) {

        List<CartData> cartData = data.first;
        ServiceFunctionData serviceFunc = data.second;
        assert serviceFunc != null;
        assert cartData != null;
        int itemCount = 0;

        // 税金計算処理
        TaxCalcBuilder builder = new TaxCalcBuilder(serviceFunc);
        for(CartData item : cartData){
            String info = "";
            if (item.is_custom_price) {
                info = String.format("[%s] %s %s円x%s",itemCount ,item.product_name ,item.custom_unit_price ,item.count);
            } else {
                info = String.format("[%s] %s %s円x%s",itemCount ,item.product_name ,item.standard_unit_price ,item.count);
            }

            Timber.i(info);
            itemCount++;
            builder.add(item);
        }
        TaxCalcData result = builder.build();
        Timber.d("計算結果: %s", JSON.stringify(result));

        // メンバー変数に結果を格納
        countAmount.setValue(String.valueOf(result.total_count));
        priceAmount.setValue(formatNumberAsCurrency(result.total_amount));
        Timber.i("合計:%s点 ￥%s",result.total_count ,result.total_amount);
        taxCalcData = result;
    }

    private final MutableLiveData<List<CartData>> _paymentOverdueItems = new MutableLiveData<>();
    public LiveData<List<CartData>> getPaymentOverdueItems() {
        return _paymentOverdueItems;
    }
    public void clearPaymentOverdueItems() {
        _paymentOverdueItems.setValue(new ArrayList<>());
    }

    private void checkPaymentOverdue(List<CartData> items) {
        List<CartData> overdueItems = new ArrayList<>();
        for(CartData item : items) {
            if (item.is_payment_overdue || item.is_filing_overdue) {
                overdueItems.add(item);
            }
        }
        _paymentOverdueItems.setValue(overdueItems);
    }

    /*
    private CartCalculateResultModel calculateCartResult(Pair<List<CartData>, ServiceFunctionData> data) {
        // 実行時パラメータ
        double standardTaxRate = 0.10;                      // 標準税率
        double reducedTaxRate = 0.08;                       // 軽減税率
        TaxRoundings taxRoundings = TaxRoundings.FLOOR;     // 端数処理

        // POSサービス機能からパラメータ取得
        ServiceFunctionData aServiceFunc = data.second;
        if (aServiceFunc != null) {
            Log.d("GET TAX----",aServiceFunc.standard_tax_rate);
            Log.d("GET TAX----",aServiceFunc.reduced_tax_rate);
            standardTaxRate =  Double.parseDouble(aServiceFunc.standard_tax_rate);
            reducedTaxRate = Double.parseDouble(aServiceFunc.reduced_tax_rate);
            taxRoundings = TaxRoundings.valueOf(aServiceFunc.tax_rounding);
            // Todo 切り上げているが店舗ごとに違うらしい
        }

        // 単価合計
        int _noTaxUnitPriceAmount = 0; // 非課税商品の単価合計
        int _excludeTaxCommonUnitPriceAmount = 0; // 一般課税商品の単価合計(外税)
        int _excludeTaxReduceUnitPriceAmount = 0; // 軽減課税商品の単価合計(外税)
        int _includeTaxCommonUnitPriceAmount = 0; // 一般課税商品の単価合計(内税)
        int _includeTaxReduceUnitPriceAmount = 0; // 軽減課税商品の単価合計(内税)

        int _allReduceAmount = 0; // 軽減税率の全ての合計 word の（サ）
        int _allCommonAmount = 0; // 一般税の全ての合計 word の （シ）
        int _onlyReduceTaxAmount = 0; // 軽減税率の税金のみの合計 word の (ス)
        int _onlyCommonTaxAmount = 0; // 軽減税率の税金のみの合計 word の （セ）

        // 個数の合計
        int _countAmount = 0; // 個数の合計

        // 商品が存在するカテゴリーを表示用リストに詰めるのと合計金額の精算
        for (CartData item : data.first) {

            if (item.count == 0) {
                Timber.d("個数0個なのでスキップ");
                continue;
            }

            _countAmount = _countAmount + item.count; // 個数合計

            if (item.tax_type == ProductTaxTypes.EXEMPTION.value) {
                // 非課税
                Timber.d("非課税");
                _noTaxUnitPriceAmount = _noTaxUnitPriceAmount + item.standard_unit_price * item.count; // 非課税の単価の加算
                continue;
            }

            if (item.included_tax_type == IncludedTaxTypes.INCLUDED.value) {
                // 内税
                Timber.d("内税");
                switch (ReducedTaxTypes.valueOf(item.reduce_tax_type)) {
                    case EXEMPTION:
                        // 非課税
                        break;

                    case REDUCED:
                        // 軽減税率(内税)
                        _includeTaxReduceUnitPriceAmount = _includeTaxReduceUnitPriceAmount + item.standard_unit_price * item.count;
                        break;

                    case GENERAL:
                    default:
                        // 一般課税(内税)
                        _includeTaxCommonUnitPriceAmount = _includeTaxCommonUnitPriceAmount + item.standard_unit_price * item.count;
                        break;
                }
            } else {
                // 外税
                Timber.d("外税");
                switch (ReducedTaxTypes.valueOf(item.reduce_tax_type)) {
                    case EXEMPTION:
                        // 非課税
                        break;

                    case REDUCED:
                        // 軽減税率(外税)
                        _excludeTaxReduceUnitPriceAmount = _excludeTaxReduceUnitPriceAmount + item.standard_unit_price * item.count;
                        break;

                    case GENERAL:
                    default:
                        // 一般課税(外税)
                        _excludeTaxCommonUnitPriceAmount = _excludeTaxCommonUnitPriceAmount + item.standard_unit_price * item.count;
                        break;
                }
            }
        }
        Log.d("TAX VALUE----" , String.valueOf(standardTaxRate));
        Log.d("TAX VALUE----" , String.valueOf(reducedTaxRate));

        // 税金合計
        int _excludeTaxCommonAmount;
        int _excludeTaxReduceAmount;
        switch (taxRoundings) {
            case ROUND:
                // 四捨五入
                _excludeTaxCommonAmount = Math.toIntExact(Math.round(_excludeTaxCommonUnitPriceAmount * standardTaxRate));
                _excludeTaxReduceAmount = Math.toIntExact(Math.round(_excludeTaxReduceUnitPriceAmount * reducedTaxRate));
                break;

            case CEILING:
                // 切り上げ
                _excludeTaxCommonAmount = Math.toIntExact((long) Math.ceil(_excludeTaxCommonUnitPriceAmount * standardTaxRate));
                _excludeTaxReduceAmount = Math.toIntExact((long) Math.ceil(_excludeTaxReduceUnitPriceAmount * reducedTaxRate));
                break;

            case FLOOR:
            default:
                // 切り捨て
                _excludeTaxCommonAmount = Math.toIntExact((long) Math.floor(_excludeTaxCommonUnitPriceAmount * standardTaxRate));
                _excludeTaxReduceAmount = Math.toIntExact((long) Math.floor(_excludeTaxReduceUnitPriceAmount * reducedTaxRate));
                break;
        }

        // 合計 = 非課税の合計 + 一般（内税）の単価の合計 + 軽減（内税）の単価の合計 + 一般（外税）の単価の合計 + 軽減（外税）の単価の合計 + 一般外税の合計 + 軽減外税の合計
        int _priceAmount = _noTaxUnitPriceAmount + _includeTaxCommonUnitPriceAmount + _includeTaxReduceUnitPriceAmount + _excludeTaxCommonUnitPriceAmount + _excludeTaxReduceUnitPriceAmount + _excludeTaxCommonAmount + _excludeTaxReduceAmount;

        CartCalculateResultModel response = new CartCalculateResultModel();
        response.countAmount = _countAmount;
        response.excludeTaxCommonAmount = _excludeTaxCommonAmount;
        response.excludeTaxReduceAmount = _excludeTaxReduceAmount;
        response.priceAmount = _priceAmount;

        taxCalcData = new TaxCalcData(
                _noTaxUnitPriceAmount,
                _excludeTaxCommonUnitPriceAmount,
                _excludeTaxCommonAmount,
                _excludeTaxReduceUnitPriceAmount,
                _excludeTaxReduceAmount,
                _includeTaxCommonUnitPriceAmount,
                _includeTaxReduceUnitPriceAmount,
                _allReduceAmount,
                _allCommonAmount,
                _onlyReduceTaxAmount,
                _onlyCommonTaxAmount,
                _countAmount,
                _priceAmount
        );

        Timber.d("個数合計" + _countAmount);
        Timber.d("外税合計（一般）" + _excludeTaxCommonAmount);
        Timber.d("外税合計（軽減）" + _excludeTaxReduceAmount);
        Timber.d("最終合計" + _priceAmount);

        countAmount.setValue(String.valueOf(_countAmount));
        priceAmount.setValue(formatNumberAsCurrency(_priceAmount));


        return response;
    }
    */

    public static String formatNumberAsCurrency(int number) {
        DecimalFormat decimalFormat = new DecimalFormat("#,###");
        return decimalFormat.format(number);
    }
    public void taxCalcSave(){
        Amount.setPosAmount(taxCalcData.total_amount);
        new Thread(() -> {
            TaxCalcDao _taxCalcDao = DBManager.getTaxCalcDao();
            _taxCalcDao.insertTaxCalcData(taxCalcData);
        }).start();
    }
}
