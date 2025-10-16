package jp.mcapps.android.multi_payment_terminal.ui.pos;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import jp.mcapps.android.multi_payment_terminal.database.DBManager;
import jp.mcapps.android.multi_payment_terminal.database.pos.CartDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.CartData;
import jp.mcapps.android.multi_payment_terminal.database.pos.IncludedTaxTypes;
import jp.mcapps.android.multi_payment_terminal.database.pos.ProductTaxTypes;
import jp.mcapps.android.multi_payment_terminal.database.pos.ReducedTaxTypes;
import jp.mcapps.android.multi_payment_terminal.util.McUtils;
import timber.log.Timber;

public class CartManualInputViewModel extends ViewModel {

    private final MutableLiveData<Integer> _unitPrice = new MutableLiveData<Integer>(-1);

    public LiveData<String> getUnitPriceText() {
        return Transformations.map(_unitPrice, input -> {
            if (input < 0) {
                return "";
            }
            return String.format(Locale.JAPANESE, "%,d", input);
        });
    }

    public LiveData<Integer> getUnitPrice() {
        return _unitPrice;
    }

    public void setUnitPrice(Integer unitPrice) {
        _unitPrice.setValue(unitPrice);
    }

    private final MutableLiveData<ProductTaxTypes> _productTaxType = new MutableLiveData<ProductTaxTypes>(ProductTaxTypes.UNKNOWN);

    public LiveData<ProductTaxTypes> getProductTaxType() {
        return _productTaxType;
    }

    public void setProductTaxType(ProductTaxTypes productTaxType) {
        _productTaxType.setValue(productTaxType);
    }

    private final MutableLiveData<ReducedTaxTypes> _reducedTaxType = new MutableLiveData<ReducedTaxTypes>(ReducedTaxTypes.GENERAL);

    public LiveData<ReducedTaxTypes> getReducedTaxType() {
        return _reducedTaxType;
    }

    public void setReducedTaxType(ReducedTaxTypes reducedTaxType) {
        _reducedTaxType.setValue(reducedTaxType);
    }

    private final MutableLiveData<IncludedTaxTypes> _includedTaxType = new MutableLiveData<>(IncludedTaxTypes.EXCLUDED);

    public LiveData<IncludedTaxTypes> getIncludedTaxType() {
        return _includedTaxType;
    }

    public void setIncludedTaxType(IncludedTaxTypes includedTaxType) {
        _includedTaxType.setValue(includedTaxType);
    }

    public void clearUnitPrice() {
        _unitPrice.setValue(-1);
    }

    public void backDelete() {
        Integer value = _unitPrice.getValue();
        if (value == null || value < 0) {
            return;
        }
        value = value / 10;
        if (value == 0) {
            value = -1;
        }
        _unitPrice.setValue(value);
    }

    public void addUnitPrice(String number) {
        Integer value = _unitPrice.getValue();

        // 0~999999 まで もしくは 0~9999999 に入力を制限する
        String priceInText = "";
        if (value != null && value > 0) {
            priceInText = String.valueOf(value);
        }

        // 入力された文字を追加
        priceInText += number;

        // 数値として取得
        int unitPriceInt = 0;
        try {
            unitPriceInt = Integer.parseInt(priceInText);
            if(!McUtils.isCheckMaxAmount(unitPriceInt)) {
                Timber.e("桁数入力オーバー: 入力値無視（%s）", number);
                return;
            }
        } catch (Exception e) {
            Timber.e(e);
            return;
        }
        _unitPrice.setValue(unitPriceInt);
    }

    public boolean addCartData() {
        AtomicBoolean result = new AtomicBoolean(false);
        if(_unitPrice.getValue() < 0){
            return result.get();
        }

        Thread thread = new Thread(() -> {
            try {
                CartDao cartDao = DBManager.getCartDao();
                ProductTaxTypes productTaxType = _productTaxType.getValue();
                IncludedTaxTypes includedTaxType = _includedTaxType.getValue();
                ReducedTaxTypes reducedTaxType = _reducedTaxType.getValue();
                if (reducedTaxType == ReducedTaxTypes.EXEMPTION) {
                    productTaxType = ProductTaxTypes.EXEMPTION;
                    includedTaxType = IncludedTaxTypes.EMPTY;
                }
                int unitPrice = getUnitPrice().getValue();
                cartDao.insertCartData(
                        new CartData(
                                unitPrice,
                                productTaxType.value,
                                reducedTaxType.value,
                                includedTaxType.value
                        )
                );
                result.set(true);
            } catch (Exception e) {
                Timber.e(e);
            }
        });
        thread.start();

        try {
            thread.join();
            return result.get();
        } catch (Exception e) {
            Timber.e(e);
            return result.get();
        }
    }
}
