package jp.mcapps.android.multi_payment_terminal.ui.auto_daily_report;
//ADD-S BMT S.Oyama 2024/11/21 フタバ双方向向け改修

import static jp.mcapps.android.multi_payment_terminal.AppPreference.isDemoMode;
import static jp.mcapps.android.multi_payment_terminal.thread.printer.PrinterConst.DuplexPrintResponseTimerSec;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
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

import java.util.Timer;
import java.util.TimerTask;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jp.mcapps.android.multi_payment_terminal.BaseFragment;
import jp.mcapps.android.multi_payment_terminal.CommonClickEvent;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.NavigationWrapper;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.ScreenData;
import jp.mcapps.android.multi_payment_terminal.SharedViewModel;
import jp.mcapps.android.multi_payment_terminal.data.IFBoxAppModels;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentAmountInputAdvancepayFdBinding;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentAutoDailyReportFuelBinding;
import jp.mcapps.android.multi_payment_terminal.model.IFBoxManager;
import jp.mcapps.android.multi_payment_terminal.thread.printer.PrinterConst;
import jp.mcapps.android.multi_payment_terminal.thread.printer.PrinterManager;
import jp.mcapps.android.multi_payment_terminal.ui.error.CommonErrorDialog;
import jp.mcapps.android.multi_payment_terminal.ui.error.CommonErrorEventHandlers;
import jp.mcapps.android.multi_payment_terminal.ui.menu.MenuAutoDailyReportFragment;
import timber.log.Timber;

public class AutoDailyReportFuelFragment extends BaseFragment implements AutoDailyReportFuelEventHandlers {
    private final String SCREEN_NAME = "燃料入力";

    private enum SEND820_AUTODAILYREPORT_FUELMODE {
        FuelIn ,
        FuelOut,
        ValueRegist,
        ValueRegistClear,
    }


