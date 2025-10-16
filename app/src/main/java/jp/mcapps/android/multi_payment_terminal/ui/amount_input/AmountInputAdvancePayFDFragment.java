package jp.mcapps.android.multi_payment_terminal.ui.amount_input;
//ADD-S BMT S.Oyama 2024/08/27 フタバ双方向向け改修

import static jp.mcapps.android.multi_payment_terminal.AppPreference.isDemoMode;
import static jp.mcapps.android.multi_payment_terminal.thread.printer.PrinterConst.DuplexPrintResponseTimerSec;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.BaseFragment;
import jp.mcapps.android.multi_payment_terminal.CommonClickEvent;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.NavigationWrapper;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.ScreenData;
import jp.mcapps.android.multi_payment_terminal.data.Amount;
import jp.mcapps.android.multi_payment_terminal.data.BusinessType;
import jp.mcapps.android.multi_payment_terminal.data.IFBoxAppModels;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentAmountInputAdvancepayFdBinding;
//import jp.mcapps.android.multi_payment_terminal.model.IFBoxManager;
import jp.mcapps.android.multi_payment_terminal.model.SeparationTicketChecker;
import jp.mcapps.android.multi_payment_terminal.thread.printer.PrinterConst;
import jp.mcapps.android.multi_payment_terminal.thread.printer.PrinterManager;
import jp.mcapps.android.multi_payment_terminal.thread.printer.PrinterProc;
import jp.mcapps.android.multi_payment_terminal.ui.dialog.ConfirmDialog;
import jp.mcapps.android.multi_payment_terminal.ui.dialog.RadioDialogFragment;
import jp.mcapps.android.multi_payment_terminal.ui.error.CommonErrorDialog;
import jp.mcapps.android.multi_payment_terminal.ui.error.CommonErrorEventHandlers;
import jp.mcapps.android.multi_payment_terminal.ui.menu.MenuViewModel;
import timber.log.Timber;

public class AmountInputAdvancePayFDFragment extends BaseFragment implements AmountInputAdvancePayFDEventHandlers, RadioDialogFragment.RadioDialogListener {
    private final String SCREEN_NAME = "立替定額入力";
    private MainApplication _app = MainApplication.getInstance();
    private MenuViewModel _menuViewModel;

    private final int RADIODLG_ADVANCEPAY_FLATRATE = 1;
    private final int RADIODLG_SEPARATION = 2;
    private final int RADIODLG_CANCEL = 3;

    public static AmountInputAdvancePayFDFragment newInstance() {
        return new AmountInputAdvancePayFDFragment();
    }
    private AmountInputAdvancePayFDViewModel _amountInputAdvancePayFDViewModel;

