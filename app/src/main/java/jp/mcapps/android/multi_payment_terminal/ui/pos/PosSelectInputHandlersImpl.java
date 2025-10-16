package jp.mcapps.android.multi_payment_terminal.ui.pos;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import jp.mcapps.android.multi_payment_terminal.CommonClickEvent;
import jp.mcapps.android.multi_payment_terminal.NavigationWrapper;
import jp.mcapps.android.multi_payment_terminal.R;
import timber.log.Timber;

public class PosSelectInputHandlersImpl implements  PosSelectInputHandlers {

    private final long _delayMillis = 1000;
    private long _pushedMillis = 0;

    @Override
    public void clickCountChange(PosSelectInputViewModel posSelectInputViewModel) {
        CommonClickEvent.RecordClickOperation("数量を変更する", true);
        Fragment fragment = posSelectInputViewModel.getInputFragment();

        // 数量変更画面へ移動
        final Bundle params = new Bundle();
        params.putString("input_cart_type", InputCartTypes.COUNT.toString());
        params.putInt("product_id", posSelectInputViewModel.getProductId());
        params.putInt("count", posSelectInputViewModel.getCount());
        NavigationWrapper.navigate(fragment, R.id.action_selectInputFragment_to_inputCartFragment, params);
    }

    @Override
    public void clickUnitPriceChange(PosSelectInputViewModel posSelectInputViewModel) {
        CommonClickEvent.RecordClickOperation("単価を変更する", true);
        Fragment fragment = posSelectInputViewModel.getInputFragment();

        // 単価変更画面へ移動
        final Bundle params = new Bundle();
        params.putString("input_cart_type", InputCartTypes.PRICE.toString());
        params.putInt("product_id", posSelectInputViewModel.getProductId());
        params.putInt("price", posSelectInputViewModel.getPrice());
        NavigationWrapper.navigate(fragment, R.id.action_selectInputFragment_to_inputCartFragment, params);
    }

    @Override
    public void clickUnitPriceReset(PosSelectInputViewModel posSelectInputViewModel) {
        CommonClickEvent.RecordClickOperation("単価を元に戻す", true);
        // 連続タップ防止
        long timeMillis = System.currentTimeMillis();
        if (timeMillis - _pushedMillis < _delayMillis)
        {
            Timber.e("１秒以内に連続タップ発生");
            return;
        }
        _pushedMillis = timeMillis;
        //Timber.d("push time millis:%s",_pushedMillis);

        // 単価リセット
        posSelectInputViewModel.resetPrice(posSelectInputViewModel.getInputFragment());
    }

    @Override
    public void clickUnitDeleteProduct(PosSelectInputViewModel posSelectInputViewModel) {
        CommonClickEvent.RecordClickOperation("商品を削除する", true);
        // 連続タップ防止
        long timeMillis = System.currentTimeMillis();
        if (timeMillis - _pushedMillis < _delayMillis)
        {
            Timber.e("１秒以内に連続タップ発生");
            return;
        }
        _pushedMillis = timeMillis;
        //Timber.d("push time millis:%s",_pushedMillis);

        // 商品削除
        posSelectInputViewModel.DeleteProduct(posSelectInputViewModel.getInputFragment());
    }

    public String getProductName(PosSelectInputViewModel posSelectInputViewModel) {

        // 商品名取得
        final String product_name = posSelectInputViewModel.getProductName();

        return product_name;
    }
}
