package jp.mcapps.android.multi_payment_terminal.ui.history;

import static jp.mcapps.android.multi_payment_terminal.thread.printer.PrinterConst.DuplexPrintResponseTimerSec;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

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
import jp.mcapps.android.multi_payment_terminal.data.IFBoxAppModels;
import jp.mcapps.android.multi_payment_terminal.data.TransMap;
import jp.mcapps.android.multi_payment_terminal.database.DBManager;
import jp.mcapps.android.multi_payment_terminal.database.history.slip.SlipData;
import jp.mcapps.android.multi_payment_terminal.model.CreditChecker;
import jp.mcapps.android.multi_payment_terminal.model.EmoneyChecker;
import jp.mcapps.android.multi_payment_terminal.model.IFBoxManager;
import jp.mcapps.android.multi_payment_terminal.model.OkicaChecker;
import jp.mcapps.android.multi_payment_terminal.model.QRChecker;
import jp.mcapps.android.multi_payment_terminal.model.SeparationTicketChecker;
import jp.mcapps.android.multi_payment_terminal.model.TransLogger;
import jp.mcapps.android.multi_payment_terminal.model.WatariChecker;
import jp.mcapps.android.multi_payment_terminal.thread.printer.PrinterManager;
import jp.mcapps.android.multi_payment_terminal.thread.printer.PrinterProc;
import jp.mcapps.android.multi_payment_terminal.ui.amount_input.AmountInputSeparationPayFDViewModel;
import jp.mcapps.android.multi_payment_terminal.ui.dialog.CancelConfirmDialog;
import jp.mcapps.android.multi_payment_terminal.ui.dialog.ConfirmDialog;
import jp.mcapps.android.multi_payment_terminal.ui.error.CommonErrorDialog;
import jp.mcapps.android.multi_payment_terminal.ui.error.CommonErrorEventHandlers;
import jp.mcapps.android.multi_payment_terminal.webapi.ifbox.data.Version;
import timber.log.Timber;

public class HistoryEventHandlersImpl implements HistoryEventHandlers {
    private final MainApplication _app = MainApplication.getInstance();
    private final Fragment _fragment;
    private boolean _isTicketIssueCancel = false;

    //ADD-S BMT S.Oyama 2024/09/19 フタバ双方向向け改修
    SlipData _slipData = null;
    private TransLogger _transLogger = null;
    { // initializer
        _transLogger = new TransLogger();               //取引歴管理クラスのインスタンスを生成
    }
    @SuppressWarnings("deprecation")
    private ProgressDialog _progressDialog;
    //ADD-E BMT S.Oyama 2024/09/19 フタバ双方向向け改修

    public HistoryEventHandlersImpl(Fragment fragment) {
        _fragment = fragment;
    }

    @Override
    public void onChangePeriodClick(View view, Date date, int type, int time) {
        CommonClickEvent.RecordButtonClickOperation(view, false);

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(type, time);

        Intent intent = new Intent();
        intent.setAction("SEND_PICKED_DATE");
        intent.putExtra("date", calendar.getTimeInMillis());

        LocalBroadcastManager.getInstance(view.getContext()).sendBroadcast(intent);
    }

