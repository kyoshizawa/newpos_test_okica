package jp.mcapps.android.multi_payment_terminal.ui.menu;

import static android.content.Context.BATTERY_SERVICE;

//import static jp.mcapps.android.multi_payment_terminal.model.IFBoxManager.exitManualModeDisposable;
//import static jp.mcapps.android.multi_payment_terminal.model.IFBoxManager.meterStatNoticeDisposable;
import static jp.mcapps.android.multi_payment_terminal.thread.printer.PrinterProc.MANUALMODE_TRANS_TYPE_CODE_CANCEL;
import static jp.mcapps.android.multi_payment_terminal.thread.printer.PrinterProc.MANUALMODE_TRANS_TYPE_CODE_SALES;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;

import java.util.Timer;
import java.util.TimerTask;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.BaseFragment;
import jp.mcapps.android.multi_payment_terminal.CommonClickEvent;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.ScreenData;
import jp.mcapps.android.multi_payment_terminal.SharedViewModel;
import jp.mcapps.android.multi_payment_terminal.data.Amount;
import jp.mcapps.android.multi_payment_terminal.data.IFBoxAppModels;
import jp.mcapps.android.multi_payment_terminal.data.OptionService;
import jp.mcapps.android.multi_payment_terminal.data.TransMap;
import jp.mcapps.android.multi_payment_terminal.database.DBManager;
import jp.mcapps.android.multi_payment_terminal.database.history.slip.SlipData;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentMenuHomeBinding;
//import jp.mcapps.android.multi_payment_terminal.model.IFBoxManager;
import jp.mcapps.android.multi_payment_terminal.model.SoundManager;
import jp.mcapps.android.multi_payment_terminal.thread.printer.PrinterConst;
import jp.mcapps.android.multi_payment_terminal.thread.printer.PrinterManager;
import jp.mcapps.android.multi_payment_terminal.thread.printer.PrinterProc;
import jp.mcapps.android.multi_payment_terminal.ui.Converters;
import jp.mcapps.android.multi_payment_terminal.ui.error.CommonErrorDialog;
import jp.mcapps.android.multi_payment_terminal.ui.error.CommonErrorEventHandlers;
import timber.log.Timber;

public class MenuHomeFragment extends BaseFragment {
    public static MenuHomeFragment newInstance() {
        return new MenuHomeFragment();
    }

    private final String SCREEN_NAME = "ホームメニュー";
    private final String SCREEN_NAME_SECOND = "決済メニュー";

    private MenuViewModel _menuViewModel;
    private SharedViewModel _sharedViewModel;

    private MenuEventHandlers _menuEventHandlers;

    private BatteryManager _batteryManager;

    private int _batteryLevel;

    private TextView _menuBatteryTextView;

    private static int _oldBatteryLevel = 0;

    //ADD-S BMT S.Oyama 2024/10/16 フタバ双方向向け改修
    private SoundManager _soundManager = SoundManager.getInstance();
    private float _soundVolume = AppPreference.getSoundPaymentVolume() / 10f;
    private int _SDNotFoundErrorCount = 0;
    //ADD-E BMT S.Oyama 2024/10/16 フタバ双方向向け改修

    //ADD-S BMT S.Oyama 2025/01/22 フタバ双方向向け改修
    @SuppressWarnings("deprecation")
    private ProgressDialog _progressDialog = null;
    private Timer _extPrintEndedtimer = null;
    private int   _extPrintReciptTicketPrintedSW = 0;           // 領収書，チケット印刷完了スイッチ　0:未印刷 1:印刷済み　<0:ガイダンスエラー等エラー
    //ADD-E BMT S.Oyama 2025/01/22 フタバ双方向向け改修
    //ADD-S BMT S.Oyama 2025/02/26 フタバ双方向向け改修
    private int   _ProcessCodeErrKeyReq = 0;               // 処理コード表示要求　要求キーでのエラー表示時１
    private SlipData _slipDataPrepaid = null;

    //ADD-E BMT S.Oyama 2025/02/26 フタバ双方向向け改修

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        setHasOptionsMenu(true);

        _extPrintReciptTicketPrintedSW = 0;                 //領収書印刷完了スイッチを落とす

        final Fragment menuFragment = getParentFragment().getParentFragment();
        _menuViewModel = new ViewModelProvider(menuFragment, MainApplication.getViewModelFactory()).get(MenuViewModel.class);

        _menuViewModel.setBodyType(MenuTypes.HOME);

        _menuEventHandlers = new MenuEventHandlersImpl(this, _menuViewModel);

