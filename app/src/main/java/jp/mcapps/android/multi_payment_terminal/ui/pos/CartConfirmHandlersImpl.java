package jp.mcapps.android.multi_payment_terminal.ui.pos;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.StringRes;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.CommonClickEvent;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.NavigationWrapper;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.SharedViewModel;
import jp.mcapps.android.multi_payment_terminal.data.Amount;
import jp.mcapps.android.multi_payment_terminal.util.McUtils;

public class CartConfirmHandlersImpl implements CartConfirmHandlers {

    private Toast mToast;

    private void toastIt(String text, int duration) {
        if (mToast != null) {
            mToast.cancel();
        }
        mToast = Toast.makeText(MainApplication.getInstance(), text, duration);
        mToast.show();
    }

    private void toastIt(@StringRes int resId, int duration) {
        if (mToast != null) {
            mToast.cancel();
        }
        mToast = Toast.makeText(MainApplication.getInstance(), resId, duration);
        mToast.show();
    }

    @Override
    public void navigateToPayment(View view, CartConfirmViewModel cartConfirmViewModel, SharedViewModel sharedViewModel) {
        CommonClickEvent.RecordClickOperation("決済に進む", true);
        final MainApplication _app = MainApplication.getInstance();
        Log.d("jump", "navigateToPayment: ");
        // 商品点数をチェックする
        int count = 0;
        try {
            count = Integer.parseInt(cartConfirmViewModel.getCountAmount().getValue());
        } catch (Exception e) {
            return;
        }
        // 2023.07.11 商品選択してなくても決済画面に遷移して良い
        // if (count <= 0) {
        //     toastIt("商品が選択されていません", Toast.LENGTH_LONG);
        //     return;
        // }
        // 決済金額をチェックする
        int priceAmount = 0;
        try {
            String priceAmountStr = cartConfirmViewModel.getPriceAmount().getValue();
            priceAmountStr = priceAmountStr != null ? priceAmountStr.replace(",", "") : "";
            priceAmount = Integer.parseInt(priceAmountStr);
        } catch (Exception e) {
            return;
        }
        if(!McUtils.isCheckMaxAmount(priceAmount)) {
            if (AppPreference.getMaxAmountType() == AppPreference.MaxAmountType.LARGE) {
                toastIt(R.string.error_detail_credit_3090_2, Toast.LENGTH_LONG);
            } else {
                toastIt(R.string.error_detail_credit_3090, Toast.LENGTH_LONG);
            }
            return;
        }
        cartConfirmViewModel.taxCalcSave();
        Amount.isPosAmount(true);
        Amount.setTotalChangeAmount(0);
        Amount.setFlatRateAmount(0);
        final Bundle params = new Bundle();
        params.putBoolean("cashMenu", true);
        NavigationWrapper.navigate(view, R.id.action_cartConfirmFragment_to_navigation_menu, params);
    }

    @Override
    public void navigateToManual(View view) {
        CommonClickEvent.RecordClickOperation("手動明細", true);
        NavigationWrapper.navigate(view, R.id.action_cartConfirmFragment_to_fragment_cart_manual_input, null);
    }

}