    private int _radioDialogKind;
    private int _radioDialogIndex;
    private View _targetView;
    @SuppressWarnings("deprecation")
    private ProgressDialog _progressDialog;
    private int _transAmountTicket = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        final FragmentAmountInputAdvancepayFdBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_amount_input_advancepay_fd, container, false);

        ViewModelProvider viewModelProvider = new ViewModelProvider(this, MainApplication.getViewModelFactory());
        _amountInputAdvancePayFDViewModel = viewModelProvider.get(AmountInputAdvancePayFDViewModel.class);

        binding.setViewModel(_amountInputAdvancePayFDViewModel);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        binding.setHandlers(this);

        ScreenData.getInstance().setScreenName(SCREEN_NAME);
        _amountInputAdvancePayFDViewModel.fetchMeterCharge();

        Fragment menuFragment = getParentFragment().getParentFragment();
        if(menuFragment == null) menuFragment = getParentFragment();
        _menuViewModel = new ViewModelProvider(menuFragment, MainApplication.getViewModelFactory()).get(MenuViewModel.class);

        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (AmountInputAdvancePayFDViewModel._meterDataV4InfoDisposable != null) {
            AmountInputAdvancePayFDViewModel._meterDataV4InfoDisposable.dispose();
            AmountInputAdvancePayFDViewModel._meterDataV4InfoDisposable = null;
        }

        if (AmountInputAdvancePayFDViewModel._meterDataV4ErrorDisposable != null) {
            AmountInputAdvancePayFDViewModel._meterDataV4ErrorDisposable.dispose();
            AmountInputAdvancePayFDViewModel._meterDataV4ErrorDisposable = null;
        }

        //ADD-S BMT S.Oyama 2025/02/27 フタバ双方向向け改修
        if (AmountInputAdvancePayFDViewModel._meterDataV4InfoDisposableAdvance != null) {
            AmountInputAdvancePayFDViewModel._meterDataV4InfoDisposableAdvance.dispose();
            AmountInputAdvancePayFDViewModel._meterDataV4InfoDisposableAdvance = null;
        }

        if (AmountInputAdvancePayFDViewModel._meterDataV4ErrorDisposableAdvance != null) {
            AmountInputAdvancePayFDViewModel._meterDataV4ErrorDisposableAdvance.dispose();
            AmountInputAdvancePayFDViewModel._meterDataV4ErrorDisposableAdvance = null;
        }
        //ADD-E BMT S.Oyama 2025/02/27 フタバ双方向向け改修
    }

    @Override
    public void onInputNumber(View view, String number) {
        CommonClickEvent.RecordButtonClickOperation(view, false);
        _amountInputAdvancePayFDViewModel.inputNumber(number);
    }

    @Override
    public void onCorrection(View view) {
        CommonClickEvent.RecordButtonClickOperation(view, false);
        _amountInputAdvancePayFDViewModel.correct();
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
        final String mode = _amountInputAdvancePayFDViewModel.getInputMode().getValue();

        if (checkInputPossible(mode)) {
            if (AmountInputAdvancePayFDViewModel.InputModes.SEPARATION_TICKET.equals(mode) ||
                AmountInputAdvancePayFDViewModel.InputModes.SEPARATION_CASH.equals(mode)) {
                // チケット金額確定 or 決済金額確定
                onEnterSeparation(view);
            } else {
                if (_amountInputAdvancePayFDViewModel.getChangeAmount().getValue() != null && _amountInputAdvancePayFDViewModel.getChangeAmount().getValue() != 0) {
                    CommonErrorDialog dialog = new CommonErrorDialog();
                    dialog.setCommonErrorEventHandlers(new CommonErrorEventHandlers() {
                        @Override
                        public void onPositiveClick(String errorCode) {
                            CommonClickEvent.RecordClickOperation("はい", "金額警告", true);
                            fixAmount(view);
                        }

                        @Override
                        public void onNegativeClick(String errorCode) {
                            CommonClickEvent.RecordClickOperation("いいえ", "金額警告", true);
                        }

                        @Override
                        public void onNeutralClick(String errorCode) {

                        }

                        @Override
                        public void onDismissClick(String errorCode) {

                        }
                    });
                    dialog.ShowErrorMessage(getContext(), "2002");
                } else {
                    fixAmount(view);
                }
            }
        } else {
            // 金額操作不可のエラー表示
            ShowErrorMessageInputError(getContext(), mode, "金額操作不可");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void onEnterSeparation(View view) {
        if (_amountInputAdvancePayFDViewModel.isSeparationPayAmountNotZero() == false)       //金額が入力されていない場合
        {
            ShowErrorMessage2001(getContext(), "金額エラー");
            return;
        }

        if (_amountInputAdvancePayFDViewModel.isSeparationPayAmountOrMore() == true)       //金額が超えている場合
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
            fixAmountSeparation(view);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    /******************************************************************************/
    /*!
     * @brief  金額確定 最終処理
     * @note   金額確定 最終処理
     * @param [in] View view
     * @retval なし
     * @return　なし
     * @private
     */
    /******************************************************************************/
    private void fixAmount(View view) {

        if (_amountInputAdvancePayFDViewModel.isConnected820() == false)
        {
            CommonErrorDialog dialog = new CommonErrorDialog();
            dialog.setCommonErrorEventHandlers(new CommonErrorEventHandlers() {
                @Override
                public void onPositiveClick(String errorCode) {
                    CommonClickEvent.RecordClickOperation("はい", "820通信未確立", true);
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
            });
            if (isDemoMode() == false) {     //デモモードでないとき
                dialog.ShowErrorMessage(getContext(), "2018");                  //エラーダイアログ表示
                return;
            }
        }

        String tmpInputMode = _amountInputAdvancePayFDViewModel.getInputMode().getValue();
        int amount = 0;

        if (AmountInputAdvancePayFDViewModel.AmountInputConverters.isSelectedAdvancePay(tmpInputMode) == true )     //立替モード
        {
            amount = _amountInputAdvancePayFDViewModel.getAdvancePayAmount().getValue();
        }
        else if (AmountInputAdvancePayFDViewModel.AmountInputConverters.isSelectedFlatRate(tmpInputMode) == true )     //定額モード
        {
            amount = _amountInputAdvancePayFDViewModel.getFlatRateAmount().getValue();
        }

        CommonClickEvent.RecordInputOperation(view, String.valueOf(amount), true);          //作業記録処理

//        _amountInputAdvancePayFDViewModel.enter();              //820へ金額情報を送付
//        view.post(() -> {
//            NavigationWrapper.popBackStack(view);               //画面遷移
//        });

//        IFBoxManager.SendMeterDataInfo_FutabaD tmpSend820Info = new IFBoxManager.SendMeterDataInfo_FutabaD();
//        tmpSend820Info.StatusCode = IFBoxManager.SendMeterDataStatus_FutabaD.NONE;
//        tmpSend820Info.IsLoopBreakOut = false;
//        tmpSend820Info.ErrorCode820 = IFBoxManager.SendMeterDataStatus_FutabaD.NONE;
//
//        AmountInputAdvancePayFDViewModel._meterDataV4InfoDisposableAdvance = _amountInputAdvancePayFDViewModel.getIfBoxManager().getMeterDataV4().subscribeOn(
//                Schedulers.io()).observeOn(Schedulers.newThread()).subscribe(meter -> {     //AndroidSchedulers.mainThread()
//
//            if(meter.meter_sub_cmd == 5 && meter.sound_no == 0) {       //処理コード表示要求中，FREE ER**の場合
//                if ((meter.line_1 != null) && (meter.line_2 != null)) {
//                    int tmpProcessCode = _menuViewModel.getIFBoxManager().send820_IsProcessCode_ErrorCD(meter.line_1);          //処理コード表示要求エラーコードはLine１に乗ってくる　エラーコードを取得
//                    if (tmpProcessCode < 0) {           //エラーコードがある場合
//                        tmpSend820Info.StatusCode = IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_PROCESSCODE_ERRRETURN;
//                    }
//                }
//            }
//        });

//        AmountInputAdvancePayFDViewModel._meterDataV4ErrorDisposableAdvance = _amountInputAdvancePayFDViewModel.getIfBoxManager().getMeterDataV4Error().subscribeOn(
//                Schedulers.io()).observeOn(Schedulers.newThread()).subscribe(error -> {         //送信中にエラー受信(タイムアウト，切断)
//            tmpSend820Info.StatusCode = error.ErrorCode;
//            tmpSend820Info.ErrorCode820 = error.ErrorCode820;
//        });

        showProgressDialog("確認中 ・・・ ");

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                _amountInputAdvancePayFDViewModel.enter();              //820へ金額情報を送付

                for(int i = 0; i < (2) * 10; i++)        //最大2秒ほど待ってみる
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
                    //fixAmountJumpAdvance2ndJob(view, tmpSend820Info);        //820送信後の処理
                });
            }
        });
        thread.start();
    }

