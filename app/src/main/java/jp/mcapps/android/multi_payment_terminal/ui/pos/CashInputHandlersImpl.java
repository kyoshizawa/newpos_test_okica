package jp.mcapps.android.multi_payment_terminal.ui.pos;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import jp.mcapps.android.multi_payment_terminal.CommonClickEvent;
import jp.mcapps.android.multi_payment_terminal.NavigationWrapper;
import jp.mcapps.android.multi_payment_terminal.R;
import timber.log.Timber;

public class CashInputHandlersImpl implements CashInputHandlers{

    @Override
    public void onEnterBtn(View view , CashInputViewModel cashInputViewModel) {
        CommonClickEvent.RecordClickOperation("決定", true);
        int deposit = cashInputViewModel.getDeposit().getValue();
        int totalPrice = cashInputViewModel.getTotalPrice().getValue();
        boolean isFixedAmountPostalOrder = cashInputViewModel.getIsFixedAmountPostalOrder().getValue();
        int over = Math.max(deposit - totalPrice, 0);
        if (deposit >= totalPrice) {
            final Bundle params = new Bundle();
            params.putInt("deposit", deposit);
            params.putInt("over", over);
            params.putBoolean("isRepay", false);
            params.putBoolean("isFixedAmountPostalOrder", isFixedAmountPostalOrder);
            NavigationWrapper.navigate(view, R.id.action_fragment_cash_input_to_fragment_cash_confirm, params);
        } else {
            Timber.e("「お預り金額が不足しています」表示");
            Toast toast = Toast.makeText(view.getContext(), "お預り金額が不足しています", Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    @Override
    public void onBackDelete(View view, CashInputViewModel cashInputViewModel) {
        CommonClickEvent.RecordClickOperation("取消", true);
        cashInputViewModel.backDeleteDeposit();
    }

    @Override
    public void onClearBtn(View view , CashInputViewModel cashInputViewModel) {
        CommonClickEvent.RecordClickOperation("クリア", true);
        cashInputViewModel.clearDeposit();
    }

    @Override
    public void onInputNumber(CashInputViewModel cashInputViewModel , String number) {
        CommonClickEvent.RecordClickOperation(number, true);
        cashInputViewModel.addDeposit(number);
    }
}
