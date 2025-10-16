package jp.mcapps.android.multi_payment_terminal.ui.amount_input;
//ADD-S BMT S.Oyama 2024/08/27 フタバ双方向向け改修

import static jp.mcapps.android.multi_payment_terminal.thread.printer.PrinterConst.DuplexPrintResponseTimerSec;
import static jp.mcapps.android.multi_payment_terminal.ui.amount_input.AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONMODE_TICKET;
import static jp.mcapps.android.multi_payment_terminal.ui.setup.IFBoxSetupViewModel.DisplayTypes.DEVICE_MENU;
import static jp.mcapps.android.multi_payment_terminal.ui.setup.IFBoxSetupViewModel.DisplayTypes.PARAMETER_MENU;
import static jp.mcapps.android.multi_payment_terminal.ui.setup.IFBoxSetupViewModel.DisplayTypes.TOP_MENU;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.BaseFragment;
import jp.mcapps.android.multi_payment_terminal.CommonClickEvent;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.NavigationWrapper;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.ScreenData;
import jp.mcapps.android.multi_payment_terminal.SharedViewModel;
import jp.mcapps.android.multi_payment_terminal.data.Amount;
import jp.mcapps.android.multi_payment_terminal.data.BusinessType;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentAmountInputSeparationpayFdBinding;
// import jp.mcapps.android.multi_payment_terminal.model.IFBoxManager;
import jp.mcapps.android.multi_payment_terminal.model.SeparationTicketChecker;
import jp.mcapps.android.multi_payment_terminal.thread.printer.PrinterManager;
import jp.mcapps.android.multi_payment_terminal.thread.printer.PrinterProc;
import jp.mcapps.android.multi_payment_terminal.ui.dialog.ConfirmDialog;
import jp.mcapps.android.multi_payment_terminal.ui.error.CommonErrorDialog;
import jp.mcapps.android.multi_payment_terminal.ui.error.CommonErrorEventHandlers;
import jp.mcapps.android.multi_payment_terminal.ui.menu.MenuEventHandlersImpl;
import jp.mcapps.android.multi_payment_terminal.ui.menu.MenuTypes;
import jp.mcapps.android.multi_payment_terminal.ui.menu.MenuViewModel;
import jp.mcapps.android.multi_payment_terminal.ui.setup.IFBoxSetupViewModel;
import timber.log.Timber;

public class AmountInputSeparationPayFDFragment extends BaseFragment implements AmountInputSeparationPayFDEventHandlers {
    private final String SCREEN_NAME = "分別入力";

    private MainApplication _app = MainApplication.getInstance();
    private SharedViewModel _sharedViewModel;

    private MenuEventHandlersImpl _menuEventHandlers ;

    public static AmountInputSeparationPayFDFragment newInstance() {
        return new AmountInputSeparationPayFDFragment();
    }
    private AmountInputSeparationPayFDViewModel _amountInputSeparationPayFDViewModel;
    @SuppressWarnings("deprecation")
    private ProgressDialog _progressDialog;
    private int _transAmountTicket = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        _sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        final MenuViewModel menuViewModel = new ViewModelProvider(this, MainApplication.getViewModelFactory()).get(MenuViewModel.class);
        menuViewModel.setBodyType(MenuTypes.EMONEY);
        _menuEventHandlers = new MenuEventHandlersImpl(this, menuViewModel);

        final FragmentAmountInputSeparationpayFdBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_amount_input_separationpay_fd, container, false);

        ViewModelProvider viewModelProvider = new ViewModelProvider(this, MainApplication.getViewModelFactory());
        _amountInputSeparationPayFDViewModel = viewModelProvider.get(AmountInputSeparationPayFDViewModel.class);

        binding.setViewModel(_amountInputSeparationPayFDViewModel);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        binding.setHandlers(this);

        ScreenData.getInstance().setScreenName(SCREEN_NAME);
        _amountInputSeparationPayFDViewModel.fetchMeterCharge();

        _sharedViewModel.setBackAction(() -> {
            showCancelConfirmDialog();
        });

        _amountInputSeparationPayFDViewModel.constructorEmu();          //各種副次初期化

        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView textView = view.findViewById(R.id.text_amount_input_ticket_payment_total);

        if (textView != null) {
            int tmpJobMode = _amountInputSeparationPayFDViewModel.getJobMode().getValue();            //表記ラベルの変更
            switch (tmpJobMode) {
                case AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONMODE_TICKET:
                    textView.setText("チケット金額");
                    break;
                case AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONMODE_CREDIT:
                    textView.setText("クレジット金額");
                    break;
                case AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONMODE_EMONEY:

                    int tmpEmoneyMode = _amountInputSeparationPayFDViewModel.getJobEmoneyMode().getValue();

                    switch (tmpEmoneyMode) {
                        case AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONEMONEYMODE_SUICA:
                            textView.setText("電子マネー（Suica）金額");
                            break;
                        case AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONEMONEYMODE_ID:
                            textView.setText("電子マネー（iD）金額");
                            break;
                        case AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONEMONEYMODE_WAON:
                            textView.setText("電子マネー（WAON）金額");
                            break;
                        case AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONEMONEYMODE_EDY:
                            textView.setText("電子マネー（Edy）金額");
                            break;
                        case AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONEMONEYMODE_QUICPAY:
                            textView.setText("電子マネー（QUICPay）金額");
                            break;
                        case AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONEMONEYMODE_NANACO:
                            textView.setText("電子マネー（nanaco）金額");
                            break;
                        case AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONEMONEYMODE_OKICA:
                            textView.setText("電子マネー（OKICA）金額");
                            break;
                        default:
                            textView.setText("電子マネー金額");
                            break;
                    }
                    break;
                case AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONMODE_PREPAID:
                    textView.setText("プリペイド金額");
                    break;
                case AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONMODE_QR:
                    textView.setText("QR決済金額");
                    break;
                default:
                    textView.setText("金額");
                    break;
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (AmountInputSeparationPayFDViewModel._meterDataV4InfoDisposable != null) {
            AmountInputSeparationPayFDViewModel._meterDataV4InfoDisposable.dispose();
            AmountInputSeparationPayFDViewModel._meterDataV4InfoDisposable = null;
        }

    }


    @Override
    public void onInputNumber(View view, String number) {
        CommonClickEvent.RecordButtonClickOperation(view, false);
        _amountInputSeparationPayFDViewModel.inputNumber(number);
    }

    @Override
    public void onCorrection(View view) {
        CommonClickEvent.RecordButtonClickOperation(view, false);
        _amountInputSeparationPayFDViewModel.correct();
    }

    /******************************************************************************/
    /*!
     * @brief  金額確定 ボタン押下
     * @note   金額確定 処理
     * @param [in] View view
     * @retval なし
     * @return　なし
     * @private
     */
    /******************************************************************************/
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onEnter(View view) {

        if (_amountInputSeparationPayFDViewModel.isSeparationPayAmountNotZero() == false)       //金額が入力されていない場合
        {
            CommonErrorDialog dialog = new CommonErrorDialog();
            dialog.setCommonErrorEventHandlers(new CommonErrorEventHandlers() {
                @Override
                public void onPositiveClick(String errorCode) {
                    CommonClickEvent.RecordClickOperation("はい", "金額エラー", true);
                    return;
                }

                @Override
                public void onNegativeClick(String errorCode) {}

                @Override
                public void onNeutralClick(String errorCode) {}

                @Override
                public void onDismissClick(String errorCode) {}
            });
            dialog.ShowErrorMessage(getContext(), "2001");
            return;
        }

        if (_amountInputSeparationPayFDViewModel.isSeparationPayAmountOrMore() == true)       //金額が超えている場合
        {
            CommonErrorDialog dialog = new CommonErrorDialog();
            dialog.setCommonErrorEventHandlers(new CommonErrorEventHandlers() {
                @Override
                public void onPositiveClick(String errorCode) {
                    CommonClickEvent.RecordClickOperation("はい", "金額エラー", true);
                    return;
                    //fixAmount(view);
                }

                @Override
                public void onNegativeClick(String errorCode) {}

                @Override
                public void onNeutralClick(String errorCode) {}

                @Override
                public void onDismissClick(String errorCode) {}
            });
            dialog.ShowErrorMessage(getContext(), "9102");
            return;
        }
        else
        {
            fixAmount(view);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void fixAmount(View view) {

        int amount = _amountInputSeparationPayFDViewModel.getSeparationPayAmount().getValue();
        CommonClickEvent.RecordInputOperation(view, String.valueOf(amount), true);

        _amountInputSeparationPayFDViewModel.enter();       //金額の確定

        int tmpJobMode = _amountInputSeparationPayFDViewModel.getJobMode().getValue();

        if (tmpJobMode == AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONMODE_TICKET) {      //チケットは別処理
            AppPreference.setAmountInputCancel(false);              //HOME等で金額入力を行えるようにする
            _sharedViewModel.setBackAction(null);

            _app.setBusinessType(BusinessType.PAYMENT);                                 //実装確認！！
            fixAmountJumpTicket(view);                                                  //チケット関連の諸々の処理へへ
        } else {

            view.post(() -> {
                //NavigationWrapper.popBackStack(view);

                AppPreference.setAmountInputCancel(false);              //HOME等で金額入力を行えるようにする
                _sharedViewModel.setBackAction(null);

                switch (tmpJobMode) {
//                    case AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONMODE_TICKET:     //チケット
//                        _app.setBusinessType(BusinessType.PAYMENT);                                 //実装確認！！
//                        fixAmountJumpTicket(view);                                                  //チケット関連の諸々の処理へへ
//                        break;
                    case AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONMODE_CREDIT:     //クレジット
                        _app.setBusinessType(BusinessType.PAYMENT);                                 //実装確認！！
                        fixAmountJumpCredit(view);                                                  //クレジット画面へ
                        break;
                    case AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONMODE_EMONEY:     //電子マネー
                        _app.setBusinessType(BusinessType.PAYMENT);                                 //実装確認！！
                        fixAmountJumpEMoney(view);                                                  //電子マネー画面へ
                        break;
                    case AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONMODE_QR:         //QRコード決済
                        _app.setBusinessType(BusinessType.PAYMENT);                                 //実装確認！！
                        fixAmountJumpQR(view);                                                      //QRコード決済画面へ
                        break;
                    case AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONMODE_PREPAID:
                        //後日(F2)実装
                        break;
                    default:
                        break;
                }
            });
        }
    }

    /******************************************************************************/
    /*!
     * @brief  チケット時　SLIPレコードの保存と，820への送信処理
     * @note　チケット時　SLIPレコードの保存と，820への送信処理
     * @param [in] View view
     * @retval なし
     * @return　なし
     * @private
     */
    /******************************************************************************/
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void fixAmountJumpTicket(View view)
    {

        final String errorCode = SeparationTicketChecker.check(view);
        if (errorCode != null) {
            CommonErrorDialog dialog = new CommonErrorDialog();
            dialog.ShowErrorMessage(view.getContext(), errorCode);
            return;
        }

        int tmpSlipID ;

        tmpSlipID = _amountInputSeparationPayFDViewModel.fixAmountRegistHistrySlipTicket();    //チケットの履歴登録

        if (tmpSlipID < 0)          //取得IDが不正の場合はレコード生成失敗
        {
            CommonErrorDialog dialog = new CommonErrorDialog();
            dialog.setCommonErrorEventHandlers(new CommonErrorEventHandlers() {
                @Override
                public void onPositiveClick(String errorCode) {
                    CommonClickEvent.RecordClickOperation("はい", "金額エラー", true);
                    return;
                }

                @Override
                public void onNegativeClick(String errorCode) {}

                @Override
                public void onNeutralClick(String errorCode) {}

                @Override
                public void onDismissClick(String errorCode) {}
            });
            dialog.ShowErrorMessage(getContext(), "2001");
            return;
        }

//        IFBoxManager.SendMeterDataInfo_FutabaD tmpSend820Info = new IFBoxManager.SendMeterDataInfo_FutabaD();
//        tmpSend820Info.StatusCode = IFBoxManager.SendMeterDataStatus_FutabaD.NONE;
//        tmpSend820Info.IsLoopBreakOut = false;
//        tmpSend820Info.ErrorCode820 = IFBoxManager.SendMeterDataStatus_FutabaD.NONE;
//
//        AmountInputSeparationPayFDViewModel._meterDataV4InfoDisposable = _amountInputSeparationPayFDViewModel.getIfBoxManager().getMeterDataV4().subscribeOn(
//                Schedulers.io()).observeOn(Schedulers.newThread()).subscribe(meter -> {     //AndroidSchedulers.mainThread()
//            Timber.i("[FUTABA-D]AmountInputSeparationPayFDFragment:750<-820 meter_data event cmd:%d ", meter.meter_sub_cmd);
//            if(meter.meter_sub_cmd == 9) {              //ファンクション通知を受信
//                //tmpSend820Info.StatusCode = IFBoxManager.SendMeterDataStatus_FutabaD.SENDOK;             //ACKが返ってきた場合
//                _transAmountTicket = meter.trans_amount;             //通知されてきた金額
//                tmpSend820Info.StatusCode = IFBoxManager.SendMeterDataStatus_FutabaD.SENDOK;
//            }
//        });
//
//        AmountInputSeparationPayFDViewModel._meterDataV4ErrorDisposable = _amountInputSeparationPayFDViewModel.getIfBoxManager().getMeterDataV4Error().subscribeOn(
//                Schedulers.io()).observeOn(Schedulers.newThread()).subscribe(error -> {         //送信中にエラー受信(タイムアウト，切断)
//            Timber.e("[FUTABA-D]HistoryEventHandlersImpl:Error event ErrCD:%d 820ErrCD:%d ", error.ErrorCode, error.ErrorCode820);
//            tmpSend820Info.StatusCode = error.ErrorCode;
//            tmpSend820Info.ErrorCode820 = error.ErrorCode820;
//        });

        showProgressDialog();

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                PrinterProc printerProc = PrinterProc.getInstance();            //プリンター処理クラスのインスタンス取得
                printerProc.printTransFutabaD(tmpSlipID);                       //820へ送信処理

                try {
                    for(int i = 0; i < 70; i++)
                    {
                        Thread.sleep(100);         //7秒待つ
                    }
                } catch (InterruptedException e) {
                }

                for(int i = 0; i < (DuplexPrintResponseTimerSec + 1) * 10; i++)        //最大26秒ほど待ってみる
                {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                    }

//                    if (tmpSend820Info.StatusCode != IFBoxManager.SendMeterDataStatus_FutabaD.NONE)         //状態に変化が出たら直ちに抜ける
//                    {
//                        tmpSend820Info.IsLoopBreakOut = true;
//                        break;
//                    }
                }

                _progressDialog.dismiss();    //ダイアログを閉じる
                _progressDialog = null;

                view.post(() -> {
                    //fixAmountJumpTicket2ndJob(view, tmpSend820Info);        //820送信後の処理
                });
            }
        });
        thread.start();

    }
