package jp.mcapps.android.multi_payment_terminal.ui.pos;

import androidx.annotation.Nullable;
import androidx.annotation.RawRes;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.CommonClickEvent;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.Result;
import jp.mcapps.android.multi_payment_terminal.database.DBManager;
import jp.mcapps.android.multi_payment_terminal.database.pos.CartDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.CartData;
import jp.mcapps.android.multi_payment_terminal.database.pos.CategoryDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.CategoryData;
import jp.mcapps.android.multi_payment_terminal.database.pos.ProductDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.ProductData;
import jp.mcapps.android.multi_payment_terminal.database.InsertMock;
import jp.mcapps.android.multi_payment_terminal.database.pos.ServiceFunctionData;
import jp.mcapps.android.multi_payment_terminal.error.DomainErrors;
import jp.mcapps.android.multi_payment_terminal.model.SoundManager;
import jp.mcapps.android.multi_payment_terminal.model.pos.CartRepository;
import timber.log.Timber;

public class ProductSelectViewModel extends ViewModel {

    // 階層が戻る用のボタン
    private final MutableLiveData<Boolean> _productSelectBackVisible = new MutableLiveData<Boolean>(false);

    public MutableLiveData<Boolean> getProductSelectBackVisible() {
        return _productSelectBackVisible;
    }

    public void setProductSelectVisible(boolean b) {
        _productSelectBackVisible.setValue(b);
    }

    private ServiceFunctionData _serviceFunctionData;
    private final CartRepository _cartRepository = new CartRepository();
    private final CartDao cartDao = DBManager.getCartDao();
    private final ProductDao productDao = DBManager.getProductDao();
    private final CategoryDao categoryDao = DBManager.getCategoryDao();

    private final SoundManager _soundManager = SoundManager.getInstance();

    private final Deque<Long> categoryStackList = new ArrayDeque<>();
    private final MutableLiveData<List<ProductCategorySelectModel>> data = new MutableLiveData<>();

    private final MutableLiveData<String> errorMessages = new MutableLiveData<>("");
    public LiveData<String> getErrorMessages() {
        return errorMessages;
    }
    public void setErrorMessages(String message) {
        errorMessages.setValue(message);
    }

    public LiveData<List<ProductCategorySelectModel>> getData() {
        return data;
    }

    Observable<List<CategoryModel>> getCategories() {
        return Observable.fromCallable(() -> categoryDao.getCategories())
                .map((items) -> {
                    List<CategoryModel> converted = new ArrayList<>();
                    for (CategoryData it : items) {
                        converted.add(CategoryModel.newInstance(it));
                    }
                    return converted;
                });
    }

    Observable<List<CategoryModel>> getCategoriesByParentId(long parent_id) {
        return Observable.fromCallable(() -> categoryDao.getCategoriesByParentId(parent_id))
                .map((items) -> {
                    List<CategoryModel> converted = new ArrayList<>();
                    for (CategoryData it : items) {
                        converted.add(CategoryModel.newInstance(it));
                    }
                    return converted;
                });
    }

    Observable<List<ProductData>> getProductWithoutParent() {
        return Observable.fromCallable(() -> productDao.getProductsWithOutCategory());
    }

    Observable<List<ProductData>> getProductsByCategoryId(long category_id) {
        return Observable.fromCallable(() -> productDao.getProductsByCategoryId(category_id));
    }

    List<ProductData> getAllProduct() {
        return productDao.getProducts();
    }


    // コンストラクタ todo insertMockはリリースの時消す
    public ProductSelectViewModel() {
        // insertMock();
    }