//    @RequiresApi(api = Build.VERSION_CODES.N)
//    private void fixAmountJumpAdvance2ndJob(View view, IFBoxManager.SendMeterDataInfo_FutabaD tmpSend820Info) {
//        String tmpErrorCode = "";
//        try {
//            if (AmountInputAdvancePayFDViewModel._meterDataV4InfoDisposableAdvance != null) {
//                AmountInputAdvancePayFDViewModel._meterDataV4InfoDisposableAdvance.dispose();
//                AmountInputAdvancePayFDViewModel._meterDataV4InfoDisposableAdvance = null;
//            }
//
//            if (AmountInputAdvancePayFDViewModel._meterDataV4ErrorDisposableAdvance != null) {
//                AmountInputAdvancePayFDViewModel._meterDataV4ErrorDisposableAdvance.dispose();
//                AmountInputAdvancePayFDViewModel._meterDataV4ErrorDisposableAdvance = null;
//            }
//
//            if (tmpSend820Info.IsLoopBreakOut == false) {                             //820から何も返却されなかった場合のループアウト
//                //tmpErrorCode = "6030";                       //IFBOX接続エラー
//                //ここに到達する場合は，エラー無しで電文が送信されたことを意味する
//            }
//            else
//            {
//                switch(tmpSend820Info.StatusCode)                       //ステータスコードのチェック
//                {
//                    case IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_NOTCONNECTED:       //切断
//                        tmpErrorCode = "6030";                       //IFBOX接続エラー
//                        break;
//                    case IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_TIMEOUT:            //タイムアウト
//                        tmpErrorCode = "6030";                       //IFBOX接続エラー
//                        break;
//                    case IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_SELECTMODE:         //選択モードエラー
//                        tmpErrorCode = "6030";                       //IFBOX接続エラー
//                        break;
//                    case IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_SENDNG:             //zandaka_flg送信エラー(1が返ってきていない)
//                        tmpErrorCode = "6030";                       //IFBOX接続エラー
//                        break;
//                    case IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_820NACK:              //820内でが返ってきた場合
//                        Timber.e("[FUTABA-D]820 Inner error! ErrCD:%d", tmpSend820Info.ErrorCode820);
//                        tmpErrorCode = "6030";                       //IFBOX接続エラー
//                        break;
//                    case IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_PAPERLACKING:       //紙切れエラー
//                        tmpErrorCode = _app.getString(R.string.error_type_FutabaD_Outofpaper_Norestart);
//                        break;
//                    case IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_PROCESSCODE_ERRRETURN:    //処理コード表示要求中，FREE ER**の場合
//                        tmpErrorCode = _app.getString(R.string.error_type_FutabaD_MeterErrorMes_Cancel_R);;
//                        break;
//                    default:
//                        //ここに到達する場合は，エラー無しで電文が送信されたことを意味する
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
//                        if (tmpSend820Info.StatusCode == IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_PROCESSCODE_ERRRETURN) {
//                            _amountInputAdvancePayFDViewModel.getIfBoxManager().send820_GenericProcessCodeErrReturn();                        //異常処理コード画面戻り要求キーを送信 ACKを必要としないモードで
//                        }
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
//        } catch (Exception e) {
//            Timber.e(e);
//            ShowErrorMessage2001(view.getContext(), "処理エラー");
//            return;
//        }
//
//        view.post(() -> {
//            NavigationWrapper.popBackStack(view);               //画面遷移
//        });
//    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    private void fixAmountSeparation(View view) {
        int amount = _amountInputAdvancePayFDViewModel.getSeparationPayAmount().getValue();
        CommonClickEvent.RecordInputOperation(view, String.valueOf(amount), true);

        _amountInputAdvancePayFDViewModel.enterSeparation();       //金額の確定

        final String mode = _amountInputAdvancePayFDViewModel.getInputMode().getValue();
        if (mode == null) {
            // 何もしない
        } else if (AmountInputAdvancePayFDViewModel.InputModes.SEPARATION_TICKET.equals(mode)) {
            // チケット金額確定
            AppPreference.setAmountInputCancel(false);              //HOME等で金額入力を行えるようにする

            _app.setBusinessType(BusinessType.PAYMENT);                                 //実装確認！！
            fixAmountJumpTicket(view);                                                  //チケット関連の諸々の処理へへ
        } else if (AmountInputAdvancePayFDViewModel.InputModes.SEPARATION_CASH.equals(mode)) {
            view.post(() -> {
                AppPreference.setAmountInputCancel(false);              //HOME等で金額入力を行えるようにする
                NavigationWrapper.popBackStack(view);               //画面遷移
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
        String[] transDate = {"0"};
        int[] termSequence = {0};

        tmpSlipID = _amountInputAdvancePayFDViewModel.fixAmountRegistHistrySlipTicket(_amountInputAdvancePayFDViewModel.getSeparationPayAmount().getValue(), transDate, termSequence);    //チケットの履歴登録
        // ここで追加したチケットの履歴は、チケット伝票を印刷後に削除する
        // その時に、transDate（取引日時）、termSequence（機器通番）を指定する

        if (tmpSlipID < 0)          //取得IDが不正の場合はレコード生成失敗
        {
            ShowErrorMessage2001(getContext(), "金額エラー");
            return;
        }
//
//        IFBoxManager.SendMeterDataInfo_FutabaD tmpSend820Info = new IFBoxManager.SendMeterDataInfo_FutabaD();
//        tmpSend820Info.StatusCode = IFBoxManager.SendMeterDataStatus_FutabaD.NONE;
//        tmpSend820Info.IsLoopBreakOut = false;
//        tmpSend820Info.ErrorCode820 = IFBoxManager.SendMeterDataStatus_FutabaD.NONE;
//
//        AmountInputAdvancePayFDViewModel._meterDataV4InfoDisposable = _amountInputAdvancePayFDViewModel.getIfBoxManager().getMeterDataV4().subscribeOn(
//                Schedulers.io()).observeOn(Schedulers.newThread()).subscribe(meter -> {     //AndroidSchedulers.mainThread()
//            Timber.i("[FUTABA-D]AmountInputSeparationPayFDFragment:750<-820 meter_data event cmd:%d ", meter.meter_sub_cmd);
//            if(meter.meter_sub_cmd == 9) {              //ファンクション通知を受信
//                //tmpSend820Info.StatusCode = IFBoxManager.SendMeterDataStatus_FutabaD.SENDOK;             //ACKが返ってきた場合
//                //_transAmountTicket = meter.trans_amount;             //通知されてきた金額
//                _transAmountTicket = _amountInputAdvancePayFDViewModel.getSeparationPayAmount().getValue();
//                tmpSend820Info.StatusCode = IFBoxManager.SendMeterDataStatus_FutabaD.SENDOK;
//            } else if(meter.meter_sub_cmd == 5 && meter.sound_no == 9) { // 紙切れの音声ガイダンス
//                tmpSend820Info.StatusCode = IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_PAPERLACKING;
//            } else if(meter.meter_sub_cmd == 5 && meter.sound_no == 0) {       //処理コード表示要求中，FREE ER**の場合
//                if ((meter.line_1 != null) && (meter.line_2 != null)) {
//                    int tmpProcessCode = _menuViewModel.getIFBoxManager().send820_IsProcessCode_ErrorCD(meter.line_1);          //処理コード表示要求エラーコードはLine１に乗ってくる　エラーコードを取得
//                    if (tmpProcessCode < 0) {           //エラーコードがある場合
//                        tmpSend820Info.StatusCode = IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_PROCESSCODE_ERRRETURN;
//                    }
//                }
//            }
//        });
//
//        AmountInputAdvancePayFDViewModel._meterDataV4ErrorDisposable = _amountInputAdvancePayFDViewModel.getIfBoxManager().getMeterDataV4Error().subscribeOn(
//                Schedulers.io()).observeOn(Schedulers.newThread()).subscribe(error -> {         //送信中にエラー受信(タイムアウト，切断)
//            Timber.e("[FUTABA-D]HistoryEventHandlersImpl:Error event ErrCD:%d 820ErrCD:%d ", error.ErrorCode, error.ErrorCode820);
//            tmpSend820Info.StatusCode = error.ErrorCode;
//            tmpSend820Info.ErrorCode820 = error.ErrorCode820;
//        });

        showProgressDialog("伝票印刷中 ・・・ ");

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                PrinterProc printerProc = PrinterProc.getInstance();            //プリンター処理クラスのインスタンス取得
                printerProc.printTransFutabaD(tmpSlipID);                       //820へ送信処理

                // チケットの履歴を削除
                _amountInputAdvancePayFDViewModel.deleteHistryTicket(transDate[0], termSequence[0]);

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
                    // fixAmountJumpTicket2ndJob(view, tmpSend820Info);        //820送信後の処理
                });
            }
        });
        thread.start();

    }