        final FragmentMenuHomeBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_menu_home, container, false);

        View view = binding.getRoot();

        binding.setViewModel(_menuViewModel);
        binding.setHandlers(_menuEventHandlers);
        binding.setLifecycleOwner(getViewLifecycleOwner());
        _menuViewModel.setIsPosActivated();
        _menuViewModel.setIsTicketActivated();
        _menuViewModel.setIsCashChangerActivated();

        final FragmentActivity activity = getActivity();

        if (activity != null) {
            _sharedViewModel = new ViewModelProvider(activity).get(SharedViewModel.class);
            _sharedViewModel.setScreenMode(SharedViewModel.ScreenMode.MAIN);
            binding.setSharedViewModel(_sharedViewModel);
            _batteryManager = (BatteryManager) activity.getSystemService(BATTERY_SERVICE);
            _batteryLevel = _batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        }
        // Register the battery level receiver
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        getContext().registerReceiver(batteryLevelReceiver, filter);
        if (AppPreference.isServicePos() && _sharedViewModel.getCashMenu().getValue()) {
            ScreenData.getInstance().setScreenName(SCREEN_NAME_SECOND);
        } else {
            ScreenData.getInstance().setScreenName(SCREEN_NAME);
        }

        //ADD-S BMT S.Oyama 2024/12/23 フタバ双方向向け改修
        AppPreference.setAmountInputCancel(false);              //空車信号時 金額入力を行えるようにする
        //ADD-E BMT S.Oyama 2024/12/23 フタバ双方向向け改修

        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        ArrayList<Long> prepaidSlipIds = AppPreference.getPrepaidSlipId();
        if (prepaidSlipIds != null && prepaidSlipIds.size() > 0) {
            int[] ids = new int[prepaidSlipIds.size()];
            for (int i = 0; i < prepaidSlipIds.size(); i++) {
                ids[i] = prepaidSlipIds.get(i).intValue();
            }
            //ADDCHG-S BMT S.Oyama 2025/03/03 フタバ双方向向け改修
            if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == true)
            {
                int tmpSepaleteCash = getSepaleteCashAmountFromPrepaidSlip();           //プリペイドの分別現金額を取得する
                int[] tmpSlipData = getPrepaidSlipDataTypeAndResult(ids);               //DBを覗いて売上かつ成功かどうかを取得する

                if ((tmpSlipData[0] == TransMap.TYPE_SALES) && (tmpSlipData[1] == TransMap.RESULT_SUCCESS)) {   //売上かつ成功の場合だけ，
                    Amount.setPaymented(1);             //支払済フラグをONする
                }
                else
                {
                    Amount.setPaymented(0);             //支払済フラグをOFFする
                }

                if (tmpSepaleteCash <= 0) {                 //プリペイドかつ分別現金がない場合
                    PrepaidSlipTransJob(view, ids);
                }
                else
                {
                    //ADD-S BMT S.Oyama 2025/03/25 フタバ双方向向け改修
                    int tmpPrepaidJobMode = PrinterProc.getInstance().printPrepaidTransGetTargetJobMode(ids);

                    if (tmpPrepaidJobMode == TransMap.TYPE_SALES) {
                        String tmpCashMesStr = "";
                        tmpCashMesStr = "残金があります。\n領収書を印刷します。\n残金 %s円";
                        final AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                                .setMessage(String.format(tmpCashMesStr, Converters.integerToNumberFormat(tmpSepaleteCash)))
                                .setCancelable(false)
                                .setOnDismissListener(dialog -> showSystemUI())
                                .setPositiveButton("はい", (dialog, which) -> {
                                    PrepaidSlipTransJob(view, ids);
                                });

                        final AlertDialog alertDialog = builder.create();
                        alertDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);

                        alertDialog.show();

                        TextView message = alertDialog.findViewById(android.R.id.message);
                        message.setTextSize(24);

                        hideSystemUI();

                        alertDialog.getWindow().
                                clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
                    }
                    else
                    {
                        PrepaidSlipTransJob(view, ids);
                    }
                    //ADD-E BMT S.Oyama 2025/03/25 フタバ双方向向け改修
                }
            }
            else {
                if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D_MANUAL) == true)
                {
                    Thread thread = new Thread(() -> {
                        int[] tmpSlipData = getPrepaidSlipDataTypeAndResult(ids);               //DBを覗いて売上かつ成功かどうかを取得する

                        if ((tmpSlipData[0] == TransMap.TYPE_SALES) && (tmpSlipData[1] == TransMap.RESULT_SUCCESS)) {   //売上かつ成功の場合だけ，
                            Amount.setPaymented(1);             //支払済フラグをONする
                        }
                        else
                        {
                            Amount.setPaymented(0);             //支払済フラグをOFFする
                        }

                        DBManager.getSlipDao().updateTransTypeCode(ids[ids.length - 1], (tmpSlipData[0] == TransMap.TYPE_CANCEL || tmpSlipData[0] == TransMap.TYPE_POINT_CANCEL || tmpSlipData[0] == TransMap.TYPE_CACHE_CHARGE_CANCEL) ? MANUALMODE_TRANS_TYPE_CODE_CANCEL : MANUALMODE_TRANS_TYPE_CODE_SALES);
                    });
                    thread.start();

                }

                PrepaidSlipTransJob(view, ids);
            }
            //ADDCHG-E BMT S.Oyama 2025/03/03 フタバ双方向向け改修
        }

        //各種マネーの有効/無効設定に合わせてボタンを有効/無効化
        (view.findViewById(R.id.btn_menu_home_credit)).setEnabled(AppPreference.isMoneyCredit());
        (view.findViewById(R.id.btn_menu_home_qr)).setEnabled(AppPreference.isMoneyQr());
        boolean isEmoney = AppPreference.isMoneySuica() //電子マネーの有効チェック
                || AppPreference.isMoneyId()
                || AppPreference.isMoneyWaon()
                || AppPreference.isMoneyNanaco()
                || AppPreference.isMoneyEdy()
                || AppPreference.isMoneyQuicpay()
                || AppPreference.isMoneyOkica();

        (view.findViewById(R.id.btn_menu_home_emoney)).setEnabled(isEmoney);

        //その他の有効チェック
        final OptionService service = MainApplication.getInstance().getOptionService();
        final boolean isWatari = AppPreference.isWatariPoint();
        final boolean isPrepaid = AppPreference.getIsPrepaid();
        // 郵便小為替はPOSモードの時のみ利用可能とする
        final boolean isPostalOrder = AppPreference.isFixedAmountPostalOrder() && AppPreference.isServicePos();
        boolean isOther = (service != null && service.isAvailable()) || isWatari || isPrepaid || isPostalOrder || AppPreference.isDemoMode();
        (view.findViewById(R.id.btn_menu_home_other)).setEnabled(isOther);

        if (IFBoxAppModels.isMatch(IFBoxAppModels.OKABE_MS70_D)) {
            // メーターからの入庫通知を受信したら業務終了
//            if (meterStatNoticeDisposable == null) {
//                meterStatNoticeDisposable = _menuViewModel.getMeterStatNotice().subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(meter -> {
//                    if (meter.info.equals("NYUUKO")) {
//                        _menuEventHandlers.businessEnd(getView(), _sharedViewModel);
//                    }
//                });
//            }
        }
        //ADD-S BMT S.Oyama 2024/09/18 フタバ双方向向け改修