//
//    @RequiresApi(api = Build.VERSION_CODES.N)
//    private void fixAmountJumpTicket2ndJob(View view, )
//    {
//        String tmpErrorCode = "";
//        try {
//            if (AmountInputSeparationPayFDViewModel._meterDataV4InfoDisposable != null) {
//                AmountInputSeparationPayFDViewModel._meterDataV4InfoDisposable.dispose();
//                AmountInputSeparationPayFDViewModel._meterDataV4InfoDisposable = null;
//            }
//
//            if (AmountInputSeparationPayFDViewModel._meterDataV4ErrorDisposable != null) {
//                AmountInputSeparationPayFDViewModel._meterDataV4ErrorDisposable.dispose();
//                AmountInputSeparationPayFDViewModel._meterDataV4ErrorDisposable = null;
//            }
//
//            if (tmpSend820Info.IsLoopBreakOut == false) {                             //820から何も返却されなかった場合のループアウト
//                tmpErrorCode = "6030";                       //IFBOX接続エラー
//            }
//            else
//            {
//                switch(tmpSend820Info.StatusCode)                       //ステータスコードのチェック
//                {
//                    case IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_NOTCONNECTED:       //切断
//                        tmpErrorCode = "6030";                       //IFBOX接続エラー
//                    case IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_TIMEOUT:            //タイムアウト
//                        tmpErrorCode = "6030";                       //IFBOX接続エラー
//                    case IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_SELECTMODE:         //選択モードエラー
//                        tmpErrorCode = "6030";                       //IFBOX接続エラー
//                    case IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_SENDNG:             //zandaka_flg送信エラー(1が返ってきていない)
//                        tmpErrorCode = "6030";                       //IFBOX接続エラー
//                    case IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_820NACK:              //820内でが返ってきた場合
//                        Timber.e("[FUTABA-D]820 Inner error! ErrCD:%d", tmpSend820Info.ErrorCode820);
//                        tmpErrorCode = "6030";                       //IFBOX接続エラー
//                    default:
//                        //ここに到達する場合は，エラー無しで決済選択モードが送信されたことを意味する
//                        break;
//                }
//            }
//
//            if (tmpErrorCode.equals("") != true) {
//                CommonErrorDialog dialog = new CommonErrorDialog();
//                dialog.setCommonErrorEventHandlers(new CommonErrorEventHandlers() {
//                    @Override
//                    public void onPositiveClick(String errorCode) {
//                        CommonClickEvent.RecordClickOperation("はい", "820エラー", true);
//                        return;
//                    }
//
//                    @Override
//                    public void onNegativeClick(String errorCode) {}
//
//                    @Override
//                    public void onNeutralClick(String errorCode) {}
//
//                    @Override
//                    public void onDismissClick(String errorCode) {}
//                });
//                dialog.ShowErrorMessage(view.getContext(), tmpErrorCode);
//                return;
//            }
//
//            _amountInputSeparationPayFDViewModel.SetFixAmountRecv820(_transAmountTicket);    //820受信後の処理
//        } catch (Exception e) {
//            Timber.e(e);
//            CommonErrorDialog dialog = new CommonErrorDialog();
//            dialog.setCommonErrorEventHandlers(new CommonErrorEventHandlers() {
//                @Override
//                public void onPositiveClick(String errorCode) {
//                    CommonClickEvent.RecordClickOperation("はい", "処理エラー", true);
//                    return;
//                }
//                @Override
//                public void onNegativeClick(String errorCode) {}
//
//                @Override
//                public void onNeutralClick(String errorCode) {}
//
//                @Override
//                public void onDismissClick(String errorCode) {}
//            });
//            dialog.ShowErrorMessage(view.getContext(), "2001");
//            return;
//        }
//
////        final String message = "現金決済時は領収書を発行してください";
////        new AlertDialog.Builder(view.getContext())
////                .setTitle("【現金決済時確認】")
////                .setMessage(message)
////                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
////                            @Override
////                            public void onClick(DialogInterface dialog, int which) {
////                                NavigationWrapper.navigate(view,  R.id.navigation_menu);                    //home画面へ
////                            }
////                        }
////                ).show();
//
//        //PrinterProc printerProc = PrinterProc.getInstance();            //プリンター処理クラスのインスタンス取得
//        //printerProc.getIFBoxManager().send820_SeparateTicketJobFix_KeyCode();       //セットキーを送信
//
//        NavigationWrapper.navigate(view,  R.id.action_navigation_separationpay_to_navigation_menu_separation_with_ticket);                    //分別のチケット＆ほげほげメニューへ
//
//    }


    /******************************************************************************/
    /*!
     * @brief  クレジット時　決済選択情報を８２０へ送信後，実際のカードスキャン画面へ遷移
     * @note　クレジット時　決済選択情報を８２０へ送信後，実際のカードスキャン画面へ遷移
     * @param [in] View view
     * @retval なし
     * @return　なし
     * @private
     */
    /******************************************************************************/
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void fixAmountJumpCredit(View view) {

        //NavigationWrapper.navigate(view,  R.id.navigation_credit_card_scan);        //クレジットスキャン画面へ

        _menuEventHandlers.navigateToCreditCardScanSeparation(view, _sharedViewModel);        //クレジットスキャン画面へ
    }

    /******************************************************************************/
    /*!
     * @brief  電子マネー時　決済選択情報を８２０へ送信後，実際のカードスキャン画面へ遷移
     * @note
     * @param [in] View view
     * @retval なし
     * @return　なし
     * @private
     */
    /******************************************************************************/
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void fixAmountJumpEMoney(View view)
    {
        int tmpEmoneyMode = _amountInputSeparationPayFDViewModel.getJobEmoneyMode().getValue();

        switch (tmpEmoneyMode) {
            case AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONEMONEYMODE_SUICA:
                _menuEventHandlers.navigateToEmoneySuicaSeparation(view, BusinessType.PAYMENT);
                break;
            case AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONEMONEYMODE_ID:
                _menuEventHandlers.navigateToEmoneyIdSeparation(view, BusinessType.PAYMENT);
                break;
            case AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONEMONEYMODE_WAON:
                _menuEventHandlers.navigateToEmoneyWaonSeparation(view, BusinessType.PAYMENT);
                break;
            case AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONEMONEYMODE_EDY:
                _menuEventHandlers.navigateToEmoneyEdySeparation(view, BusinessType.PAYMENT);
                break;
            case AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONEMONEYMODE_QUICPAY:
                _menuEventHandlers.navigateToEmoneyQuicPaySeparation(view, BusinessType.PAYMENT);
                break;
            case AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONEMONEYMODE_NANACO:
                _menuEventHandlers.navigateToEmoneyNanacoSeparation(view, BusinessType.PAYMENT);
                break;
            case AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONEMONEYMODE_OKICA:
                _menuEventHandlers.navigateToEmoneyOkicaSeparation(view, BusinessType.PAYMENT);
                break;
            default:
                break;
        }
    }

    /******************************************************************************/
    /*!
     * @brief  QR時　決済選択情報を８２０へ送信後，実際のカードスキャン画面へ遷移
     * @note　QR時　決済選択情報を８２０へ送信後，実際のカードスキャン画面へ遷移
     * @param [in] View view
     * @retval なし
     * @return　なし
     * @private
     */
    /******************************************************************************/
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void fixAmountJumpQR(View view) {
        //NavigationWrapper.navigate(view,  R.id.navigation_qr_payment);              //QRスキャン画面へ
        _menuEventHandlers.navigateToQRSeparation(view, _sharedViewModel);              //QRスキャン画面へ
    }

    /******************************************************************************/
    /*!
     * @brief  立替分割払い　キャンセル処理　画面 ボタン押下
     * @note   立替分割払い　キャンセル時の画面系処理
     * @param [in] View view
     * @retval なし
     * @return　なし
     * @private
     */
    /******************************************************************************/
    @Override
    public void onCancel(View view) {
        CommonClickEvent.RecordButtonClickOperation(view, true);
        _amountInputSeparationPayFDViewModel.cancel();
    }

    @Override
    public void onReset(View view) {
        CommonClickEvent.RecordButtonClickOperation(view, true);
        _amountInputSeparationPayFDViewModel.reset();
    }

    @Override
    public void onChangeBack(View view) {
        CommonClickEvent.RecordButtonClickOperation(view, false);
        _amountInputSeparationPayFDViewModel.changeBack();
    }

    @Override
    public void onApply(View view) {
        CommonClickEvent.RecordButtonClickOperation(view, false);
        _amountInputSeparationPayFDViewModel.apply();
    }

    /******************************************************************************/
    /*!
     * @brief  本画面中戻るボタンを押下時に続行確認するダイアログ表示
     * @note   中段確認ダイアログを表示させる．中断時は入力内容を破棄する
     * @param [in] View view
     * @retval なし
     * @return　なし
     * @private
     */
    /******************************************************************************/
    private void showCancelConfirmDialog() {
        Timber.i("中断確認:分割入力を中断した場合，金額はクリアされます．中断してよろしいですか？");
        final String message =
                "中断確認:分割入力を中断した場合，金額はクリアされます．\n中断してよろしいですか？";
        ConfirmDialog.newInstance("【中断確認】",message, () -> {
            CommonClickEvent.RecordClickOperation("はい", "中断確認", false);
            //disposables.clear();
            NavigationWrapper.popBackStack(this);
            _sharedViewModel.setBackAction(null);
        },() ->{
            CommonClickEvent.RecordClickOperation("いいえ", "中断確認", false);
        }).show(getChildFragmentManager(), null);
    }

    @SuppressWarnings("deprecation")
    private void showProgressDialog() {
        _progressDialog = new ProgressDialog(getContext());
        _progressDialog.setMessage("伝票印刷中 ・・・ ");                   // 内容(メッセージ)設定
        _progressDialog.setCancelable(false);                              // キャンセル無効
        _progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);    // スタイル設定
        _progressDialog.show();                                            // ダイアログを表示
    }
}
//ADD-E BMT S.Oyama 2024/08/27 フタバ双方向向け改修

