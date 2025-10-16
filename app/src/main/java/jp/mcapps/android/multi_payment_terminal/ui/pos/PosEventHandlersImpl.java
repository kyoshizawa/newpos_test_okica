package jp.mcapps.android.multi_payment_terminal.ui.pos;

import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import jp.mcapps.android.multi_payment_terminal.CommonClickEvent;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.NavigationWrapper;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.SharedViewModel;
import timber.log.Timber;

public class PosEventHandlersImpl implements PosEventHandlers {
    private MainApplication _app = MainApplication.getInstance();
    private final Fragment _fragment;
    private final NavHostFragment _childNavFragment;

    public PosEventHandlersImpl(Fragment fragment) {
        this(fragment, null);
    }

    public PosEventHandlersImpl(Fragment fragment, NavHostFragment childNavController) {
        _fragment = fragment;
        _childNavFragment = childNavController;
    }

    @Override
    public void navigateMain(View view , SharedViewModel sharedViewModel) {
        CommonClickEvent.RecordClickOperation("ホーム", true);
        NavigationWrapper.navigate(view, R.id.action_global_navigation_menu);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void navigateToQR(View view) {
        CommonClickEvent.RecordClickOperation("スキャン", true);
        NavigationWrapper.navigate(view, R.id.action_global_navigation_pos_qr, null);
    }

    @Override
    public void navigateToCartConfirm(View view) {
        CommonClickEvent.RecordClickOperation("合計", true);
        NavigationWrapper.navigate(view, R.id.action_navigation_product_select_to_cartConfirmFragment, null);
    }
    @Override
    public void navigateFromManualToCartConfirm(View view) {
        CommonClickEvent.RecordClickOperation("戻る", true);
        NavigationWrapper.navigate(view, R.id.action_fragment_cart_manual_input_to_cartConfirmFragment, null);
    }

    @Override
    public void navigateToInputCartCount(Fragment fragment, Integer product_id, Integer count) {
        final Bundle params = new Bundle();
        params.putString("input_cart_type", InputCartTypes.COUNT.toString());
        params.putInt("product_id", product_id);
        params.putInt("count", count);
        NavigationWrapper.navigate(fragment, R.id.action_cartConfirmFragment_to_inputCartFragment, params);
    }

    @Override
    public void navigateToSelectInputType(Fragment fragment, CartModel item) {
        final Bundle params = new Bundle();
        params.putInt("product_id", item.id);
        params.putInt("count", item.count);
        params.putInt("price", item.unitPrice);
        params.putBoolean("is_custom_price", item.isCustomPrice);
        params.putBoolean("is_count_editable", item.isCountEditable);
        params.putBoolean("is_price_editable", item.isPriceEditable);
        params.putString("product_name", item.name);
        NavigationWrapper.navigate(fragment, R.id.action_cartConfirmFragment_to_selectInputFragment, params);
    }

    @Override
    public void navigateFromProductSelectToManualInput(View view) {
        CommonClickEvent.RecordClickOperation("手動明細", true);
        NavigationWrapper.navigate(view, R.id.action_navigation_product_select_to_fragment_cart_manual_input, null);
    }

    @Override
    public void navigateUp(View view) {
        CommonClickEvent.RecordClickOperation("戻る", true);
        if (_childNavFragment != null && (NavigationWrapper.navigateUp(_childNavFragment))) {
            // 子ナビゲーションが成功した場合は、ここで終了（仮）
            return;
        }

        // 親のナビゲーションを実行
        NavigationWrapper.navigateUp(view);
    }


    @Override
    public void navigateToProductSelect(View view) {
        CommonClickEvent.RecordClickOperation("追加選択", true);
        NavigationWrapper.navigate(view, R.id.action_cartConfirmFragment_to_navigation_product_select, null);
    }

}