    // 初回取得
    // カテゴリーグループ機能がONとOFFで取得するクエリが違います。
    private void fetchData() {

        boolean isCategorize = true;

        // カテゴリーグループ化フラグの取得
        isCategorize = getIsProductCategory();

        Timber.d("isCategorize: %s", isCategorize);
        if (isCategorize) {
            Observable.combineLatest(
                            getCategories(),
                            getProductWithoutParent(),
                            (dataList1, dataList2) -> {
                                // データの結合処理を行う
                                return convertDisplayModel(dataList1, dataList2);
                            }
                    )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            combinedDataList -> {
                                // データの更新処理を行う
                                // RecyclerViewなどにcombinedDataListをセットしてUIを更新する処理を記述
                                data.setValue(combinedDataList);
                            },
                            error -> {
                                // エラーハンドリングの処理を記述
                                Timber.e(error);
                            }
                    );
        } else {
            Observable.fromCallable(() -> getAllProduct())  // データを非同期に取得するメソッドを呼び出す
                    .subscribeOn(Schedulers.single())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            result -> {
                                // データ取得が成功した場合の処理
                                List<ProductCategorySelectModel> _cartModelList = convertDisplayModel(null, result);
                                data.setValue(_cartModelList);
                            },
                            error -> {
                                // エラーハンドリングの処理
                                Timber.e(error);
                            }
                    );
        }
    }

    public Observable<List<ProductCategorySelectModel>> initFetchData() {

        boolean isCategorize = true;

        // カテゴリーグループ化フラグの取得
        isCategorize = getIsProductCategory();

        if (isCategorize) {
            return Observable.combineLatest(
                            getCategories(),
                            getProductWithoutParent(),
                            (dataList1, dataList2) -> {
                                // データの結合処理を行う
                                return convertDisplayModel(dataList1, dataList2);
                            }
                    )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread());
        } else {
            return Observable.fromCallable(() -> {
                        List<ProductData> result = getAllProduct();
                        return convertDisplayModel(null, result);
                    })
                    .subscribeOn(Schedulers.single())
                    .observeOn(AndroidSchedulers.mainThread());
        }

    }

    public void back() {
        CommonClickEvent.RecordClickOperation("戻る", true);
        if (categoryStackList.isEmpty()) {
            return;
        }

        categoryStackList.removeLast(); // 一番後ろを消す(現在の検索結果のcategory_idが取得できます)

        if (categoryStackList.size() == 0) {
            // 抜き取られた後にサイズが0ならrootにいることになる
            fetchData();
            setProductSelectVisible(false);
        } else {
            Long before_category_id = categoryStackList.getLast();
            fetchDataWithParentId(before_category_id);
        }

        Timber.d("抜き取り後のスタック:%s", categoryStackList.toString());
    }

    public void addQue(long category_id) {
        categoryStackList.add(category_id);
        Timber.d("追加後のスタック:%s", categoryStackList.toString());
    }

    // カテゴリ選択された際の取得
    public void fetchDataWithParentId(long category_id) {
        Observable.combineLatest(
                        getCategoriesByParentId(category_id),
                        getProductsByCategoryId(category_id),
                        (dataList1, dataList2) -> {
                            // データの結合処理を行う
                            return convertDisplayModel(dataList1, dataList2);
                        }
                )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        combinedDataList -> {
                            // データの更新処理を行う
                            // RecyclerViewなどにcombinedDataListをセットしてUIを更新する処理を記述
                            data.setValue(combinedDataList);
                            setProductSelectVisible(true);
                        },
                        error -> {
                            // エラーハンドリングの処理を記述
                            Timber.e(error);
                        }
                );
    }

    public Observable<Boolean> insertDataIntoDatabase(ProductCategorySelectModel item) {
        return Observable.fromCallable(() -> {
                    // 商品をカートに追加
                    Result<CartData, DomainErrors> result = _cartRepository.insertProduct(item.code);
                    if (result.isOk()) {
                        makeSound(R.raw.pos_scan);
                        return true;
                    } else {
                        String msg;
                        switch (result.err) {
                            case NOT_FOUND:
                                // 読み取った商品コードがマスタにない
                                msg = "商品が見つかりませんでした";
                                errorMessages.postValue(msg);   // 非同期に設定
                                Timber.w(msg + ":" + item.code);
                                makeSound(R.raw.pos_ng);
                                break;

                            case OUT_OF_RANGE:
                                // カートに入れられる上限に達している
                                msg = "999以上は設定できません";
                                errorMessages.postValue(msg);    // 非同期に設定
                                Timber.w(msg + ":" + item.code);
                                makeSound(R.raw.pos_ng);
                                break;
                        }
                        return false;
                    }
                }).
                subscribeOn(Schedulers.io()).
                observeOn(AndroidSchedulers.mainThread());
    }

    private Observable<Boolean> xxx_insertDataIntoDatabase(ProductCategorySelectModel item) {
        return Observable.fromCallable(() -> {
                    // 外付けバーコードリーダーから登録する場合はproductDataがnullのためDBから当該商品IDを探す
                    if(item.productData == null) {
                        List<ProductData> products = productDao.getProductsByCode(item.code);
                        if (products.size() > 0) {
                            item.productData = products.get(0);
                        }
                    }

                    if(item.productData != null) {
                        // 商品をカートに追加
                        List<CartData> carts = cartDao.getProductByProductCode(item.code);
                        if (carts.size() > 0) {
                            CartData cart = carts.get(0);
                            try {
                                // 商品の加算
                                cart.Increment();
                                cartDao.updateCountById(cart.id, cart.count);
                            } catch (DomainErrors.Exception ex) {
                                // カートに入れられる上限に達している
                                String msg = "999以上は設定できません";
                                errorMessages.postValue(msg);   // 非同期に設定
                                Timber.w(msg+ ":" + cart.count);
                                makeSound(R.raw.pos_ng);
                                return false;
                            }
                        } else {
                            // 商品の新規追加
                            CartData cart = new CartData(item.productData);
                            cartDao.insertCartData(cart);
                        }
                        makeSound(R.raw.pos_scan);
                        return true;
                    } else {
                        // 読み取った商品コードがマスタにない
                        String msg = "商品が見つかりませんでした";
                        errorMessages.postValue(msg);   // 非同期に設定
                        Timber.w(msg + ":" + item.code);
                        makeSound(R.raw.pos_ng);
                        return false;
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public void insertMock() {
        Observable.fromCallable(() -> {
                    InsertMock.CreateProductCategoryMock();
                    return true;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {
                            Timber.d("モックデータ作成成功！！");
                            fetchData();
                        },
                        error -> {
                            Timber.e(error);
                        }
                );

    }

    // RecyclerViewに表示するためのListに変換するメソッド
    private List<ProductCategorySelectModel> convertDisplayModel(@Nullable List<CategoryModel> dataList1, @Nullable List<ProductData> dataList2) {
        // データの結合処理を行う
        List<ProductCategorySelectModel> combinedDataList = new ArrayList<>();

        if (dataList1 != null) {
            // 商品が存在するカテゴリーを表示用リストに詰める
            for (CategoryModel item : dataList1) {
                combinedDataList.add(ProductCategorySelectModel.newInstance(item));
            }
        }

        if (dataList2 != null) {
            // カテゴリーに属していない商品を表示用リストに詰める
            for (ProductData item : dataList2) {
                combinedDataList.add(ProductCategorySelectModel.newInstance(item));
            }
        }

        return combinedDataList;
    }

    private void makeSound(@RawRes int soundId) {
        // 決済音の音量を設定
        float volume = AppPreference.getSoundPaymentVolume() / 10f;
        _soundManager.load(MainApplication.getInstance(), soundId, 1);

        _soundManager.setOnLoadCompleteListener((soundPool, sampleId, status) -> {
            _soundManager.play(sampleId, volume, volume, 1, 0, 1);
        });
    }

    // カテゴリーグループ機能フラグを取得
    private boolean getIsProductCategory() {
        boolean isProductCategory = false;

        Thread thread = new Thread(() -> {
            _serviceFunctionData = DBManager.getServiceFunctionDao().getServiceFunction();
        });
        thread.start();

        try {
            thread.join();
            if(_serviceFunctionData != null){
                isProductCategory = _serviceFunctionData.is_product_category;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return isProductCategory;
    }
}
