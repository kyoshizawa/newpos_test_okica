package jp.mcapps.android.multi_payment_terminal.ui.history;

import android.view.View;

import java.util.Date;

import jp.mcapps.android.multi_payment_terminal.SharedViewModel;

public interface HistoryEventHandlers {
    void onChangePeriodClick(View view, Date date, int type, int time);

    void onDatePickerClick(View view, Date date, int month);

    void onCancelClick(View view, String moneyBrand, int slipId, int transactionTerminalType, String purchasedTicketDealId, SharedViewModel sharedViewModel, int tranType, String transTypeCode);

    void onCancelClick(View view, String moneyBrand, int slipId, int transactionTerminalType, String purchasedTicketDealId, SharedViewModel sharedViewModel);

    void onCancelClick(View view, String moneyBrand, int slipId, int transactionTerminalType, SharedViewModel sharedViewModel, String purchasedTicketDealId, boolean isTicketIssueCancel);

    void onRePrintingClick(View view, int id, String transBrand, int transType, int transactionTerminalType);

    void onReceiptIssueClick(View view, int id, int transactionTerminalType);

    void onSelectReceiptTypeClick(boolean receiptType, HistoryReceiptIssueViewModel viewModel);

    void onReceiptClick(View view, int id, boolean isDetail);

    void onRecoveryClick(View view, String moneyBrand, int slipId, int transType);
}
