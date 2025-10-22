package jp.mcapps.android.multi_payment_terminal.ui.menu;

import static jp.mcapps.android.multi_payment_terminal.AppPreference.isDemoMode;
//import static jp.mcapps.android.multi_payment_terminal.model.IFBoxManager.meterStatNoticeDisposable;
import static jp.mcapps.android.multi_payment_terminal.thread.printer.PrinterConst.DuplexPrintResponseTimerSec;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.RawRes;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.navigation.Navigation;

import com.pos.device.sys.SystemManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.BuildConfig;
import jp.mcapps.android.multi_payment_terminal.CommonClickEvent;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.NavigationWrapper;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.SharedViewModel;
import jp.mcapps.android.multi_payment_terminal.data.Amount;
import jp.mcapps.android.multi_payment_terminal.data.BusinessType;
import jp.mcapps.android.multi_payment_terminal.data.EmoneyOpeningInfo;
import jp.mcapps.android.multi_payment_terminal.data.IFBoxAppModels;
import jp.mcapps.android.multi_payment_terminal.data.OptionService;
import jp.mcapps.android.multi_payment_terminal.data.TransMap;
import jp.mcapps.android.multi_payment_terminal.database.DBManager;
import jp.mcapps.android.multi_payment_terminal.database.DemoDatabase;
import jp.mcapps.android.multi_payment_terminal.database.DemoValidationCheckDatabase;
import jp.mcapps.android.multi_payment_terminal.database.LocalDatabase;
import jp.mcapps.android.multi_payment_terminal.database.ValidationCheckDatabase;
import jp.mcapps.android.multi_payment_terminal.database.history.error.ErrorStackingDao;
import jp.mcapps.android.multi_payment_terminal.database.history.error.ErrorStackingData;
import jp.mcapps.android.multi_payment_terminal.database.history.slip.SlipDao;
import jp.mcapps.android.multi_payment_terminal.database.history.slip.SlipData;
import jp.mcapps.android.multi_payment_terminal.error.DomainErrors;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketGateSettingsDao;
//import jp.mcapps.android.multi_payment_terminal.model.CashChecker;
//import jp.mcapps.android.multi_payment_terminal.model.CreditChecker;
import jp.mcapps.android.multi_payment_terminal.model.DiscountInfo;
import jp.mcapps.android.multi_payment_terminal.model.EmoneyChecker;
import jp.mcapps.android.multi_payment_terminal.model.ErrorStackingRepository;
//import jp.mcapps.android.multi_payment_terminal.model.IFBoxManager;
//import jp.mcapps.android.multi_payment_terminal.model.JremOpener;
import jp.mcapps.android.multi_payment_terminal.model.McTerminal;
import jp.mcapps.android.multi_payment_terminal.model.OkicaChecker;
import jp.mcapps.android.multi_payment_terminal.model.QRChecker;
import jp.mcapps.android.multi_payment_terminal.model.SoundManager;
import jp.mcapps.android.multi_payment_terminal.model.ValidationCheckChecker;
import jp.mcapps.android.multi_payment_terminal.model.Validator;
//import jp.mcapps.android.multi_payment_terminal.model.WatariChecker;
import jp.mcapps.android.multi_payment_terminal.model.pos.ProductRepository;
// import jp.mcapps.android.multi_payment_terminal.model.ticket.TicketRepository;
import jp.mcapps.android.multi_payment_terminal.service.GetGpsService;
import jp.mcapps.android.multi_payment_terminal.service.GetRadioService;
//import jp.mcapps.android.multi_payment_terminal.service.PeriodicErrorCheckService;
//import jp.mcapps.android.multi_payment_terminal.service.PeriodicGateCheckService;
import jp.mcapps.android.multi_payment_terminal.thread.printer.PrinterConst;
import jp.mcapps.android.multi_payment_terminal.thread.printer.PrinterManager;
import jp.mcapps.android.multi_payment_terminal.ui.Converters;
import jp.mcapps.android.multi_payment_terminal.ui.amount_input.AmountInputSeparationPayFDViewModel;
import jp.mcapps.android.multi_payment_terminal.ui.dialog.ConfirmDialog;
import jp.mcapps.android.multi_payment_terminal.ui.dialog.ErrorDialog;
import jp.mcapps.android.multi_payment_terminal.ui.dialog.SuccessDialog;
import jp.mcapps.android.multi_payment_terminal.ui.discount.DiscountJobFutabaDViewModel;
import jp.mcapps.android.multi_payment_terminal.ui.error.CommonErrorDialog;
import jp.mcapps.android.multi_payment_terminal.ui.error.CommonErrorEventHandlers;
import jp.mcapps.android.multi_payment_terminal.util.BitmapSaver;
import jp.mcapps.android.multi_payment_terminal.webapi.ifbox.data.Version;
import timber.log.Timber;

//ADD-S BMT S.Oyama 2024/09/12 フタバ双方向向け改修
//class MenuEventHandlersImpl implements MenuEventHandlers {
public class MenuEventHandlersImpl implements MenuEventHandlers {
    //ADD-E BMT S.Oyama 2024/09/12 フタバ双方向向け改修
    private MainApplication _app = MainApplication.getInstance();
    private final Fragment _fragment;
    private final MenuViewModel _menuViewModel;
    private SoundManager _soundManager = SoundManager.getInstance();

    //ADD-S BMT S.Oyama 2024/10/11 フタバ双方向向け改修
    private static Disposable _meterDataV4ErrorDisposable = null;
    private static Disposable _meterDataV4InfoDisposable = null;
    private static Disposable _meterStatNoticeDisposable = null;
    @SuppressWarnings("deprecation")
    private ProgressDialog _progressDialog;
    private Thread _thread;
    //ADD-E BMT S.Oyama 2024/10/11 フタバ双方向向け改修


    public MenuEventHandlersImpl(Fragment fragment, MenuViewModel menuViewModel) {
        _fragment = fragment;
        _menuViewModel = menuViewModel;
    }

    @Override
    public void navigateMain(Fragment fragment, int id) {
        final FragmentActivity activity = fragment.getActivity();
        if (activity == null) return;

        fragment.requireActivity().runOnUiThread(() -> {
            NavigationWrapper.navigate(activity, R.id.fragment_main_nav_host, id);
        });
    }

    @Override
    public void navigateMain(View view, int id) {
        CommonClickEvent.RecordButtonClickOperation(view, true);
        Activity activity = (Activity) view.getContext();
        if (activity == null) return;

        view.post(() -> {
            NavigationWrapper.navigate(activity, R.id.fragment_main_nav_host, id);
        });
    }

    @Override
    public void navigateMainWithAmountFix(View view, int id) {
        CommonClickEvent.RecordButtonClickOperation(view, true);
        Activity activity = (Activity) view.getContext();
        if (activity == null) return;

        Amount.fix();
        view.post(() -> {
            NavigationWrapper.navigate(activity, R.id.fragment_main_nav_host, id);
        });
    }

    @Override
    public void navigateMenu(View view, int id) {
        CommonClickEvent.RecordButtonClickOperation(view, true);
        view.post(() -> {
            NavigationWrapper.navigate(view, id);
        });
    }