//    @RequiresApi(api = Build.VERSION_CODES.N)
//    private void fixAmountJumpTicket2ndJob(View view, IFBoxManager.SendMeterDataInfo_FutabaD tmpSend820Info)
//    {
//        String tmpErrorCode = "";
//        try {
//            if (AmountInputAdvancePayFDViewModel._meterDataV4InfoDisposable != null) {
//                AmountInputAdvancePayFDViewModel._meterDataV4InfoDisposable.dispose();
//                AmountInputAdvancePayFDViewModel._meterDataV4InfoDisposable = null;
//            }
//
//            if (AmountInputAdvancePayFDViewModel._meterDataV4ErrorDisposable != null) {
//                AmountInputAdvancePayFDViewModel._meterDataV4ErrorDisposable.dispose();
//                AmountInputAdvancePayFDViewModel._meterDataV4ErrorDisposable = null;
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
//                        break;
//                    case IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_TIMEOUT:            //タイムアウト
//                        tmpErrorCode = "6030";                       //IFBOX接続エラー
//                        break;
//                    case IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_SELECTMODE:         //選択モードエラー
//                        tmpErrorCode = "6030";                       //IFBOX接続エラー
//                        break;
//                    case IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_SENDNG:             //zandaka_flg送信エラー(1が返ってきていない)
//                        tmpErrorCode = "6030";                       //IFBOX接続エラー
//                        break;
//                    case IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_820NACK:              //820内でが返ってきた場合
//                        Timber.e("[FUTABA-D]820 Inner error! ErrCD:%d", tmpSend820Info.ErrorCode820);
//                        tmpErrorCode = "6030";                       //IFBOX接続エラー
//                        break;
//                    case IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_PAPERLACKING:       //紙切れエラー
//                        tmpErrorCode = _app.getString(R.string.error_type_FutabaD_Outofpaper_Norestart);
//                        break;
//                    case IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_PROCESSCODE_ERRRETURN:    //処理コード表示要求中，FREE ER**の場合
//                        tmpErrorCode = _app.getString(R.string.error_type_FutabaD_MeterErrorMes_Cancel_R);;
//                        break;
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
//                        if (tmpSend820Info.StatusCode == IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_PROCESSCODE_ERRRETURN) {
//                            _amountInputAdvancePayFDViewModel.getIfBoxManager().send820_GenericProcessCodeErrReturn();                        //異常処理コード画面戻り要求キーを送信 ACKを必要としないモードで
//                        }
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
//            _amountInputAdvancePayFDViewModel.SetFixAmountRecv820(_transAmountTicket);    //820受信後の処理
//        } catch (Exception e) {
//            Timber.e(e);
//            ShowErrorMessage2001(view.getContext(), "処理エラー");
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
////        NavigationWrapper.navigate(view,  R.id.action_navigation_separationpay_to_navigation_menu_separation_with_ticket);                    //分別のチケット＆ほげほげメニューへ
//        view.post(() -> {
//            NavigationWrapper.popBackStack(view);               //画面遷移
//        });
//    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void advancepay(View view) {
//        CommonClickEvent.RecordInputOperation(view, String.valueOf(_amountInputFutabaDViewModel.getChangeAmount().getValue()), true);
//        _amountInputFutabaDViewModel.setInputMode(AmountInputFutabaDViewModel.InputModes.ADVANCEPAY);
////        _amountInputFutabaDViewModel.increase();
        String mode = AmountInputAdvancePayFDViewModel.InputModes.ADVANCEPAY;
        if (checkInputPossible(mode)) {
            CommonClickEvent.RecordInputOperation(view, String.valueOf(_amountInputAdvancePayFDViewModel.getChangeAmount().getValue()), true);
            _amountInputAdvancePayFDViewModel.setConfAmountAreaLabel(_app.getString(R.string.list_amount_input_advancepay));
            _amountInputAdvancePayFDViewModel.setInputMode(mode);
        } else {
            ShowErrorMessageInputError(getContext(), mode, "金額操作不可");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    /******************************************************************************/
    /*!
     * @brief  定額払い処理　画面 ボタン押下
     * @note   定額払い時の画面系処理
     * @param [in] View view
     * @retval なし
     * @return　なし
     * @private
     */
    /******************************************************************************/
    private void flatRate(View view) {
        String mode = AmountInputAdvancePayFDViewModel.InputModes.FLAT_RATE;
        if (checkInputPossible(mode)) {
            CommonClickEvent.RecordInputOperation(view, String.valueOf(_amountInputAdvancePayFDViewModel.getChangeAmount().getValue()), true);
            _amountInputAdvancePayFDViewModel.setConfAmountAreaLabel(_app.getString(R.string.list_amount_input_flatrate));
            _amountInputAdvancePayFDViewModel.setInputMode(mode);
        } else {
            ShowErrorMessageInputError(getContext(), mode, "金額操作不可");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void separationTicket(View view) {
        String mode = AmountInputAdvancePayFDViewModel.InputModes.SEPARATION_TICKET;
        if (checkInputPossible(mode)) {
            CommonClickEvent.RecordInputOperation(view, String.valueOf(_amountInputAdvancePayFDViewModel.getChangeAmount().getValue()), true);
            _amountInputAdvancePayFDViewModel.setConfAmountAreaLabel(_app.getString(R.string.list_amount_input_ticket));
            _amountInputAdvancePayFDViewModel.setInputMode(mode);
        } else {
            ShowErrorMessageInputError(getContext(), mode, "金額操作不可");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void separationCash(View view) {
        String mode = AmountInputAdvancePayFDViewModel.InputModes.SEPARATION_CASH;
        if (checkInputPossible(mode)) {
            CommonClickEvent.RecordInputOperation(view, String.valueOf(_amountInputAdvancePayFDViewModel.getChangeAmount().getValue()), true);
            _amountInputAdvancePayFDViewModel.setConfAmountAreaLabel(_app.getString(R.string.list_amount_input_settlement));
            _amountInputAdvancePayFDViewModel.setInputMode(mode);
        } else {
            ShowErrorMessageInputError(getContext(), mode, "金額操作不可");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    /******************************************************************************/
    /*!
     * @brief  立替・定額払い取消 画面 ボタン押下
     * @note   立替・定額払い取消時の画面系処理
     * @param [in] View view
     * @retval なし
     * @return　なし
     * @private
     */
    /******************************************************************************/
    public void advanceFlatClear(View view) {
        String mode = AmountInputAdvancePayFDViewModel.InputModes.ADVANCEPAY; // 立替・定額の取消だが、modeはADVANCEPAYを指定
        if (checkCancelPossible(mode)) {
            CommonClickEvent.RecordInputOperation(view, String.valueOf(_amountInputAdvancePayFDViewModel.getChangeAmount().getValue()), true);
            _amountInputAdvancePayFDViewModel.flatClear();
            view.post(() -> {
                NavigationWrapper.popBackStack(view);               //画面遷移
            });
        } else {
            // 金額操作不可のエラー表示
            ShowErrorMessageCancelError(getContext(), mode, "金額操作不可");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void separationCancel(View view) {
        String mode = AmountInputAdvancePayFDViewModel.InputModes.SEPARATION_TICKET; // チケット金額・決済金額の取消だが、modeはSEPARATION_TICKETを指定
        if (checkCancelPossible(mode)) {
            if (Amount.getTicketAmount() > 0 || Amount.getCashAmount() > 0) {
                CommonClickEvent.RecordButtonClickOperation(view, true);
                Timber.i("分別金額を取消します．よろしいですか？");
                final String message =
                        "分別金額を取消します\nよろしいですか？";
                ConfirmDialog.newInstance("【分別金額取消確認】", message, () -> {
                    CommonClickEvent.RecordClickOperation("はい", "取消確認", false);
                    //disposables.clear();

                    if (Amount.getCashAmount() > 0) {
                        Amount.setCashAmount(0);            // 現金分割分を初期化
                    }

                    if (Amount.getTicketAmount() > 0) {
                        boolean fl = separationCancelSendCancel(view);    //分割払いキャンセル送信
                        if (fl == true) {
                            Amount.setTicketAmount(0);              // チケット金額を初期化
                        }
                    }

                    Amount.setTotalChangeAmount(0);         // 変更金額を初期化

                    _menuViewModel.onResumeExt();      // Amount系変数を整える

                    //NavigationWrapper.navigate(view,  R.id.action_navigation_menu_separation_to_navigation_home);                    //home画面へ

                    view.post(() -> {
                        NavigationWrapper.popBackStack(view);
                        //NavigationWrapper.popBackStack(view);
                        //NavigationWrapper.navigate(activity, R.id.fragment_menu_separationpay, R.id.action_navigation_menu_separation_to_navigation_home);
                    });
                }, () -> {
                    CommonClickEvent.RecordClickOperation("いいえ", "取消確認", false);
                }).show(getChildFragmentManager(), null);
            } else {
                Timber.i("分別金額の入力なし");
            }
        } else {
            ShowErrorMessageCancelError(getContext(), mode, "金額操作不可");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public boolean separationCancelSendCancel(View view)
    {
        boolean result = false;

        if ( (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) != true)) {
            return result;
        }

        PrinterManager printerManager = PrinterManager.getInstance();
        printerManager.setView(view);

//        if (_menuViewModel.getIFBoxManager() == null) {
//            printerManager.PrinterDuplexError(PrinterConst.DuplexPrintStatus_IFBOXERROR);       //IFBOX接続エラー
//            return result;
//        }

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
        _amountInputAdvancePayFDViewModel.cancel();
    }

    @Override
    public void onReset(View view) {
        CommonClickEvent.RecordButtonClickOperation(view, true);
        _amountInputAdvancePayFDViewModel.reset();
    }

    @Override
    public void onChangeBack(View view) {
        CommonClickEvent.RecordButtonClickOperation(view, false);
        _amountInputAdvancePayFDViewModel.changeBack();
    }

    @Override
    public void onApply(View view) {
        CommonClickEvent.RecordButtonClickOperation(view, false);
        _amountInputAdvancePayFDViewModel.apply();
    }

    /* 立替定額ボタン押下 */
    @Override
    public void onAdvancepayFlatRate(View view) {
        requireActivity().runOnUiThread(() -> {
            String[] list = {
                _app.getString(R.string.list_amount_input_flatrate),
                _app.getString(R.string.list_amount_input_advancepay)
            };
            _radioDialogKind = RADIODLG_ADVANCEPAY_FLATRATE;
            _radioDialogIndex = 0;
            _targetView = view;
            // 立替・定額選択ダイアログ表示
            RadioDialogFragment dialog = RadioDialogFragment.newInstance("定額・立替選択", list, true);
            dialog.show(getChildFragmentManager(), "");
        });
    }

    /* 分別ボタン押下 */
    @Override
    public void onSeparation(View view) {
        requireActivity().runOnUiThread(() -> {
            String[] list = {
                _app.getString(R.string.list_amount_input_ticket),
                _app.getString(R.string.list_amount_input_settlement)
            };
            _radioDialogKind = RADIODLG_SEPARATION;
            _radioDialogIndex = 0;
            _targetView = view;
            // 分別種別選択ダイアログ表示
            RadioDialogFragment dialog = RadioDialogFragment.newInstance("分別種別選択", list, true);
            dialog.show(getChildFragmentManager(), "");
        });
    }

    /* 取消ボタン押下 */
    @Override
    public void onClear(View view) {
        requireActivity().runOnUiThread(() -> {
            String[] list = {
                _app.getString(R.string.list_amount_input_cancel_advancepay_flatRate),
                _app.getString(R.string.list_amount_input_cancel_separation)
            };
            _radioDialogKind = RADIODLG_CANCEL;
            _radioDialogIndex = 0;
            _targetView = view;
            // 取消種別選択ダイアログ表示
            RadioDialogFragment dialog = RadioDialogFragment.newInstance("取消種別選択", list, true);
            dialog.show(getChildFragmentManager(), "");
        });
    }

    /* 選択ダイアログでOK押下 */
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        switch(_radioDialogKind) {
            // 立替・定額選択ダイアログ
            case RADIODLG_ADVANCEPAY_FLATRATE:
                if(_radioDialogIndex == 0) {
                    // 定額を選択
                    flatRate(_targetView);
                } else {
                    // 立替を選択
                    advancepay(_targetView);
                }
                _amountInputAdvancePayFDViewModel.reset();
                break;
            // 分別種別選択ダイアログ
            case RADIODLG_SEPARATION:
                if(_radioDialogIndex == 0) {
                    // チケット金額を選択
                    separationTicket(_targetView);
                }
                else {
                    // 決済金額を選択
                    separationCash(_targetView);
                }
                _amountInputAdvancePayFDViewModel.reset();
                break;
            // 取消種別選択ダイアログ
            case RADIODLG_CANCEL:
                if(_radioDialogIndex == 0) {
                    // 立替・定額取消を選択
                    advanceFlatClear(_targetView);
                }
                else {
                    // 分別金額取消を選択
                    separationCancel(_targetView);
                }
                break;
            default:
                break;
        }
        dialog.dismiss();
    }

    /* 選択ダイアログでradioボタン選択 */
    @Override
    public void onRadioButtonClick(DialogFragment dialog, int which)
    {
        _radioDialogIndex = which;
    }

    private boolean checkInputPossible(String mode) {
        boolean inputPossible = true;

        if (AppPreference.isMeterStatusSiharai() && Amount.getPaymented() == 0) {
            if (Amount.getTicketAmount() > 0 || Amount.getCashAmount() > 0) {
                // 分別金額（チケットor決済金額）を入力済
                Timber.i("[FUTABA-D]分別金額入力済");
                inputPossible = false;
            } else {
                inputPossible = true;
            }
        } else {
            if (AmountInputAdvancePayFDViewModel.InputModes.ADVANCEPAY.equals(mode) || AmountInputAdvancePayFDViewModel.InputModes.FLAT_RATE.equals(mode)) {
                if (Amount.getPaymented() > 0) {
                    Timber.i("[FUTABA-D]決済済");
                    inputPossible = false;
                }
            } else {
                // 各種金額の入力不可
                Timber.i("[FUTABA-D]金額入力操作無効");
                inputPossible = false;
            }
        }

        return inputPossible;
    }

    private boolean checkCancelPossible(String mode) {
        boolean inputPossible = true;

        if (AmountInputAdvancePayFDViewModel.InputModes.ADVANCEPAY.equals(mode) && (Amount.getTicketAmount() > 0 || Amount.getCashAmount() > 0)) {
            // 分別金額（チケットor決済金額）を入力済状態での立替・定額取消は不可
            Timber.i("[FUTABA-D]分別金額入力済");
            inputPossible = false;
        } else if (AppPreference.isMeterStatusSiharai() && Amount.getPaymented() == 0) {
            inputPossible = true;
        } else {
            if (AmountInputAdvancePayFDViewModel.InputModes.ADVANCEPAY.equals(mode) || AmountInputAdvancePayFDViewModel.InputModes.FLAT_RATE.equals(mode)) {
                if (Amount.getPaymented() > 0) {
                    Timber.i("[FUTABA-D]決済済");
                    inputPossible = false;
                }
            } else {
                // 各種金額の取消不可
                Timber.i("[FUTABA-D]金額取消操作無効");
                inputPossible = false;
            }
        }

        return inputPossible;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void ShowErrorMessage2001(Context context, String msg) {
        CommonErrorDialog dialog = new CommonErrorDialog();
        dialog.setCommonErrorEventHandlers(new CommonErrorEventHandlers() {
            @Override
            public void onPositiveClick(String errorCode) {
                CommonClickEvent.RecordClickOperation("はい", msg, true);
            }

            @Override
            public void onNegativeClick(String errorCode) {}

            @Override
            public void onNeutralClick(String errorCode) {}

            @Override
            public void onDismissClick(String errorCode) {}
        });
        dialog.ShowErrorMessage(context, _app.getInstance().getString(R.string.error_type_payment_system_2001));
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void ShowErrorMessageInputError(Context context, String mode, String msg) {
        CommonErrorDialog dialog = new CommonErrorDialog();
        dialog.setCommonErrorEventHandlers(new CommonErrorEventHandlers() {
            @Override
            public void onPositiveClick(String errorCode) {
                CommonClickEvent.RecordClickOperation("はい", msg, true);
                return;
            }

            @Override
            public void onNegativeClick(String errorCode) {}

            @Override
            public void onNeutralClick(String errorCode) {}

            @Override
            public void onDismissClick(String errorCode) {}
        });

        if (Amount.getTicketAmount() > 0 || Amount.getCashAmount() > 0) {
            dialog.ShowErrorMessage(context, _app.getInstance().getString(R.string.error_type_FutabaD_Amount_OperationError4));
        } else if (Amount.getPaymented() != 0) {
            dialog.ShowErrorMessage(context, _app.getInstance().getString(R.string.error_type_FutabaD_Amount_OperationError3));
        } else if (AmountInputAdvancePayFDViewModel.InputModes.ADVANCEPAY.equals(mode)) {
            dialog.ShowErrorMessage(context, _app.getInstance().getString(R.string.error_type_FutabaD_Amount_OperationError2));
        } else {
            dialog.ShowErrorMessage(context, _app.getInstance().getString(R.string.error_type_FutabaD_Amount_OperationError1));
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void ShowErrorMessageCancelError(Context context, String mode, String msg) {
        CommonErrorDialog dialog = new CommonErrorDialog();
        dialog.setCommonErrorEventHandlers(new CommonErrorEventHandlers() {
            @Override
            public void onPositiveClick(String errorCode) {
                CommonClickEvent.RecordClickOperation("はい", msg, true);
                return;
            }

            @Override
            public void onNegativeClick(String errorCode) {}

            @Override
            public void onNeutralClick(String errorCode) {}

            @Override
            public void onDismissClick(String errorCode) {}
        });

        if (Amount.getPaymented() != 0) {
            dialog.ShowErrorMessage(context, _app.getInstance().getString(R.string.error_type_FutabaD_Amount_OperationError3));
        } else if (AmountInputAdvancePayFDViewModel.InputModes.ADVANCEPAY.equals(mode) && (Amount.getTicketAmount() > 0 || Amount.getCashAmount() > 0)) {
            // 分別金額（チケットor決済金額）を入力済状態での立替・定額取消は不可
            dialog.ShowErrorMessage(context, _app.getInstance().getString(R.string.error_type_FutabaD_Amount_OperationError4));
        } else if (AmountInputAdvancePayFDViewModel.InputModes.ADVANCEPAY.equals(mode)) {
            dialog.ShowErrorMessage(context, _app.getInstance().getString(R.string.error_type_FutabaD_Amount_OperationError2));
        } else {
            dialog.ShowErrorMessage(context, _app.getInstance().getString(R.string.error_type_FutabaD_Amount_OperationError1));
        }
    }

    @SuppressWarnings("deprecation")
    private void showProgressDialog(String message){
        _progressDialog = new ProgressDialog(getContext());
        _progressDialog.setMessage(message);                               // 内容(メッセージ)設定
        _progressDialog.setCancelable(false);                              // キャンセル無効
        _progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);    // スタイル設定
        _progressDialog.show();                                            // ダイアログを表示
    }
}
//ADD-E BMT S.Oyama 2024/08/27 フタバ双方向向け改修