    @Override
    public void onDatePickerClick(View view, Date date, int month) {
        CommonClickEvent.RecordButtonClickOperation(view, false);

        DateTimePickerDialog dialog = new DateTimePickerDialog(view.getContext(), date, month);
        dialog.showDialog();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onCancelClick(View view, String moneyBrand, int slipId ,int transactionTerminalType, String purchasedTicketDealId, SharedViewModel sharedViewModel, int tranType, String transTypeCode) {
        if (transTypeCode != null && transTypeCode.equals(PrinterProc.MANUALMODE_TRANS_TYPE_CODE_SALES)) {
            Version.Response ifboxVersionInfo = AppPreference.getIFBoxVersionInfo();
            ifboxVersionInfo.appModel = IFBoxAppModels.FUTABA_D_MANUAL;
            AppPreference.setIFBoxVersionInfo(ifboxVersionInfo);
            AppPreference.setIsTemporaryManualMode(true);
        }
        if (moneyBrand.equals(_app.getString(R.string.prepaid_brand))) {

            Intent launchIntent = new Intent();
            launchIntent.setComponent(new ComponentName(BuildConfig.PREPAID_APP_MODEL, BuildConfig.PREPAID_APP_ACTIVITY));

            String startupType = "";
            switch (tranType) {
                case 0:
                    startupType = "prepaid_pay_cancel";
                    break;
                case 2:
                    startupType = "point_add_cancel";
                    break;
                case 4:
                    startupType = "prepaid_charge_cancel";
                    break;
                default:
                    break;
            }

            try {
                if (launchIntent != null && !startupType.equals("")) {
                    launchIntent.putExtra("startupType", startupType);
                    launchIntent.putExtra("amount", Amount.getTotalAmount());
                    launchIntent.putExtra("driverCd", AppPreference.getDriverCode());
                    launchIntent.putExtra("driverName", AppPreference.getDriverName());
                    launchIntent.putExtra("carNo", AppPreference.getMcCarId());
                    launchIntent.putExtra("organizationId", AppPreference.getOrganizationId());
                    launchIntent.putExtra("deviceId", AppPreference.getMcTermId());
                    launchIntent.putExtra("domain", AppPreference.getPrepaidServiceDomain());
                    launchIntent.putExtra("key", AppPreference.getPrepaidServiceKey());
                    launchIntent.putExtra("isDemoMode", AppPreference.isDemoMode());
                    launchIntent.putExtra("organizationName", AppPreference.getMerchantOffice());
                    launchIntent.putExtra("organizationParentName", AppPreference.getMerchantName());
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
        } else {
            onCancelClick(view, moneyBrand, slipId, transactionTerminalType, sharedViewModel, purchasedTicketDealId, false);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onCancelClick(View view, String moneyBrand, int slipId ,int transactionTerminalType, String purchasedTicketDealId, SharedViewModel sharedViewModel) {
        onCancelClick(view, moneyBrand, slipId, transactionTerminalType, sharedViewModel, purchasedTicketDealId, false);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onCancelClick(View view, String moneyBrand, int slipId ,int transactionTerminalType, SharedViewModel sharedViewModel, String purchasedTicketDealId, boolean isTicketIssueCancel) {
        CommonClickEvent.RecordButtonClickOperation(view, true);
        //画面オフを無効化 MenuFragmentに戻るタイミングで再び有効化
        keepScreen(view.getContext());
        AppPreference.setTransactionTerminalType(transactionTerminalType);
        _app.setBusinessType(BusinessType.REFUND);
        _isTicketIssueCancel = isTicketIssueCancel;

        final boolean isEmoney = moneyBrand.equals(_app.getString(R.string.money_brand_suica))
                || moneyBrand.equals(_app.getString(R.string.money_brand_waon))
                || moneyBrand.equals(_app.getString(R.string.money_brand_id))
                || moneyBrand.equals(_app.getString(R.string.money_brand_nanaco))
                || moneyBrand.equals(_app.getString(R.string.money_brand_edy))
                || moneyBrand.equals(_app.getString(R.string.money_brand_qp));

        if (isEmoney) {
            refundEmoney(view, moneyBrand, slipId, purchasedTicketDealId);
        }

        final boolean isCredit = moneyBrand.equals(_app.getString(R.string.money_brand_credit));
        if (isCredit) {
            refundCredit(view, moneyBrand, slipId, sharedViewModel, purchasedTicketDealId);
        }

        /*
        final boolean isQR = moneyBrand.equals(QRPayTypeNameMap.get(QRPayTypeCodes.Wechat))
                || moneyBrand.equals(QRPayTypeNameMap.get(QRPayTypeCodes.Alipay))
                || moneyBrand.equals(QRPayTypeNameMap.get(QRPayTypeCodes.Docomo))
                || moneyBrand.equals(QRPayTypeNameMap.get(QRPayTypeCodes.auPAY))
                || moneyBrand.equals(QRPayTypeNameMap.get(QRPayTypeCodes.PayPay))
                || moneyBrand.equals(QRPayTypeNameMap.get(QRPayTypeCodes.LINEPay))
                || moneyBrand.equals(QRPayTypeNameMap.get(QRPayTypeCodes.RakutenPay))
                || moneyBrand.equals(QRPayTypeNameMap.get(QRPayTypeCodes.GinkoPay))
                || moneyBrand.equals(QRPayTypeNameMap.get(QRPayTypeCodes.merpay))
                || moneyBrand.equals(QRPayTypeNameMap.get(QRPayTypeCodes.QUOPay));
        */
        final boolean isQR = moneyBrand.equals(_app.getString(R.string.money_brand_codetrans));
        if (isQR) {
            refundQR(view, slipId, purchasedTicketDealId);
        }

        final boolean isOkica = moneyBrand.equals(_app.getString(R.string.money_brand_okica));
        if (isOkica) {
            refundOkica(view, slipId, purchasedTicketDealId);
        }
        final boolean isCash = moneyBrand.equals(_app.getString(R.string.money_brand_cash));
        if (isCash) {
            int id;

            view.post(() -> {
                final Bundle params = new Bundle();
                params.putInt("slipId", slipId);
                params.putBoolean("isRepay", true);
                params.putString("purchasedTicketDealId", purchasedTicketDealId);

                if (_isTicketIssueCancel) {
                    NavigationWrapper.navigate(view, R.id.action_ticketIssueFragment_to_fragment_cash_confirm, params);
                } else {
                    if (AppPreference.getIsCashChanger()) {
                        /* つり銭機連動 */
                        params.putString("moneyBrand", moneyBrand);
                        NavigationWrapper.navigate(view, R.id.action_navigation_history_transaction_detail_to_fragment_cash_changer_payment, params);
                    } else {
                        NavigationWrapper.navigate(view, R.id.action_navigation_history_transaction_detail_to_fragment_cash_confirm, params);
                    }
                }
            });
        }

        final boolean isFixedAmountPostalOrder = moneyBrand.equals(_app.getString(R.string.money_brand_postal_order));
        if(isFixedAmountPostalOrder) {
            view.post(() -> {
                final Bundle params = new Bundle();
                params.putInt("slipId", slipId);
                params.putBoolean("isRepay", true);
                params.putString("purchasedTicketDealId", purchasedTicketDealId);
                params.putBoolean("isFixedAmountPostalOrder", true);

                NavigationWrapper.navigate(view, R.id.action_navigation_history_transaction_detail_to_fragment_cash_confirm, params);
            });
        }

        final boolean isWatari = moneyBrand.equals(_app.getString(R.string.point_brand_watari));
        if (isWatari) {
            refundWatari(view, slipId, sharedViewModel);
        }

        //ADD-S BMT S.Oyama 2024/09/19 フタバ双方向向け改修
        final boolean isSeparationTicket = moneyBrand.equals("分別チケット");         //分別：チケット
        if (isSeparationTicket == true) {
            refundSeparationTicket(view, slipId, sharedViewModel);
        }
        //ADD-E BMT S.Oyama 2024/09/19 フタバ双方向向け改修

    }

    @Override
    public void onRecoveryClick(View view, String moneyBrand, int slipId, int transType) {
        CommonClickEvent.RecordButtonClickOperation(view, true);
        final boolean isEmoney = moneyBrand.equals(_app.getString(R.string.money_brand_suica))
                || moneyBrand.equals(_app.getString(R.string.money_brand_waon))
                || moneyBrand.equals(_app.getString(R.string.money_brand_id))
                || moneyBrand.equals(_app.getString(R.string.money_brand_nanaco))
                || moneyBrand.equals(_app.getString(R.string.money_brand_edy))
                || moneyBrand.equals(_app.getString(R.string.money_brand_qp));

        if (isEmoney) {
            recoveryEmoney(view, moneyBrand, slipId, transType);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onRePrintingClick(View view, int id, String transBrand, int transType, int transactionTerminalType) {
        CommonClickEvent.RecordButtonClickOperation(view, true);

        AppPreference.setTransactionTerminalType(transactionTerminalType);
        PrinterManager printerManager = new PrinterManager();
        if (transBrand.equals(MainApplication.getInstance().getString(R.string.money_brand_cash)) || transBrand.equals(MainApplication.getInstance().getString(R.string.money_brand_postal_order))) {
            if (AppPreference.isServicePos() || AppPreference.isServiceTicket()) {
                // 現金決済、為替類の再印字
                if (transType == TransMap.TYPE_SALES) {
                    printerManager.print_trans_cash(view, id);
                } else if (transType == TransMap.TYPE_CANCEL) {
                    printerManager.print_cancel_ticket(view, id);
                }
            }
        } else if (transBrand.equals(MainApplication.getInstance().getString(R.string.prepaid_brand))) {
            // プリペイドの再印字
            int[] ids = new int[1];
            ids[0] = id;
            printerManager.prepaid_print_trans(view, ids);
        } else {
            // 現金決済以外の再印字
            printerManager.print_trans(view, id);
        }
    }

    @Override
    public void onReceiptIssueClick(View view, int id, int transactionTerminalType) {
        CommonClickEvent.RecordButtonClickOperation(view, true);

        AppPreference.setTransactionTerminalType(transactionTerminalType);
        final Bundle params = new Bundle();
        params.putInt("slipId", id);
        view.post(() -> {
            NavigationWrapper.navigate(view, R.id.action_navigation_history_transaction_detail_to_navigation_history_receipt_issue, params);
        });
    }

    @Override
    public void onSelectReceiptTypeClick(boolean receiptType, HistoryReceiptIssueViewModel viewModel) {
        if (receiptType) {
            CommonClickEvent.RecordClickOperation("明細付き", true);
        } else {
            CommonClickEvent.RecordClickOperation("金額のみ", true);
        }
        viewModel.setIsDetail(receiptType);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onReceiptClick(View view, int id, boolean isDetail) {
        CommonClickEvent.RecordButtonClickOperation(view, true);

        PrinterManager printerManager = new PrinterManager();
        printerManager.print_receipt(view,id,isDetail);
    }

    // クレジット取消
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void refundCredit(View view, String moneyBrand, int slipId, SharedViewModel sharedViewModel, String purchasedTicketDealId) {
        final Bundle params = new Bundle();
        params.putInt("slipId", slipId);

        if (moneyBrand.equals(_app.getString(R.string.money_brand_credit)))  {
            Activity activity = (Activity) view.getContext();
            CommonErrorDialog dialog = new CommonErrorDialog();
            Handler handler = new Handler(Looper.getMainLooper());

            final BusinessType type = BusinessType.REFUND;

            //クレジット決済前チェック
            //売上送信で時間がかかる場合があるため、メインスレッドをロックさせない
            CreditChecker checker = new CreditChecker();
            checker.setListener(errCode -> {
                handler.post(() -> {
                    sharedViewModel.setLoading(false);
                    //決済前チェックの結果を確認
                    if (errCode != null) {
                        //エラーの場合はダイアログ表示して終了
                        dialog.ShowErrorMessage(activity, errCode);
                        return;
                    }

                    view.post(() -> {
                        if (_isTicketIssueCancel) {
                            NavigationWrapper.navigate(view, R.id.action_ticketIssueFragment_to_navigation_credit, params);
                        } else {
                            NavigationWrapper.navigate(view, R.id.action_navigation_history_transaction_detail_to_navigation_credit, params);
                        }
                    });
                });

                //取消対象の印刷データから金額を取得して表示
//                Integer refundAmount = DBManager.getSlipDao().getOneById(slipId).transAmount;
//                Amount.reset();
//                Amount.setFlatRateAmount(refundAmount);
//                Amount.fix();
            });

            sharedViewModel.setLoading(true);
            checker.check(view, type, purchasedTicketDealId); //クレジット決済前チェック開始
        }
    }

    // QR取消
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void refundQR(View view, int slipId, String purchasedTicketDealId) {
        final CommonErrorDialog dialog = new CommonErrorDialog();

        final String errorCode = QRChecker.check(view, BusinessType.REFUND, purchasedTicketDealId);

        if (errorCode != null) {
            dialog.ShowErrorMessage(view.getContext(), errorCode);
            return;
        }

        CancelConfirmDialog.newInstance("QR取消確認", "取消を行いますか？", () -> {
            final Bundle params = new Bundle();
            params.putInt("slipId", slipId);

            view.post(() -> {
                if (_isTicketIssueCancel) {
                    NavigationWrapper.navigate(view, R.id.action_ticketIssueFragment_to_navigation_qr_refund, params);
                } else {
                    NavigationWrapper.navigate(view, R.id.action_navigation_history_transaction_detail_to_navigation_qr_refund, params);
                }
            });
        }).show(_fragment.getChildFragmentManager(), null);
    }

    // 電マネ取消
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void refundEmoney(View view, String moneyBrand, int slipId, String purchasedTicketDealId) {
        final Bundle params = new Bundle();
        params.putInt("slipId", slipId);

        final CommonErrorDialog dialog = new CommonErrorDialog();
        final String errorCode = EmoneyChecker.check(view, moneyBrand, BusinessType.REFUND, purchasedTicketDealId);

        if (errorCode != null) {
            dialog.ShowErrorMessage(view.getContext(), errorCode);
            return;
        }

        if (moneyBrand.equals(_app.getString(R.string.money_brand_suica)))  {
            view.post(() -> {
                if (_isTicketIssueCancel) {
                    NavigationWrapper.navigate(view, R.id.action_ticketIssueFragment_to_navigation_emoney_suica, params);
                } else {
                    NavigationWrapper.navigate(view, R.id.action_navigation_history_transaction_detail_to_navigation_emoney_suica, params);
                }
            });
        }
        else if (moneyBrand.equals(_app.getString(R.string.money_brand_waon)))  {
            view.post(() -> {
                if (_isTicketIssueCancel) {
                    NavigationWrapper.navigate(view, R.id.action_ticketIssueFragment_to_navigation_emoney_waon, params);
                } else {
                    NavigationWrapper.navigate(view, R.id.action_navigation_history_transaction_detail_to_navigation_emoney_waon, params);
                }
            });
        }
        else if (moneyBrand.equals(_app.getString(R.string.money_brand_id)))  {
            view.post(() -> {
                if (_isTicketIssueCancel) {
                    NavigationWrapper.navigate(view, R.id.action_ticketIssueFragment_to_navigation_emoney_id, params);
                } else {
                    NavigationWrapper.navigate(view, R.id.action_navigation_history_transaction_detail_to_navigation_emoney_id, params);
                }
            });
        }
        else if (moneyBrand.equals(_app.getString(R.string.money_brand_nanaco)))  {
        }
        else if (moneyBrand.equals(_app.getString(R.string.money_brand_edy)))  {
            // Edyは取消なし
        }
        else if (moneyBrand.equals(_app.getString(R.string.money_brand_qp)))  {
            view.post(() -> {
                if (_isTicketIssueCancel) {
                    NavigationWrapper.navigate(view, R.id.action_ticketIssueFragment_to_navigation_emoney_quicpay, params);
                } else {
                    NavigationWrapper.navigate(view, R.id.action_navigation_history_transaction_detail_to_navigation_emoney_quicpay, params);
                }
            });
        }
        else {
            return;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void refundOkica(View view, int slipId, String purchasedTicketDealId) {
        final Bundle params = new Bundle();
        params.putInt("slipId", slipId);

        final CommonErrorDialog dialog = new CommonErrorDialog();

        final String errorCode = OkicaChecker.check(view, BusinessType.REFUND, purchasedTicketDealId);

        if (errorCode != null) {
            dialog.ShowErrorMessage(view.getContext(), errorCode);
            return;
        }

        view.post(() -> {
            if (_isTicketIssueCancel) {
                NavigationWrapper.navigate(view, R.id.action_ticketIssueFragment_to_navigation_emoney_okica, params);
            } else {
                NavigationWrapper.navigate(view, R.id.action_navigation_history_transaction_detail_to_navigation_emoney_okica, params);
            }
        });
    }

    public void recoveryEmoney(View view, String moneyBrand, int slipId, int transType) {
        final Bundle params = new Bundle();
        params.putInt("slipId", slipId);

        if (transType == TransMap.TYPE_SALES) {
            _app.setBusinessType(BusinessType.RECOVERY_PAYMENT);
        }
        else if (transType == TransMap.TYPE_CANCEL) {
            _app.setBusinessType(BusinessType.RECOVERY_REFUND);
        }
        else {
            Timber.e("無効な取引種別");
            return;
        }

        if (moneyBrand.equals(_app.getString(R.string.money_brand_suica)))  {
            view.post(() -> {
                NavigationWrapper.navigate(view, R.id.action_navigation_history_transaction_detail_to_navigation_emoney_suica, params);
            });
        }
        else if (moneyBrand.equals(_app.getString(R.string.money_brand_waon)))  {
            view.post(() -> {
                NavigationWrapper.navigate(view, R.id.action_navigation_history_transaction_detail_to_navigation_emoney_waon, params);
            });
        }
        else if (moneyBrand.equals(_app.getString(R.string.money_brand_id)))  {
            view.post(() -> {
                NavigationWrapper.navigate(view, R.id.action_navigation_history_transaction_detail_to_navigation_emoney_id, params);
            });
        }
        else if (moneyBrand.equals(_app.getString(R.string.money_brand_nanaco)))  {
        }
        else if (moneyBrand.equals(_app.getString(R.string.money_brand_edy)))  {
        }
        else if (moneyBrand.equals(_app.getString(R.string.money_brand_qp)))  {
        }
        else {
            return;
        }
    }

    // 和多利取消
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void refundWatari(View view, int slipId, SharedViewModel sharedViewModel) {
        final Bundle params = new Bundle();
        params.putInt("slipId", slipId);

        final CommonErrorDialog dialog = new CommonErrorDialog();
        final BusinessType type = BusinessType.POINT_REFUND;

        Activity activity = (Activity) view.getContext();
        if (activity == null) return;

        Handler handler = new Handler(Looper.getMainLooper());

        WatariChecker checker = new WatariChecker();
        checker.setListener(errCode -> handler.post(() -> {
            sharedViewModel.setLoading(false);
            //決済前チェックの結果を確認
            if (errCode != null) {
                //エラーの場合はダイアログ表示して終了
                dialog.ShowErrorMessage(activity, errCode);
                return;
            }

            _app.setBusinessType(type);
            view.post(() -> {
                NavigationWrapper.navigate(view, R.id.action_navigation_history_transaction_detail_to_navigation_watari, params);
            });
        }));

        sharedViewModel.setLoading(true);
        checker.check(view, type); //クレジット決済前チェック開始
    }

    //ADD-S BMT S.Oyama 2024/09/19 フタバ双方向向け改修
    /******************************************************************************/
    /*!
     * @brief  チケットの取り消し処理（フタバ双方向用）
     * @note   チケットの取り消し処理
     * @param [in] View view,
     *              int slipId, 旧slipid
     *              SharedViewModel sharedViewModel
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void refundSeparationTicket(View view, int slipId, SharedViewModel sharedViewModel)
    {
        final CommonErrorDialog dialog = new CommonErrorDialog();
        final BusinessType type = BusinessType.POINT_REFUND;

        Activity activity = (Activity) view.getContext();
        if (activity == null) return;

        final String errorCode = SeparationTicketChecker.check(view);
        if (errorCode != null) {
            dialog.ShowErrorMessage(activity, errorCode);
            return;
        }

        // 別スレッド：伝票印刷関連データ取得
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                _slipData  = DBManager.getSlipDao().getOneById(slipId);
            }       //旧取引データ取得
        });
        thread.start();

        try {
            thread.join();

            if (_slipData == null) {
                Timber.e("SlipData is null");
                return;
            }

            int tmpSeparationTicketAmount = _slipData.transOtherAmountOne;       //分別チケット金額

            Date exDate = new Date();   // 取引時間
            SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.JAPANESE);
            String payTime = dateFmt.format(exDate);

            _transLogger.separateTicket(payTime, tmpSeparationTicketAmount);     //分別チケットモードで取引歴登録：取消
            int tmpNewSlipID =  _transLogger.insert();

            if (tmpNewSlipID <= 0)
            {
                Timber.e("SlipData insert error");
                return;
            }

            refundSeparationTicket2(view, tmpNewSlipID);  // チケットの取り消し処理（フタバ双方向用）

        } catch (Exception e) {
            Timber.tag("HistoryEventHandlersImpl").e("%s：refundSeparationTicket->Exception e <%s>", "slipData Broken", e);
            e.printStackTrace();
        }


    }

    /******************************************************************************/
    /*!
     * @brief  チケットの取り消し処理（フタバ双方向用）
     * @note   チケットの取り消し処理
     * @param [in] View view,
     *              int slipId,  新slipID
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    public void refundSeparationTicket2(View view, int slipId) {
        PrinterManager printerManager = PrinterManager.getInstance();

        IFBoxManager.SendMeterDataInfo_FutabaD tmpSend820Info = new IFBoxManager.SendMeterDataInfo_FutabaD();
        tmpSend820Info.StatusCode = IFBoxManager.SendMeterDataStatus_FutabaD.NONE;
        tmpSend820Info.IsLoopBreakOut = false;
        tmpSend820Info.ErrorCode820 = IFBoxManager.SendMeterDataStatus_FutabaD.NONE;

        HistoryTransactionDetailViewModel._meterDataV4InfoDisposable = printerManager.getIFBoxManager().getMeterDataV4().subscribeOn(
                Schedulers.io()).observeOn(Schedulers.newThread()).subscribe(meter -> {     //AndroidSchedulers.mainThread()
            Timber.i("[FUTABA-D]HistoryEventHandlersImpl:750<-820 meter_data event cmd:%d ", meter.meter_sub_cmd);
            if(meter.meter_sub_cmd == 9) {              //ファンクション通知を受信
                //tmpSend820Info.StatusCode = IFBoxManager.SendMeterDataStatus_FutabaD.SENDOK;             //ACKが返ってきた場合
                tmpSend820Info.StatusCode = IFBoxManager.SendMeterDataStatus_FutabaD.SENDOK;
            }
        });

        HistoryTransactionDetailViewModel._meterDataV4ErrorDisposable = printerManager.getIFBoxManager().getMeterDataV4Error().subscribeOn(
                Schedulers.io()).observeOn(Schedulers.newThread()).subscribe(error -> {         //送信中にエラー受信(タイムアウト，切断)
            Timber.e("[FUTABA-D]HistoryEventHandlersImpl:Error event ErrCD:%d 820ErrCD:%d ", error.ErrorCode, error.ErrorCode820);
            tmpSend820Info.StatusCode = error.ErrorCode;
            tmpSend820Info.ErrorCode820 = error.ErrorCode820;
        });

        showProgressDialog(view.getContext());

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                printerManager.print_trans(view, slipId);

                try {
                    for(int i = 0; i < 30; i++)
                    {
                        Thread.sleep(100);         //3秒待つ
                    }
                } catch (InterruptedException e) {
                }

                for(int i = 0; i < (DuplexPrintResponseTimerSec + 1) * 10; i++)        //最大26秒ほど待ってみる
                {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                    }

                    if (tmpSend820Info.StatusCode != IFBoxManager.SendMeterDataStatus_FutabaD.NONE)         //状態に変化が出たら直ちに抜ける
                    {
                        tmpSend820Info.IsLoopBreakOut = true;
                        break;
                    }
                }

                _progressDialog.dismiss();    //ダイアログを閉じる
                _progressDialog = null;

                view.post(() -> {
                    refundSeparationTicket3(view, tmpSend820Info);
                });
            }
        });
        thread.start();
    }
    @SuppressWarnings("deprecation")
    private void showProgressDialog(Context context){
        _progressDialog = new ProgressDialog(context);
        _progressDialog.setMessage("伝票印刷中 ・・・ ");                   // 内容(メッセージ)設定
        _progressDialog.setCancelable(false);                              // キャンセル無効
        _progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);    // スタイル設定
        _progressDialog.show();
    }

    private void refundSeparationTicket3(View view, IFBoxManager.SendMeterDataInfo_FutabaD tmpSend820Info)
    {
        String tmpErrorCode = "";
        try {
            if (AmountInputSeparationPayFDViewModel._meterDataV4InfoDisposable != null) {
                AmountInputSeparationPayFDViewModel._meterDataV4InfoDisposable.dispose();
                AmountInputSeparationPayFDViewModel._meterDataV4InfoDisposable = null;
            }

            if (AmountInputSeparationPayFDViewModel._meterDataV4ErrorDisposable != null) {
                AmountInputSeparationPayFDViewModel._meterDataV4ErrorDisposable.dispose();
                AmountInputSeparationPayFDViewModel._meterDataV4ErrorDisposable = null;
            }

            if (tmpSend820Info.IsLoopBreakOut == false) {                             //820から何も返却されなかった場合のループアウト
                tmpErrorCode = "6030";                       //IFBOX接続エラー
            }
            else
            {
                switch(tmpSend820Info.StatusCode)                       //ステータスコードのチェック
                {
                    case IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_NOTCONNECTED:       //切断
                        tmpErrorCode = "6030";                       //IFBOX接続エラー
                    case IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_TIMEOUT:            //タイムアウト
                        tmpErrorCode = "6030";                       //IFBOX接続エラー
                    case IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_SELECTMODE:         //選択モードエラー
                        tmpErrorCode = "6030";                       //IFBOX接続エラー
                    case IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_SENDNG:             //zandaka_flg送信エラー(1が返ってきていない)
                        tmpErrorCode = "6030";                       //IFBOX接続エラー
                    case IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_820NACK:              //820内でが返ってきた場合
                        Timber.e("[FUTABA-D]820 Inner error! ErrCD:%d", tmpSend820Info.ErrorCode820);
                        tmpErrorCode = "6030";                       //IFBOX接続エラー
                    default:
                        //ここに到達する場合は，エラー無しで決済選択モードが送信されたことを意味する
                        break;
                }
            }

            if (tmpErrorCode.equals("") != true) {
                CommonErrorDialog dialog = new CommonErrorDialog();
                dialog.setCommonErrorEventHandlers(new CommonErrorEventHandlers() {
                    @Override
                    public void onPositiveClick(String errorCode) {
                        CommonClickEvent.RecordClickOperation("はい", "820エラー", true);
                        return;
                    }

                    @Override
                    public void onNegativeClick(String errorCode) {}

                    @Override
                    public void onNeutralClick(String errorCode) {}

                    @Override
                    public void onDismissClick(String errorCode) {}
                });
                dialog.ShowErrorMessage(view.getContext(), tmpErrorCode);
                return;
            }

        } catch (Exception e) {
            Timber.e(e);
            CommonErrorDialog dialog = new CommonErrorDialog();
            dialog.setCommonErrorEventHandlers(new CommonErrorEventHandlers() {
                @Override
                public void onPositiveClick(String errorCode) {
                    CommonClickEvent.RecordClickOperation("はい", "処理エラー", true);
                    return;
                }
                @Override
                public void onNegativeClick(String errorCode) {}

                @Override
                public void onNeutralClick(String errorCode) {}

                @Override
                public void onDismissClick(String errorCode) {}
            });
            dialog.ShowErrorMessage(view.getContext(), "2001");
            return;
        }

        NavigationWrapper.popBackStack(view);
    }

    //ADD-E BMT S.Oyama 2024/09/19 フタバ双方向向け改修

    private void keepScreen(Context context) {
        try {
            final Activity activity = (Activity) context;
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } catch (Exception ignore) {}
    }
}


//------- 仮実装
//----   820との通信部を実装加味の上でキャンセル処理を実装する

//    final Bundle params = new Bundle();
//    params.putInt("slipId", slipId);
//        Timber.i("分別チケット払いを取消します．よろしいですか？");
//        final String message =
//                "分別チケット払いを取消します\nよろしいですか？";
//        ConfirmDialog.newInstance("【分別チケット払い取消確認】",message, () -> {
//            CommonClickEvent.RecordClickOperation("はい", "取消確認", false);
//            //disposables.clear();
//
//            Amount.setTotalChangeAmount(0);         // 変更金額を初期化
//            Amount.setTicketAmount(0);      // チケット金額を初期化
//
//            Date exDate = new Date();   // 取引時間
//            SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.JAPANESE);
//            String payTime = dateFmt.format(exDate);
//
//            _transLogger.setRefundParam(slipId);
//
//            _transLogger.separateTicket(payTime, 0);            // 分別チケット　取消
//            int slipId2 = _transLogger.insert();
//            _transLogger.updateCancelFlg();
//
//            NavigationWrapper.popBackStack(view);
//
//        },() ->{
//            CommonClickEvent.RecordClickOperation("いいえ", "取消確認", false);
//        }).show(_fragment.getChildFragmentManager(), null);
//------- 仮実装