    @Override
    public void navigateHomeOperation(View view) {
        CommonClickEvent.RecordButtonClickOperation(view, true);
        final SlipData[] slipData = {new SlipData()};
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                SlipDao dao = DBManager.getSlipDao();

                slipData[0] = dao.getLatestOne();
            }
        });
        thread.start();
        try {
            thread.join();

            Bundle args = new Bundle();
            args.putInt("SLIP_ID", slipData[0].id);

            view.post(() -> {
                NavigationWrapper.navigate((Activity) view.getContext(), R.id.fragment_main_nav_host,
                        R.id.action_navigation_menu_to_navigation_history_transaction_detail, args);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void backMenu(View view) {
        CommonClickEvent.RecordButtonClickOperation(view, true);
        view.post(() -> {
            NavigationWrapper.popBackStack(view);
        });
    }

    @Override
    public void backMenu(View view, boolean showHead) {
        CommonClickEvent.RecordButtonClickOperation(view, true);
        view.post(() -> {
            NavigationWrapper.popBackStack(view);
        });
    }

    private void showDialog(Activity activity, String message) {
        new AlertDialog.Builder(activity)
                .setIcon(R.drawable.ic_success)
                .setTitle("正常終了")
                .setMessage(message)
                .setPositiveButton("閉じる", null)
                .show();
    }

    @Override
    public void showDialog(View view, String message) {
        Activity activity = (Activity) view.getContext();
        if (activity != null) {
            showDialog(activity, message);
        }
    }

    @Override
    public void showDialog(Fragment fragment, String message) {
        Activity activity = fragment.getActivity();
        if (activity != null) {
            showDialog(activity, message);
        }
    }

    //ADD-S BMT S.Oyama 2024/10/09 フタバ双方向向け改修
    /******************************************************************************/
    /*!
     * @brief  クレカ決済処理（分別処理時の分岐）
     * @note   クレカ決済処理
     * @param [in] View view
     * @param [in] SharedViewModel sharedViewModel
     * @retval なし
     * @return
     * @private
     */

    /******************************************************************************/
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void navigateToCreditCardScanSeparation(View view, SharedViewModel sharedViewModel) {
        navigateToCreditCardScan(view, sharedViewModel);
    }
    //ADD-E BMT S.Oyama 2024/10/09 フタバ双方向向け改修

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void navigateToCreditCardScan(View view, SharedViewModel sharedViewModel) {
        CommonClickEvent.RecordButtonClickOperation(view, true);
        Amount.fix();

        Activity activity = (Activity) view.getContext();
        if (activity == null) return;
        AppPreference.setTransactionTerminalType();

        final CommonErrorDialog dialog = new CommonErrorDialog();

        final BusinessType type = BusinessType.PAYMENT;
        _app.setBusinessType(type);

        Handler handler = new Handler(Looper.getMainLooper());

        //クレジット決済前チェック
        //売上送信で時間がかかる場合があるため、メインスレッドをロックさせない
//        CreditChecker checker = new CreditChecker();
//        checker.setListener(errCode -> handler.post(() -> {
//            sharedViewModel.setLoading(false);
//            //決済前チェックの結果を確認
//            if (errCode != null) {
//                //エラーの場合はダイアログ表示して終了
//                dialog.ShowErrorMessage(activity, errCode);
//                return;
//            }
//
//            //ADD-S BMT S.Oyama 2024/11/05 フタバ双方向向け改修
//            //最後のチェックとして、直前取引が1分以内に同じ金額であったら2重決済じゃないかの確認ダイアログを出して「はい」で決済に進む、エラーコード2009
//            lastCheck(activity, dialog, null, R.id.action_navigation_menu_to_navigation_credit_card_scan);
//            //ADD-E BMT S.Oyama 2024/11/05 フタバ双方向向け改修
//
//        }));

        sharedViewModel.setLoading(true);
        //checker.check(view, type); //クレジット決済前チェック開始
    }

    //ADD-S BMT S.Oyama 2024/09/12 フタバ双方向向け改修
    /******************************************************************************/
    /*!
     * @brief  電子マネー決済前段処理（分別処理時の分岐）　SUICA
     * @note   分別処理時は，金額入力画面へ分岐．旧来は，決済処理へ分岐
     * @param [in] View view
     * @param [in] BusinessType type
     * @retval なし
     * @return
     * @private
     */

    /******************************************************************************/
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void navigateToEmoneySuicaPrestageProcess(View view, BusinessType type, SharedViewModel sharedViewModel) {
        navigateToEmoneySuica(view, type);
    }

    //分別専用
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void navigateToEmoneySuicaSeparation(View view, BusinessType type) {
        CommonClickEvent.RecordButtonClickOperation(view, true);
        Amount.fix();

        Activity activity = (Activity) view.getContext();
        if (activity == null) return;
        AppPreference.setTransactionTerminalType();

        final CommonErrorDialog dialog = new CommonErrorDialog();

        String errorCode = EmoneyChecker.check(view, _app.getString(R.string.money_brand_suica), type);

        if (errorCode != null) {
            dialog.ShowErrorMessage(activity, errorCode);
            return;
        }

        //最後のチェックとして、直前取引が1分以内に同じ金額であったら2重決済じゃないかの確認ダイアログを出して「はい」で決済に進む、エラーコード2009
        //lastCheckExt(activity, dialog, type, R.id.action_navigation_separationpay_to_navigation_emoney_suica, R.id.fragment_amount_input_separationpay_fd);
    }
    //ADD-E BMT S.Oyama 2024/09/12 フタバ双方向向け改修

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void navigateToEmoneySuica(View view, BusinessType type) {
        CommonClickEvent.RecordButtonClickOperation(view, true);
        Amount.fix();

        Activity activity = (Activity) view.getContext();
        if (activity == null) return;
        AppPreference.setTransactionTerminalType();

        final CommonErrorDialog dialog = new CommonErrorDialog();

        String errorCode = EmoneyChecker.check(view, _app.getString(R.string.money_brand_suica), type);

        if (errorCode != null) {
            dialog.ShowErrorMessage(activity, errorCode);
            return;
        }

        //最後のチェックとして、直前取引が1分以内に同じ金額であったら2重決済じゃないかの確認ダイアログを出して「はい」で決済に進む、エラーコード2009
        //lastCheck(activity, dialog, type, R.id.action_navigation_menu_to_navigation_emoney_suica);
    }

    //ADD-S BMT S.Oyama 2024/09/12 フタバ双方向向け改修
    /******************************************************************************/
    /*!
     * @brief  電子マネー決済前段処理（分別処理時の分岐）　SUICA
     * @note   分別処理時は，金額入力画面へ分岐．旧来は，決済処理へ分岐
     * @param [in] View view
     * @param [in] BusinessType type
     * @retval なし
     * @return
     * @private
     */

    /******************************************************************************/
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void navigateToEmoneyIdPrestageProcess(View view, BusinessType type, SharedViewModel sharedViewModel) {
        navigateToEmoneyId(view, type);
    }

    //分別向け
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void navigateToEmoneyIdSeparation(View view, BusinessType type) {
        CommonClickEvent.RecordButtonClickOperation(view, true);
        Amount.fix();

        Activity activity = (Activity) view.getContext();
        if (activity == null) return;
        AppPreference.setTransactionTerminalType();

        final CommonErrorDialog dialog = new CommonErrorDialog();

        String errorCode = EmoneyChecker.check(view, _app.getString(R.string.money_brand_id), type);
        if (errorCode != null) {
            dialog.ShowErrorMessage(activity, errorCode);
            return;
        }

        //最後のチェックとして、直前取引が1分以内に同じ金額であったら2重決済じゃないかの確認ダイアログを出して「はい」で決済に進む、エラーコードXX
        //lastCheckExt(activity, dialog, type, R.id.action_navigation_separationpay_to_navigation_emoney_id, R.id.fragment_amount_input_separationpay_fd);
    }
    //ADD-E BMT S.Oyama 2024/09/12 フタバ双方向向け改修

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void navigateToEmoneyId(View view, BusinessType type) {
        CommonClickEvent.RecordButtonClickOperation(view, true);
        Amount.fix();

        Activity activity = (Activity) view.getContext();
        if (activity == null) return;
        AppPreference.setTransactionTerminalType();

        final CommonErrorDialog dialog = new CommonErrorDialog();

        String errorCode = EmoneyChecker.check(view, _app.getString(R.string.money_brand_id), type);
        if (errorCode != null) {
            dialog.ShowErrorMessage(activity, errorCode);
            return;
        }

        //最後のチェックとして、直前取引が1分以内に同じ金額であったら2重決済じゃないかの確認ダイアログを出して「はい」で決済に進む、エラーコードXX
        //lastCheck(activity, dialog, type, R.id.action_navigation_menu_to_navigation_emoney_id);
    }

    //ADD-S BMT S.Oyama 2024/09/12 フタバ双方向向け改修
    /******************************************************************************/
    /*!
     * @brief  電子マネー決済前段処理（分別処理時の分岐）　WAON
     * @note   分別処理時は，金額入力画面へ分岐．旧来は，決済処理へ分岐
     * @param [in] View view
     * @param [in] BusinessType type
     * @retval なし
     * @return
     * @private
     */

    /******************************************************************************/
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void navigateToEmoneyWaonPrestageProcess(View view, BusinessType type, SharedViewModel sharedViewModel) {
        navigateToEmoneyWaon(view, type);
    }

    //分別向け
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void navigateToEmoneyWaonSeparation(View view, BusinessType type) {
        CommonClickEvent.RecordButtonClickOperation(view, true);
        Amount.fix();

        Activity activity = (Activity) view.getContext();
        if (activity == null) return;
        AppPreference.setTransactionTerminalType();

        final CommonErrorDialog dialog = new CommonErrorDialog();

        String errorCode = EmoneyChecker.check(view, _app.getString(R.string.money_brand_waon), type);
        if (errorCode != null) {
            dialog.ShowErrorMessage(activity, errorCode);
            return;
        }

        //最後のチェックとして、直前取引が1分以内に同じ金額であったら2重決済じゃないかの確認ダイアログを出して「はい」で決済に進む、エラーコードXX
        //lastCheckExt(activity, dialog, type, R.id.action_navigation_separationpay_to_navigation_emoney_waon, R.id.fragment_amount_input_separationpay_fd);
    }
    //ADD-E BMT S.Oyama 2024/09/12 フタバ双方向向け改修

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void navigateToEmoneyWaon(View view, BusinessType type) {
        CommonClickEvent.RecordButtonClickOperation(view, true);
        Amount.fix();

        Activity activity = (Activity) view.getContext();
        if (activity == null) return;
        AppPreference.setTransactionTerminalType();

        final CommonErrorDialog dialog = new CommonErrorDialog();

        String errorCode = EmoneyChecker.check(view, _app.getString(R.string.money_brand_waon), type);
        if (errorCode != null) {
            dialog.ShowErrorMessage(activity, errorCode);
            return;
        }

        //最後のチェックとして、直前取引が1分以内に同じ金額であったら2重決済じゃないかの確認ダイアログを出して「はい」で決済に進む、エラーコードXX
        //lastCheck(activity, dialog, type, R.id.action_navigation_menu_to_navigation_emoney_waon);
    }

    //ADD-S BMT S.Oyama 2024/09/12 フタバ双方向向け改修
    /******************************************************************************/
    /*!
     * @brief  電子マネー決済前段処理（分別処理時の分岐）　EDY
     * @note   分別処理時は，金額入力画面へ分岐．旧来は，決済処理へ分岐
     * @param [in] View view
     * @param [in] BusinessType type
     * @retval なし
     * @return
     * @private
     */

    /******************************************************************************/
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void navigateToEmoneyEdyPrestageProcess(View view, BusinessType type, SharedViewModel sharedViewModel) {
        navigateToEmoneyEdy(view, type);
    }

    //分別向け
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void navigateToEmoneyEdySeparation(View view, BusinessType type) {
        CommonClickEvent.RecordButtonClickOperation(view, true);
        Amount.fix();

        Activity activity = (Activity) view.getContext();
        if (activity == null) return;
        AppPreference.setTransactionTerminalType();

        final CommonErrorDialog dialog = new CommonErrorDialog();

        String errorCode = EmoneyChecker.check(view, _app.getString(R.string.money_brand_edy), type);

        if (errorCode != null) {
            dialog.ShowErrorMessage(activity, errorCode);
            return;
        }

        // Edyは初回通信業務を行ってない場合は取引させない
        if (!AppPreference.isDemoMode() && EmoneyOpeningInfo.getEdy().initCommunicationFlg) {
            dialog.ShowErrorMessage(activity, _app.getString(
                    R.string.error_type_edy_before_init_communication_error));
            return;
        }

        //最後のチェックとして、直前取引が1分以内に同じ金額であったら2重決済じゃないかの確認ダイアログを出して「はい」で決済に進む、エラーコード2009
        //lastCheckExt(activity, dialog, type, R.id.action_navigation_separationpay_to_navigation_emoney_edy, R.id.fragment_amount_input_separationpay_fd);
    }
    //ADD-E BMT S.Oyama 2024/09/12 フタバ双方向向け改修

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void navigateToEmoneyEdy(View view, BusinessType type) {
        CommonClickEvent.RecordButtonClickOperation(view, true);
        Amount.fix();

        Activity activity = (Activity) view.getContext();
        if (activity == null) return;
        AppPreference.setTransactionTerminalType();

        final CommonErrorDialog dialog = new CommonErrorDialog();

        String errorCode = EmoneyChecker.check(view, _app.getString(R.string.money_brand_edy), type);

        if (errorCode != null) {
            dialog.ShowErrorMessage(activity, errorCode);
            return;
        }

        // Edyは初回通信業務を行ってない場合は取引させない
        if (!AppPreference.isDemoMode() && EmoneyOpeningInfo.getEdy().initCommunicationFlg) {
            dialog.ShowErrorMessage(activity, _app.getString(
                    R.string.error_type_edy_before_init_communication_error));
            return;
        }

        //最後のチェックとして、直前取引が1分以内に同じ金額であったら2重決済じゃないかの確認ダイアログを出して「はい」で決済に進む、エラーコード2009
        //lastCheck(activity, dialog, type, R.id.action_navigation_menu_to_navigation_emoney_edy);
    }

    //ADD-S BMT S.Oyama 2024/09/12 フタバ双方向向け改修
    /******************************************************************************/
    /*!
     * @brief  電子マネー決済前段処理（分別処理時の分岐）　QUICKPAY
     * @note   分別処理時は，金額入力画面へ分岐．旧来は，決済処理へ分岐
     * @param [in] View view
     * @param [in] BusinessType type
     * @retval なし
     * @return
     * @private
     */

    /******************************************************************************/
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void navigateToEmoneyQuicPayPrestageProcess(View view, BusinessType type, SharedViewModel sharedViewModel) {
        navigateToEmoneyQuicPay(view, type);
    }

    //分別向け
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void navigateToEmoneyQuicPaySeparation(View view, BusinessType type) {
        CommonClickEvent.RecordButtonClickOperation(view, true);
        Amount.fix();

        Activity activity = (Activity) view.getContext();
        if (activity == null) return;
        AppPreference.setTransactionTerminalType();

        final CommonErrorDialog dialog = new CommonErrorDialog();

        String errorCode = EmoneyChecker.check(view, _app.getString(R.string.money_brand_qp), type);

        if (errorCode != null) {
            dialog.ShowErrorMessage(activity, errorCode);
            return;
        }

        //最後のチェックとして、直前取引が1分以内に同じ金額であったら2重決済じゃないかの確認ダイアログを出して「はい」で決済に進む、エラーコード2009
        //lastCheckExt(activity, dialog, type, R.id.action_navigation_separationpay_to_navigation_emoney_quicpay, R.id.fragment_amount_input_separationpay_fd);
    }
    //ADD-E BMT S.Oyama 2024/09/12 フタバ双方向向け改修

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void navigateToEmoneyQuicPay(View view, BusinessType type) {
        CommonClickEvent.RecordButtonClickOperation(view, true);
        Amount.fix();

        Activity activity = (Activity) view.getContext();
        if (activity == null) return;
        AppPreference.setTransactionTerminalType();

        final CommonErrorDialog dialog = new CommonErrorDialog();

        String errorCode = EmoneyChecker.check(view, _app.getString(R.string.money_brand_qp), type);

        if (errorCode != null) {
            dialog.ShowErrorMessage(activity, errorCode);
            return;
        }

        //最後のチェックとして、直前取引が1分以内に同じ金額であったら2重決済じゃないかの確認ダイアログを出して「はい」で決済に進む、エラーコード2009
        //lastCheck(activity, dialog, type, R.id.action_navigation_menu_to_navigation_emoney_quicpay);
    }

    //ADD-S BMT S.Oyama 2024/09/12 フタバ双方向向け改修
    /******************************************************************************/
    /*!
     * @brief  電子マネー決済前段処理（分別処理時の分岐）　NANACO
     * @note   分別処理時は，金額入力画面へ分岐．旧来は，決済処理へ分岐
     * @param [in] View view
     * @param [in] BusinessType type
     * @retval なし
     * @return
     * @private
     */

    /******************************************************************************/
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void navigateToEmoneyNanacoPrestageProcess(View view, BusinessType type, SharedViewModel sharedViewModel) {
        navigateToEmoneyNanaco(view, type);
    }

    //分別向け
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void navigateToEmoneyNanacoSeparation(View view, BusinessType type) {
        CommonClickEvent.RecordButtonClickOperation(view, true);
        Amount.fix();

        Activity activity = (Activity) view.getContext();
        if (activity == null) return;
        AppPreference.setTransactionTerminalType();

        final CommonErrorDialog dialog = new CommonErrorDialog();

        String errorCode = EmoneyChecker.check(view, _app.getString(R.string.money_brand_nanaco), type);

        if (errorCode != null) {
            dialog.ShowErrorMessage(activity, errorCode);
            return;
        }

        //最後のチェックとして、直前取引が1分以内に同じ金額であったら2重決済じゃないかの確認ダイアログを出して「はい」で決済に進む、エラーコード2009
        //lastCheckExt(activity, dialog, type, R.id.action_navigation_separationpay_to_navigation_emoney_nanaco, R.id.fragment_amount_input_separationpay_fd);
    }
    //ADD-E BMT S.Oyama 2024/09/12 フタバ双方向向け改修

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void navigateToEmoneyNanaco(View view, BusinessType type) {
        CommonClickEvent.RecordButtonClickOperation(view, true);
        Amount.fix();

        Activity activity = (Activity) view.getContext();
        if (activity == null) return;
        AppPreference.setTransactionTerminalType();

        final CommonErrorDialog dialog = new CommonErrorDialog();

        String errorCode = EmoneyChecker.check(view, _app.getString(R.string.money_brand_nanaco), type);

        if (errorCode != null) {
            dialog.ShowErrorMessage(activity, errorCode);
            return;
        }

        //最後のチェックとして、直前取引が1分以内に同じ金額であったら2重決済じゃないかの確認ダイアログを出して「はい」で決済に進む、エラーコード2009
        //lastCheck(activity, dialog, type, R.id.action_navigation_menu_to_navigation_emoney_nanaco);
    }

    //ADD-S BMT S.Oyama 2024/09/12 フタバ双方向向け改修
    /******************************************************************************/
    /*!
     * @brief  電子マネー決済前段処理（分別処理時の分岐）　OKIKA
     * @note   分別処理時は，金額入力画面へ分岐．旧来は，決済処理へ分岐
     * @param [in] View view
     * @param [in] BusinessType type
     * @retval なし
     * @return
     * @private
     */

    /******************************************************************************/
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void navigateToEmoneyOkicaPrestageProcess(View view, BusinessType type, SharedViewModel sharedViewModel) {
        navigateToEmoneyOkica(view, type);
    }

    //分別向け
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void navigateToEmoneyOkicaSeparation(View view, BusinessType type) {
        CommonClickEvent.RecordButtonClickOperation(view, true);
        Amount.fix();

        Activity activity = (Activity) view.getContext();
        if (activity == null) return;
        AppPreference.setTransactionTerminalType();

        final CommonErrorDialog dialog = new CommonErrorDialog();

        String errorCode = OkicaChecker.check(view, type);

        if (errorCode != null) {
            dialog.ShowErrorMessage(activity, errorCode);
            return;
        }

        //最後のチェックとして、直前取引が1分以内に同じ金額であったら2重決済じゃないかの確認ダイアログを出して「はい」で決済に進む、エラーコード2009
        lastCheckExt(activity, dialog, type, R.id.action_navigation_separationpay_to_navigation_emoney_okica, R.id.fragment_amount_input_separationpay_fd);
    }
    //ADD-E BMT S.Oyama 2024/09/12 フタバ双方向向け改修

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void navigateToEmoneyOkica(View view, BusinessType type) {
        CommonClickEvent.RecordButtonClickOperation(view, true);
        Amount.fix();

        Activity activity = (Activity) view.getContext();
        if (activity == null) return;
        AppPreference.setTransactionTerminalType();

        final CommonErrorDialog dialog = new CommonErrorDialog();

        String errorCode = OkicaChecker.check(view, type);

        if (errorCode != null) {
            dialog.ShowErrorMessage(activity, errorCode);
            return;
        }

        //最後のチェックとして、直前取引が1分以内に同じ金額であったら2重決済じゃないかの確認ダイアログを出して「はい」で決済に進む、エラーコード2009
        lastCheck(activity, dialog, type, R.id.action_navigation_menu_to_navigation_emoney_okica);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void navigateToInputChargeAmount(View view, String brand) {
        CommonClickEvent.RecordButtonClickOperation(view, true);

        final Bundle args = new Bundle();
        args.putString("brand", brand);

        Activity activity = (Activity) view.getContext();
        if (activity == null) return;
        AppPreference.setTransactionTerminalType();

        final CommonErrorDialog dialog = new CommonErrorDialog();
        String errorCode;

        if (brand.equals(_app.getString(R.string.money_brand_okica))) {
            errorCode = OkicaChecker.check(view, BusinessType.CHARGE);
        } else {
            errorCode = EmoneyChecker.check(view, brand, BusinessType.CHARGE);
        }

        if (errorCode != null) {
            dialog.ShowErrorMessage(activity, errorCode);
            return;
        }

        view.post(() -> {
            NavigationWrapper.navigate(activity, R.id.fragment_main_nav_host, R.id.action_navigation_menu_to_navigation_charge_amount_input, args);
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void navigateToValidationCheck(View view) {
        // センターから配信される名称をセットする
        final OptionService service = _app.getOptionService();
        int index = service != null ? service.indexOfFunc(OptionService.FuncIds.MAGNETIC_CARD_VALIDATION) : -1;
        String operationName = index >= 0
                ? service.getFunc(index).getDisplayName()
                : _app.getString(R.string.btn_other_validation);

        CommonClickEvent.RecordClickOperation(operationName, true);
        Amount.fix();

        Activity activity = (Activity) view.getContext();
        if (activity == null) return;

        final CommonErrorDialog dialog = new CommonErrorDialog();

        String errorCode = ValidationCheckChecker.check(view);

        if (errorCode != null) {
            dialog.ShowErrorMessage(activity, errorCode);
            return;
        }

        NavigationWrapper.navigate(
                activity, R.id.fragment_main_nav_host, R.id.action_navigation_menu_to_navigation_validation_check);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void navigateToWatari(View view, BusinessType type, SharedViewModel sharedViewModel) {
        CommonClickEvent.RecordButtonClickOperation(view, true);
        Amount.fix();

        Activity activity = (Activity) view.getContext();
        if (activity == null) return;

        final CommonErrorDialog dialog = new CommonErrorDialog();

        Handler handler = new Handler(Looper.getMainLooper());

//        WatariChecker checker = new WatariChecker();
//        checker.setListener(errCode -> handler.post(() -> {
//            sharedViewModel.setLoading(false);
//            //決済前チェックの結果を確認
//            if (errCode != null) {
//                //エラーの場合はダイアログ表示して終了
//                dialog.ShowErrorMessage(activity, errCode);
//                return;
//            }
//
//            //最後のチェックとして、直前取引が1分以内に同じ金額であったら2重決済じゃないかの確認ダイアログを出して「はい」で決済に進む、エラーコード2009
//            lastCheck(activity, dialog, type, R.id.action_navigation_menu_to_navigation_watari);
//        }));

        sharedViewModel.setLoading(true);
        // checker.check(view, type); //クレジット決済前チェック開始
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void navigateToProductSelect(View view) {
        CommonClickEvent.RecordClickOperation("商品カート", true);
        // センターから配信される名称をセットする
//        final OptionService service = _app.getOptionService();
//        int index = service != null ? service.indexOfFunc(OptionService.FuncIds.MAGNETIC_CARD_VALIDATION) : -1;
//        String operationName = index >= 0
//                ? service.getFunc(index).getDisplayName()
//                : _app.getString(R.string.btn_other_validation);

//        CommonClickEvent.RecordClickOperation(operationName, true);
//        Amount.fix();
        CommonClickEvent.RecordButtonClickOperation(view, true);
        Activity activity = (Activity) view.getContext();
        if (activity == null) return;

//        final CommonErrorDialog dialog = new CommonErrorDialog();
//
//        String errorCode = ValidationCheckChecker.check(view);
//
//        if (errorCode != null) {
//            dialog.ShowErrorMessage(activity, errorCode);
//            return;
//        }

//        // 商品選択ではなくてカート画面に遷移する
//        NavigationWrapper.navigate(
//                activity, R.id.fragment_main_nav_host, R.id.action_navigation_menu_to_fragment_cart_confirm);
    }

    //ADD-S BMT S.Oyama 2024/10/09 フタバ双方向向け改修
    /******************************************************************************/
    /*!
     * @brief  QR決済処理（分別処理時の分岐）
     * @note   QR決済処理
     * @param [in] View view
     * @param [in] SharedViewModel sharedViewModel
     * @retval なし
     * @return
     * @private
     */

    /******************************************************************************/
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void navigateToQRSeparation(View view, SharedViewModel sharedViewModel) {
        //navigateToQR(view, sharedViewModel);

        // 残高照会はないので支払決め打ち
        final BusinessType type = BusinessType.PAYMENT;

        CommonClickEvent.RecordButtonClickOperation(view, true);
        Amount.fix();

        Activity activity = (Activity) view.getContext();
        if (activity == null) return;
        AppPreference.setTransactionTerminalType();

        final CommonErrorDialog dialog = new CommonErrorDialog();

        Completable.create(emitter -> {
                    final String errorCode = QRChecker.check(view, type);

                    if (!emitter.isDisposed()) {
                        if (errorCode != null) {
                            emitter.onError(new Throwable(errorCode));
                        } else {
                            emitter.onComplete();
                        }
                    }
                }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(d -> {
                    sharedViewModel.setLoading(true);
                })
                .doFinally(() -> {
                    sharedViewModel.setLoading(false);
                })
                .subscribe(() -> {
                            //最後のチェックとして、直前取引が1分以内に同じ金額であったら2重決済じゃないかの確認ダイアログを出して「はい」で決済に進む、エラーコード2009
                            lastCheckExt(activity, dialog, type, R.id.action_navigation_separationpay_to_navigation_qr, R.id.fragment_amount_input_separationpay_fd);
                        }, e -> {
                            String errorCode = e.getMessage();
                            dialog.ShowErrorMessage(activity, errorCode);
                        }
                );
    }
    //ADD-E BMT S.Oyama 2024/10/09 フタバ双方向向け改修

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void navigateToQR(View view, SharedViewModel sharedViewModel) {
        // 残高照会はないので支払決め打ち
        final BusinessType type = BusinessType.PAYMENT;

        CommonClickEvent.RecordButtonClickOperation(view, true);
        Amount.fix();

        Activity activity = (Activity) view.getContext();
        if (activity == null) return;
        AppPreference.setTransactionTerminalType();

        final CommonErrorDialog dialog = new CommonErrorDialog();

        Completable.create(emitter -> {
                    final String errorCode = QRChecker.check(view, type);

                    if (!emitter.isDisposed()) {
                        if (errorCode != null) {
                            emitter.onError(new Throwable(errorCode));
                        } else {
                            emitter.onComplete();
                        }
                    }
                }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(d -> {
                    sharedViewModel.setLoading(true);
                })
                .doFinally(() -> {
                    sharedViewModel.setLoading(false);
                })
                .subscribe(() -> {
                    //最後のチェックとして、直前取引が1分以内に同じ金額であったら2重決済じゃないかの確認ダイアログを出して「はい」で決済に進む、エラーコード2009
                    //lastCheck(activity, dialog, type, R.id.action_navigation_menu_to_navigation_qr_payment);
                }, e -> {
                    String errorCode = e.getMessage();
                    dialog.ShowErrorMessage(activity, errorCode);
                });
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void navigateToCashInput(View view, SharedViewModel sharedViewModel, int id) {
        CommonClickEvent.RecordButtonClickOperation(view, true);
        Activity activity = (Activity) view.getContext();
        if (activity == null) return;
        AppPreference.setTransactionTerminalType();

        // 現金決済に進む場合のチェック
        final CommonErrorDialog dialog = new CommonErrorDialog();

        new Thread(() -> {
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.post(() -> {
                sharedViewModel.setLoading(true);
            });

            String errorCode = CashChecker.check(view);

            handler.post(() -> {
                sharedViewModel.setLoading(false);
            });

            if (errorCode != null) {
                handler.post(() -> {
                    dialog.ShowErrorMessage(activity, errorCode);
                });
                return;
            }

//            if (id == R.id.action_navigation_menu_to_fragment_cash_changer_payment) {
//                final Bundle params = new Bundle();
//                params.putBoolean("isRepay", false);
//                view.post(() -> {
//                    NavigationWrapper.navigate(activity, R.id.fragment_main_nav_host, id, params);
//                });
//            } else if (id == R.id.action_navigation_others_to_fragment_cash_input) {
//                final Bundle params = new Bundle();
//                params.putBoolean("isFixedAmountPostalOrder", true);
//                view.post(() -> {
//                    NavigationWrapper.navigate(activity, R.id.fragment_main_nav_host, id, params);
//                });
//            } else {
//                view.post(() -> {
//                    NavigationWrapper.navigate(activity, R.id.fragment_main_nav_host, id);
//                });
//            }
        }).start();
    }

    @Override
    public void navigateToInputCarId(View view) {
        if (AppPreference.isCarIdInput()) {
            CommonClickEvent.RecordButtonClickOperation(view, true);

            Activity activity = (Activity) view.getContext();
            if (activity == null) return;

            view.post(() -> {
                NavigationWrapper.navigate(activity, R.id.fragment_main_nav_host, R.id.action_navigation_menu_to_navigation_car_id);
            });
        }
    }


    @Override
    public void businessEndConfirm(View view, SharedViewModel sharedViewModel) {
        CommonClickEvent.RecordButtonClickOperation(view, true);

        //確認ダイアログ表示
        new AlertDialog.Builder(view.getContext())
                .setTitle("業務終了")
                .setMessage("業務を終了して電源を切ります\nよろしいですか？")
                .setPositiveButton("はい", new DialogInterface.OnClickListener() {
                    @RequiresApi(api = Build.VERSION_CODES.N)
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        CommonClickEvent.RecordClickOperation("はい", "業務終了", true);
                        businessEnd(view, sharedViewModel);
                    }
                })
                .setNegativeButton("いいえ", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        CommonClickEvent.RecordClickOperation("いいえ", "業務終了", true);
                    }
                })
                .show();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void manualOpening(View view, SharedViewModel sharedViewModel) {
        CommonClickEvent.RecordButtonClickOperation(view, true);

        new Thread(() -> {
            final Handler handler = new Handler(Looper.getMainLooper());

            handler.post(() -> {
                sharedViewModel.setLoading(true);
            });

            final String[] errors = _menuViewModel.manualOpening();
            if (errors.length > 0) {
                final Context context = view.getContext();
                final CommonErrorDialog dialog = new CommonErrorDialog();

                for (String errCode : errors) {
                    handler.post(() -> {
                        dialog.ShowErrorMessage(context, errCode);
                    });
                }
            }

            handler.post(() -> {
                sharedViewModel.setLoading(false);
                if (errors.length == 0) {
                    SuccessDialog.show(view.getContext(), "開局しました");
                }
            });

            removeEmoneyClosedErrorStacking(); //スタッキングされている開局/閉局エラーを削除

//            //ログ送信
//            _menuViewModel.getEventLogger().submit();

        }).start();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void businessEnd(View view, SharedViewModel sharedViewModel) {
        Activity mainActivity = (Activity) view.getContext();
        final AtomicBoolean isJournalEnd = new AtomicBoolean(false);
        final AtomicBoolean isPostPaymentEnd = new AtomicBoolean(false);
        final AtomicBoolean isPostOkicaPaymentEnd = new AtomicBoolean(false);
        final AtomicBoolean isValidationResultSendEnd = new AtomicBoolean(false);
        final TicketGateSettingsDao ticketGateSettingsDao = LocalDatabase.getInstance().ticketGateSettingsDao();
        Timber.i("業務終了");

        // レシート画像ファイルの削除
        BitmapSaver.deleteReceipts();

        // バッテリー状態のチェック
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = view.getContext().registerReceiver(null, intentFilter);
        int batteryLevel = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        Timber.i("battery %d %%", batteryLevel);

        //業務終了日時の設定
        new Thread(() -> {
            SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.JAPANESE);
            String datetimeBusinessEnd = dateFmt.format(new Date());
            DBManager.getAggregateDao().updateAggregateEnd(datetimeBusinessEnd);
        }).start();

        Thread jremJournalThread = new Thread(() -> {
            try {
//                final JremOpener opener = new JremOpener();
//
//                if (AppPreference.isMoneyEdy() && EmoneyOpeningInfo.getEdy() != null) {
//                    opener.journalEdy();
//                    opener.journalEdyResult();
//                }
//
//                if (AppPreference.isMoneyNanaco() && EmoneyOpeningInfo.getNanaco() != null) {
//                    opener.journalNanaco();
//                    opener.journalNanacoResult();
//                }
//
//                if (AppPreference.isMoneyQuicpay() && EmoneyOpeningInfo.getQuicpay() != null) {
//                    opener.journalQUICPay();
//                    opener.journalQUICPayResult();
//                }

                isJournalEnd.set(true);
            } catch (Exception e) {
                isJournalEnd.set(true);
            }
        });

        jremJournalThread.start();

        // 集計印刷
        PrinterManager printerManager = PrinterManager.getInstance();
        if (IFBoxAppModels.isMatch(IFBoxAppModels.OKABE_MS70_D)) {
            // オカベメーターの場合はメーター側で日計を出力する為、ここでは集計クリアのみ実施する
            printerManager.OkabeTotallingClearJudge();  /*Add k.Fukumitsu  2024/1/30 オカベ双_ホーム画面で入庫または業務終了時、集計をクリア*/
            //ADD-S BMT S.Oyama 2024/09/24 フタバ双方向向け改修
        } else if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D)) {
            printerManager.print_AggregateForFutabaDBusinessEnd(view, 0, AppPreference.isAggregateDetail());
            //ADD-E BMT S.Oyama 2024/09/24 フタバ双方向向け改修
        } else {
            printerManager.print_Aggregate(view, 0, AppPreference.isAggregateDetail());
        }

        //MC認証成功している場合のみ
        if (_app.isMcAuthSuccess()) {
            Thread postPaymentThread = new Thread(() -> {
                McTerminal mcTerminal = new McTerminal();
                //端末稼働情報連携
                // 業務終了後は電源を落とすだけで他の処理に影響もない為、このタイミングのエラーは無視
                String postInfoErr = mcTerminal.postTerminalInfo(1);
                //売上情報送信
                // 業務終了後は電源を落とすだけで他の処理に影響もない為、このタイミングのエラーは無視
                String errCode = new McTerminal().postPayment();

                if (AppPreference.isServiceTicket()) {
                    errCode = new McTerminal().postTicketCancel();
                }
                isPostPaymentEnd.set(true);
            });

            postPaymentThread.start();

            // 有効性確認を利用している場合結果送信を行う
            final OptionService s = _app.getOptionService();
            if (s != null && s.indexOfFunc(OptionService.FuncIds.MAGNETIC_CARD_VALIDATION) >= 0) {
                new Validator().sendResult()
                        .subscribeOn(Schedulers.io())
                        .doFinally(() -> {
                            isValidationResultSendEnd.set(true);
                        })
                        .subscribe(() -> {
                        }, e -> {
                        });
            } else {
                isValidationResultSendEnd.set(true);
            }
        } else {
            isPostPaymentEnd.set(true);
            isValidationResultSendEnd.set(true);
        }

        Thread postOkicaPaymentThread = new Thread(() -> {
            // OKICA売上送信
            if (AppPreference.isOkicaCommunicationAvailable()) {
                String mcTerminalErrCode = new McTerminal().postOkicaPayment();
            }
            isPostOkicaPaymentEnd.set(true);
        });
        postOkicaPaymentThread.start();

        //プログレス表示
        sharedViewModel.setLoading(true);

        //UIスレッドがsleepするとサービスの終了ができないため別スレッド
        new Thread(new Runnable() {
            @Override
            public void run() {
                int activeServiceCnt = 0; //起動中のサービスの数

                ActivityManager am = (ActivityManager) ((Activity) view.getContext()).getSystemService(Context.ACTIVITY_SERVICE);
                @SuppressWarnings("deprecation")
                List<ActivityManager.RunningServiceInfo> serviceInfoList = am.getRunningServices(Integer.MAX_VALUE);
                boolean foundRadioService = false;
                boolean foundGpsService = false;
                boolean foundErrorCheckService = false;
                boolean foundGateCheckService = false;

                for (ActivityManager.RunningServiceInfo runningServiceInfo : serviceInfoList) {
                    //サービスが起動しているかを確認
                    if (runningServiceInfo.service.getClassName().equals(GetRadioService.class.getName())) {
                        activeServiceCnt++;
                        foundRadioService = true;
                    } else if (runningServiceInfo.service.getClassName().equals(GetGpsService.class.getName())) {
                        activeServiceCnt++;
                        foundGpsService = true;
                    }

                    if (foundRadioService && foundGpsService && foundErrorCheckService && foundGateCheckService)
                        break;
                }

                Timber.d("active service : %d", activeServiceCnt);
                CountDownLatch serviceLatch = new CountDownLatch(activeServiceCnt);

                //サービスの終了通知を受け取るレシーバ
                BroadcastReceiver receiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        serviceLatch.countDown(); //ラッチをカウントダウン
                    }
                };

                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction("SERVICE_STOP");

                LocalBroadcastManager.getInstance(view.getContext())
                        .registerReceiver(receiver, intentFilter);

                //起動中のバックグラウンドサービスを停止
                if (foundRadioService) {
                    Intent radioService = new Intent(mainActivity.getApplication(), GetRadioService.class);
                    mainActivity.stopService(radioService);
                }

                if (foundGpsService) {
                    Intent gpsService = new Intent(mainActivity.getApplication(), GetGpsService.class);
                    mainActivity.stopService(gpsService);
                }

//                if (foundErrorCheckService) {
//                    Intent errorService = new Intent(mainActivity.getApplication(), PeriodicErrorCheckService.class);
//                    mainActivity.stopService(errorService);
//                }
//
//                if (foundGateCheckService) {
//                    Intent gateService = new Intent(mainActivity.getApplication(), PeriodicGateCheckService.class);
//                    mainActivity.stopService(gateService);
//                }

                try {
                    serviceLatch.await(); //全てのサービスの終了を待機
                    Thread.sleep(1000); //確認用

                    LocalBroadcastManager.getInstance(view.getContext())
                            .unregisterReceiver(receiver);

                    while (!isJournalEnd.get() || !isPostPaymentEnd.get() || !isPostOkicaPaymentEnd.get() || !isValidationResultSendEnd.get()) {
                        Thread.sleep(1000);
                    }

                    // 集計印刷終了検知
                    while (true) {
                        Thread.sleep(1000);
                        if (printerManager.getPrintStatus() == PrinterConst.PrintStatus_IDLE) {
                            if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D)) {
                                PrinterManager.getInstance().FutabaDTotallingClearJudge();
                            }
                            break;
                        }
                    }

                    //業務終了時に改札設定はクリアする
                    ticketGateSettingsDao.deleteAll();

                    //ログ送信
                    _menuViewModel.getEventLogger().submit();

                    //正常シャットダウンフラグON
                    AppPreference.setConfirmNormalShutdown(true);

                    //DBクローズ
                    LocalDatabase.closeDatabase();
                    DemoDatabase.closeDatabase();
                    ValidationCheckDatabase.closeDatabase();
                    DemoValidationCheckDatabase.closeDatabase();

                    //プログレスダイアログ削除
                    sharedViewModel.setLoading(false);

                    //電源オフ　開発用設定ではアプリ終了
                    if (AppPreference.isDeveloperMode()) {
                        mainActivity.finishAndRemoveTask();

                        //プロセスの終了
                        Process.killProcess(Process.myPid());
                    } else {
                        Timber.d("shutdown");
                        SystemManager.shutdown();
                    }

                } catch (InterruptedException e) {
                    //ToDo エラー処理
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    public void sendLog(View view, SharedViewModel sharedViewModel) {
        CommonClickEvent.RecordButtonClickOperation(view, true);

        //端末内部のログを送信する これとは別に自動でもログ送信はする
        //通常はKinesisFirehoseRecorderで送ってフルログはファイルに残してボタン押下時に送るのがいいか？
        //KinesisFirehoseRecorderログ送信
        new Thread(() -> {
            sharedViewModel.setLoading(true);
            _menuViewModel.getEventLogger().submit();
            sharedViewModel.setLoading(false);
            view.post(() -> showDialog(view, "端末情報を送信しました"));
        }).start();
    }

    @Override
    public void navigateToPrepaidApp(View view) {
        Intent launchIntent = new Intent();
        launchIntent.setComponent(new ComponentName(BuildConfig.PREPAID_APP_MODEL, BuildConfig.PREPAID_APP_ACTIVITY));

        try {
            if (launchIntent != null) {
                launchIntent.putExtra("startupType", "main_menu");
                launchIntent.putExtra("amount", Amount.getTotalAmount());
                launchIntent.putExtra("driverCd", AppPreference.getDriverCode());
                launchIntent.putExtra("driverName", AppPreference.getDriverName());
                launchIntent.putExtra("carNo", AppPreference.getMcCarId());
                launchIntent.putExtra("organizationId", AppPreference.getOrganizationId());
                launchIntent.putExtra("deviceId", AppPreference.getMcTermId());
                launchIntent.putExtra("isInput1yen", AppPreference.isInput1yenEnabled());
                launchIntent.putExtra("domain", AppPreference.getPrepaidServiceDomain());
                launchIntent.putExtra("key", AppPreference.getPrepaidServiceKey());
                launchIntent.putExtra("isDemoMode", AppPreference.isDemoMode());
                launchIntent.putExtra("organizationName", AppPreference.getMerchantOffice());
                launchIntent.putExtra("organizationParentName", AppPreference.getMerchantName());
                launchIntent.putExtra("isInputAmount", IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == true ? false : true);
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                Context context = view.getContext();
                context.startActivity(launchIntent);

            } else {
                // アプリがインストールされていない場合の処理
//            Toast.makeText(getContext(), "アプリがインストールされていません", Toast.LENGTH_LONG).show();
                return;
            }
        } catch (Exception e) {
            // 念のため
            return;
        }
    }

    private boolean sameAmountCheck(BusinessType type) {

        final boolean[] result = {false};
        Thread thread = new Thread(() -> {
            SlipDao dao = DBManager.getSlipDao();
            SlipData latestTrans = dao.getLatestOne();

            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MINUTE, -1);
            String date = Converters.dateToString(calendar.getTime());

            //直前取引が1分以内かつ引去かつ同一金額ならtrue, それ以外はfalse
            result[0] = latestTrans != null
                    && latestTrans.transDate.compareTo(date) >= 0
                    && latestTrans.transType == TransMap.TYPE_SALES
                    && latestTrans.transAmount == Amount.getFixedAmount();

            // ポイント付与の時だけ
            if (type == BusinessType.POINT_ADD) {
                result[0] = latestTrans != null
                        && latestTrans.transDate.compareTo(date) >= 0
                        && latestTrans.transType == TransMap.TYPE_POINT
                        && latestTrans.transAmount == Amount.getFixedAmount();
            }

            // 直前取引がクレジット、および、取引結果がNGの場合
            // 同一金額でも警告は表示しない
            if (latestTrans != null &&
                    latestTrans.transBrand.equals(_app.getString(R.string.money_brand_credit)) &&
                    latestTrans.transResult != TransMap.RESULT_SUCCESS) {
                result[0] = false;
            }
        });
        thread.start();

        try {
            thread.join();
        } catch (Exception e) {
            //ToDo エラー処理
            e.printStackTrace();
        }
        return result[0];
    }

    private boolean sameEigyoCountCheck() {

        final boolean[] result = {false};
        Thread thread = new Thread(() -> {
            SlipDao dao = DBManager.getSlipDao();
            SlipData latestTrans = dao.getLatestOne();

            //引去かつ同一営業回数ならtrue, それ以外はfalse
            result[0] = latestTrans != null
                    && latestTrans.transType == TransMap.TYPE_SALES
                    && latestTrans.freeCountOne == Amount.getEigyoCount();

            // 直前取引がクレジット、および、取引結果がNGの場合
            // 同一営業回数でも警告は表示しない
            if (latestTrans != null &&
                    latestTrans.transBrand.equals(_app.getString(R.string.money_brand_credit)) &&
                    latestTrans.transResult != TransMap.RESULT_SUCCESS) {
                result[0] = false;
            }

            if (result[0] == true) {
                Timber.i("同一営業回数で2回以上の決済");
            }
        });
        thread.start();

        try {
            thread.join();
        } catch (Exception e) {
            //ToDo エラー処理
            e.printStackTrace();
        }
        return result[0];
    }

    //ADD-S BMT S.Oyama 2024/09/13 フタバ双方向向け改修
    /******************************************************************************/
    /*!
     * @brief  ２重の引き落としチェックし，問題なければ決済画面へ遷移
     * @note
     * @param [in] Activity activity
     * @param [in] CommonErrorDialog dialog
     * @param [in] BusinessType type
     * @param [in] int navigateId
     * @param [in] int viewID
     * @retval なし
     * @return
     * @private
     */

    /******************************************************************************/
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void lastCheckExt(Activity activity, CommonErrorDialog dialog, BusinessType type, int navigateId, int viewID) {
        Runnable runnable = () -> {
            //画面オフを無効化 MenuFragmentに戻るタイミングで再び有効化
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            //ビジネスタイプがある場合はセット
            if (type != null) {
                MainApplication.getInstance().setBusinessType(type);
            }

            //残高照会ではチェックしない
            if (type != BusinessType.BALANCE && sameAmountCheck(type)) {
                //同一金額の決済あり
                CommonErrorEventHandlers handlers = new CommonErrorEventHandlers() {
                    @Override
                    public void onPositiveClick(String errorCode) {
                        CommonClickEvent.RecordClickOperation("はい", "2重決済確認", true);
                        activity.runOnUiThread(() -> {
                            NavigationWrapper.navigate(activity, viewID, navigateId);
                        });
                    }

                    @Override
                    public void onNegativeClick(String errorCode) {
                    }

                    @Override
                    public void onNeutralClick(String errorCode) {
                    }

                    @Override
                    public void onDismissClick(String errorCode) {
                    }
                };

                dialog.setCommonErrorEventHandlers(handlers);
                // ポイント付与の時だけ
                if (type == BusinessType.POINT_ADD) {
                    dialog.ShowErrorMessage(activity, "5991");
                } else {
                    dialog.ShowErrorMessage(activity, "2009");
                }
            } else if (IFBoxAppModels.isMatch(IFBoxAppModels.OKABE_MS70_D) &&
                    type != BusinessType.BALANCE && sameEigyoCountCheck()) {
                //同一営業回数の決済あり
                CommonErrorEventHandlers handlers = new CommonErrorEventHandlers() {
                    @Override
                    public void onPositiveClick(String errorCode) {
                        CommonClickEvent.RecordClickOperation("はい", "2重決済確認", true);
                        activity.runOnUiThread(() -> {
                            NavigationWrapper.navigate(activity, viewID, navigateId);
                        });
                    }

                    @Override
                    public void onNegativeClick(String errorCode) {
                    }

                    @Override
                    public void onNeutralClick(String errorCode) {
                    }

                    @Override
                    public void onDismissClick(String errorCode) {
                    }
                };

                dialog.setCommonErrorEventHandlers(handlers);
                dialog.ShowErrorMessage(activity, "2095");
                //ADD-S BMT S.Oyama 2024/09/24 フタバ双方向向け改修
            } else if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) &&
                    type != BusinessType.BALANCE && sameEigyoCountCheck()) {
                //同一営業回数の決済あり
                CommonErrorEventHandlers handlers = new CommonErrorEventHandlers() {
                    @Override
                    public void onPositiveClick(String errorCode) {
                        CommonClickEvent.RecordClickOperation("はい", "2重決済確認", true);
                        activity.runOnUiThread(() -> {
                            NavigationWrapper.navigate(activity, viewID, navigateId);
                        });
                    }

                    @Override
                    public void onNegativeClick(String errorCode) {
                    }

                    @Override
                    public void onNeutralClick(String errorCode) {
                    }

                    @Override
                    public void onDismissClick(String errorCode) {
                    }
                };

                dialog.setCommonErrorEventHandlers(handlers);
                dialog.ShowErrorMessage(activity, "2095");
                //ADD-E BMT S.Oyama 2024/09/24 フタバ双方向向け改修
            } else {
                //同一金額の決済なし
                activity.runOnUiThread(() -> {
                    NavigationWrapper.navigate(activity, viewID, navigateId);
                });
            }
        };

        // 定額料金未使用かつ残照ではない場合はメーター金額のチェックを行う
//        if (Amount.getFlatRateAmount() <= 0 && type != BusinessType.BALANCE) {
//            _menuViewModel.checkMeterCharge().subscribe(() -> {
//                int tmpFixedAmount = Amount.getFixedAmount();
//                int tmpTotalAmount = Amount.getTotalAmount();
//                if (tmpFixedAmount == tmpTotalAmount) {
//                    runnable.run();
//                } else {
//                    // メータの金額が更新されていた場合
//                    CommonErrorEventHandlers handlers = new CommonErrorEventHandlers() {
//                        @Override
//                        public void onPositiveClick(String errorCode) {
//                            CommonClickEvent.RecordClickOperation("はい", "メーター料金不一致", true);
//                            runnable.run();
//                        }
//
//                        @Override
//                        public void onNegativeClick(String errorCode) {
//                        }
//
//                        @Override
//                        public void onNeutralClick(String errorCode) {
//                        }
//
//                        @Override
//                        public void onDismissClick(String errorCode) {
//                        }
//                    };
//                    dialog.setCommonErrorEventHandlers(handlers);
//                    dialog.ShowErrorMessage(_fragment.requireContext(), _app.getString(R.string.error_type_ifbox_amount_difference));
//                }
//            }, error -> {
//                // IF-BOXと通信できなかった場合
//                CommonErrorEventHandlers handlers = new CommonErrorEventHandlers() {
//                    @Override
//                    public void onPositiveClick(String errorCode) {
//                        CommonClickEvent.RecordClickOperation("はい", "メーター接続エラー", true);
//                        runnable.run();
//                    }
//
//                    @Override
//                    public void onNegativeClick(String errorCode) {
//                    }
//
//                    @Override
//                    public void onNeutralClick(String errorCode) {
//                    }
//
//                    @Override
//                    public void onDismissClick(String errorCode) {
//                    }
//                };
//                dialog.setCommonErrorEventHandlers(handlers);
//                dialog.ShowErrorMessage(_fragment.requireContext(), _app.getString(R.string.error_type_ifbox_connection_error));
//            });
//        } else {
//            runnable.run();
//        }
    }
    //ADD-E BMT S.Oyama 2024/09/13 フタバ双方向向け改修

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void lastCheck(Activity activity, CommonErrorDialog dialog, BusinessType type, int navigateId) {

        //ADD-S BMT S.Oyama 2024/10/23 フタバ双方向向け改修
        PrinterManager tmpPrinterManager = PrinterManager.getInstance();
        tmpPrinterManager.changePrintStatusEx(PrinterConst.PrintStatus_IDLE);           //プリンタステータスをアイドル化する
        //ADD-E BMT S.Oyama 2024/10/23 フタバ双方向向け改修

        Runnable runnable = () -> {
            //画面オフを無効化 MenuFragmentに戻るタイミングで再び有効化
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            //ビジネスタイプがある場合はセット
            if (type != null) {
                MainApplication.getInstance().setBusinessType(type);
            }

            //残高照会ではチェックしない
            if (type != BusinessType.BALANCE && sameAmountCheck(type)) {
                //同一金額の決済あり
                CommonErrorEventHandlers handlers = new CommonErrorEventHandlers() {
                    @Override
                    public void onPositiveClick(String errorCode) {
                        CommonClickEvent.RecordClickOperation("はい", "2重決済確認", true);
                        activity.runOnUiThread(() -> {
                            NavigationWrapper.navigate(activity, R.id.fragment_main_nav_host, navigateId);
                        });
                    }

                    @Override
                    public void onNegativeClick(String errorCode) {
                    }

                    @Override
                    public void onNeutralClick(String errorCode) {
                    }

                    @Override
                    public void onDismissClick(String errorCode) {
                    }
                };

                dialog.setCommonErrorEventHandlers(handlers);
                // ポイント付与の時だけ
                if (type == BusinessType.POINT_ADD) {
                    dialog.ShowErrorMessage(activity, "5991");
                } else {
                    dialog.ShowErrorMessage(activity, "2009");
                }
            } else if (IFBoxAppModels.isMatch(IFBoxAppModels.OKABE_MS70_D) &&
                    type != BusinessType.BALANCE && sameEigyoCountCheck()) {
                //同一営業回数の決済あり
                CommonErrorEventHandlers handlers = new CommonErrorEventHandlers() {
                    @Override
                    public void onPositiveClick(String errorCode) {
                        CommonClickEvent.RecordClickOperation("はい", "2重決済確認", true);
                        activity.runOnUiThread(() -> {
                            NavigationWrapper.navigate(activity, R.id.fragment_main_nav_host, navigateId);
                        });
                    }

                    @Override
                    public void onNegativeClick(String errorCode) {
                    }

                    @Override
                    public void onNeutralClick(String errorCode) {
                    }

                    @Override
                    public void onDismissClick(String errorCode) {
                    }
                };

                dialog.setCommonErrorEventHandlers(handlers);
                dialog.ShowErrorMessage(activity, "2095");
                //ADD-S BMT S.Oyama 2024/09/24 フタバ双方向向け改修
            } else if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) &&
                    type != BusinessType.BALANCE && sameEigyoCountCheck()) {
                //同一営業回数の決済あり
                CommonErrorEventHandlers handlers = new CommonErrorEventHandlers() {
                    @Override
                    public void onPositiveClick(String errorCode) {
                        CommonClickEvent.RecordClickOperation("はい", "2重決済確認", true);
                        activity.runOnUiThread(() -> {
                            NavigationWrapper.navigate(activity, R.id.fragment_main_nav_host, navigateId);
                        });
                    }

                    @Override
                    public void onNegativeClick(String errorCode) {
                    }

                    @Override
                    public void onNeutralClick(String errorCode) {
                    }

                    @Override
                    public void onDismissClick(String errorCode) {
                    }
                };

                dialog.setCommonErrorEventHandlers(handlers);
                dialog.ShowErrorMessage(activity, "2095");
                //ADD-E BMT S.Oyama 2024/09/24 フタバ双方向向け改修
            } else {
                //同一金額の決済なし
                activity.runOnUiThread(() -> {
                    NavigationWrapper.navigate(activity, R.id.fragment_main_nav_host, navigateId);
                });
            }
        };

        // 定額料金未使用かつ残照ではない場合はメーター金額のチェックを行う
//        if (Amount.getFlatRateAmount() <= 0 && type != BusinessType.BALANCE) {
//            _menuViewModel.checkMeterCharge().subscribe(() -> {
//                if (Amount.getFixedAmount() == Amount.getTotalAmount()) {
//                    runnable.run();
//                } else {
//                    // メータの金額が更新されていた場合
//                    CommonErrorEventHandlers handlers = new CommonErrorEventHandlers() {
//                        @Override
//                        public void onPositiveClick(String errorCode) {
//                            CommonClickEvent.RecordClickOperation("はい", "メーター料金不一致", true);
//                            runnable.run();
//                        }
//
//                        @Override
//                        public void onNegativeClick(String errorCode) {
//                        }
//
//                        @Override
//                        public void onNeutralClick(String errorCode) {
//                        }
//
//                        @Override
//                        public void onDismissClick(String errorCode) {
//                        }
//                    };
//                    dialog.setCommonErrorEventHandlers(handlers);
//                    dialog.ShowErrorMessage(_fragment.requireContext(), _app.getString(R.string.error_type_ifbox_amount_difference));
//                }
//            }, error -> {
//                // IF-BOXと通信できなかった場合
//                CommonErrorEventHandlers handlers = new CommonErrorEventHandlers() {
//                    @Override
//                    public void onPositiveClick(String errorCode) {
//                        CommonClickEvent.RecordClickOperation("はい", "メーター接続エラー", true);
//                        runnable.run();
//                    }
//
//                    @Override
//                    public void onNegativeClick(String errorCode) {
//                    }
//
//                    @Override
//                    public void onNeutralClick(String errorCode) {
//                    }
//
//                    @Override
//                    public void onDismissClick(String errorCode) {
//                    }
//                };
//                dialog.setCommonErrorEventHandlers(handlers);
//
//                //ADD-S BMT S.Oyama 2024/10/23 フタバ双方向向け改修         フタバ時クレカ決済実施で本エラーが必ず発生する．後日エラー原因を調査の上if文を撤去のこと
//                if ((IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == false)) {
//                    dialog.ShowErrorMessage(_fragment.requireContext(), _app.getString(R.string.error_type_ifbox_connection_error));
//                } else {
//                    runnable.run();         //ここも削除のこと
//                }
//                //ADD-E BMT S.Oyama 2024/10/23 フタバ双方向向け改修
//            });
//        } else {
//            runnable.run();
//        }
    }

    private void removeEmoneyClosedErrorStacking() {
        List<String> errorCodeList = new ArrayList<>();

        //各マネーの開局状態をチェック
        if (EmoneyOpeningInfo.getSuica() != null) {
            errorCodeList.addAll(Arrays.asList(
                    _app.getString(R.string.error_type_opening_suica_warning),
                    _app.getString(R.string.error_type_opening_suica_error),
                    _app.getString(R.string.error_type_suica_closed_error)
            ));
        }
        if (EmoneyOpeningInfo.getId() != null) {
            errorCodeList.addAll(Arrays.asList(
                    _app.getString(R.string.error_type_opening_id_warning),
                    _app.getString(R.string.error_type_opening_id_error),
                    _app.getString(R.string.error_type_id_closed_error)
            ));
        }
        if (EmoneyOpeningInfo.getWaon() != null) {
            errorCodeList.addAll(Arrays.asList(
                    _app.getString(R.string.error_type_opening_waon_warning),
                    _app.getString(R.string.error_type_opening_waon_error),
                    _app.getString(R.string.error_type_waon_closed_error)
            ));
        }
        if (EmoneyOpeningInfo.getNanaco() != null) {
            errorCodeList.addAll(Arrays.asList(
                    _app.getString(R.string.error_type_opening_nanaco_warning),
                    _app.getString(R.string.error_type_opening_nanaco_error),
                    _app.getString(R.string.error_type_nanaco_closed_error)
            ));
        }
        if (EmoneyOpeningInfo.getQuicpay() != null) {
            errorCodeList.addAll(Arrays.asList(
                    _app.getString(R.string.error_type_opening_quicpay_warning),
                    _app.getString(R.string.error_type_opening_quicpay_error),
                    _app.getString(R.string.error_type_quicpay_closed_error)
            ));
        }
        if (EmoneyOpeningInfo.getEdy() != null) {
            errorCodeList.addAll(Arrays.asList(
                    _app.getString(R.string.error_type_opening_edy_warning),
                    _app.getString(R.string.error_type_opening_edy_error),
                    _app.getString(R.string.error_type_edy_closed_error)
            ));
        }

        ErrorStackingDao errorStackingDao = DBManager.getErrorStackingDao();

        for (String errorCode : errorCodeList) {
            ErrorStackingData errorStackingData = errorStackingDao.getErrorStackingData(errorCode);
            if (errorStackingData != null) {
                //開局完了したマネーがスタックされていた場合削除
                errorStackingDao.deleteErrorStackingData(errorStackingData.id);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void refreshPosProducts(View view, SharedViewModel sharedViewModel) {
        CommonClickEvent.RecordButtonClickOperation(view, true);

        new Thread(() -> {
            final Handler handler = new Handler(Looper.getMainLooper());

            handler.post(() -> {
                sharedViewModel.setLoading(true);
            });

            final String[] errors = _menuViewModel.refreshPosProducts();
            if (errors.length > 0) {
                final Context context = view.getContext();
                final CommonErrorDialog dialog = new CommonErrorDialog();

                for (String errCode : errors) {
                    handler.post(() -> {
                        dialog.ShowErrorMessage(context, errCode);
                    });
                }

                // エラー発生している場合は手動マスタ情報更新を実施しない
                handler.post(() -> {
                    sharedViewModel.setLoading(false);
                });
                return;
            }

            try {
                // 手動マスタ情報更新
                ProductRepository repo = new ProductRepository();
                repo.refreshProducts();

                final List<String> removeList = new ArrayList<>();
                removeList.add(Integer.toString(DomainErrors.POS_DOMAIN_ERRORS.code));
                removeList.add(Integer.toString(DomainErrors.POS_SERVICE_INSTANCE_IS_NOT_ASSIGNED.code));
                removeList.add(Integer.toString(DomainErrors.POS_SERVICE_RESPONSE_ERROR.code));
                removeList.add(Integer.toString(DomainErrors.POS_SERVICE_NETWORK_ERROR.code));
                removeList.add(Integer.toString(DomainErrors.POS_SERVICE_UNKNOWN_ERROR.code));
                // POSデータ更新が正常に完了した場合はPOS関連のスタックエラークリア
                ErrorStackingRepository errorStackingRepository = new ErrorStackingRepository();
                errorStackingRepository.removeErrorStacking(removeList);

                handler.post(() -> {
                    sharedViewModel.setLoading(false);
                    SuccessDialog.show(view.getContext(), "POSデータを更新しました");
                });
            } catch (Exception e) {
                Timber.e(e, "(paypf) Error on refreshing products manually");
                handler.post(() -> {
                    sharedViewModel.setLoading(false);
                    ErrorDialog.show(view.getContext(), e.getMessage());
                });
            }

        }).start();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void navigateToTicketSearch(View view) {
        CommonClickEvent.RecordButtonClickOperation(view, true);
        Activity activity = (Activity) view.getContext();
        if (activity == null) return;

        // チケット検索条件画面に遷移する
//        NavigationWrapper.navigate(activity, R.id.fragment_main_nav_host, R.id.action_navigation_menu_to_fragment_ticket_search);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void navigateToTicketGateSettings(View view) {
        CommonClickEvent.RecordButtonClickOperation(view, true);
        Activity activity = (Activity) view.getContext();
        if (activity == null) return;

        // 改札設定画面に遷移する
//        NavigationWrapper.navigate(activity, R.id.fragment_main_nav_host, R.id.action_navigation_menu_to_fragment_ticket_gate_settings);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void refreshTicketSales(View view, SharedViewModel sharedViewModel) {
        CommonClickEvent.RecordButtonClickOperation(view, true);

        new Thread(() -> {
            final Handler handler = new Handler(Looper.getMainLooper());

            handler.post(() -> {
                sharedViewModel.setLoading(true);
            });

            final String[] errors = _menuViewModel.refreshTicketSales();
            if (errors.length > 0) {
                final Context context = view.getContext();
                final CommonErrorDialog dialog = new CommonErrorDialog();

                for (String errCode : errors) {
                    handler.post(() -> {
                        dialog.ShowErrorMessage(context, errCode);
                    });
                }

                // エラー発生している場合は手動マスタ情報更新を実施しない
                handler.post(() -> {
                    sharedViewModel.setLoading(false);
                });
                return;
            }

//            try {
//                // 手動マスタ情報更新
//                TicketRepository repo = new TicketRepository();
//                if (repo.refreshTicketSales()) {
//                    handler.post(() -> {
//                        sharedViewModel.setLoading(false);
//                        SuccessDialog.show(view.getContext(), "チケットデータ更新成功しました");
//                    });
//                } else {
//                    handler.post(() -> {
//                        sharedViewModel.setLoading(false);
//                        ErrorDialog.show(view.getContext(), "チケットデータ更新失敗しました");
//                    });
//                }
//            } catch (Exception e) {
//                Timber.e(e, "チケットデータ更新失敗しました");
//                handler.post(() -> {
//                    sharedViewModel.setLoading(false);
//                    ErrorDialog.show(view.getContext(), "チケットデータ更新失敗しました");
//                });
//            }

        }).start();
    }

//ADD-S BMT S.Oyama 2024/09/03 フタバ双方向向け改修
    /******************************************************************************/
    /*!
     * @brief  立替払い向けフラグメントの表示
     * @note   立替払い向けフラグメントの表示
     * @param [in] View view
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
//    @RequiresApi(api = Build.VERSION_CODES.N)
//    @Override
//    public void navigateToAdvancePay(View view)           //立替払い
//    {
//        CommonClickEvent.RecordButtonClickOperation(view, true);
//        Activity activity = (Activity) view.getContext();
//        if (activity == null) return;
//
//        // 立替払い画面に遷移する
//        NavigationWrapper.navigate(activity, R.id.fragment_main_nav_host, R.id.action_navigation_menu_to_navigation_amount_input_advancepay_fd);
//    }

    /******************************************************************************/
    /*!
     * @brief  分割払い向けフラグメントの表示
     * @note   分割払い向けフラグメントの表示
     * @param [in] View view
     * @retval なし
     * @return
     * @private
     */

    /******************************************************************************/
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void navigateToSeparationPay(View view, SharedViewModel sharedViewModel, int tmpSeparationJobMode)           //分割払い
    {

        CommonClickEvent.RecordButtonClickOperation(view, true);
        Activity activity = (Activity) view.getContext();
        if (activity == null) return;

        // 分割払いチケット画面に遷移する
        NavigationWrapper.navigate(activity, R.id.fragment_main_nav_host, R.id.action_navigation_menu_to_navigation_amount_input_separationpay_fd);
    }

    /******************************************************************************/
    /*!
     * @brief  分割払い:チケット＆（現金クレカ電マネQR）フラグメントの表示
     * @note   分割払い:チケット＆（現金クレカ電マネQR）フラグメントの表示
     * @param [in] View view
     * @retval なし
     * @return
     * @private
     */

    /******************************************************************************/
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void navigateToSeparationPayWithTicket(View view, SharedViewModel sharedViewModel, int tmpSeparationJobMode)           //分割払い
    {

        CommonClickEvent.RecordButtonClickOperation(view, true);
        Activity activity = (Activity) view.getContext();
        if (activity == null) return;

        switch (tmpSeparationJobMode) {
            case AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONMODE_CREDIT:            //クレジット
                navigateToCreditCardScan(view, sharedViewModel);        //クレジットスキャン画面へ
                break;
            case AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONMODE_EMONEY:            //電子マネー
                view.post(() -> {
                    NavigationWrapper.navigate(view, R.id.action_navigation_menu_separation_with_ticket_to_navigation_menu_emoney); //電子マネーメニュー画面へ
                });
                break;
            case AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONMODE_QR:            //QR
                navigateToQRSeparation(view, sharedViewModel);
                break;
            case AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONMODE_PREPAID:            //プリペイド
            case AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONMODE_NONE:            //なし(あるいはエラー終了)
            case AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONMODE_TICKET:            //チケット
            default:
                return;
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    /******************************************************************************/
    /*!
     * @brief  分割払いチケットキャンセル向け表示
     * @note   分割払いチケットキャンセル向け表示
     * @param [in] View view
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    public void navigateToSeparationPayCancel(View view) {
        CommonClickEvent.RecordButtonClickOperation(view, true);
        Activity activity = (Activity) view.getContext();
        if (activity == null) return;

        Timber.i("分割チケット払いを取消します．よろしいですか？");
        final String message =
                "分割チケット払いを取消します\nよろしいですか？";
        ConfirmDialog.newInstance("【分割チケット払い取消確認】", message, () -> {
            CommonClickEvent.RecordClickOperation("はい", "取消確認", false);
            //disposables.clear();

            boolean fl = navigateToSeparationPayCancelSendCancel(view);    //分割払いキャンセル送信
            if (fl == false) {
                return;
            }

            Amount.setTotalChangeAmount(0);         // 変更金額を初期化
            Amount.setTicketAmount(0);              // チケット金額を初期化

            _menuViewModel.onResumeExt();      // Amount系変数を整える

            //NavigationWrapper.navigate(view,  R.id.action_navigation_menu_separation_to_navigation_home);                    //home画面へ

            view.post(() -> {
                NavigationWrapper.popBackStack(view);
                NavigationWrapper.popBackStack(view);
                //NavigationWrapper.navigate(activity, R.id.fragment_menu_separationpay, R.id.action_navigation_menu_separation_to_navigation_home);
            });
        }, () -> {
            CommonClickEvent.RecordClickOperation("いいえ", "取消確認", false);
        }).show(_fragment.getChildFragmentManager(), null);
    }

    /******************************************************************************/
    /*!
     * @brief  分割払いチケットキャンセル向け　取り消しキーの送信
     * @note   分割払いチケットキャンセル向け　取り消しキーの送信
     * @param [in] View view
     * @retval なし
     * @return
     * @private
     */

    /******************************************************************************/
    @RequiresApi(api = Build.VERSION_CODES.N)
    public boolean navigateToSeparationPayCancelSendCancel(View view) {
        boolean result = false;

        if ((IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) != true)) {
            return result;
        }

        PrinterManager printerManager = PrinterManager.getInstance();
        printerManager.setView(view);

//        if (_menuViewModel.getIFBoxManager() == null) {
//            printerManager.PrinterDuplexError(PrinterConst.DuplexPrintStatus_IFBOXERROR);       //IFBOX接続エラー
//            return result;
//        }
//
//        if (_menuViewModel.getIFBoxManager().getIsConnected820() == false)             //820未接続の場合
//        {
//            printerManager.PrinterDuplexError(PrinterConst.DuplexPrintStatus_IFBOXERROR);       //IFBOX接続エラー
//            return result;
//        }

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
//                _menuViewModel.getIFBoxManager().send820_SeparateTicketJobFix_KeyCode();        //セットキーを送信
//                for (int i = 0; i < 5; i++)        //最大300msほど待ってみる
//                {
//                    try {
//                        Thread.sleep(100);
//                    } catch (InterruptedException e) {
//                    }
//                }
//
//                _menuViewModel.getIFBoxManager().send820_FunctionCodeErrorResult(IFBoxManager.SendMeterDataStatus_FutabaD.SEPARATION_TIKECT_CANCEL, false);        //分別払いキャンセル送信
//                for (int i = 0; i < 3; i++)        //最大300msほど待ってみる
//                {
//                    try {
//                        Thread.sleep(100);
//                    } catch (InterruptedException e) {
//                    }
//
//                }
            }
        });
        thread.start();

        try {
            thread.join();
            result = true;
        } catch (Exception e) {
            Timber.e(e);
            return result;
        }

        return result;
    }

    /******************************************************************************/
    /*!
     * @brief  分割払いチケット向けフラグメントの表示（電子マネー専用）
     * @note   分割払いチケット向けフラグメントの表示（電子マネー専用）
     * @param [in] View view
     * @retval なし
     * @return
     * @private
     */

    /******************************************************************************/
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void navigateToSeparationPayMenuToEMoneyMenu(View view, SharedViewModel sharedViewModel) {
        CommonClickEvent.RecordButtonClickOperation(view, true);
        Activity activity = (Activity) view.getContext();
        if (activity == null) return;

        // 電子マネーメニュー画面に遷移する
        navigateMenu(view, R.id.action_navigation_menu_separation_to_navigation_menu_emoney);
    }

    /******************************************************************************/
    /*!
     * @brief  分割払いチケット向けフラグメントの表示（電子マネー専用）
     * @note   分割払いチケット向けフラグメントの表示（電子マネー専用）
     * @param [in] View view
     * @retval なし
     * @return
     * @private
     */

    /******************************************************************************/
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void navigateToSeparationPayMenuWithTicketToEMoneyMenu(View view, SharedViewModel sharedViewModel) {
        CommonClickEvent.RecordButtonClickOperation(view, true);
        Activity activity = (Activity) view.getContext();
        if (activity == null) return;

        //sharedViewModel.setSeparationJobMode(AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONMODE_EMONEY);     // 分割払いジョブモードを電子マネーへ設定

        // 電子マネーメニュー画面に遷移する
        navigateMenu(view, R.id.action_navigation_menu_separation_with_ticket_to_navigation_menu_emoney);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void navigateToDiscountMenu(View view) {
//        CommonClickEvent.RecordButtonClickOperation(view, true);
        Activity activity = (Activity) view.getContext();
        if (activity == null) return;

        Intent launchIntent = new Intent();
        launchIntent.setComponent(new ComponentName(BuildConfig.PREPAID_APP_MODEL, BuildConfig.PREPAID_APP_ACTIVITY));

        try {
            launchIntent.putExtra("startupType", "discount_menu_info");
            launchIntent.putExtra("amount", Amount.getTotalAmount());
            launchIntent.putExtra("driverCd", AppPreference.getDriverCode());
            launchIntent.putExtra("driverName", AppPreference.getDriverName());
            launchIntent.putExtra("carNo", AppPreference.getMcCarId());
            launchIntent.putExtra("organizationId", AppPreference.getOrganizationId());
            launchIntent.putExtra("deviceId", AppPreference.getMcTermId());
            launchIntent.putExtra("isInput1yen", AppPreference.isInput1yenEnabled());
            launchIntent.putExtra("domain", AppPreference.getPrepaidServiceDomain());
            launchIntent.putExtra("key", AppPreference.getPrepaidServiceKey());
            launchIntent.putExtra("isDemoMode", AppPreference.isDemoMode());
            launchIntent.putExtra("organizationName", AppPreference.getMerchantOffice());
            launchIntent.putExtra("organizationParentName", AppPreference.getMerchantName());
            launchIntent.putExtra("isInputAmount", IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == true ? false : true);
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            activity.startActivityForResult(launchIntent, MenuDiscountFragment.DISCOUNT_MENU_INFO_REQUEST_CODE);
        } catch (Exception e) {
            // 念のため
            return;
        }
        //CHG-S BMT S.Oyama 2025/01/09 フタバ双方向向け改修
        navigateMenu(view, R.id.action_navigation_menu_others_to_navigation_menu_discount);
        //CHG-E BMT S.Oyama 2025/01/09 フタバ双方向向け改修
    }

    /******************************************************************************/
    /*!
     * @brief  割引確認向けフラグメントの表示
     * @note   割引確認向けフラグメントの表示
     * @param [in] View view
     * @retval なし
     * @return
     * @private
     */

    /******************************************************************************/
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void navigateToDiscount(View view)           //分割払い
    {

        CommonClickEvent.RecordButtonClickOperation(view, true);
        Activity activity = (Activity) view.getContext();
        if (activity == null) return;

        Intent launchIntent = new Intent();
        launchIntent.setComponent(new ComponentName(BuildConfig.PREPAID_APP_MODEL, "jp.mcapps.pt_prepaid.MainActivity"));

        try {
            launchIntent.putExtra("startupType", "discount_info");
            launchIntent.putExtra("amount", Amount.getTotalAmount());
            launchIntent.putExtra("driverCd", AppPreference.getDriverCode());
            launchIntent.putExtra("driverName", AppPreference.getDriverName());
            launchIntent.putExtra("carNo", AppPreference.getMcCarId());
            launchIntent.putExtra("organizationId", AppPreference.getOrganizationId());
            launchIntent.putExtra("deviceId", AppPreference.getMcTermId());
            launchIntent.putExtra("isInput1yen", AppPreference.isInput1yenEnabled());
            launchIntent.putExtra("domain", AppPreference.getPrepaidServiceDomain());
            launchIntent.putExtra("key", AppPreference.getPrepaidServiceKey());
            launchIntent.putExtra("isDemoMode", AppPreference.isDemoMode());
            launchIntent.putExtra("organizationName", AppPreference.getMerchantOffice());
            launchIntent.putExtra("organizationParentName", AppPreference.getMerchantName());
            launchIntent.putExtra("isInputAmount", IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == true ? false : true);
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            activity.startActivityForResult(launchIntent, MenuDiscountFragment.DISCOUNT_INFO_REQUEST_CODE);
        } catch (Exception e) {
            // 念のため
            return;
        }
    }

    /******************************************************************************/
    /*!
     * @brief  割引確認向けフラグメントの表示
     * @note   割引確認向けフラグメントの表示
     * @param [in] View view
     * @param [in] SharedViewModel sharedViewModel
     * @param [in] int tmpDiscountMode 選択された割引モード(1～5)
     * @retval なし
     * @return
     * @private
     */

    /******************************************************************************/
    @RequiresApi(api = Build.VERSION_CODES.N)
    @SuppressWarnings("deprecation")
    @Override
    public void navigateToDiscountJob(View view, SharedViewModel sharedViewModel, int tmpDiscountMode) {

        //CreateDiscountJobInfo(sharedViewModel);         //割引関連情報をサーバから得る
        Timber.i("[FUTABA-D]navigateToDiscountJob(): tmpDiscountMode:%d ", tmpDiscountMode);

        CommonClickEvent.RecordButtonClickOperation(view, true);
        Activity activity = (Activity) view.getContext();
        if (activity == null) return;

        sharedViewModel.setDiscountJobMode(tmpDiscountMode - 1);               // 割引処理モードを設定 モードから配列インデックスへ変更

        //ADD-S BMT S.Oyama 2024/10/11 フタバ双方向向け改修
        if ((IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == true)) {
            PrinterManager printerManager = PrinterManager.getInstance();
            printerManager.setView(view);
//
//            if (_menuViewModel.getIFBoxManager() == null) {
//                printerManager.PrinterDuplexError(PrinterConst.DuplexPrintStatus_IFBOXERROR);       //IFBOX接続エラー
//                return;
//            }
//
//            if (_menuViewModel.getIFBoxManager().getIsConnected820() == false)             //820未接続の場合
//            {
//                printerManager.PrinterDuplexError(PrinterConst.DuplexPrintStatus_IFBOXERROR);       //IFBOX接続エラー
//                return;
//            }
//
//            IFBoxManager.SendMeterDataInfo_FutabaD tmpSend820InfoDiscount = new IFBoxManager.SendMeterDataInfo_FutabaD();
//            tmpSend820InfoDiscount.StatusCode = IFBoxManager.SendMeterDataStatus_FutabaD.NONE;
//            tmpSend820InfoDiscount.IsLoopBreakOut = false;
//            tmpSend820InfoDiscount.ErrorCode820 = IFBoxManager.SendMeterDataStatus_FutabaD.NONE;
//
//            _meterDataV4InfoDisposable = _menuViewModel.getIFBoxManager().getMeterDataV4().subscribeOn(
//                    Schedulers.io()).observeOn(Schedulers.newThread()).subscribe(meter -> {
//                Timber.i("[FUTABA-D]navigateToDiscountJob():750<-820 meter_data event cmd:%d ", meter.meter_sub_cmd);
//                if (meter.meter_sub_cmd == 12) {              //割引の通知
//                    Timber.i("[FUTABA-D]navigateToDiscountJob():Discount event ");
//                    tmpSend820InfoDiscount.StatusCode = IFBoxManager.SendMeterDataStatus_FutabaD.SENDOK;             //ACKが返ってきた場合
//                }
//            });
//
//            _meterDataV4ErrorDisposable = _menuViewModel.getIFBoxManager().getMeterDataV4Error().subscribeOn(
//                    Schedulers.io()).observeOn(Schedulers.newThread()).subscribe(error -> {         //送信中にエラー受信(タイムアウト，切断)
//                Timber.e("[FUTABA-D]navigateToDiscountJob():Error event ErrCD:%d 820ErrCD:%d ", error.ErrorCode, error.ErrorCode820);
//                tmpSend820InfoDiscount.StatusCode = error.ErrorCode;
//                tmpSend820InfoDiscount.ErrorCode820 = error.ErrorCode820;
//
//            });

            _progressDialog = new ProgressDialog(view.getContext());
            _progressDialog.setMessage("割引可否確認中 ・・・ ");                   // 内容(メッセージ)設定
            _progressDialog.setCancelable(false);                              // キャンセル無効
            _progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);    // スタイル設定
            _progressDialog.show();

//            Thread thread = new Thread(new Runnable() {
//                @Override
//                public void run() {
//
//                    int tmpDiscountJobMode_FutabaD = 0;
////                    switch (tmpDiscountMode) {
////                        case 1:
////                            tmpDiscountJobMode_FutabaD = IFBoxManager.SendMeterDataStatus_FutabaD.DISCOUNTTYPE_NOTIFY_JOB1;
////                            break;
////                        case 2:
////                            tmpDiscountJobMode_FutabaD = IFBoxManager.SendMeterDataStatus_FutabaD.DISCOUNTTYPE_NOTIFY_JOB2;
////                            break;
////                        case 3:
////                            tmpDiscountJobMode_FutabaD = IFBoxManager.SendMeterDataStatus_FutabaD.DISCOUNTTYPE_NOTIFY_JOB3;
////                            break;
////                        case 4:
////                            tmpDiscountJobMode_FutabaD = IFBoxManager.SendMeterDataStatus_FutabaD.DISCOUNTTYPE_NOTIFY_JOB4;
////                            break;
////                        case 5:
////                            tmpDiscountJobMode_FutabaD = IFBoxManager.SendMeterDataStatus_FutabaD.DISCOUNTTYPE_NOTIFY_JOB5;
////                            break;
////                    }
//
//                    if (tmpDiscountJobMode_FutabaD == 0) {
//                        Timber.e("navigateToDiscountJob::割引処理モードが不正です。");
//                        return;
//                    }
//
//                    _menuViewModel.getIFBoxManager().send820_DiscountType(tmpDiscountJobMode_FutabaD);               //割引処理開始情報の通知
//
//                    for (int i = 0; i < 4 * 10; i++)        //最大4秒ほど待ってみる
//                    {
//                        try {
//                            Thread.sleep(100);
//                        } catch (InterruptedException e) {
//                        }
//
//                        if (tmpSend820InfoDiscount.StatusCode != IFBoxManager.SendMeterDataStatus_FutabaD.NONE)         //状態に変化が出たら直ちに抜ける
//                        {
//                            tmpSend820InfoDiscount.IsLoopBreakOut = true;
//                            break;
//                        }
//                    }
//
//                    ClearV4DataCallback_FutabaD();          //V4データコールバックのクリア
//
//                    view.post(() -> {
//                        try {
//
//                            if (tmpSend820InfoDiscount.IsLoopBreakOut == false) {                             //820から何も返却されなかった場合のループアウト
//                                _menuViewModel.getIFBoxManager().killRetryTimerFutabaD();            //タイマーの停止
//                                printerManager.PrinterDuplexError(PrinterConst.DuplexPrintStatus_DISCOUNT_REJECT);       //割引処理拒否とみなしてエラー表示
//                                if (_progressDialog != null) {
//                                    _progressDialog.dismiss();
//                                }
//                                _progressDialog = null;
//                                return;
//                            } else {
//                                switch (tmpSend820InfoDiscount.StatusCode)                       //ステータスコードのチェック
//                                {
//                                    case IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_NOTCONNECTED:       //切断
//                                        printerManager.PrinterDuplexError(PrinterConst.DuplexPrintStatus_IFBOXERROR);       //IFBOX接続エラー
//                                        if (_progressDialog != null) {
//                                            _progressDialog.dismiss();
//                                        }
//                                        _progressDialog = null;
//                                        return;
//                                    case IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_TIMEOUT:           //タイムアウト
//                                        printerManager.PrinterDuplexError(PrinterConst.DuplexPrintStatus_IFBOXERROR_TIMEOUT);       //IFBOXタイムアウトエラー
//                                        if (_progressDialog != null) {
//                                            _progressDialog.dismiss();
//                                        }
//                                        _progressDialog = null;
//                                        return;
//                                    case IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_820NACK:              //820内でが返ってきた場合
//                                        Timber.e("[FUTABA-D]820 Inner error! ErrCD:%d", tmpSend820InfoDiscount.ErrorCode820);
//                                        printerManager.PrinterDuplexError(PrinterConst.DuplexPrintStatus_IFBOXERROR);       //IFBOX接続エラー
//                                        if (_progressDialog != null) {
//                                            _progressDialog.dismiss();
//                                        }
//                                        _progressDialog = null;
//                                        return;
//                                    default:
//                                        //ここに到達する場合は，エラー無しで決済選択モードが送信されたことを意味する
//                                        if (_progressDialog != null) {
//                                            _progressDialog.dismiss();
//                                        }
//                                        _progressDialog = null;
//                                        break;
//                                }
//                            }
//                            //ADD-E BMT S.Oyama 2024/10/11 フタバ双方向向け改修
//                        } catch (Exception e) {
//                            Timber.e(e);
//                            if (_progressDialog != null) {
//                                _progressDialog.dismiss();
//                            }
//                            _progressDialog = null;
//                            return;
//                        }
//
//                        // 割引処理１～５画面に遷移する
//                        NavigationWrapper.navigate(activity, R.id.fragment_main_nav_host, R.id.action_navigation_menu_to_navigation_discountjob_fd);
//                    });
//                }
//            });
//            thread.start();

        }
    }

    /******************************************************************************/
    /*!
     * @brief  割引確認(カード使用) 処理
     * @note   割引確認(カード使用) 処理
     * @param [in] View view
     * @param [in] SharedViewModel sharedViewModel
     * @param [in] DiscountInfo discountInfo
     * @retval なし
     * @return
     * @private
     */

    /******************************************************************************/
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void navigateToDiscountCard(View view, SharedViewModel sharedViewModel, DiscountInfo discountInfo) {
        boolean fl = send820DiscountInfoKeyCode(view, sharedViewModel, discountInfo);
        if (fl == false) {
            return;
        }

        send820DiscountInfoMain(view, sharedViewModel, discountInfo);
        Amount.setDiscountAvailable(1);     //  割引実施フラグをセット
    }

    /******************************************************************************/
    /*!
     * @brief  割引確認(カード使用) 処理 キーコード送付
     * @note   割引確認(カード使用) 処理 キーコード送付
     * @param [in] View view
     * @param [in] SharedViewModel sharedViewModel
     * @param [in] DiscountInfo discountInfo
     * @retval なし
     * @return
     * @private
     */

    /******************************************************************************/
    @RequiresApi(api = Build.VERSION_CODES.N)
    private boolean send820DiscountInfoKeyCode(View view, SharedViewModel sharedViewModel, DiscountInfo discountInfo) {
//        IFBoxManager.SendMeterDataInfo_FutabaD tmpSend820Info = new IFBoxManager.SendMeterDataInfo_FutabaD();
//        tmpSend820Info.StatusCode = IFBoxManager.SendMeterDataStatus_FutabaD.NONE;
//        tmpSend820Info.IsLoopBreakOut = false;
//        tmpSend820Info.ErrorCode820 = IFBoxManager.SendMeterDataStatus_FutabaD.NONE;
//
//        _meterDataV4InfoDisposable = _menuViewModel.getIFBoxManager().getMeterDataV4().subscribeOn(
//                Schedulers.io()).observeOn(Schedulers.newThread()).subscribe(meter -> {
//            Timber.i("[FUTABA-D]navigateToDiscountJob():750<-820 meter_data event cmd:%d ", meter.meter_sub_cmd);
//            if (meter.meter_sub_cmd == 12) {              //割引の通知
//                Timber.i("[FUTABA-D]navigateToDiscountJob():Discount event ");
//                tmpSend820Info.StatusCode = IFBoxManager.SendMeterDataStatus_FutabaD.SENDOK;             //ACKが返ってきた場合
//            }
//        });
//
//        _meterDataV4ErrorDisposable = _menuViewModel.getIFBoxManager().getMeterDataV4Error().subscribeOn(
//                Schedulers.io()).observeOn(Schedulers.newThread()).subscribe(error -> {         //送信中にエラー受信(タイムアウト，切断)
//            Timber.e("[FUTABA-D]navigateToDiscountJob():Error event ErrCD:%d 820ErrCD:%d ", error.ErrorCode, error.ErrorCode820);
//            tmpSend820Info.StatusCode = error.ErrorCode;
//            tmpSend820Info.ErrorCode820 = error.ErrorCode820;
//
//        });

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

//                int tmpDiscountJobMode_FutabaD = IFBoxManager.SendMeterDataStatus_FutabaD.DISCOUNTTYPE_NOTIFY_CONFIRMATION;
//
//                _menuViewModel.getIFBoxManager().send820_DiscountType(tmpDiscountJobMode_FutabaD);               //割引処理開始情報の通知
//
//                for (int i = 0; i < 4 * 10; i++)        //最大4秒ほど待ってみる
//                {
//                    try {
//                        Thread.sleep(100);
//                    } catch (InterruptedException e) {
//                    }
//
//                    if (tmpSend820Info.StatusCode != IFBoxManager.SendMeterDataStatus_FutabaD.NONE)         //状態に変化が出たら直ちに抜ける
//                    {
//                        tmpSend820Info.IsLoopBreakOut = true;
//                        break;
//                    }
//                }
            }
        });
        thread.start();

        try {
            thread.join();

            ClearV4DataCallback_FutabaD();          //V4データコールバックのクリア

//            PrinterManager printerManager = PrinterManager.getInstance();
//            if (tmpSend820Info.IsLoopBreakOut == false) {                             //820から何も返却されなかった場合のループアウト
//                _menuViewModel.getIFBoxManager().killRetryTimerFutabaD();            //タイマーの停止
//                printerManager.PrinterDuplexError(PrinterConst.DuplexPrintStatus_DISCOUNT_REJECT);        //割引処理拒否とみなしてエラー表示
//                return false;
//            } else {
//                switch (tmpSend820Info.StatusCode)                       //ステータスコードのチェック
//                {
//                    case IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_NOTCONNECTED:       //切断
//                        printerManager.PrinterDuplexError(PrinterConst.DuplexPrintStatus_IFBOXERROR);       //IFBOX接続エラー
//                        return false;
//                    case IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_TIMEOUT:           //タイムアウト
//                        printerManager.PrinterDuplexError(PrinterConst.DuplexPrintStatus_IFBOXERROR_TIMEOUT);       //IFBOXタイムアウトエラー
//                        return false;
//                    case IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_820NACK:              //820内でが返ってきた場合
//                        Timber.e("[FUTABA-D]820 Inner error! ErrCD:%d", tmpSend820Info.ErrorCode820);
//                        printerManager.PrinterDuplexError(PrinterConst.DuplexPrintStatus_IFBOXERROR);       //IFBOX接続エラー
//                        return false;
//                    default:
//                        //ここに到達する場合は，エラー無しで決済選択モードが送信されたことを意味する
//                        break;
//                }
//            }
            //ADD-E BMT S.Oyama 2024/10/11 フタバ双方向向け改修

        } catch (Exception e) {
            Timber.e(e);
            return false;
        }

        return true;
    }
    /******************************************************************************/
    /*!
     * @brief  割引確認(カード使用) 処理　主処理
     * @note   割引確認(カード使用) 処理　主処理
     * @param [in] View view
     * @param [in] SharedViewModel sharedViewModel
     * @param [in] DiscountInfo discountInfo
     * @retval なし
     * @return
     * @private
     */

    /******************************************************************************/
    private void send820DiscountInfoMain(View view, SharedViewModel sharedViewModel, DiscountInfo discountInfo) {
//        int tmpDiscountMode = IFBoxManager.SendMeterDataStatus_FutabaD.DISCOUNTTYPE_CONFIRMATION;
//        String tmpJobDateTime = Converters.convertDatetime(discountInfo.getTransactionDateTime());
//        _menuViewModel.getIFBoxManager().send820_DiscountExecution(tmpDiscountMode, tmpJobDateTime, discountInfo.getExpiredDateTo(), discountInfo.getDiscountType());        //割引登録を送信 ACKなし
    }

    /******************************************************************************/
    /*!
     * @brief  領収書処理(分別チケット->現金時も使用)
     * @note   領収書処理
     * @param [in] View view
     * @retval なし
     * @return
     * @private
     */

    /******************************************************************************/
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void navigateToReceipt(View view)                 //領収書
    {
        CommonClickEvent.RecordButtonClickOperation(view, true);
        Activity activity = (Activity) view.getContext();
        if (activity == null) return;

//        if (_menuViewModel.getIFBoxManager().getIsConnected820() == false) {                    //IFBOX未接続
//            PrinterManager printerManager = PrinterManager.getInstance();
//            ;
//            printerManager.PrinterDuplexError(PrinterConst.DuplexPrintStatus_IFBOXERROR);       //IFBOX接続エラー
//            return;
//        }

        NavigationWrapper.popBackStack(view);                    //home画面へ(戻る)
        AppPreference.setAmountInputCancel(false);              //金額入力を行えるようにする

//        _menuViewModel.getIFBoxManager().setExtPrintJobMode(IFBoxManager.SendMeterDataStatus_FutabaD.RECEIPT_PRINT);           //領収書印刷モード設定
//        TimerTask timerTask = new TimerTask() {
//            @Override
//            public void run() {
//
//                view.post(() -> {
//                    _menuViewModel.getIFBoxManager().send820_ReceiptTicketPrint(IFBoxManager.SendMeterDataStatus_FutabaD.RECEIPT_PRINT);           //領収書印刷開始情報の通知
//                });
//            }
//        };

        Timer timer = new Timer();
//        timer.schedule(timerTask, 500);
    }

    /******************************************************************************/
    /*!
     * @brief  チケット伝票処理
     * @note   チケット伝票処理
     * @param [in] View view
     * @retval なし
     * @return
     * @private
     */

    /******************************************************************************/
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void navigateToTicketPrint(View view)              //チケット伝票
    {
        CommonClickEvent.RecordButtonClickOperation(view, true);
        Activity activity = (Activity) view.getContext();
        if (activity == null) return;

//        if (_menuViewModel.getIFBoxManager().getIsConnected820() == false) {                    //IFBOX未接続
//            PrinterManager printerManager = PrinterManager.getInstance();
//            ;
//            printerManager.PrinterDuplexError(PrinterConst.DuplexPrintStatus_IFBOXERROR);       //IFBOX接続エラー
//            return;
//        }
//
//        NavigationWrapper.popBackStack(view);                    //home画面へ
//        AppPreference.setAmountInputCancel(false);              //金額入力を行えないようにする
//
//        _menuViewModel.getIFBoxManager().setExtPrintJobMode(IFBoxManager.SendMeterDataStatus_FutabaD.TICKET_PRINT);           //チケット印刷モード設定
//
//        TimerTask timerTask = new TimerTask() {
//            @Override
//            public void run() {
//
//                view.post(() -> {
//                    _menuViewModel.getIFBoxManager().send820_ReceiptTicketPrint(IFBoxManager.SendMeterDataStatus_FutabaD.TICKET_PRINT);           //チケット伝票印刷開始情報の通知
//                });
//            }
//        };

//        Timer timer = new Timer();
//        timer.schedule(timerTask, 500);
    }

    /******************************************************************************/
    /*!
     * @brief  集計印刷処理
     * @note   集計印刷処理
     * @param [in] View view
     * @retval なし
     * @return
     * @private
     */

    /******************************************************************************/
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void navigateToAggregate(View view) {
        if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D)) {
            CommonClickEvent.RecordButtonClickOperation(view, true);
            Activity activity = (Activity) view.getContext();
            if (activity == null) return;

//            if (_menuViewModel.getIFBoxManager().getIsConnected820() == false) {                    //IFBOX未接続
//                PrinterManager printerManager = PrinterManager.getInstance();
//                printerManager.PrinterDuplexError(PrinterConst.DuplexPrintStatus_IFBOXERROR);       //IFBOX接続エラー
//                return;
//            }

            NavigationWrapper.popBackStack(view);                    //home画面へ(戻る)
            AppPreference.setAmountInputCancel(false);              //金額入力を行えるようにする

            //_menuViewModel.getIFBoxManager().setExtPrintJobMode(IFBoxManager.SendMeterDataStatus_FutabaD.RECEIPT_PRINT);           //領収書印刷モード設定
            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {

                    view.post(() -> {
                        PrinterManager printerManager = PrinterManager.getInstance();
                        printerManager.changeIsSlipTypeEx(PrinterConst.SlipType_AggregateFutabaD);      // 集計印刷モード設定
                        // 通／他キーを送信
                        //_menuViewModel.getIFBoxManager().send820_ReceiptTicketPrint(IFBoxManager.SendMeterDataStatus_FutabaD.AGGREGATE_PRINT);           //集計印刷開始情報の通知
                        // この後、メーターから「集計印字」や「セットキー」の文字を含んだ処理コード表示要求を受信する
                    });
                }
            };

            Timer timer = new Timer();
            timer.schedule(timerTask, 500);
        } else {
            // navigateMain(view, R.id.action_navigation_menu_to_navigation_history_aggregate);
        }
    }
    /******************************************************************************/
    /*!
     * @brief  手動決済
     * @note   手動決済
     * @param [in] View view
     * @param [in] SharedViewModel sharedViewModel
     * @retval なし
     * @return
     * @private
     */

    /******************************************************************************/
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void navigateToManualMenu(View view, SharedViewModel sharedViewModel)              //手動決済
    {
        CommonClickEvent.RecordButtonClickOperation(view, true);
        Activity activity = (Activity) view.getContext();
        if (activity == null) return;

        ConfirmDialog.newInstance("【手動決済モード設定確認】", "設定しますか？", () -> {
            CommonClickEvent.RecordClickOperation("はい", "手動決済モード設定確認", false);
            // モード移行の処理追加
            Version.Response ifboxVersionInfo = AppPreference.getIFBoxVersionInfo();
            ifboxVersionInfo.appModel = IFBoxAppModels.FUTABA_D_MANUAL;
            AppPreference.setIFBoxVersionInfo(ifboxVersionInfo);

            sharedViewModel.setUpdatedFlag(true);
            SuccessDialog.show(view.getContext(), "手動決済モードに切り替わりました。");

            NavigationWrapper.popBackStack(view);                    //home画面へ
        }, () -> {
            CommonClickEvent.RecordClickOperation("いいえ", "手動決済モード設定確認", false);
        }).show(_fragment.getChildFragmentManager(), null);
    }

    /******************************************************************************/
    /*!
     * @brief  双方向決済
     * @note   双方向決済
     * @param [in] View view
     * @param [in] SharedViewModel sharedViewModel
     * @retval なし
     * @return
     * @private
     */

    /******************************************************************************/
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void navigateToDuplexMenu(View view, SharedViewModel sharedViewModel)              //双方向決済
    {
        CommonClickEvent.RecordButtonClickOperation(view, true);
        Activity activity = (Activity) view.getContext();
        if (activity == null) return;

        ConfirmDialog.newInstance("【双方向決済モード設定確認】", "設定しますか？", () -> {
            CommonClickEvent.RecordClickOperation("はい", "双方向決済モード設定確認", false);
            // モード移行の処理追加
            Version.Response ifboxVersionInfo = AppPreference.getIFBoxVersionInfo();
            ifboxVersionInfo.appModel = IFBoxAppModels.FUTABA_D;
            AppPreference.setIFBoxVersionInfo(ifboxVersionInfo);
            AppPreference.setIsTemporaryManualMode(false);

            sharedViewModel.setUpdatedFlag(true);
            SuccessDialog.show(view.getContext(), "双方向決済モードに切り替わりました。");

            NavigationWrapper.popBackStack(view);                    //home画面へ
        }, () -> {
            CommonClickEvent.RecordClickOperation("いいえ", "双方向決済モード設定確認", false);
        }).show(_fragment.getChildFragmentManager(), null);
    }

    /******************************************************************************/
    /*!
     * @brief  自動日報：燃料入力
     * @note   自動日報：燃料入力
     * @param [in] View view
     * @retval なし
     * @return
     * @private
     */

    /******************************************************************************/
    public void navigateToAutoDailyReportFuel(View view) {
        CommonClickEvent.RecordButtonClickOperation(view, true);
        Activity activity = (Activity) view.getContext();
        if (activity == null) return;

//        MenuAutoDailyReportFragment fragment = (MenuAutoDailyReportFragment) _fragment;
//
//        boolean isAutoDailyReportIn = fragment.send820AutoDailyReport(MenuAutoDailyReportFragment.SEND820_AUTODAILYREPORT_MODE.MenuIn);
//
//        if (isAutoDailyReportIn == false) {         //エラー発生時は抜ける
//            return;
//        }

        // 自動日報：入力画面へ遷移する
        //view.post(() -> {
        //NavigationWrapper.navigate(activity, R.id.fragment_menu_auto_daily_report, R.id.action_navigation_menu_auto_daily_report_to_navigation_auto_daily_report_fuel);
        //navigateMenu(view, R.id.action_navigation_menu_auto_daily_report_to_navigation_auto_daily_report_fuel);
        //});
    }

    /******************************************************************************/
    /*!
     * @brief  v4meterdata系コールバックのクリア(フタバD)
     * @note   チケット伝票処理
     * @param [in] View view
     * @retval なし
     * @return
     * @private
     */

    /******************************************************************************/
    private void ClearV4DataCallback_FutabaD() {
        if (_meterDataV4InfoDisposable != null) {       //コールバック系を後始末
            _meterDataV4InfoDisposable.dispose();
            _meterDataV4InfoDisposable = null;
        }

        if (_meterDataV4ErrorDisposable != null)        //コールバック系を後始末
        {
            _meterDataV4ErrorDisposable.dispose();
            _meterDataV4ErrorDisposable = null;
        }

        if (_meterStatNoticeDisposable != null)        //コールバック系を後始末
        {
            _meterStatNoticeDisposable.dispose();
            _meterStatNoticeDisposable = null;
        }
    }