//ProgressDialog progressDialog;
//Thread thread;
//progressDialog = new ProgressDialog(this.getContext());
//progressDialog.setTitle("通信中");
//progressDialog.setMessage("金額情報を通信中");
//progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
//progressDialog.show();
//new Thread(new Runnable(){
//@Override
//public void run(){
//        // todo
//        try {
//        thread.sleep(2000);
//        } catch (InterruptedException e)
//        {
//
//        }
//
//        progressDialog.dismiss();
//        progressDialog = null;
//}).start();

//        Integer amount = _amountInputInstallmentPayFDViewModel.getFlatRateAmount().getValue() > 0
//                ? _amountInputInstallmentPayFDViewModel.getFlatRateAmount().getValue()
//                : _amountInputInstallmentPayFDViewModel.getMeterCharge().getValue();
//        amount += _amountInputInstallmentPayFDViewModel.getTotalChangeAmount().getValue();

//        AppPreference.setAmountInputCancel(false);              //HOME等で金額入力を行えるようにする
//        _sharedViewModel.setBackAction(null);
//        _sharedViewModel.setSeparationJobMode(AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONMODE_NONE);     // 分割払いジョブモードをクリア
//        _sharedViewModel.setSeparationJobEmoneyMode(AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONEMONEYMODE_NONE);   // 電子マネー種別をクリア
//
//        _app.setBusinessType(BusinessType.PAYMENT);                                             //実装確認
//
//        fixAmountJumpEMoney(view);



