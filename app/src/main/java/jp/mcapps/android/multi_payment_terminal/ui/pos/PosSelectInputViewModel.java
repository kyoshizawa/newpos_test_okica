package jp.mcapps.android.multi_payment_terminal.ui.pos;

import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.w3c.dom.Text;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.NavigationWrapper;
import jp.mcapps.android.multi_payment_terminal.database.DBManager;
import timber.log.Timber;

public class PosSelectInputViewModel extends ViewModel {
    private Fragment inputFragment = null;
    public void setInputFragment(Fragment fragment) {
        inputFragment = fragment;
    }
    public Fragment getInputFragment() {
        return inputFragment;
    }

    private Integer product_id = null;
    public void setProductId(Integer id) {
        product_id = id;
    }
    public Integer getProductId() {
        Timber.d("getProductId");
        return product_id;
    }

    private Integer count = null;
    public void setCount(Integer value) {
        count = value;
    }
    public Integer getCount() {
        return count;
    }

    private Integer price = null;
    public void setPrice(Integer value) {
        price = value;
    }
    public Integer getPrice() {
        return price;
    }

    private String product_name = null;
    public void setProductName(String value) { product_name = value; }
    public String getProductName() {
        return product_name;
    }

    private final MutableLiveData<Boolean> _isCustomPrice = new MutableLiveData<>(false);
    public LiveData<Boolean> getIsCustomPrice() {
        return _isCustomPrice;
    }
    public void setIsCustomPrice(Boolean value) {
        _isCustomPrice.setValue(value);
    }

    private final MutableLiveData<Boolean> _isCountEditable = new MutableLiveData<>(true);
    public LiveData<Boolean> getIsCountEditable() {
        return _isCountEditable;
    }
    public void setIsCountEditable(Boolean value) {
        _isCountEditable.setValue(value);
    }

    private final MutableLiveData<Boolean> _isPriceEditable = new MutableLiveData<>(true);
    public LiveData<Boolean> getIsPriceEditable() {
        return _isPriceEditable;
    }
    public void setIsPriceEditable(Boolean value) {
        _isPriceEditable.setValue(value);
    }

    // 単価リセット処理
    public void resetPrice(Fragment fragment) {
        Single.fromCallable(() -> {
            DBManager.getCartDao().resetUnitPriceById(product_id);
            return true;
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
                result -> {
                    Timber.d("resetPrice: Success");
                    NavigationWrapper.navigateUp(fragment);
                    },
                error -> {
                    Timber.e(error);
                    Timber.e("「処理失敗しました」表示");
                    Toast.makeText(MainApplication.getInstance(), "処理失敗しました", Toast.LENGTH_LONG).show();
                });

    }

    // 商品削除処理
    public void DeleteProduct(Fragment fragment) {
        Single.fromCallable(() -> {
                    DBManager.getCartDao().deleteProduct(product_id);
                    return true;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {
                            Timber.d("deleteProduct: Success");
                            NavigationWrapper.navigateUp(fragment);
                        },
                        error -> {
                            Timber.e(error);
                            Timber.e("「処理失敗しました」表示");
                            Toast.makeText(MainApplication.getInstance(), "処理失敗しました", Toast.LENGTH_LONG).show();
                        }
                );
    }
}
