package jp.mcapps.android.multi_payment_terminal.ui.menu;
//ADD-S BMT S.Oyama 2024/11/21 フタバ双方向向け改修

import static jp.mcapps.android.multi_payment_terminal.thread.printer.PrinterConst.DuplexPrintResponseTimerSec;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

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
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentMenuAutoDailyReportBinding;
import jp.mcapps.android.multi_payment_terminal.model.IFBoxManager;
import jp.mcapps.android.multi_payment_terminal.ui.error.CommonErrorDialog;
import jp.mcapps.android.multi_payment_terminal.ui.error.CommonErrorEventHandlers;
import timber.log.Timber;

public class MenuAutoDailyReportFragment extends BaseFragment {

    public enum SEND820_AUTODAILYREPORT_MODE {
        MenuIn,
        MenuOut
    }

    public static MenuAutoDailyReportFragment newInstance() {
        return new MenuAutoDailyReportFragment();
    }

    private SharedViewModel _sharedViewModel;
    private MenuViewModel _menuViewModel;

    private final String SCREEN_NAME = "自動日報メニュー";
    private final String BUTTON_BACKCOLOR_ENABLED = "#004D63";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        _sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        final Fragment menuFragment = getParentFragment();
        _menuViewModel = new ViewModelProvider(menuFragment, MainApplication.getViewModelFactory()).get(MenuViewModel.class);

        FragmentActivity activity = getActivity();
        final SharedViewModel sharedViewModel = new ViewModelProvider(activity).get(SharedViewModel.class);

        _menuViewModel.setBodyType(MenuTypes.BUSINESS);

        final FragmentMenuAutoDailyReportBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_menu_auto_daily_report, container, false);

        binding.setViewModel(_menuViewModel);
        binding.setSharedViewModel(sharedViewModel);
        binding.setHandlers(new MenuEventHandlersImpl(this, _menuViewModel));

        ScreenData.getInstance().setScreenName(SCREEN_NAME);

        _sharedViewModel.setBackAction(() -> {
            backFragment();
        });


        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == true) {
            if (IFBoxManager.meterDataV4Disposable_DailyReportTop == null) {
                IFBoxManager.meterDataV4Disposable_DailyReportTop = _menuViewModel.getIFBoxManager().getMeterInfo()
                        .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(meter -> {
                            ButtonEnabledFromTariff();
                        });
            }
        }

        ButtonEnabledFromTariff();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        int tmpMenuHierarchy = _sharedViewModel.getAutoDailyReportMenuHierarchy().getValue();           //メニュー階層取得

        //if (tmpMenuHierarchy == SharedViewModel.AutoDailyReport_MenuHierarchy.LEVEL0) {                 //上位から本メニューへ遷移時
        //send820AutoDailyReport(SEND820_AUTODAILYREPORT_MODE.MenuIn);
        //}
        _sharedViewModel.setAutoDailyReportMenuHierarchy(SharedViewModel.AutoDailyReport_MenuHierarchy.LEVEL1);  //メニュー階層設定(自動日報メニュー指定)
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        //レシーバーの削除

        if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == true) {
            if (IFBoxManager.meterDataV4Disposable_DailyReportTop != null) {
                IFBoxManager.meterDataV4Disposable_DailyReportTop.dispose();
                IFBoxManager.meterDataV4Disposable_DailyReportTop = null;
            }
        }


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
    private void backFragment() {

        //send820AutoDailyReport(SEND820_AUTODAILYREPORT_MODE.MenuOut);

        NavigationWrapper.popBackStack(this);
        _sharedViewModel.setBackAction(null);
        _sharedViewModel.setAutoDailyReportMenuHierarchy(SharedViewModel.AutoDailyReport_MenuHierarchy.LEVEL0);  //メニュー階層設定(上位層メニュー指定)
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
    private void ButtonEnabledFromTariff() {
        if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == false) {
            return;
        }

        String tmpTariffStatus = _menuViewModel.getIFBoxManager().getMeterStatus();         //現在

        View view = this.getView();

        if (tmpTariffStatus.equals("KUUSYA") == true)           //空車時
        {
            (view.findViewById(R.id.btn_menu_auto_daily_report_fuel)).setEnabled(true);         //燃料ボタン有効
            (view.findViewById(R.id.btn_menu_auto_daily_report_fuel)).setBackgroundColor(Color.parseColor(BUTTON_BACKCOLOR_ENABLED));
            (view.findViewById(R.id.btn_menu_auto_daily_report_metercheck)).setEnabled(true);         //メータ状態確認ボタン有効
            (view.findViewById(R.id.btn_menu_auto_daily_report_metercheck)).setBackgroundColor(Color.parseColor(BUTTON_BACKCOLOR_ENABLED));
        } else {
            (view.findViewById(R.id.btn_menu_auto_daily_report_fuel)).setEnabled(false);        //燃料ボタン無効
            (view.findViewById(R.id.btn_menu_auto_daily_report_fuel)).setBackgroundColor(getResources().getColor(R.color.gray, MainApplication.getInstance().getTheme()));
            (view.findViewById(R.id.btn_menu_auto_daily_report_metercheck)).setEnabled(false);        //メータ状態確認ボタン無効
            (view.findViewById(R.id.btn_menu_auto_daily_report_metercheck)).setBackgroundColor(getResources().getColor(R.color.gray, MainApplication.getInstance().getTheme()));
        }
    }

    /******************************************************************************/
    /*!
     * @brief  820通信部　主処理
     * @note   820通信部　主処理
     * @param [in] SEND820_AUTODAILYREPORT_MODE mode
     * @retval なし
     * @return　なし
     * @private
     */

    /******************************************************************************/
    //通信部は，燃料画面に移動させる(24/12/20)
    public boolean send820AutoDailyReport(SEND820_AUTODAILYREPORT_MODE mode) {

        if ((IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == false)) {              //フタバD以外は処理しない
            return false;
        }

        if (_menuViewModel.getIFBoxManager().getIsConnected820() == false)             //820未接続の場合
        {
            CommonErrorDialog dialog = new CommonErrorDialog();
            dialog.setCommonErrorEventHandlers(new CommonErrorEventHandlers() {
                @Override
                public void onPositiveClick(String errorCode) {
                    CommonClickEvent.RecordClickOperation("はい", "820未接続エラー(1)", true);
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
            dialog.ShowErrorMessage(getContext(), "6030");
            return false;
        }

        IFBoxManager.SendMeterDataInfo_FutabaD tmpSend820Info = new IFBoxManager.SendMeterDataInfo_FutabaD();
        tmpSend820Info.StatusCode = IFBoxManager.SendMeterDataStatus_FutabaD.NONE;
        tmpSend820Info.IsLoopBreakOut = false;
        tmpSend820Info.ErrorCode820 = IFBoxManager.SendMeterDataStatus_FutabaD.NONE;

        if (mode == SEND820_AUTODAILYREPORT_MODE.MenuIn) {                  //メニューin時
            _menuViewModel.meterDataV4InfoDisposable_ADR = _menuViewModel.getIFBoxManager().getMeterDataV4().subscribeOn(
                    Schedulers.io()).observeOn(Schedulers.newThread()).subscribe(meter -> {     //AndroidSchedulers.mainThread()
                Timber.i("[FUTABA-D]MenuAutoDailyReportFragment:750<-820 meter_data event cmd:%d ", meter.meter_sub_cmd);
                Timber.i("[FUTABA-D]MenuAutoDailyReportFragment:FREE Mes  %s, %s, %s ", meter.line_41, meter.line_42, meter.line_43);
                if (meter.meter_sub_cmd == 13) {              //自動日報イベント
                    tmpSend820Info.StatusCode = IFBoxManager.SendMeterDataStatus_FutabaD.SENDOK;             //ACKが返ってきた場合
                }
            });

            _menuViewModel.meterDataV4ErrorDisposable_ADR = _menuViewModel.getIFBoxManager().getMeterDataV4Error().subscribeOn(
                    Schedulers.io()).observeOn(Schedulers.newThread()).subscribe(error -> {         //送信中にエラー受信(タイムアウト，切断)
                Timber.e("[FUTABA-D]MenuAutoDailyReportFragment:Error event ErrCD:%d 820ErrCD:%d ", error.ErrorCode, error.ErrorCode820);
                tmpSend820Info.StatusCode = error.ErrorCode;
                tmpSend820Info.ErrorCode820 = error.ErrorCode820;

            });
        }
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                if (mode == SEND820_AUTODAILYREPORT_MODE.MenuIn) {                  //メニューin時
                    _menuViewModel.getIFBoxManager().send820_AutoDailyRepotIn();    //820へin情報を送信
                    for (int i = 0; i < 5 * 10; i++)        //最大5秒ほど待ってみる
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

                } else {                                                            //メニューout時(ACKが返らないのですぐに処理を継続)
                    _menuViewModel.getIFBoxManager().send820_AutoDailyRepotOut();   //820へout情報を送信
                    for (int i = 0; i < 5; i++)        //最大0.5秒ほど待ってみる
                    {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                        }
                    }
                    tmpSend820Info.IsLoopBreakOut = true;
                    tmpSend820Info.StatusCode = IFBoxManager.SendMeterDataStatus_FutabaD.SENDOK;             //ACKが返ってきた場合
                }

            }
        });
        thread.start();

        String tmpErrorCode = "";               //通信等でエラー発生時は""以外のエラーコードをセットする
        try {
            thread.join();

            if (_menuViewModel.meterDataV4InfoDisposable_ADR != null) {       //コールバック系を後始末
                _menuViewModel.meterDataV4InfoDisposable_ADR.dispose();
                _menuViewModel.meterDataV4InfoDisposable_ADR = null;
            }

            if (_menuViewModel.meterDataV4ErrorDisposable_ADR != null)        //コールバック系を後始末
            {
                _menuViewModel.meterDataV4ErrorDisposable_ADR.dispose();
                _menuViewModel.meterDataV4ErrorDisposable_ADR = null;
            }

            if (tmpSend820Info.IsLoopBreakOut == false) {                             //820から何も返却されなかった場合のループアウト
                _menuViewModel.getIFBoxManager().killRetryTimerFutabaD();            //タイマーの停止
                _menuViewModel.getIFBoxManager().send820_FunctionCodeErrorResult(IFBoxManager.SendMeterDataStatus_FutabaD.GENERICABORTCODE_NONACK, false);  //820へキャンセルコードを送信
                tmpErrorCode = "9108";                       //自動日報の未出庫エラー
            } else {
                switch (tmpSend820Info.StatusCode)                       //ステータスコードのチェック
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
                    default:
                        //ここに到達する場合は，エラー無しで処理完了されたことを意味する
                        break;
                }
            }


        } catch (Exception e) {
            Timber.e(e);
            tmpErrorCode = "8097";
        }

        if (tmpErrorCode.equals("") != true) {           //エラーコードが設定されている場合
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
            return false;
        }

        return true;
    }

}
//ADD-S BMT S.Oyama 2024/11/21 フタバ双方向向け改修
