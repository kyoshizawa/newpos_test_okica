package jp.mcapps.android.multi_payment_terminal.ui.pos;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import jp.mcapps.android.multi_payment_terminal.CommonClickEvent;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.NavigationWrapper;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.database.pos.IncludedTaxTypes;
import jp.mcapps.android.multi_payment_terminal.database.pos.ReducedTaxTypes;
import timber.log.Timber;

public class CartManualInputHandlersImpl implements  CartManualInputHandlers{
    @Override
    public void onEnterBtn(View view, CartManualInputViewModel cartManualInputViewModel) {

        CommonClickEvent.RecordClickOperation("決定", true);
        final MainApplication _app = MainApplication.getInstance();
        if(cartManualInputViewModel.getUnitPrice().getValue() < 0){
            Toast.makeText(_app, "金額が入力されていません", Toast.LENGTH_LONG).show();
            Timber.e("「金額が入力されていません」表示");
            return;
        }

        if (cartManualInputViewModel.addCartData()) {
            final Bundle params = new Bundle();
            params.putString("productTaxType" , cartManualInputViewModel.getProductTaxType().getValue().key);
            params.putString("reducedTaxType" , cartManualInputViewModel.getReducedTaxType().getValue().key);
            params.putString("includedTaxType", cartManualInputViewModel.getIncludedTaxType().getValue().key);
            NavigationWrapper.navigate(view, R.id.action_fragment_cart_manual_input_to_cartConfirmFragment,params);
        } else {
            cartManualInputViewModel.clearUnitPrice();
            Timber.e("「処理失敗しました」表示");
            Toast.makeText(_app, "処理失敗しました", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onBackDelete(View view, CartManualInputViewModel cartManualInputViewModel) {
        CommonClickEvent.RecordClickOperation("取消", true);
        cartManualInputViewModel.backDelete();
    }

    @Override
    public void onClearBtn(View view, CartManualInputViewModel cartManualInputViewModel) {
        CommonClickEvent.RecordClickOperation("クリア", true);
        cartManualInputViewModel.clearUnitPrice();
    }

    @Override
    public void onInputNumber(CartManualInputViewModel cartManualInputViewModel, String number) {
        CommonClickEvent.RecordClickOperation(number, true);
        cartManualInputViewModel.addUnitPrice(number);
    }

    @Override
    public void onRedycedBtn(CartManualInputViewModel cartManualInputViewModel, ReducedTaxTypes reducedTaxType) {
        if (reducedTaxType == ReducedTaxTypes.GENERAL) {
            CommonClickEvent.RecordClickOperation("標準税率", true);
        } else if (reducedTaxType == ReducedTaxTypes.REDUCED) {
            CommonClickEvent.RecordClickOperation("軽減税率", true);
        } else if (reducedTaxType == ReducedTaxTypes.EXEMPTION) {
            CommonClickEvent.RecordClickOperation("非課税", true);
        }

        cartManualInputViewModel.setReducedTaxType(reducedTaxType);
    }

    @Override
    public void onIncludedBtn(CartManualInputViewModel cartManualInputViewModel, IncludedTaxTypes includedTaxType) {
        if (includedTaxType == IncludedTaxTypes.EXCLUDED) {
            CommonClickEvent.RecordClickOperation("外税", true);
        } else if (includedTaxType == IncludedTaxTypes.INCLUDED) {
            CommonClickEvent.RecordClickOperation("内税", true);
        }

        cartManualInputViewModel.setIncludedTaxType(includedTaxType);
    }
}
