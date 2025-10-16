package jp.mcapps.android.multi_payment_terminal.ui.discount;
//ADD-S BMT S.Oyama 2024/09/06 フタバ双方向向け改修

import static jp.mcapps.android.multi_payment_terminal.AppPreference.isDemoMode;
import static jp.mcapps.android.multi_payment_terminal.thread.printer.PrinterConst.DuplexPrintResponseTimerSec;

import android.app.ProgressDialog;
import android.os.Build;
import android.view.View;

import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jp.mcapps.android.multi_payment_terminal.CommonClickEvent;
import jp.mcapps.android.multi_payment_terminal.NavigationWrapper;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.SharedViewModel;
import jp.mcapps.android.multi_payment_terminal.data.Amount;
import jp.mcapps.android.multi_payment_terminal.data.IFBoxAppModels;
// import jp.mcapps.android.multi_payment_terminal.model.IFBoxManager;
import jp.mcapps.android.multi_payment_terminal.thread.printer.PrinterConst;
import jp.mcapps.android.multi_payment_terminal.thread.printer.PrinterManager;
import jp.mcapps.android.multi_payment_terminal.ui.dialog.ConfirmDialog;

public class DiscountJobFutabaDEventHandlersImpl implements DiscountJobFutabaDEventHandlers{
    private final Fragment _fragment;

    public DiscountJobFutabaDEventHandlersImpl(Fragment fragment) {
        _fragment = fragment;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onOkClick(View view, DiscountJobFutabaDViewModel _discountJobFutabaDViewModel, SharedViewModel sharedViewModel) {
        CommonClickEvent.RecordClickOperation("閉じる", "割引確認結果", false);
        //_discountFutabaDViewModel.stop();

        if ( (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == true)) {
            PrinterManager printerManager = PrinterManager.getInstance();
            printerManager.setView(view);

//            if (_discountJobFutabaDViewModel.getIfBoxManager() == null) {
//                printerManager.PrinterDuplexError(PrinterConst.DuplexPrintStatus_IFBOXERROR);       //IFBOX接続エラー
//                return;
//            }
//
//            if (_discountJobFutabaDViewModel.getIfBoxManager().getIsConnected820() == false)             //820未接続の場合
//            {
//                printerManager.PrinterDuplexError(PrinterConst.DuplexPrintStatus_IFBOXERROR);       //IFBOX接続エラー
//                return;
//            }

            int tmpDiscountMode = _discountJobFutabaDViewModel.getdiscountJobMode().getValue();
            int tmpDiscountJobMode_FutabaD = 0;
//            switch (tmpDiscountMode) {
//                case 0:
//                    tmpDiscountJobMode_FutabaD = IFBoxManager.SendMeterDataStatus_FutabaD.DISCOUNTTYPE_JOB1;
//                    break;
//                case 1:
//                    tmpDiscountJobMode_FutabaD = IFBoxManager.SendMeterDataStatus_FutabaD.DISCOUNTTYPE_JOB2;
//                    break;
//                case 2:
//                    tmpDiscountJobMode_FutabaD = IFBoxManager.SendMeterDataStatus_FutabaD.DISCOUNTTYPE_JOB3;
//                    break;
//                case 3:
//                    tmpDiscountJobMode_FutabaD = IFBoxManager.SendMeterDataStatus_FutabaD.DISCOUNTTYPE_JOB4;
//                    break;
//                case 4:
//                    tmpDiscountJobMode_FutabaD = IFBoxManager.SendMeterDataStatus_FutabaD.DISCOUNTTYPE_JOB5;
//                    break;
//            }

            Date exDate = new Date();   // 取引時間
            SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.JAPANESE);
            String tmpJobDateTime = dateFmt.format(exDate);

//            _discountJobFutabaDViewModel.getIfBoxManager().send820_DiscountExecution(tmpDiscountJobMode_FutabaD, tmpJobDateTime,"FFFFFFFF", 0);        //割引登録を送信 ACKなし
        }

        Amount.setDiscountAvailable(1);     // 割引実施フラグをセット

        sharedViewModel.setBackAction(null);

        view.post(() -> {
            //NavigationWrapper.navigateUp(view);

            NavigationWrapper.navigate(view, R.id.action_navigation_discountjob_fd_to_navigation_menu2);
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onCancelClick(View view , DiscountJobFutabaDViewModel _discountJobFutabaDViewModel, SharedViewModel sharedViewModel){
        CommonClickEvent.RecordClickOperation("キャンセル", "割引処理", false);


        if ( (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == true)) {
            PrinterManager printerManager = PrinterManager.getInstance();
            printerManager.setView(view);
//
//            if (_discountJobFutabaDViewModel.getIfBoxManager() == null) {
//                printerManager.PrinterDuplexError(PrinterConst.DuplexPrintStatus_IFBOXERROR);       //IFBOX接続エラー
//                return;
//            }
//
//            if (_discountJobFutabaDViewModel.getIfBoxManager().getIsConnected820() == false)             //820未接続の場合
//            {
//                printerManager.PrinterDuplexError(PrinterConst.DuplexPrintStatus_IFBOXERROR);       //IFBOX接続エラー
//                return;
//            }

            int tmpDiscountMode = _discountJobFutabaDViewModel.getdiscountJobMode().getValue();
            int tmpDiscountJobMode_FutabaD = 0;
//            switch (tmpDiscountMode) {
//                case 0:
//                    tmpDiscountJobMode_FutabaD = IFBoxManager.SendMeterDataStatus_FutabaD.DISCOUNTTYPE_JOB1;
//                    break;
//                case 1:
//                    tmpDiscountJobMode_FutabaD = IFBoxManager.SendMeterDataStatus_FutabaD.DISCOUNTTYPE_JOB2;
//                    break;
//                case 2:
//                    tmpDiscountJobMode_FutabaD = IFBoxManager.SendMeterDataStatus_FutabaD.DISCOUNTTYPE_JOB3;
//                    break;
//                case 3:
//                    tmpDiscountJobMode_FutabaD = IFBoxManager.SendMeterDataStatus_FutabaD.DISCOUNTTYPE_JOB4;
//                    break;
//                case 4:
//                    tmpDiscountJobMode_FutabaD = IFBoxManager.SendMeterDataStatus_FutabaD.DISCOUNTTYPE_JOB5;
//                    break;
//            }

//            _discountJobFutabaDViewModel.getIfBoxManager().send820_FunctionCodeErrorResult(tmpDiscountJobMode_FutabaD, false);        //キャンセルを送信 ACKなし
        }


        //NavigationWrapper.navigateUp(view);

        sharedViewModel.setBackAction(null);

        view.post(() -> {
            //NavigationWrapper.navigateUp(view);

            NavigationWrapper.navigate(view, R.id.action_navigation_discountjob_fd_to_navigation_menu2);
        });

    }
}
//ADD-E BMT S.Oyama 2024/09/06 フタバ双方向向け改修
