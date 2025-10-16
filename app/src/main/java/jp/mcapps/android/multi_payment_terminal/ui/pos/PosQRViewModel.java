package jp.mcapps.android.multi_payment_terminal.ui.pos;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.RawRes;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.Result;
import jp.mcapps.android.multi_payment_terminal.database.pos.CartDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.CartData;
import jp.mcapps.android.multi_payment_terminal.database.pos.ProductDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.ProductData;
import jp.mcapps.android.multi_payment_terminal.error.DomainErrors;
import jp.mcapps.android.multi_payment_terminal.model.SoundManager;
import jp.mcapps.android.multi_payment_terminal.model.pos.CartRepository;
import timber.log.Timber;

public class PosQRViewModel extends AndroidViewModel {

    public PosQRViewModel(Application app) {
        super(app);
    }

    private final Handler _handler = new Handler(Looper.getMainLooper());
    private SoundManager _soundManager = SoundManager.getInstance();

    private ProductData _product = null;

    public ProductData getProductId() {
        return _product;
    }

    private final MutableLiveData<Boolean> _result = new MutableLiveData<>(false);
    public MutableLiveData<Boolean> getResult() {
        return _result;
    }
    public void setResult(boolean b) {
        _handler.post(() -> {
            _result.setValue(b);
        });
    }

    private final MutableLiveData<Boolean> _isProcessing = new MutableLiveData<>(false);
    public MutableLiveData<Boolean> isProcessing() {
        return _isProcessing;
    }
    public void isProcessing(boolean b) {
        _handler.post(() -> {
            _isProcessing.setValue(b);
        });
    }

    private final MutableLiveData<Boolean> _isFinished = new MutableLiveData<>(false);
    public MutableLiveData<Boolean> isFinished() {
        return _isFinished;
    }
    public void isFinished(boolean b) {
        _handler.post(() -> {
            _isFinished.setValue(b);
        });
    }

    private final MutableLiveData<String> _finishedMessage = new MutableLiveData<>("");
    public MutableLiveData<String> getFinishedMessage() {
        return _finishedMessage;
    }
    public void setFinishedMessage(String msg) {
        _handler.post(() -> {
            _finishedMessage.setValue(msg);
        });
    }

    public void setProduct(String code, ProductDao productDao, CartDao cartDao) {
        // 処理中
        isProcessing(true);

        CartRepository cartRepository = new CartRepository();
        String msg = "";
        boolean isOk = false;

        // 商品をカートに追加する
        Result<CartData, DomainErrors> result = cartRepository.insertProduct(code);
        if (result.isOk()) {
            CartData cartItem = result.ok;
            makeSound(R.raw.pos_scan);
            msg = cartItem.product_name;
            isOk = true;
            Timber.i("読取成功：商品名（%s）", msg);
        } else {
            switch (result.err) {
                case NOT_FOUND:
                    // 読み取った商品コードがマスタにない
                    makeSound(R.raw.pos_ng);
                    msg = "商品を識別できませんでした";
                    Timber.e("読取失敗：%s", msg);
                    break;

                case OUT_OF_RANGE:
                    // カートに入れられる上限に達している
                    // toast message
                    _handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplication(), "999以上は設定できません", Toast.LENGTH_SHORT).show();
                        }
                    });
                    makeSound(R.raw.pos_scan);
                    msg = _product.name;
                    isOk = true;
                    Timber.i("読取成功：商品名（%s）", msg);
                    break;
            }
        }

        setFinishedMessage(msg);
        isProcessing(false);
        isFinished(true);

        // Fragmentでobserveしてるので最後にセットする
        setResult(isOk);
    }

    private void xxx_setProduct(String code, ProductDao productDao, CartDao cartDao) {
        // 処理中
        isProcessing(true);

        // DBから当該商品IDを探す
        List<ProductData> products = productDao.getProductsByCode(code);
        if(products.size() > 0) {
            _product = products.get(0);
        }

        String msg;
        Boolean isOk;
        if (_product != null) {
            // 商品コードをカートに突っ込む
            List<CartData> carts = cartDao.getProductByProductCode(code);
            if (carts.size() > 0) {
                CartData cart = carts.get(0);
                try {
                    cart.Increment();
                    cartDao.updateCountById(cart.id, cart.count);
                } catch (DomainErrors.Exception ex) {
                    // toast message
                    _handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplication(), "999以上は設定できません", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } else {
                CartData cart = new CartData(_product);
                cartDao.insertCartData(cart);
            }
            makeSound(R.raw.pos_scan);
            msg = _product.name;
            isOk = true;
            Timber.i("読取成功：商品名（%s）", msg);
        } else {
            makeSound(R.raw.pos_ng);
            msg = "商品を識別できませんでした";
            isOk = false;
            Timber.e("読取失敗：%s", msg);
        }

        setFinishedMessage(msg);
        isProcessing(false);
        isFinished(true);

        // Fragmentでobserveしてるので最後にセットする
        setResult(isOk);
    }

    public void makeSound(@RawRes int id) {
        // 決済音の音量を設定
        float volume =  AppPreference.getSoundPaymentVolume() / 10f;
        _soundManager.load(MainApplication.getInstance(), id, 1);

        _soundManager.setOnLoadCompleteListener((soundPool, soundId, status) -> {
            soundPool.play(soundId, volume, volume, 1, 0, 1);
        });
    }

}