//ADD-E BMT S.Oyama 2024/09/03 フタバ双方向向け改修
    //ADD-S BMT S.Oyama 2025/03/14 フタバ双方向向け改修
    /******************************************************************************/
    /*!
     * @brief  自動日報：メータ状態確認
     * @note   自動日報：メータ状態確認
     * @param [in] View view
     * @retval なし
     * @return
     * @private
     */

    /******************************************************************************/
    public void navigateToAutoDailyReportMeterCheck(View view, SharedViewModel sharedViewModel) {
        CommonClickEvent.RecordButtonClickOperation(view, true);
        Activity activity = (Activity) view.getContext();
        if (activity == null) return;

        if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == false) {             //フタバD以外は何もせず戻る
            return;
        }

//        if (_menuViewModel.getIFBoxManager().getIsConnected820() == false) {                    //IFBOX未接続
//            PrinterManager printerManager = PrinterManager.getInstance();
//            printerManager.PrinterDuplexError(PrinterConst.DuplexPrintStatus_IFBOXERROR);       //IFBOX接続エラー
//            return;
//        }

        CommonClickEvent.RecordButtonClickOperation(view, true);
        Timber.i("メーター作業されていない状態で操作して下さい");
        final String message =
                "メーター作業されていない状態で操作して下さい";
        ConfirmDialog.newInstance("【メータ状態確認】", message, () -> {
            CommonClickEvent.RecordClickOperation("はい", "メータ状態確認", false);

            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    // 999キーを送信
                    // _menuViewModel.getIFBoxManager().send820_MeterRecovery_Keycode999_NonAck();           //コード999 ACKなしで送付
                    // この後、メーターから「集計印字」や「セットキー」の文字を含んだ処理コード表示要求を受信する
                }
            };

            Timer timer = new Timer();
            timer.schedule(timerTask, 100);

            new AlertDialog.Builder(view.getContext()) // FragmentではgetActivity()を使用
                    .setTitle("送信完了")
                    .setMessage("メータ状態確認を実施しました")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // TODO: Yesが押された時の挙動
                            //NavigationWrapper.navigate(activity, R.id.fragment_menu_auto_daily_report, R.id.action_navigation_menu_auto_daily_report_to_navigation_menu);

                            //ADD-S BMT S.Oyama 2025/03/31 フタバ双方向向け改修
                            sharedViewModel.setBackAction(null);
                            //ADD-E BMT S.Oyama 2025/03/31 フタバ双方向向け改修

                        }
                    })
                    .show();

        }, () -> {
            CommonClickEvent.RecordClickOperation("いいえ", "メータ状態確認", false);
        }).show(_fragment.getChildFragmentManager(), null);
    }

    /******************************************************************************/
    /*!
     * @brief  自動日報：エラー解除
     * @note   自動日報：エラー解除
     * @param [in] View view
     * @retval なし
     * @return
     * @private
     */

    /******************************************************************************/
    public void navigateToAutoDailyReportErrorClear(View view, SharedViewModel sharedViewModel) {
        CommonClickEvent.RecordButtonClickOperation(view, true);
        Activity activity = (Activity) view.getContext();
        if (activity == null) return;

        if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == false) {
            return;
        }
