package jp.mcapps.android.multi_payment_terminal.ui.menu;

import android.view.View;

import androidx.fragment.app.Fragment;

import jp.mcapps.android.multi_payment_terminal.SharedViewModel;
import jp.mcapps.android.multi_payment_terminal.data.BusinessType;
import jp.mcapps.android.multi_payment_terminal.model.DiscountInfo;
import jp.mcapps.android.multi_payment_terminal.ui.discount.DiscountJobFutabaDViewModel;


public interface MenuEventHandlers {
    void navigateMain(Fragment fragment, int id);
    void navigateMain(View view, int id);
    void navigateMainWithAmountFix(View view, int id);
    void navigateMenu(View view, int id);
    void navigateHomeOperation(View view);
    void backMenu(View view);
    void backMenu(View view, boolean showHead);
    void showDialog(View view, String message);
    void showDialog(Fragment fragment, String message);
    void navigateToInputCarId(View view);
    void navigateToCreditCardScan(View view, SharedViewModel sharedViewModel);
    void navigateToEmoneySuica(View view, BusinessType type);
    void navigateToEmoneyId(View view, BusinessType type);
    void navigateToEmoneyWaon(View view, BusinessType type);
    void navigateToEmoneyEdy(View view, BusinessType type);
    void navigateToEmoneyQuicPay(View view, BusinessType type);
    void navigateToEmoneyNanaco(View view, BusinessType type);
    void navigateToEmoneyOkica(View view, BusinessType type);
    void navigateToInputChargeAmount(View view, String brand);
    void navigateToValidationCheck(View view);
    void navigateToWatari(View view, BusinessType type, SharedViewModel sharedViewModel);
    void navigateToProductSelect(View view);
    void navigateToCashInput(View view, SharedViewModel sharedViewModel, int id);
    void navigateToQR(View view, SharedViewModel sharedViewModel);
    void businessEndConfirm(View view, SharedViewModel sharedViewModel);
    void manualOpening(View view, SharedViewModel sharedViewModel);
    void sendLog(View view, SharedViewModel sharedViewModel);
    void businessEnd(View view, SharedViewModel sharedViewModel);
    void refreshPosProducts(View view, SharedViewModel sharedViewModel);
    void navigateToTicketSearch(View view);
    void navigateToTicketGateSettings(View view);
    void refreshTicketSales(View view, SharedViewModel sharedViewModel);
//ADD-S BMT S.Oyama 2024/09/03 フタバ双方向向け改修
    //void navigateToAdvancePay(View view);             //立替払い
    void navigateToSeparationPay(View view, SharedViewModel sharedViewModel, int tmpSeparationJobMode);            //分割払い
    void navigateToSeparationPayWithTicket(View view, SharedViewModel sharedViewModel, int tmpSeparationJobMode);           //分割払い(チケット処理向け)
    void navigateToSeparationPayMenuToEMoneyMenu(View view, SharedViewModel sharedViewModel);            //分割払いメニュー -> 電子マネーメニュー表示
    void navigateToSeparationPayMenuWithTicketToEMoneyMenu(View view, SharedViewModel sharedViewModel);            //分割払いメニュー -> 電子マネーメニュー表示
    void navigateToSeparationPayCancel(View view);      //分割払い取消
    void navigateToDiscountMenu(View view);             // 割引メニュー
    void navigateToDiscount(View view);                 //割引確認
    void navigateToDiscountJob(View view, SharedViewModel sharedViewModel, int tmpDiscountMode);          //割引処理本体　割引１～５
    void navigateToDiscountCard(View view, SharedViewModel sharedViewModel, DiscountInfo discountInfo) ;          //割引処理本体　割引カード
    void navigateToReceipt(View view);                 //領収書
    void navigateToTicketPrint(View view);              //チケット伝票
    void navigateToAggregate(View view);                //集計印刷
    void navigateToManualMenu(View view, SharedViewModel sharedViewModel);                //手動決済モード移行
    void navigateToDuplexMenu(View view, SharedViewModel sharedViewModel);                //双方向決済モード移行

    void navigateToEmoneySuicaPrestageProcess(View view, BusinessType type, SharedViewModel sharedViewModel);
    void navigateToEmoneyIdPrestageProcess(View view, BusinessType type, SharedViewModel sharedViewModel);
    void navigateToEmoneyWaonPrestageProcess(View view, BusinessType type, SharedViewModel sharedViewModel);
    void navigateToEmoneyEdyPrestageProcess(View view, BusinessType type, SharedViewModel sharedViewModel);
    void navigateToEmoneyQuicPayPrestageProcess(View view, BusinessType type, SharedViewModel sharedViewModel);
    void navigateToEmoneyNanacoPrestageProcess(View view, BusinessType type, SharedViewModel sharedViewModel);
    void navigateToEmoneyOkicaPrestageProcess(View view, BusinessType type, SharedViewModel sharedViewModel);

    void navigateToEmoneySuicaSeparation(View view, BusinessType type);
    void navigateToEmoneyIdSeparation(View view, BusinessType type);
    void navigateToEmoneyWaonSeparation(View view, BusinessType type);
    void navigateToEmoneyEdySeparation(View view, BusinessType type);
    void navigateToEmoneyQuicPaySeparation(View view, BusinessType type);
    void navigateToEmoneyNanacoSeparation(View view, BusinessType type);
    void navigateToEmoneyOkicaSeparation(View view, BusinessType type);

    void navigateToCreditCardScanSeparation(View view, SharedViewModel sharedViewModel);
    void navigateToQRSeparation(View view, SharedViewModel sharedViewModel);

    void navigateToAutoDailyReportFuel(View view);
//ADD-E BMT S.Oyama 2024/09/03 フタバ双方向向け改修
//ADD-S BMT S.Oyama 2025/03/14 フタバ双方向向け改修
    void navigateToAutoDailyReportMeterCheck(View view, SharedViewModel sharedViewModel);
    void navigateToAutoDailyReportErrorClear(View view, SharedViewModel sharedViewModel);
//ADD-E BMT S.Oyama 2024/05/03/14 フタバ双方向向け改修

    void navigateToPrepaidApp(View view);
}