    public static AutoDailyReportFuelFragment newInstance() {
        return new AutoDailyReportFuelFragment();
    }
    private AutoDailyReportFuelViewModel _autoDailyReportFuelViewModel;
    private SharedViewModel _sharedViewModel;
    private boolean _isNotKuusyaStatusFound = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        _sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        final FragmentAutoDailyReportFuelBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_auto_daily_report_fuel, container, false);

        ViewModelProvider viewModelProvider = new ViewModelProvider(this, MainApplication.getViewModelFactory());
        _autoDailyReportFuelViewModel = viewModelProvider.get(AutoDailyReportFuelViewModel.class);

        binding.setViewModel(_autoDailyReportFuelViewModel);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        binding.setHandlers(this);

        ScreenData.getInstance().setScreenName(SCREEN_NAME);


        _sharedViewModel.setBackAction(() -> {
            backFragment();
        });

        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == true)
        {
            if (IFBoxManager.meterDataV4Disposable_DailyReportJob == null) {
                IFBoxManager.meterDataV4Disposable_DailyReportJob = _autoDailyReportFuelViewModel.getIfBoxManager().getMeterInfo()
                        .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(meter -> {
                    KuusyaCheckFromTariff();
                });
            }

        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        int tmpMenuHierarchy = _sharedViewModel.getAutoDailyReportMenuHierarchy().getValue();           //メニュー階層取得

        if (tmpMenuHierarchy == SharedViewModel.AutoDailyReport_MenuHierarchy.LEVEL1) {                 //上位から本メニューへ遷移時
            send820AutoDailyReportFuel(SEND820_AUTODAILYREPORT_FUELMODE.FuelIn, 0);         //820送信：燃料入力in
        }
        _sharedViewModel.setAutoDailyReportMenuHierarchy(SharedViewModel.AutoDailyReport_MenuHierarchy.LEVEL2);  //メニュー階層設定(自動日報子画面指定)

        /*
         * 820から受信処理
         * */
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        //レシーバーの削除

        if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == true)
        {
            if (IFBoxManager.meterDataV4Disposable_DailyReportJob != null) {
                IFBoxManager.meterDataV4Disposable_DailyReportJob.dispose();
                IFBoxManager.meterDataV4Disposable_DailyReportJob = null;
            }

            //if (IFBoxManager.meterDataV4InfoDisposable_DailyReportFuel != null)
            //{
            //    IFBoxManager.meterDataV4InfoDisposable_DailyReportFuel.dispose();
            //    IFBoxManager.meterDataV4InfoDisposable_DailyReportFuel = null;
            //}
        }
    }


    /******************************************************************************/
    /*!
     * @brief  数値 ボタン押下
     * @note   数値 ボタン押下
     * @param [in] View view
     * @param [in] String number
     * @retval なし
     * @return　なし
     * @private
     */
    /******************************************************************************/
    @Override
    public void onInputNumber(View view, String number) {
        CommonClickEvent.RecordButtonClickOperation(view, false);

        if (_autoDailyReportFuelViewModel.getIsRegistJob().getValue() == true)
        {
            return;
        }

        if (_autoDailyReportFuelViewModel.isNumericalValidity(number) == false) {
            CommonErrorDialog dialog = new CommonErrorDialog();
            dialog.setCommonErrorEventHandlers(new CommonErrorEventHandlers() {
                @Override
                public void onPositiveClick(String errorCode) {
                    CommonClickEvent.RecordClickOperation("はい", "入力内容エラー", true);
                }
                @Override
                public void onNegativeClick(String errorCode) {}
                @Override
                public void onNeutralClick(String errorCode) {}
                @Override
                public void onDismissClick(String errorCode) {}
            });
            dialog.ShowErrorMessage(getContext(), "9104");
            return;
        }

        if (_autoDailyReportFuelViewModel.isNumericalLength(number) == false) {
            CommonErrorDialog dialog = new CommonErrorDialog();
            dialog.setCommonErrorEventHandlers(new CommonErrorEventHandlers() {
                @Override
                public void onPositiveClick(String errorCode) {
                    CommonClickEvent.RecordClickOperation("はい", "入力長エラー", true);
                }
                @Override
                public void onNegativeClick(String errorCode) {}
                @Override
                public void onNeutralClick(String errorCode) {}
                @Override
                public void onDismissClick(String errorCode) {}
            });
            dialog.ShowErrorMessage(getContext(), "9105");
            return;
        }

        _autoDailyReportFuelViewModel.inputNumber(number);
    }

    /******************************************************************************/
    /*!
     * @brief  取り消し ボタン押下
     * @note   取り消し ボタン押下
     * @param [in] View view
     * @param [in] String number
     * @retval なし
     * @return　なし
     * @private
     */
    /******************************************************************************/
    @Override
    public void onBackDelete(View view) {
        CommonClickEvent.RecordButtonClickOperation(view, false);
        if (_autoDailyReportFuelViewModel.getIsRegistJob().getValue() == true)
        {
            return;
        }

        _autoDailyReportFuelViewModel.deleteNumberStrRight();
    }

    /******************************************************************************/
    /*!
     * @brief クリアボタン押下
     * @note
     * @param [in] View view
     * @retval なし
     * @return　なし
     * @private
     */
    /******************************************************************************/
    @Override
    public void onClear(View view) {
        CommonClickEvent.RecordButtonClickOperation(view, true);
        if (_autoDailyReportFuelViewModel.getIsRegistJob().getValue() == true)
        {
            return;
        }

        _autoDailyReportFuelViewModel.clearInputValueTxt();
    }


    /******************************************************************************/
    /*!
     * @brief  登録 ボタン押下
     * @note   登録 処理
     * @param [in] View view
     * @retval なし
     * @return　なし
     * @private
     */
    /******************************************************************************/
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onRegist(View view) {
        CommonClickEvent.RecordButtonClickOperation(view, false);

        if (_autoDailyReportFuelViewModel.isNumericalValidity() == false) {
            CommonErrorDialog dialog = new CommonErrorDialog();
            dialog.setCommonErrorEventHandlers(new CommonErrorEventHandlers() {
                @Override
                public void onPositiveClick(String errorCode) {
                    CommonClickEvent.RecordClickOperation("はい", "入力内容エラー", true);
                }
                @Override
                public void onNegativeClick(String errorCode) {}
                @Override
                public void onNeutralClick(String errorCode) {}
                @Override
                public void onDismissClick(String errorCode) {}
            });
            dialog.ShowErrorMessage(getContext(), "9104");
            return;
        }

        if (_autoDailyReportFuelViewModel.isNumericalLength() == false) {
            CommonErrorDialog dialog = new CommonErrorDialog();
            dialog.setCommonErrorEventHandlers(new CommonErrorEventHandlers() {
                @Override
                public void onPositiveClick(String errorCode) {
                    CommonClickEvent.RecordClickOperation("はい", "入力長エラー", true);
                }
                @Override
                public void onNegativeClick(String errorCode) {}
                @Override
                public void onNeutralClick(String errorCode) {}
                @Override
                public void onDismissClick(String errorCode) {}
            });
            dialog.ShowErrorMessage(getContext(), "9105");
            return;
        }

        if (_autoDailyReportFuelViewModel.isMaxOver() == true)
        {
            CommonErrorDialog dialog = new CommonErrorDialog();
            dialog.setCommonErrorEventHandlers(new CommonErrorEventHandlers() {
                @Override
                public void onPositiveClick(String errorCode) {
                    CommonClickEvent.RecordClickOperation("はい", "入力値最大値超エラー", true);
                }
                @Override
                public void onNegativeClick(String errorCode) {}
                @Override
                public void onNeutralClick(String errorCode) {}
                @Override
                public void onDismissClick(String errorCode) {}
            });
            dialog.ShowErrorMessage(getContext(), "9106");
            return;
        }
        if (_autoDailyReportFuelViewModel.getIsRegistJob().getValue() == true)
        {
            return;
        }
        _autoDailyReportFuelViewModel.setIsRegistJob(true);

        _autoDailyReportFuelViewModel.convertDecimalFromInputValueTxt();

        view.post(() -> {
            int tmpValue = _autoDailyReportFuelViewModel.getInputValueInt();

            send820AutoDailyReportFuel(SEND820_AUTODAILYREPORT_FUELMODE.ValueRegist, tmpValue);         //820送信：燃料値送信

            NavigationWrapper.popBackStack(view);           //メニュー画面へ戻る
            _sharedViewModel.setBackAction(null);
            _sharedViewModel.setAutoDailyReportMenuHierarchy(SharedViewModel.AutoDailyReport_MenuHierarchy.LEVEL1);  //メニュー階層設定(自動日報メニュー指定)

            _autoDailyReportFuelViewModel.setIsRegistJob(false);
        });

        //メータ側が自動で自動日報トップメニューへ戻るため，SEND820_AUTODAILYREPORT_FUELMODE.FuelOutを利用したバック処理は実施しないこと
    }

    /******************************************************************************/
    /*!
     * @brief  画面戻る ボタン押下
     * @note   画面戻る ボタン 処理
     * @param [in] View view
     * @retval なし
     * @return　なし
     * @private
     */
    /******************************************************************************/
    private void backFragment()
    {
        if (_autoDailyReportFuelViewModel.getIsRegistJob().getValue() == true)
        {
            return;
        }

        send820AutoDailyReportFuel(SEND820_AUTODAILYREPORT_FUELMODE.FuelOut, 0);         //820送信：燃料入力out

        NavigationWrapper.popBackStack(this);
        _sharedViewModel.setBackAction(null);
        _sharedViewModel.setAutoDailyReportMenuHierarchy(SharedViewModel.AutoDailyReport_MenuHierarchy.LEVEL1);  //メニュー階層設定(自動日報メニュー指定)

    }

    /******************************************************************************/
    /*!
     * @brief  タリフの変化に合わせてボタンのEnabledを変更する
     * @note   タリフの変化に合わせてボタンのEnabledを変更する
     * @param [in]
     * @retval なし
     * @return　なし
     * @private
     */
    /******************************************************************************/
    private void KuusyaCheckFromTariff()
    {
        if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == false) {
            return;
        }

        String tmpTariffStatus = _autoDailyReportFuelViewModel.getIfBoxManager().getMeterStatus();         //現在のステータス

        if (tmpTariffStatus.equals("KUUSYA") == true)           //空車時
        {
        }
        else                                                    //空車以外は抜ける処理を実施
        {
            if (_isNotKuusyaStatusFound == false) {
                NavigationWrapper.popBackStack(this);
                _sharedViewModel.setBackAction(null);
                _sharedViewModel.setAutoDailyReportMenuHierarchy(SharedViewModel.AutoDailyReport_MenuHierarchy.LEVEL1);  //メニュー階層設定(自動日報メニュー指定)
                _isNotKuusyaStatusFound = true;
            }
        }
    }

    /******************************************************************************/
    /*!
     * @brief  820通信部　主処理
     * @note   820通信部　主処理
     * @param [in] View view
     * @retval なし
     * @return　なし
     * @private
     */
    /******************************************************************************/
    private boolean send820AutoDailyReportFuel(SEND820_AUTODAILYREPORT_FUELMODE mode, int tmpInputValue)
    {

        if ( (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == false)) {              //フタバD以外は処理しない
            return false;
        }

        if (_autoDailyReportFuelViewModel.getIfBoxManager().getIsConnected820() == false)             //820未接続の場合
        {
            CommonErrorDialog dialog = new CommonErrorDialog();
            dialog.setCommonErrorEventHandlers(new CommonErrorEventHandlers() {
                @Override
                public void onPositiveClick(String errorCode) {
                    CommonClickEvent.RecordClickOperation("はい", "820未接続エラー(1)", true);
                }
                @Override
                public void onNegativeClick(String errorCode) {}
                @Override
                public void onNeutralClick(String errorCode) {}
                @Override
                public void onDismissClick(String errorCode) {}
            });
            dialog.ShowErrorMessage(getContext(), "6030");
            return false;
        }

        IFBoxManager.SendMeterDataInfo_FutabaD tmpSend820Info = new IFBoxManager.SendMeterDataInfo_FutabaD();
        tmpSend820Info.StatusCode = IFBoxManager.SendMeterDataStatus_FutabaD.NONE;
        tmpSend820Info.IsLoopBreakOut = false;
        tmpSend820Info.ErrorCode820 = IFBoxManager.SendMeterDataStatus_FutabaD.NONE;

        if ((mode == SEND820_AUTODAILYREPORT_FUELMODE.ValueRegist) || (mode == SEND820_AUTODAILYREPORT_FUELMODE.ValueRegistClear) ||(mode == SEND820_AUTODAILYREPORT_FUELMODE.FuelIn)

            ) {                  //燃料入力 数値登録時
            IFBoxManager.meterDataV4InfoDisposable_DailyReportFuel = _autoDailyReportFuelViewModel.getIfBoxManager().getMeterDataV4().subscribeOn(
                    Schedulers.io()).observeOn(Schedulers.newThread()).subscribe(meter -> {     //AndroidSchedulers.mainThread()
                Timber.i("[FUTABA-D]AutoDailyReportFuelFragment:750<-820 meter_data event cmd:%d ", meter.meter_sub_cmd);
                Timber.i("[FUTABA-D]AutoDailyReportFuelFragment:FREE Mes  %s, %s, %s ", meter.line_41, meter.line_42, meter.line_43);
                if (meter.meter_sub_cmd == 13) {              //自動日報イベント
                    tmpSend820Info.StatusCode = IFBoxManager.SendMeterDataStatus_FutabaD.SENDOK;             //ACKが返ってきた場合
                }
                else if (meter.meter_sub_cmd == 5)
                {
                    int result = meter.line_42.indexOf("燃料回数オーバー");             //燃料回数オーバー時

                    if (result != -1) {
                        tmpSend820Info.StatusCode = IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_PROCESSCODE_ERROR;
                        tmpSend820Info.FREEMessage1 = meter.line_41;
                        tmpSend820Info.FREEMessage2 = meter.line_42;
                        tmpSend820Info.FREEMessage3 = meter.line_43;
                    }
                }
            });

            _autoDailyReportFuelViewModel.meterDataV4ErrorDisposable = _autoDailyReportFuelViewModel.getIfBoxManager().getMeterDataV4Error().subscribeOn(
                    Schedulers.io()).observeOn(Schedulers.newThread()).subscribe(error -> {         //送信中にエラー受信(タイムアウト，切断)
                Timber.e("[FUTABA-D]AutoDailyReportFuelFragment:Error event ErrCD:%d 820ErrCD:%d ", error.ErrorCode, error.ErrorCode820);
                tmpSend820Info.StatusCode = error.ErrorCode;
                tmpSend820Info.ErrorCode820 = error.ErrorCode820;

            });
        }


        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                switch(mode)
                {
                    case ValueRegist:                   //値登録
                        _autoDailyReportFuelViewModel.getIfBoxManager().send820_AutoDailyRepotFuelInput(tmpInputValue);    //820へ燃料量情報を送信
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

                        break;
                    case ValueRegistClear:              //値登録クリア
                        _autoDailyReportFuelViewModel.getIfBoxManager().send820_AutoDailyRepotFuelClear() ;             //820へ燃料量情報クリアを送信
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
                        break;
                    case FuelIn:                        //燃料入力画面in
                        _autoDailyReportFuelViewModel.getIfBoxManager().send820_AutoDailyRepotFuelIn();    //820へ燃料入力in情報を送信
                        for(int i = 0; i < 10; i++)        //最大2秒ほど待ってみる
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
                        if (tmpSend820Info.IsLoopBreakOut == false)
                        {
                            tmpSend820Info.IsLoopBreakOut = true;
                            tmpSend820Info.StatusCode = IFBoxManager.SendMeterDataStatus_FutabaD.SENDOK;             //ACKが返ってきた場合
                        }
                        break;

                    case FuelOut:                       //燃料入力画面out
                        _autoDailyReportFuelViewModel.getIfBoxManager().send820_AutoDailyRepotFuelOut();   //820へ燃料入力out情報を送信
                        for(int i = 0; i < 5; i++)        //最大0.5秒ほど待ってみる
                        {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                            }
                        }
                        tmpSend820Info.IsLoopBreakOut = true;
                        tmpSend820Info.StatusCode = IFBoxManager.SendMeterDataStatus_FutabaD.SENDOK;             //ACKが返ってきた場合
                        break;

                    default:
                        break;
                }

            }
        });
        thread.start();

        String tmpErrorCode = "";               //通信等でエラー発生時は""以外のエラーコードをセットする
        try {
            thread.join();

            if (IFBoxManager.meterDataV4InfoDisposable_DailyReportFuel != null) {       //コールバック系を後始末
                IFBoxManager.meterDataV4InfoDisposable_DailyReportFuel.dispose();
                IFBoxManager.meterDataV4InfoDisposable_DailyReportFuel = null;
            }

            if (_autoDailyReportFuelViewModel.meterDataV4ErrorDisposable != null)        //コールバック系を後始末
            {
                _autoDailyReportFuelViewModel.meterDataV4ErrorDisposable.dispose();
                _autoDailyReportFuelViewModel.meterDataV4ErrorDisposable = null;
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
                        Timber.e("[FUTABA-D](demo)820 Inner error! ErrCD:%d", tmpSend820Info.ErrorCode820);
                        tmpErrorCode = "6030";                       //IFBOX接続エラー
                    case IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_PROCESSCODE_ERROR:
                        tmpErrorCode = "20001";                       //FREEメッセージ表示モード
                        break;
                    default:
                        //ここに到達する場合は，エラー無しで決済選択モードが送信されたことを意味する
                        break;
                }
            }


        } catch (Exception e) {
            Timber.e(e);
            tmpErrorCode =  "8097";
        }

        if (tmpErrorCode.equals("") != true){           //エラーコードが設定されている場合

            if (tmpErrorCode.equals("20001") == true)           //フリーメッセージのエラー
            {

                String tmpFreeMsg = tmpSend820Info.FREEMessage1 + "\n" + tmpSend820Info.FREEMessage2 + "\n「はい」を押してエラーを解除してください";
                String setTitle = "メータエラー"; // タイトルに残金＋金額

                AlertDialog.Builder alertDialog = new AlertDialog.Builder(getContext());
                alertDialog.setTitle(setTitle);        // タイトル設定
                alertDialog.setMessage(tmpFreeMsg);  // 内容(メッセージ)設定
                alertDialog.setCancelable(true);       // キャンセル有効
                // はいボタンの設定
                alertDialog.setPositiveButton("はい", new DialogInterface.OnClickListener() {
                    @RequiresApi(api = Build.VERSION_CODES.N)
                    public void onClick(DialogInterface dialog, int which) {
                        _autoDailyReportFuelViewModel.getIfBoxManager().send820_TeiseiKeyNonAck( );           //訂正キー ACKなしで送付

                        TimerTask timerTask = new TimerTask() {
                            @Override
                            public void run() {
                                getView().post(() -> {
                                    backFragment();
                                });
                            }
                        };

                        Timer timer = new Timer();
                        timer.schedule(timerTask, 300);
                    }
                });
                // ダイアログを表示
                alertDialog.show();
            }
            else {
                CommonErrorDialog dialog = new CommonErrorDialog();
                dialog.setCommonErrorEventHandlers(new CommonErrorEventHandlers() {
                    @Override
                    public void onPositiveClick(String errorCode) {
                        CommonClickEvent.RecordClickOperation("はい", "820通信エラー(2)", true);
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
                dialog.ShowErrorMessage(getContext(), tmpErrorCode);
            }
            return false;
        }

        return true;
    }


}

//ADD-E BMT S.Oyama 2024/11/21 フタバ双方向向け改修

//ProgressDialog progressDialog;
//Thread thread;

//Integer amount = _amountInputAdvancePayFDViewModel.getFlatRateAmount().getValue() > 0
//        ? _amountInputAdvancePayFDViewModel.getFlatRateAmount().getValue()
//        : _amountInputAdvancePayFDViewModel.getMeterCharge().getValue();
//amount += _amountInputAdvancePayFDViewModel.getTotalChangeAmount().getValue();
//CommonClickEvent.RecordInputOperation(view, String.valueOf(amount), true);

//        progressDialog = new ProgressDialog(this.getContext());
//        progressDialog.setTitle("通信中");
//        progressDialog.setMessage("金額情報を通信中");
//        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
//        progressDialog.show();

//        new Thread(new Runnable(){
//            @Override
//            public void run(){
//                // todo
//                try {
//                    thread.sleep(2000);
//                } catch (InterruptedException e)
//                {
//
//                }
//
//                progressDialog.dismiss();
//                progressDialog = null;
//
//                view.post(() -> {
//                    NavigationWrapper.popBackStack(view);
//                });
//            }
//        }).start();