//        else if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == true || IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D_MANUAL) == true)
//        {
//            if (IFBoxManager.meterDataV4Disposable_MenuHome == null) {
//                IFBoxManager.meterDataV4Disposable_MenuHome = _menuViewModel.getMeterDataV4().subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(meter -> {
//                    Timber.i("[FUTABA-D]HOME Fragment:750<-820 meter_data event cmd:%d soundNo:%d zandaka_flg:%d trans_brand:%s",
//                            meter.meter_sub_cmd, meter.sound_no, meter.zandaka_flg, meter.trans_brand);
//
//                    if(meter.meter_sub_cmd == 4) {              //入庫通知
//                        Timber.i("[FUTABA-D]HOME Fragment:nyuko event   ");
////                        _menuViewModel.send820AckNyuuko();          //820への入庫通知応答
//                        _menuEventHandlers.businessEnd(getView(), _sharedViewModel);
//                    } else if(meter.meter_sub_cmd == 3) {       //出庫通知
//                        //出庫処理は無し．応答ステータスのみ返す
//                        Timber.i("[FUTABA-D]HOME Fragment:syuko event   ");
////                        _menuViewModel.send820AckSyukko("0000");          //820への出庫通知応答
//                    } else if (meter.meter_sub_cmd == 5) {       //処理コード通知
//                        PrinterManager printerManager = PrinterManager.getInstance();
//                        printerManager.setView(view);
//                        if (meter.sound_no != null) {
//                            switch (meter.sound_no) {
//                                case -3:            //メモリーカード未挿入時の「挿入してください」時の処理
//                                    if (_SDNotFoundErrorCount == 0) {           //複数回飛んでくるので抑止をいれる
//                                        _SDNotFoundErrorCount++;
//
//                                        if (_ProcessCodeErrKeyReq != 0)      //エラー表示中に他のエラーが発生した場合は非表示
//                                        {
//                                            printerManager.DissmissPrinterDuplexError();     //
//                                        }
//
//                                        Timber.i("[FUTABA-D]HOME Fragment:Sound event No -3 ");
//                                        printerManager.PrinterDuplexError(PrinterConst.DuplexPrintStatus_SDCARD_NOTFOUND);     //SDカード未挿入エラーを表示
//                                    }
//                                    break;
//                                case 3:             //メモリーカード未挿入時の「挿入してください」表示中止要求時
//                                    Timber.i("[FUTABA-D]HOME Fragment:Sound event No 3 ");
//                                    printerManager.DissmissPrinterDuplexError();     //SDカード未挿入エラーを非表示
//                                    _SDNotFoundErrorCount = 0;
//                                    break;
//                                case 9:             //用紙切れの処理
//                                    //ADD-S BMT S.Oyama 2025/01/22 フタバ双方向向け改修
//                                    Timber.i("[FUTABA-D]HOME Fragment:Sound event No 9 (Recipt Ticket Print) ");
//                                    _extPrintReciptTicketPrintedSW = -1;                     //領収書印刷完了スイッチをエラーありで立てる
//                                    try {
//                                        if (_progressDialog != null) {
//                                            _progressDialog.dismiss();
//                                            _progressDialog = null;
//                                        }
//                                    }
//                                    catch (Exception e) {
//                                        Timber.e(e);
//                                        _progressDialog = null;
//                                    }
//
//                                    //ADD-S BMT S.Oyama 2025/03/11 フタバ双方向向け改修
//                                    boolean tmpPaperLackingWithSetkey = _menuViewModel.getIFBoxManager().getIsPaperlackingWithSetkey();     //セットキー付きかどうか
//                                    if (tmpPaperLackingWithSetkey == true)  {
//                                        _menuViewModel.getIFBoxManager().setIsPaperlackingWithSetkey(false);     //セットキー付き印刷に失敗しましたフラグをリセット
//                                        printerManager.PrinterDuplexError(PrinterConst.DuplexPrintStatus_PAPERLACKING);             //印刷に失敗しました(セットキー付き)9116  WAON歴印字時等
//                                    }
//                                    else {
//                                        PrinterProc printerProc = PrinterProc.getInstance();
//                                        String tmpBlandName = printerProc.getDuplexPrint_BlandName();
//                                        if (tmpBlandName.equals(MainApplication.getInstance().getString(R.string.money_brand_credit)) == true) {
//                                            // ブランド名がクレジットの場合は，print_endで紙切れダイアログを出しているので，表示させない
//                                        } else {
//                                            printerManager.PrinterDuplexError(PrinterConst.DuplexPrintStatus_OUTOFPAPER_NORESTART);       //印刷に失敗しました9110
//                                        }
//                                    }
//                                    //ADD-E BMT S.Oyama 2025/03/11 フタバ双方向向け改修
//
//                                    view.post(() -> {
//                                        _menuViewModel.getIFBoxManager().setExtPrintJobMode(IFBoxManager.SendMeterDataStatus_FutabaD.NONE);           //印刷モード初期値へ
//                                    });
//                                    //ADD-E BMT S.Oyama 2025/01/22 フタバ双方向向け改修
//                                    break;
//                                case 1:             //領収書　チケット伝票時の印刷完了信号
//                                    Timber.i("[FUTABA-D]HOME Fragment:Sound event No 1 ");
//                                    _extPrintReciptTicketPrintedSW = 1;                     //領収書印刷完了スイッチを立てる
//                                    break;
//                                //ADD-S BMT S.Oyama 2025/02/26 フタバ双方向向け改修
//                                case 0:             //音声ガイダンス指定無し時に，FREE ER**系エラーチェックを実施する
//                                    Timber.i("[FUTABA-D]HOME Fragment:Sound event No 0 ");
//                                    Timber.i("[FUTABA-D]HOME Fragment:FREE Mes  %s, %s, %s ", meter.line_41, meter.line_42, meter.line_43);
//                                    if ((meter.line_1 != null) && (meter.line_2 != null)) {
//                                        Timber.i("[FUTABA-D]HOME Fragment:line1, lin2, line3  %s, %s, %s ", meter.line_1, meter.line_2, meter.line_3);
//                                        int tmpProcessCode = _menuViewModel.getIFBoxManager().send820_IsProcessCode_ErrorCD(meter.line_1);          //処理コード表示要求エラーコードはLine１に乗ってくる　エラーコードを取得
//                                        if (tmpProcessCode < 0) {           //エラーコードがある場合
//                                            String tmpProcessCodeStr[] = _menuViewModel.getIFBoxManager().send820_KeyAndErrDetail(meter.line_2);      //キー要求，エラーコード詳細を取得(配列２要素)
//
//                                            //ADD-S BMT S.Oyama 2025/04/02 フタバ双方向向け改修
//                                            if (tmpProcessCode == -20)          //FREE時
//                                            {
//                                                Timber.i("[FUTABA-D]HOME Fragment:FREE In ");
//                                                if ((meter.line_3 != null) && (meter.line_3.trim().length() > 0)) {
//                                                    // 処理コード表示要求に機能名（跳ね上がりなど）が含まれる場合
//                                                    recvMeterFuncMsg(view, tmpProcessCodeStr[0], tmpProcessCodeStr[1], meter.line_3);
//                                                }
//                                                else {
//                                                    int result = tmpProcessCodeStr[1].indexOf("TS ");             //エラーコード詳細に"TS "が含まれる場合はエラー発生
//                                                    if (result != -1) {
//                                                        OutputFreeMessageDialog(meter.line_41, meter.line_42, tmpProcessCodeStr[0]);     //フリーエラー発生時のメッセージ表示
//                                                        _ProcessCodeErrKeyReq = 1;           //処理コードエラー時のキー要求でのエラー表示フラグを立てる(SDカード未挿入時のエラー表示を抑止するため)
//                                                    }
//                                                }
//                                            }
//                                            else {              //FREE以外 ER**系エラー時
//                                                if (tmpProcessCodeStr[0].equals(IFBoxManager.Send820Status_ProcessCode_KeyREQ.KEYREQ_TEISEI) == true) {     //訂正キーが送られてきた場合
//                                                    printerManager.PrinterDuplexError(PrinterConst.DuplexPrintStatus_PROCESSCODE_TEISEIREQ);             //処理コード要求時　取消キー要求の場合　のエラー
//                                                    _ProcessCodeErrKeyReq = 1;           //処理コードエラー時のキー要求でのエラー表示フラグを立てる(SDカード未挿入時のエラー表示を抑止するため)
//                                                }
//                                            }
//                                            //ADD-E BMT S.Oyama 2025/04/02 フタバ双方向向け改修
//                                        }
//                                    }
//                                    break;
//                                //ADD-E BMT S.Oyama 2025/02/26 フタバ双方向向け改修
//                            }
//                        }
//                    } else if (meter.meter_sub_cmd == 9) {       //ファンクション実行要求：
//                        boolean tmpActFLG = false;
//                        String tmpBrand = "";
//                        if (meter.trans_brand != null ){
//                            tmpBrand = meter.trans_brand;
//                            if (meter.trans_brand.equals("クレジット") == true) {
//                                tmpActFLG = true;
//                            } else if (meter.trans_brand.equals("交通系電子マネー") == true) {
//                                tmpActFLG = true;
//                            } else if (meter.trans_brand.equals("コード決済") == true){
//                                tmpActFLG = true;
//                            }
//                        }
//
//                        if ((tmpActFLG == true) && (meter.separate_flg != 0)) {
//                            //PrinterProc printerProc = PrinterProc.getInstance();
//                            //printerProc.printTransFutabaD_Separation2ndSend();              //分別の2nd送信要求を返す
//
//                            PrinterManager printerManager = PrinterManager.getInstance();     //分別の2nd送信要求を返す
//                            printerManager.print_transFutabaDSeparation2nd(view);
//                        }
//
//                        Timber.i("[FUTABA-D]HOME Fragment:Function function event code:%d brand:%s", meter.meter_sub_cmd, tmpBrand);
//
//                    } else {
//                        if ((meter.trans_brand != null) && (meter.trans_brand.equals("決済確認") == true))               //ブランド名に決済確認指示が記載されている場合は
//                        {
//                            Timber.i("[FUTABA-D]HOME Fragment:Kessai Kakunin event ");
//
//                            PrinterProc printerProc = PrinterProc.getInstance();
//                            printerProc.printTransFutabaD_KessaiKakunin();              //直前の決済情報込で，決済確認ACKを返す
//                        }
//                    }
//                });
//
//                if (IFBoxManager.meterDataV4ErrorDisposable_MenuHome == null) {                 //送信中にエラー受信(タイムアウト，切断)
//                    IFBoxManager.meterDataV4ErrorDisposable_MenuHome =  _menuViewModel.getIFBoxManager().getMeterDataV4Error().subscribeOn(
//                            Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(error -> {         //送信中にエラー受信(タイムアウト，切断)
//                        Timber.e("[FUTABA-D]onViewCreated() HomeFragment:Error event ErrCD:%d 820ErrCD:%d ", error.ErrorCode, error.ErrorCode820);
//
//                        if (_menuViewModel.getIFBoxManager().getExtPrintJobMode().getValue() != IFBoxManager.SendMeterDataStatus_FutabaD.NONE)
//                        {
//                            _extPrintReciptTicketPrintedSW = -1;                     //領収書印刷完了スイッチをエラーありで立てる
//
//                            try {
//                                if (_progressDialog != null) {
//                                    _progressDialog.dismiss();
//                                    _progressDialog = null;
//                                }
//
//                            }
//                            catch (Exception e) {
//                                Timber.e(e);
//                                _progressDialog = null;
//                            }
//                            view.post(() -> {
//                                _menuViewModel.getIFBoxManager().setExtPrintJobMode(IFBoxManager.SendMeterDataStatus_FutabaD.NONE);           //印刷モード初期値へ
//                            });
//                        }
//
//                        PrinterManager printerManager = PrinterManager.getInstance();
//                        printerManager.setView(view);
//                        if (error.ErrorCode820 == IFBoxManager.Send820Status_Error_FutabaD.ERROR_STATUS820_PAPERLACKING)       //用紙無しエラー
//                        {
//                            PrinterProc printerProc = PrinterProc.getInstance();
//                            String tmpBlandName = printerProc.getDuplexPrint_BlandName();
//                            if (tmpBlandName.equals(MainApplication.getInstance().getString(R.string.money_brand_credit)) == true) {
//                                // ブランド名がクレジットの場合は，print_endで紙切れダイアログを出しているので，表示させない
//                            } else {
//                                printerManager.PrinterDuplexError(PrinterConst.DuplexPrintStatus_OUTOFPAPER_NORESTART);       //印刷に失敗しました
//                            }
//                        }
//                        else
//                        {
//                            //printerManager.PrinterDuplexError(PrinterConst.DuplexPrintStatus_IFBOXERROR);       //820エラー
//                        }
//                    });
//                }
//            }
//        }
        //ADD-E BMT S.Oyama 2024/09/18 フタバ双方向向け改修

        //ADD-S BMT S.Oyama 2025/01/22 フタバ双方向向け改修
        if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == true) {
//            if (_menuViewModel.getIFBoxManager().getExtPrintJobMode().getValue() != IFBoxManager.SendMeterDataStatus_FutabaD.NONE) {
//                showProgressDialog(view.getContext());
//
//                TimerTask timerTask = new TimerTask() {
//                    @Override
//                    public void run() {
//
//                        try {
//                            if (_progressDialog != null) {
//                                _progressDialog.dismiss();
//                                _progressDialog = null;
//                            }
//
//                        } catch (Exception e) {
//                            Timber.e(e);
//                            _progressDialog = null;
//                        }
//
//                        _extPrintEndedtimer = null;
//                        view.post(() -> {
//                            _menuViewModel.getIFBoxManager().setExtPrintJobMode(IFBoxManager.SendMeterDataStatus_FutabaD.NONE);           //印刷モード初期値へ
//
//                            //DEL-S BMT S.Oyama 2025/02/05 フタバ双方向向け改修
//                            //if (_extPrintReciptTicketPrintedSW == 0)            //0の場合はガイダンス１．あるいは用紙切れを返却してこなかった＝印字タイムアウト
//                            //{
//                            //    PrinterManager printerManager = PrinterManager.getInstance();
//                            //    printerManager.setView(view);
//                            //    printerManager.PrinterDuplexError(PrinterConst.DuplexPrintStatus_TICKETRECEIPTKEY_ERR);       //レシートエラー　お取り扱いできません
//                            //}
//                            //DEL-E BMT S.Oyama 2025/02/05 フタバ双方向向け改修
//                            _extPrintReciptTicketPrintedSW = 0;                 //領収書印刷完了スイッチを落とす
//                        });
//                    }
//                };
//
//                _extPrintEndedtimer = new Timer();
//                _extPrintEndedtimer.schedule(timerTask, 5000);
//            }
        }
        //ADD-E BMT S.Oyama 2025/01/22 フタバ双方向向け改修

        if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D_MANUAL)) {
//            if (exitManualModeDisposable == null) {
//                exitManualModeDisposable = _menuViewModel.getExitManualMode().subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(flg -> {
//                    _sharedViewModel.setUpdatedFlag(true);
//
//                    new AlertDialog.Builder(view.getContext())
//                            .setTitle("手動決済モード終了")
//                            .setMessage("手動決済モードを終了しました")
//                            .setPositiveButton("確認", new DialogInterface.OnClickListener() {
//                                @Override
//                                public void onClick(DialogInterface dialogInterface, int i) {
//                                }
//                            })
//                            .show();
//                });
//            }
        }
    }

    private void showSystemUI() {
        if (getActivity() == null) return;
        getActivity().getWindow().setDecorFitsSystemWindows(true);
        WindowInsetsController controller = getActivity().getWindow().getInsetsController();
        if (controller != null) {
            controller.show(WindowInsets.Type.systemBars() | WindowInsets.Type.navigationBars());
        }
    }

    private void hideSystemUI() {
        if (getActivity() == null) return;
        getActivity().getWindow().setDecorFitsSystemWindows(false);
        WindowInsetsController controller = getActivity().getWindow().getInsetsController();
        if (controller != null) {
            controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
            controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }
    }
    @SuppressWarnings("deprecation")
    private void showProgressDialog(Context context){
        _progressDialog = new ProgressDialog(context);
        _progressDialog.setMessage("レシート印刷中 ・・・ ");                   // 内容(メッセージ)設定
        _progressDialog.setCancelable(false);                              // キャンセル無効
        _progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);    // スタイル設定
        _progressDialog.show();
    }

    //ADD-S BMT S.Oyama 2025/04/02 フタバ双方向向け改修
    /******************************************************************************/
    /*!
     * @brief  メータから送らてきたフリーメッセージのダイアログ表示（フタバ双方向用）
     * @note
     * @param [in] view : ビュー
     * @param [in] msg41 : メッセージ１
     * @param [in] msg42 : メッセージ２
     * @param [in] KeyCodeReqStr : 要求キー
     * @retval なし
     * @return なし
     * @private
     */
    /******************************************************************************/
    @RequiresApi(api = Build.VERSION_CODES.N)
    void OutputFreeMessageDialog( String msg41, String msg42, String KeyCodeReqStr) {
        String tmpFreeMsg = msg41 + "\n" + msg42 + "\n「はい」を押してエラーを解除してください";
        String setTitle = "メータエラー"; // タイトル

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getContext());
        alertDialog.setTitle(setTitle);        // タイトル設定
        alertDialog.setMessage(tmpFreeMsg);  // 内容(メッセージ)設定
        alertDialog.setCancelable(true);       // キャンセル有効
        // はいボタンの設定
        alertDialog.setPositiveButton("はい", new DialogInterface.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            public void onClick(DialogInterface dialog, int which) {
//                if (KeyCodeReqStr.equals(IFBoxManager.Send820Status_ProcessCode_KeyREQ.KEYREQ_TEISEI) == true) {     //訂正キーが送られてきた場合
//                    _menuViewModel.getIFBoxManager().send820_TeiseiKeyNonAck();           //訂正キー ACKなしで送付
//                }
            }
        });
        // ダイアログを表示
        alertDialog.show();
    }
    //ADD-E BMT S.Oyama 2025/04/02 フタバ双方向向け改修

    @RequiresApi(api = Build.VERSION_CODES.N)
    void recvMeterFuncMsg(View view, String msg1, String msg2, String funcName) {
        // 跳ね上がり
//        if (funcName.equals(IFBoxManager.Send820Status_ProcessCode_FuncNAME.FUNCNAME_HANEAGARI)) {
//            String kingaku = msg2.replaceAll(" ", "");
//            Timber.i("跳ね上がり %s", kingaku);
//            CommonErrorDialog dialog = new CommonErrorDialog();
//            dialog.ShowErrorMessage(getContext(), MainApplication.getInstance().getString(R.string.error_type_FutabaD_FareUp_Warning) + "@@@" + IFBoxManager.getFareUpMessage(kingaku) + "@@@");
//            dialog.setCommonErrorEventHandlers(new CommonErrorEventHandlers() {
//                @Override
//                public void onPositiveClick(String errorCode) {
//                    _menuViewModel.getIFBoxManager().send820_KeyCode(IFBoxManager.SendMeterDataStatus_FutabaD.RECEIPT_PRINT, 34, false); // 現金キーを送信
//                }
//                @Override
//                public void onNegativeClick(String errorCode) {}
//                @Override
//                public void onNeutralClick(String errorCode) {}
//                @Override
//                public void onDismissClick(String errorCode) {}
//            });
//
//        }

        // 集計印字
//        if (funcName.equals(IFBoxManager.Send820Status_ProcessCode_FuncNAME.FUNCNAME_SYUUKEI)) {
//            // 通／他キーを送信後、メーターから「集計印字」や「セットキー」の文字を含んだ処理コード表示要求を受信するので、セットキーを送信する
//            //_menuViewModel.getIFBoxManager().setExtPrintJobMode(IFBoxManager.SendMeterDataStatus_FutabaD.RECEIPT_PRINT); //領収書印刷モード設定
//            _menuViewModel.getIFBoxManager().send820_KeyCode(IFBoxManager.SendMeterDataStatus_FutabaD.RECEIPT_PRINT, 22, false);
//        }
//
//        // 紙切れ
//        if (funcName.equals(IFBoxManager.Send820Status_ProcessCode_FuncNAME.FUNCNAME_KAMIGIRE) && msg1.equals(IFBoxManager.Send820Status_ProcessCode_KeyREQ.KEYREQ_SETKEY) == true) {
//            PrinterManager printerManager = PrinterManager.getInstance();
//            printerManager.setView(view);
//            printerManager.PrinterDuplexError(PrinterConst.DuplexPrintStatus_PAPERLACKING);
//        }
    }
    //ADD-S BMT S.Oyama 2025/03/03 フタバ双方向向け改修
    /******************************************************************************/
    /*!
     * @brief  プリペイド向け印刷処理実施（フタバ双方向用）
     * @note   プリペイド向け印刷処理実施
     * @param  View view
     * @param  int[] slipid配列
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    private void PrepaidSlipTransJob(View view, int[] slipids)
    {
        boolean isPrepaidCardSale = PrinterManager.getInstance().prepaid_print_trans(view, slipids);
        //ADD-S BMT S.Oyama 2025/03/17 フタバ双方向向け改修
        String[] tmpBlandInfo = PrinterManager.getInstance().getBrandNameFromSlipIDForPrepaid();      //直近のSLIPIDの決済ブランド情報を取得
        //ADD-E BMT S.Oyama 2025/03/17 フタバ双方向向け改修
        // 成功しても失敗してもIDはリセット
        AppPreference.setPrepaidSlipId(new ArrayList<>());

        //ADD-S BMT S.Oyama 2025/03/17 フタバ双方向向け改修
        int tmpTranstype = 0;
        try {
            tmpTranstype = Integer.parseInt(tmpBlandInfo[1]);
        }
        catch (Exception e) {
            tmpTranstype = TransMap.TYPE_SALES;
        }

        if (tmpTranstype == TransMap.TYPE_SALES || tmpTranstype == TransMap.TYPE_CANCEL) {      //売上か売上取消時
            // 料金もリセット
            Amount.reset();
        }
        else {
            //それ以外のプリペイド関連処理は，料金リセットを実施しない
        }
        //ADD-E BMT S.Oyama 2025/03/17 フタバ双方向向け改修

        // カード発売の場合はプリペイドアプリへ移動
        if (isPrepaidCardSale) {
            _menuEventHandlers.navigateToPrepaidApp(view);
        }
    }
    //ADD-E BMT S.Oyama 2025/03/03 フタバ双方向向け改修

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_fragment_home, menu);
        // 電池 level
        MenuItem _menuBattery= menu.findItem(R.id.menu_fragment_battery);
        View view = _menuBattery.getActionView();
        _menuBatteryTextView = (TextView) view.findViewById(R.id.menuTitle);
        Timber.d("onCreateOptionsMenu _batteryLevel = %d%%", _batteryLevel);
        updateBatteryLevel(_batteryLevel, true);

        // バージョン
        menu.findItem(R.id.action_home_fragment_info).setOnMenuItemClickListener(v -> {
            CommonClickEvent.RecordClickOperation((String) v.getTitle(), true);
            _menuEventHandlers.navigateMain(
                    this, R.id.action_navigation_menu_to_navigation_info);

            return false;
        });

        // デバイスチェック
        menu.findItem(R.id.action_home_fragment_device_check).setOnMenuItemClickListener(v -> {
            CommonClickEvent.RecordClickOperation((String) v.getTitle(), true);
            _menuEventHandlers.navigateMain(
                    this, R.id.action_navigation_menu_to_navigation_device_check);

            return false;
        });

        // 設定
        menu.findItem(R.id.action_home_fragment_settings).setOnMenuItemClickListener(v -> {
            CommonClickEvent.RecordClickOperation((String) v.getTitle(), true);
            _menuEventHandlers.navigateMain(
                    this, R.id.action_navigation_menu_to_navigation_settings);

            return false;
        });

        // 設置/撤去
        menu.findItem(R.id.action_home_fragment_installation_and_removal).setOnMenuItemClickListener(v -> {
            CommonClickEvent.RecordClickOperation((String) v.getTitle(), true);
            _menuEventHandlers.navigateMain(
                    this, R.id.action_navigation_menu_to_navigation_installation_and_removal);

            return false;
        });

        // 更新確認
        menu.findItem(R.id.action_home_fragment_update_check).setOnMenuItemClickListener(v -> {
            CommonClickEvent.RecordClickOperation((String) v.getTitle(), true);
//            _menuEventHandlers.showDialog(this, "更新確認\n・マルチ決済端末のアプリ更新\n・IF-BOXの更新や選択を想定");
            _menuEventHandlers.navigateMain(
                    this, R.id.action_navigation_menu_to_navigation_update);

            return false;
        });

        // 閉じる
        menu.findItem(R.id.action_home_fragment_menu_close).setOnMenuItemClickListener(v -> {
            // 処理は行わずメニューを閉じるのみ
            return false;
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        //レシーバーの削除
        getContext().unregisterReceiver(batteryLevelReceiver);

//        if (IFBoxAppModels.isMatch(IFBoxAppModels.OKABE_MS70_D)) {
//            if (meterStatNoticeDisposable != null) {
//                meterStatNoticeDisposable.dispose();
//                meterStatNoticeDisposable = null;
//            }
//        }

        //ADD-S BMT S.Oyama 2024/09/18 フタバ双方向向け改修
//        if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == true || IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D_MANUAL) == true)
//        {
//            if (IFBoxManager.meterDataV4Disposable_MenuHome != null) {
//                IFBoxManager.meterDataV4Disposable_MenuHome.dispose();
//                IFBoxManager.meterDataV4Disposable_MenuHome = null;
//            }
//
//            //ADD-S BMT S.Oyama 2025/01/22 フタバ双方向向け改修
//            if (IFBoxManager.meterDataV4ErrorDisposable_MenuHome != null) {
//                IFBoxManager.meterDataV4ErrorDisposable_MenuHome.dispose();
//                IFBoxManager.meterDataV4ErrorDisposable_MenuHome = null;
//            }
//            //ADD-E BMT S.Oyama 2025/01/22 フタバ双方向向け改修
//
//            if (exitManualModeDisposable != null) {
//                exitManualModeDisposable.dispose();
//                exitManualModeDisposable = null;
//            }
//        }
        //ADD-E BMT S.Oyama 2024/09/18 フタバ双方向向け改修

        //ADD-S BMT S.Oyama 2025/01/22 フタバ双方向向け改修
        //_menuViewModel.getIFBoxManager().setExtPrintJobMode(IFBoxManager.SendMeterDataStatus_FutabaD.NONE);           //印刷モード初期値へ
        //ADD-E BMT S.Oyama 2025/01/22 フタバ双方向向け改修
    }

    @Override
    public void onPrimaryNavigationFragmentChanged(boolean isPrimaryNavigationFragment) {
        super.onPrimaryNavigationFragmentChanged(isPrimaryNavigationFragment);

        //onCreateViewより先に呼ばれるためSharedViewModelのインスタンスを取得しているか確認
        if (_sharedViewModel != null) {
            //このFragmentに遷移する場合：isPrimaryNavigationFragment = true
            //このFragmentから遷移する場合：isPrimaryNavigationFragment = false
        }
    }

    private void updateBatteryLevel(int nowBatteryLevel, boolean nowUpdate) {

        int batteryLevel = 0;

        if (nowBatteryLevel >= 20 && nowBatteryLevel <= 100) {
            // 電池残量が20%～100%の場合、10%毎に変動値を設定
            batteryLevel = nowBatteryLevel/10*10;
        } else {
            // 電池残量が0%～19%の場合、1%毎に変動値を設定
            batteryLevel = nowBatteryLevel;
        }

        if (_oldBatteryLevel == batteryLevel && nowUpdate == false) {
            // 前回の電池残量値と変わらず、且つ即表示更新ではない場合は、表示更新しない
            return;
        }

        // Update the UI with the battery level
        if (batteryLevel >= 20) {
            _menuBatteryTextView.setTextColor(Color.WHITE);
            if (batteryLevel == 100) {
                _menuBatteryTextView.setText("　   　　　 　 電池：" + batteryLevel + "%");
            } else {
                _menuBatteryTextView.setText("　　  　 　　    電池：" + batteryLevel + "%");
            }
            if (_oldBatteryLevel != batteryLevel){
                _oldBatteryLevel = batteryLevel;
                Timber.i("電池：%d%%",batteryLevel);
            }
        }else if (batteryLevel >= 10 && batteryLevel < 20) {
            if (AppPreference.isDemoMode()) {
                _menuBatteryTextView.setTextColor(Color.WHITE);
            } else {
                _menuBatteryTextView.setTextColor(getResources().getColor(R.color.radio_level_low, MainApplication.getInstance().getTheme()));
            }
            _menuBatteryTextView.setText("   充電してください：" + batteryLevel + "%");
            if ((_oldBatteryLevel >= 20 || _oldBatteryLevel < 10) && _oldBatteryLevel != batteryLevel ){
                _oldBatteryLevel = batteryLevel;
                Timber.i("充電してください：%d%%",batteryLevel);
            }
        }else if(batteryLevel >= 0 && batteryLevel < 10) {
            if (AppPreference.isDemoMode()) {
                _menuBatteryTextView.setTextColor(Color.WHITE);
            } else {
                _menuBatteryTextView.setTextColor(getResources().getColor(R.color.radio_level_low, MainApplication.getInstance().getTheme()));
            }
            _menuBatteryTextView.setText(" 【決済不可】要充電：" + batteryLevel + "%");
            if ((_oldBatteryLevel == 0 || _oldBatteryLevel >= 10) && _oldBatteryLevel != batteryLevel ){
                _oldBatteryLevel = batteryLevel;
            	Timber.i("【決済不可】要充電：%d%%",batteryLevel);
            }            
        }else{
            //この中には入らない事
        }
    }

    private BroadcastReceiver batteryLevelReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int nowBatteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            if (_menuBatteryTextView == null)
            {
                // 端末起動時にHOME画面へ遷移するまでは更新させない
                // Timber.d("_menuBatteryTextView = null");
            } else {
                updateBatteryLevel(nowBatteryLevel, false);
                // Timber.d("batteryLevelReceiver nowBatteryLevel = %d%%", nowBatteryLevel);
            }
        }
    };

    //ADD-S BMT S.Oyama 2025/03/03 フタバ双方向向け改修
    /******************************************************************************/
    /*!
     * @brief  プリペイドのslipデータ中最新レコードを得て，分別現金額を得る（フタバ双方向用）
     * @note   プリペイドのslipデータ中最新レコードを得て，分別現金額を得る
     * @param なし
     * @retval なし
     * @return　int tmpresult  分別現金額
     * @private
     */
    /******************************************************************************/
    private int getSepaleteCashAmountFromPrepaidSlip()
    {
        int tmpresult = 0;
        tmpresult = Amount.getCashAmount();                //メータ側の金額
        return tmpresult;
    }
    /******************************************************************************/
    /*!
     * @brief  プリペイドのslipデータ中最新レコードを得て，取引種別と取引結果を返す（フタバ双方向用）
     * @note   プリペイドのslipデータ中最新レコードを得て，取引種別と取引結果を返す
     * @param int[] ids  プリペイドのslipdataのID
     * @retval なし
     * @return int[] tmpresult  取引種別[0]と取引結果[1]
     * @private
     */
    /******************************************************************************/
    private int[] getPrepaidSlipDataTypeAndResult(int[] ids)
    {
        int[] tmpresult = new int[2];
        tmpresult[0] = 0;
        tmpresult[1] = 0;

        if ( (ids == null) || (ids.length == 0) )
        {
            return tmpresult;
        }

        int tmpSlipid = ids[ids.length - 1];            //最新のslipdataのIDを取得:配列末尾

        // 別スレッド：伝票印刷関連データ取得
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                _slipDataPrepaid = DBManager.getSlipDao().getOneById(tmpSlipid);
            }
        });
        thread.start();

        try {
            // スレッドが完了するのを待つ
            thread.join();
            if (_slipDataPrepaid == null)
            {
                return tmpresult;
            }

            tmpresult[0] = _slipDataPrepaid.transType;
            tmpresult[1] = _slipDataPrepaid.transResult;

            Timber.i("[FUTABA-D]getPrepaidSlipDataTYpeAndResult(): slipdata.transCashTogetherAmount: %d", _slipDataPrepaid.transCashTogetherAmount);

        } catch (Exception e) {
            Timber.i("[FUTABA-D]getPrepaidSlipDataTYpeAndResult():%s", e);
            e.printStackTrace();
        }

        return tmpresult;
    }

    //ADD-E BMT S.Oyama 2025/03/03 フタバ双方向向け改修
}

//if ((meter.zandaka_flg != null) && (meter.zandaka_flg == 2))             //残高フラグが取引履歴照会(2)の場合
//{
//    Timber.i("[FUTABA-D]HOME Fragment:WAON History event ");
//    PrinterProc printerProc = PrinterProc.getInstance();
//    printerProc.sendWsPrintHistryFutabaDAfterJob();              //取引履歴照会の後の印刷ジョブを実行
//}