//
//        if (_menuViewModel.getIFBoxManager().getIsConnected820() == false) {                    //IFBOX未接続
//            PrinterManager printerManager = PrinterManager.getInstance();
//            printerManager.PrinterDuplexError(PrinterConst.DuplexPrintStatus_IFBOXERROR);       //IFBOX接続エラー
//            return;
//        }

        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                // 訂正キーを送信
                // _menuViewModel.getIFBoxManager().send820_TeiseiKeyNonAck( );           //訂正キー ACKなしで送付
                // この後、メーターから「集計印字」や「セットキー」の文字を含んだ処理コード表示要求を受信する
            }
        };

        Timer timer = new Timer();
        timer.schedule(timerTask, 100);

        new AlertDialog.Builder(view.getContext()) // FragmentではgetActivity()を使用
                .setTitle("送信完了")
                .setMessage("エラー解除を実施しました")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO: Yesが押された時の挙動
                        //NavigationWrapper.navigate(activity, R.id.fragment_menu_auto_daily_report, R.id.action_navigation_menu_auto_daily_report_to_navigation_menu);
                        //ADD-S BMT S.Oyama 2025/03/31 フタバ双方向向け改修
                        sharedViewModel.setBackAction(null);
                        //ADD-E BMT S.Oyama 2025/03/31 フタバ双方向向け改修
                    }
                })
                .show();
    }
    //ADD-E BMT S.Oyama 2025/03/14 フタバ双方向向け改修
}

//SuccessDialog.show(view.getContext(), "エラー解除を実施しました");
//        new AlertDialog.Builder(view.getContext()) // FragmentではgetActivity()を使用
//                .setTitle("タイトル")
//                .setMessage("メッセージ")
//                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        // TODO: Yesが押された時の挙動
//                    }
//                })
//                .show();