//        AmountInputSeparationPayFDViewModel._meterDataV4InfoDisposable = _amountInputSeparationPayFDViewModel.getIfBoxManager().getMeterDataV4().subscribeOn(
//                Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(meter -> {
//                Timber.i("[FUTABA-D]AmountInputSeparationPayFDFragment:750<-820 meter_data event cmd:%d ", meter.meter_sub_cmd);
//                if(meter.meter_sub_cmd == 9) {              //ファンクション通知を受信
//                //tmpSend820Info.StatusCode = IFBoxManager.SendMeterDataStatus_FutabaD.SENDOK;             //ACKが返ってきた場合
//                int tmpAmount = meter.trans_amount;             //通知されてきた金額
//
//                _amountInputSeparationPayFDViewModel.SetFixAmountRecv820(tmpAmount);    //820受信後の処理
//
//                _sharedViewModel.setSeparationJobMode(AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONMODE_NONE);     // 分割払いジョブモードをクリア
//                _sharedViewModel.setSeparationJobEmoneyMode(AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONEMONEYMODE_NONE);   // 電子マネー種別をクリア
//
//
//
//                NavigationWrapper.navigate(view,  R.id.navigation_menu);                    //home画面へ
//
//                if (AmountInputSeparationPayFDViewModel._meterDataV4InfoDisposable != null) {
//                AmountInputSeparationPayFDViewModel._meterDataV4InfoDisposable.dispose();
//                AmountInputSeparationPayFDViewModel._meterDataV4InfoDisposable = null;
//                }
//                }
//                });
//
//
//                PrinterProc printerProc = PrinterProc.getInstance();            //プリンター処理クラスのインスタンス取得
//                printerProc.printTransFutabaD(tmpSlipID);                       //820へ送信処理
