package jp.mcapps.android.multi_payment_terminal.ui.developer;

import android.view.View;

import java.util.Date;

import jp.mcapps.android.multi_payment_terminal.SharedViewModel;

public interface DeveloperEventHandlers {
    void onDatePickerClick(View view, Date date, int month);

    void onAddDummyTransactionClick(View view, DeveloperAddDummyTransactionViewModel viewModel, SharedViewModel sharedViewModel);

    void onChangePeriodClick(View view, Date date, int type, int time);

    void onDeleteHistoryClick(View view, String target);

    void onAddDummyDriverClick(View view, DeveloperAddDummyDriverViewModel viewModel);
}
