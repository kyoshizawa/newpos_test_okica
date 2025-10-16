package jp.mcapps.android.multi_payment_terminal.thread.printer;

import static jp.mcapps.android.multi_payment_terminal.thread.printer.PrinterProc.MANUALMODE_TRANS_TYPE_CODE_CANCEL;
import static jp.mcapps.android.multi_payment_terminal.thread.printer.PrinterProc.MANUALMODE_TRANS_TYPE_CODE_SALES;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.annotation.RequiresApi;

import com.epson.epos2.Epos2CallbackCode;
import com.pos.device.printer.Printer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.CommonClickEvent;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.NavigationWrapper;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.data.IFBoxAppModels;
import jp.mcapps.android.multi_payment_terminal.data.TransMap;
import jp.mcapps.android.multi_payment_terminal.database.DBManager;
import jp.mcapps.android.multi_payment_terminal.database.history.error.ErrorStackingDao;
import jp.mcapps.android.multi_payment_terminal.database.history.error.ErrorStackingData;
import jp.mcapps.android.multi_payment_terminal.database.history.slip.SlipData;
import jp.mcapps.android.multi_payment_terminal.database.pos.ServiceFunctionData;
import jp.mcapps.android.multi_payment_terminal.iCAS.data.DeviceClient;
//import jp.mcapps.android.multi_payment_terminal.model.IFBoxManager;
import jp.mcapps.android.multi_payment_terminal.ui.emoney.okica.BaseEMoneyOkicaViewModel;
import jp.mcapps.android.multi_payment_terminal.ui.error.CommonErrorDialog;
import jp.mcapps.android.multi_payment_terminal.ui.error.CommonErrorEventHandlers;
import jp.mcapps.android.multi_payment_terminal.ui.menu.MenuCashChangerViewModel;
import timber.log.Timber;

public class PrinterManager implements CommonErrorEventHandlers {

    private static PrinterManager _instance = null;
    @SuppressWarnings("deprecation")
    private static ProgressDialog _progressDialog;
    private static AlertDialog.Builder _alertDialog;
    private static CommonErrorDialog _commonErrorDialog;
    private static View _view = null;
    //ADD-S BMT S.Oyama 2024/10/11 フタバ双方向向け改修
    public void setView(View view) {
        _view = view;
    }
    //ADD-E BMT S.Oyama 2024/10/11 フタバ双方向向け改修

    private static int isSlipDataId;
    private static int isAggregateOrder;

    private static int[] isSlipDataIds;

    private static int isSlipType;
    private static int isSlipCopy;
    private static int isAggregateType;
    private static int printerDuplexErrorCode = 0;

    private static Integer isTransType;
    private static Integer isTransResult;
    private static boolean isRePrinter;
    private ServiceFunctionData _serviceFunctionData;
    private static boolean isReceiptDetail; // 領収書明細付フラグ
    private static Integer detailStatementPrintCount = 0;   // 取引明細書印字カウンタ
    private static Integer detailStatementPrintMax = 0;     // 取引明細書印字最大回数


    private static String isMaskCardId;
    private static String isDeviceCheckResult;

    private  static MenuCashChangerViewModel.AmountValue isAmountValue;

    private static int isPrintStatus = PrinterConst.PrintStatus_IDLE;

    private DeviceClient.ResultWAON isResultWAON;
    private BaseEMoneyOkicaViewModel.HistoryData isHistoryDataOKICA;
    private String isOkicaHistoryPrintDateTime;

//    private static IFBoxManager _ifBoxManager;
//    public void setIFBoxManager( IFBoxManager ifBoxManager) {
//        _ifBoxManager = ifBoxManager ;
//    }
//    //ADD-S BMT S.Oyama 2024/11/18 フタバ双方向向け改修
//    public IFBoxManager getIFBoxManager() {
//        return _ifBoxManager;
//    }
//    //ADD-E BMT S.Oyama 2024/11/18 フタバ双方向向け改修

    private SlipData _latestSlipData = null;

    private Handler _handler = new Handler(Looper.getMainLooper());
    private Runnable _printNext;
    //ADD-S BMT S.Oyama 2025/01/28 フタバ双方向向け改修
    private Handler _handlerErrorDialog = new Handler(Looper.getMainLooper());
    private Runnable _printErrorDialog;
    //ADD-E BMT S.Oyama 2024/01/28 フタバ双方向向け改修
    private static List<Integer> _historySlipIds;

    private static Boolean isTransResultUnFinish = false;

    //ADD-S BMT S.Oyama 2024/10/31 フタバ双方向向け改修
    private static Disposable _meterDataV4ErrorDisposable = null;
    private static Disposable _meterDataV4InfoDisposable = null;
    private AlertDialog _alertDialogCtrl = null;                        //アラートダイアログ制御用(フタバD勝手印刷用)
    //ADD-E BMT S.Oyama 2024/10/31 フタバ双方向向け改修


    public static PrinterManager getInstance() {
        if(_instance == null){
            _instance = new PrinterManager();
        }
        return _instance;
    }

    // 取引伝票印刷命令
    // view：ダイアログ表示用
    // id：伝票印刷関連データ(SQLite)のテーブルID番号
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void print_trans(View view, int id){
        // 印刷命令を１回のみ受令許可
        if(isPrintStatus != PrinterConst.PrintStatus_IDLE)return;
        changePrintStatus(PrinterConst.PrintStatus_PRINTING);
        _view = view;
        isSlipDataId = id;
        isSlipType = PrinterConst.SlipType_Trans;
        isSlipCopy = PrinterConst.SlipCopy_Merchant;
        isMaskCardId = null;
        isTransResultUnFinish = false;

        isDemo();
        showPrintingDialog();

        new Thread(new Runnable() {
            @Override
            public void run() {
                PrinterProc printerProc = PrinterProc.getInstance();
                printerProc.printTrans(isSlipDataId, PrinterConst.SlipCopy_Merchant);
            }
        }).start();

    }


    //ADD-S BMT S.Oyama 2024/11/12 フタバ双方向向け改修
    /******************************************************************************/
    /*!
     * @brief  印刷の実施：分別の印字本体電文送出処理（フタバ双方向用）
     * @note   印刷の実施：分別の印字本体電文送出処理
     * @param [in] View view
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void print_transFutabaDSeparation2nd(View view){
        // 印刷命令を１回のみ受令許可
        //if(isPrintStatus != PrinterConst.PrintStatus_IDLE)return;
        changePrintStatus(PrinterConst.PrintStatus_PRINTING);
        _view = view;
        //isSlipDataId = id;
        isSlipType = PrinterConst.SlipType_Trans;
        isSlipCopy = PrinterConst.SlipCopy_Merchant;
        isMaskCardId = null;
        isTransResultUnFinish = false;

        isDemo();
        //showPrintingDialog();

        new Thread(new Runnable() {
            @Override
            public void run() {
                PrinterProc printerProc = PrinterProc.getInstance();
                //printerProc.printTrans(isSlipDataId, PrinterConst.SlipCopy_Merchant);
                printerProc.printTransFutabaD_Separation2ndSend();
            }
        }).start();

    }
    //ADD-E BMT S.Oyama 2024/11/12 フタバ双方向向け改修

    // 取引伝票印刷命令
    // view：ダイアログ表示用
    // id：伝票印刷関連データ(SQLite)のテーブルID番号
    @RequiresApi(api = Build.VERSION_CODES.N)
    public boolean prepaid_print_trans(View view, int[] ids){
        // 印刷命令を１回のみ受令許可
        if(isPrintStatus != PrinterConst.PrintStatus_IDLE)return false;
        changePrintStatus(PrinterConst.PrintStatus_PRINTING);
        _view = view;
        isSlipDataIds = ids;
        isSlipType = PrinterConst.SlipType_Prepaid;
        isSlipCopy = PrinterConst.SlipCopy_Merchant;
        isMaskCardId = null;
        isTransResultUnFinish = false;

        isDemo();
        showPrintingDialog();

        final boolean[] isPrepaidCardSale = {false};

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                PrinterProc printerProc = PrinterProc.getInstance();
                isPrepaidCardSale[0] = printerProc.printPrepaidTrans(ids, PrinterConst.SlipCopy_Merchant);
            }
        });

        thread.start();

        try {
            thread.join();
            return isPrepaidCardSale[0];
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    //ADD-S BMT S.Oyama 2025/03/21 フタバ双方向向け改修
    /******************************************************************************/
    /*!
     * @brief  プリペイド処理時，メータ側で金額変更された場合にエラー表示させる
     * @note
     * @param なし
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    public void prepaid_MeterAmountChangeErr()
    {

        dismissPrintingDialog();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {

                _view.post(() -> {
                    String errMsg = "プリペイド決済中にメータ金額が変更されました。\nレシート印刷は実施しません";
                    String setTitle = "プリペイド決済警告";

                    AlertDialog.Builder alertDialog = new AlertDialog.Builder(_view.getContext());
                    alertDialog.setTitle(setTitle);        // タイトル設定
                    alertDialog.setMessage(errMsg);  // 内容(メッセージ)設定
                    alertDialog.setCancelable(true);       // キャンセル有効
                    // はいボタンの設定
                    alertDialog.setPositiveButton("はい", new DialogInterface.OnClickListener() {
                        @RequiresApi(api = Build.VERSION_CODES.N)
                        public void onClick(DialogInterface dialog, int which) {
                            // ダイアログを閉じる
                            //dialog.dismiss();
                        }
                    });
                    // ダイアログを表示
                    //alertDialog.create().show();

                    alertDialog.show();
                    changePrintStatus(PrinterConst.PrintStatus_IDLE);
                });
            }
        };

        Timer timer = new Timer();
        timer.schedule(timerTask, 1500);
    }
    //ADD-E BMT S.Oyama 2025/03/21 フタバ双方向向け改修

    //ADD-S BMT S.Oyama 2025/02/18 フタバ双方向向け改修
    /******************************************************************************/
    /*!
     * @brief  プリペイド処理時，単品isSlipDataIdを設定する
     * @note   プリペイド処理時，単品isSlipDataIdを設定する（updatePrintCntにて使用するため）
     * @param なし
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    public void setprepaid_print_trans_fix_IsSlipDataId(int tmpNewSlipdataID){
        isSlipDataId = tmpNewSlipdataID;
    }
    //ADD-E BMT S.Oyama 2025/02/18 フタバ双方向向け改修

    //ADD-S BMT S.Oyama 2025/03/17 フタバ双方向向け改修
    /******************************************************************************/
    /*!
     * @brief  現処理slipidのブランド名を返す（プリペイド処理時の詳細情報が欲しいため）
     * @note   現処理slipidのブランド名を返す（プリペイド処理時の詳細情報が欲しいため）
     * @param なし
     * @retval なし
     * @return      第0要素：ブランド名　第１要素：transType
     * @private
     */
    /******************************************************************************/
    public String[] getBrandNameFromSlipIDForPrepaid(){
        PrinterProc printerProc = PrinterProc.getInstance();

        // プリペイド用なのでslipDataIdは配列の方を使う、他マネーでは使わない想定
        // 複数IDがある場合があるが、その時は最後の取引を見る（支払いが最後になるパターンがあるため）
        return printerProc.getBrandNameFromSlipID(isSlipDataIds[isSlipDataIds.length-1]);
    }
    //ADD-E BMT S.Oyama 2025/03/17 フタバ双方向向け改修


    // 現段階では、print_req コマンドは未使用
    //@RequiresApi(api = Build.VERSION_CODES.N)
    //public void req_trans_info() {
    //    // メーターからの決済明細情報要求
    //    Thread updatePrintCnt = new Thread(new Runnable() {
    //        @Override
    //        public void run() {
    //            // 最新の印字データ取得
    //            _latestSlipData = DBManager.getSlipDao().getLatestOne();
    //        }
    //    });
    //    updatePrintCnt.start();

    //    try {
    //        updatePrintCnt.join();
    //        if (_latestSlipData != null && _view != null) {
    //            // メーターへ最新の印字データを送信
    //            print_trans(_view, _latestSlipData.id);
    //        }
    //        _latestSlipData = null;
    //    } catch (Exception e) {
    //        e.printStackTrace();
    //    }
    //}

    // 現金決済時の取引明細書印刷命令
    // view：ダイアログ表示用
    // id：伝票印刷関連データ(SQLite)のテーブルID番号
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void print_trans_cash(View view, int id) {
        // 印刷命令を１回のみ受令許可
        if(isPrintStatus != PrinterConst.PrintStatus_IDLE)return;
        changePrintStatus(PrinterConst.PrintStatus_PRINTING);
        _view = view;
        isSlipDataId = id;
        isSlipType = PrinterConst.SlipType_TransCash_DetailStatement;
        isTransResultUnFinish = false;
        detailStatementPrintMax = 1;
        detailStatementPrintCount = 1;
        if (AppPreference.isPosTransaction()) detailStatementPrintMax = getDetailStatementPrintMax();

        isDemo();
        showPrintingDialog();

        new Thread(new Runnable() {
            @Override
            public void run() {
                PrinterProc printerProc = PrinterProc.getInstance();
                if (AppPreference.isPosTransaction()) {
                    printerProc.printDetailStatement(isSlipDataId);
                }

                if (AppPreference.isTicketTransaction()) {
                    printerProc.printTicketDetailStatement(isSlipDataId);
                }
            }
        }).start();
    }

    // 取消票印刷命令
    // view：ダイアログ表示用
    // id：伝票印刷関連データ(SQLite)のテーブルID番号
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void print_cancel_ticket(View view, int id) {
        // 印刷命令を１回のみ受令許可
        if(isPrintStatus != PrinterConst.PrintStatus_IDLE)return;
        changePrintStatus(PrinterConst.PrintStatus_PRINTING);
        _view = view;
        isSlipDataId = id;
        isSlipType = PrinterConst.SlipType_CancelTicket;
        isTransResultUnFinish = false;
        detailStatementPrintMax = 1;
        detailStatementPrintCount = 1;
        if (AppPreference.isPosTransaction()) detailStatementPrintMax = getDetailStatementPrintMax();

        isDemo();
        showPrintingDialog();

        new Thread(new Runnable() {
            @Override
            public void run() {
                PrinterProc printerProc = PrinterProc.getInstance();
                if (AppPreference.isPosTransaction()) {
                    printerProc.printCancelTicket(isSlipDataId);
                }

                if (AppPreference.isTicketTransaction()) {
                    printerProc.printTicketCancel(isSlipDataId);
                }
            }
        }).start();
    }

    // 履歴照会伝票印刷命令（WAON専用）
    // view：ダイアログ表示用
    // resultWAON：
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void print_trans_history_waon(View view, DeviceClient.ResultWAON resultWAON){
        // 印刷命令を１回のみ受令許可
        if(isPrintStatus != PrinterConst.PrintStatus_IDLE)return;
        changePrintStatus(PrinterConst.PrintStatus_PRINTING);
        _view = view;
        isSlipType = PrinterConst.SlipType_TransHistory_Waon;
        isTransResultUnFinish = false;
        isResultWAON = resultWAON;

        isDemo();
        showPrintingDialog();

        new Thread(new Runnable() {
            @Override
            public void run() {
                PrinterProc printerProc = PrinterProc.getInstance();
                printerProc.printTransHistory_WAON(isResultWAON);
            }
        }).start();

    }

    //ADD-S BMT S.Oyama 2024/10/31 フタバ双方向向け改修
    /******************************************************************************/
    /*!
     * @brief  履歴照会伝票印刷命令（WAON専用）（フタバ双方向用）
     * @note   履歴照会伝票印刷命令（WAON専用）
     * @param [in] View view
     *              DeviceClient.ResultWAON resultWAON WAON取引情報
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void print_trans_history_waon_FutabaD(View view, DeviceClient.ResultWAON resultWAON){
        // 印刷命令を１回のみ受令許可
        if(isPrintStatus != PrinterConst.PrintStatus_IDLE)return;
        changePrintStatus(PrinterConst.PrintStatus_PRINTING);
        _view = view;
        isSlipType = PrinterConst.SlipType_TransHistory_Waon;
        isTransResultUnFinish = false;
        isResultWAON = resultWAON;

//        if (_ifBoxManager == null) {
//            PrinterDuplexError(PrinterConst.DuplexPrintStatus_IFBOXERROR);       //IFBOX接続エラー
//            return;
//        }
//
//        if (_ifBoxManager.getIsConnected820() == false)             //820未接続の場合
//        {
//            PrinterDuplexError(PrinterConst.DuplexPrintStatus_IFBOXERROR);       //IFBOX接続エラー
//            return;
//        }

        DeviceClient.HistoryData[] historyData = new DeviceClient.HistoryData[3];
        if(isResultWAON.addInfo == null) isResultWAON.addInfo = new DeviceClient.AddInfo();
        for(int i = 0; i < 3; i++) {
            if(isResultWAON.addInfo.historyData != null && isResultWAON.addInfo.historyData.length > i) {
                historyData[i] = isResultWAON.addInfo.historyData[i];
            } else {

                DeviceClient.HistoryData hd = new DeviceClient.HistoryData();
                hd.tradeTypeCode = "00";
                hd.historyDate = "0001010000";
                hd.chargeValue = "0";
                hd.value       = "0";
                hd.balance     = "0";
                hd.terminalId  = "0";
                hd.cardThroughNum = "0";
                historyData[i] = hd;
            }
        }
        isResultWAON.addInfo.historyData = historyData;

//        IFBoxManager.SendMeterDataInfo_FutabaD tmpSend820Info = new IFBoxManager.SendMeterDataInfo_FutabaD();
//        tmpSend820Info.StatusCode = IFBoxManager.SendMeterDataStatus_FutabaD.NONE;
//        tmpSend820Info.IsLoopBreakOut = false;
//        tmpSend820Info.ErrorCode820 = IFBoxManager.SendMeterDataStatus_FutabaD.NONE;


        isDemo();
        showPrintingDialog();

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
//                _meterDataV4InfoDisposable = _ifBoxManager.getMeterDataV4().subscribeOn(
//                        Schedulers.io()).observeOn(Schedulers.newThread()).subscribe(meter -> {     //AndroidSchedulers.mainThread()  Schedulers.newThread() Schedulers.io()
//                    Timber.i("[FUTABA-D]print_trans_history_waon_FutabaD():750<-820 meter_data event cmd:%d ", meter.meter_sub_cmd);
//                    if (meter.meter_sub_cmd == 9) {
//                        if ((meter.zandaka_flg != null) && (meter.zandaka_flg == 2)) {             //残高フラグが取引履歴照会(2)の場合WAON歴通知
//                            Timber.i("[FUTABA-D]print_trans_history_waon_FutabaD():Waon History print  event ");
//                            tmpSend820Info.StatusCode = IFBoxManager.SendMeterDataStatus_FutabaD.SENDOK;             //ACKが返ってきた場合
//                        }
//                    }
//                });
//
//                _meterDataV4ErrorDisposable = _ifBoxManager.getMeterDataV4Error().subscribeOn(
//                        Schedulers.io()).observeOn(Schedulers.newThread()).subscribe(error -> {         //送信中にエラー受信(タイムアウト，切断)
//                    Timber.e("[FUTABA-D]navigateToDiscountJob():Error event ErrCD:%d 820ErrCD:%d ", error.ErrorCode, error.ErrorCode820);
//                    tmpSend820Info.StatusCode = error.ErrorCode;
//                    tmpSend820Info.ErrorCode820 = error.ErrorCode820;
//
//                });
//
//                _ifBoxManager.send820_WaonHistoryStart( );               //WAON歴印刷開始要求 820へ
//
//                for (int i = 0; i < (PrinterConst.DuplexPrintResponseTimerSec + 1) * 10; i++)        //最大26秒ほど待ってみる
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

            if (_meterDataV4InfoDisposable != null) {       //コールバック系を後始末
                _meterDataV4InfoDisposable.dispose();
                _meterDataV4InfoDisposable = null;
            }

            if (_meterDataV4ErrorDisposable != null)        //コールバック系を後始末
            {
                _meterDataV4ErrorDisposable.dispose();
                _meterDataV4ErrorDisposable = null;
            }

//            if (tmpSend820Info.IsLoopBreakOut == false) {                             //820から何も返却されなかった場合のループアウト
//                dismissPrintingDialog();
//                PrinterDuplexError(PrinterConst.DuplexPrintStatus_IFBOXERROR);       //IFBOX接続エラー
//                return;
//            } else {
//                switch (tmpSend820Info.StatusCode)                       //ステータスコードのチェック
//                {
//                    case IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_NOTCONNECTED:       //切断
//                        dismissPrintingDialog();
//                        PrinterDuplexError(PrinterConst.DuplexPrintStatus_IFBOXERROR);       //IFBOX接続エラー
//                        return;
//                    case IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_TIMEOUT:           //タイムアウト
//                        dismissPrintingDialog();
//                        PrinterDuplexError(PrinterConst.DuplexPrintStatus_IFBOXERROR_TIMEOUT);       //IFBOXタイムアウトエラー
//                        return;
//                    case IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_820NACK:              //820内でが返ってきた場合
//                        dismissPrintingDialog();
//                        PrinterDuplexError(PrinterConst.DuplexPrintStatus_IFBOXERROR);       //IFBOX接続エラー
//                        Timber.e("[FUTABA-D]820 Inner error! ErrCD:%d", tmpSend820Info.ErrorCode820);
//                        return;
//
//                    default:
//                        //ここに到達する場合は，エラー無しでWAON歴印刷開始要求が送信されたことを意味する
//                        break;
//                }
//            }
            //ADD-E BMT S.Oyama 2024/10/11 フタバ双方向向け改修

        } catch (Exception e) {
            Timber.e(e);
            dismissPrintingDialog();
            return ;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                PrinterProc printerProc = PrinterProc.getInstance();
                printerProc.printTransHistory_WAON(isResultWAON);
            }
        }).start();
    }

    //ADD-E BMT S.Oyama 2024/10/31 フタバ双方向向け改修



    // 残高履歴伝票印刷命令（OKICA専用）
    // view：ダイアログ表示用
    // historyData：
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void print_trans_history_okica(View view, BaseEMoneyOkicaViewModel.HistoryData historyData){
        // 印刷命令を１回のみ受令許可
        if(isPrintStatus != PrinterConst.PrintStatus_IDLE)return;
        changePrintStatus(PrinterConst.PrintStatus_PRINTING);
        _view = view;
        isSlipType = PrinterConst.SlipType_TransHistory_Okica;
        isTransResultUnFinish = false;
        isHistoryDataOKICA = historyData;
        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy/MM/dd              HH:mm:ss", Locale.JAPANESE);
        isOkicaHistoryPrintDateTime = dateFmt.format(new Date());

        isDemo();
        showPrintingDialog();

        new Thread(new Runnable() {
            @Override
            public void run() {
                PrinterProc printerProc = PrinterProc.getInstance();
                printerProc.printTransHistory_OKICA(isHistoryDataOKICA, isOkicaHistoryPrintDateTime);
            }
        }).start();

    }

    // 集計印刷命令
    // view：ダイアログ表示用　
    // Order：未印刷(0), 過去の最新(1), ・・・ , 過去の最古(5)
    // Detail：true（取引・処理未了・明細）,false（取引・処理未了）
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void print_Aggregate(View view, int Order, Boolean Detail){
        // 印刷命令を１回のみ受令許可
        if(isPrintStatus != PrinterConst.PrintStatus_IDLE)return;
        changePrintStatus(PrinterConst.PrintStatus_PRINTING);
        _view = view;
        isAggregateOrder = Order;
        isSlipType = PrinterConst.SlipType_Aggregate;
        isTransResultUnFinish = false;

        if(Detail == true){
            isAggregateType = PrinterConst.AggregateType_Detail;
        }else{
            isAggregateType = PrinterConst.AggregateType_NoDetail;
        }

        isDemo();
        showPrintingDialog();

        new Thread(new Runnable() {
            @Override
            public void run() {
                PrinterProc printerProc = PrinterProc.getInstance();
                printerProc.printAggregate(isAggregateOrder, isAggregateType);
            }
        }).start();

    }

    // 集計印刷命令(フタバ双方向連動時の業務終了だけ)
    // view：ダイアログ表示用　
    // Order：未印刷(0), 過去の最新(1), ・・・ , 過去の最古(5)
    // Detail：true（取引・処理未了・明細）,false（取引・処理未了）
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void print_AggregateForFutabaDBusinessEnd(View view, int Order, Boolean Detail){
        // 印刷命令を１回のみ受令許可
        if(isPrintStatus != PrinterConst.PrintStatus_IDLE)return;
        changePrintStatus(PrinterConst.PrintStatus_PRINTING);
        _view = view;
        isAggregateOrder = Order;
        isSlipType = PrinterConst.SlipType_Aggregate;
        isTransResultUnFinish = false;

        if(Detail == true){
            isAggregateType = PrinterConst.AggregateType_Detail;
        }else{
            isAggregateType = PrinterConst.AggregateType_NoDetail;
        }

        isDemo();
        showPrintingDialog();

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                PrinterProc printerProc = PrinterProc.getInstance();
                printerProc.printAggregate(isAggregateOrder, isAggregateType);
            }
        });

        thread.start();
        try {
            thread.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 領収書印刷命令
    // view：ダイアログ表示用
    // slipDataId：伝票印刷関連データ(SQLite)のテーブルID番号
    // isDetail：true（明細付）,false（金額のみ）
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void print_receipt(View view, int id, boolean isDetail){
        // 印刷命令を１回のみ受令許可
        if(isPrintStatus != PrinterConst.PrintStatus_IDLE)return;
        changePrintStatus(PrinterConst.PrintStatus_PRINTING);
        _view = view;
        isSlipDataId = id;
        isReceiptDetail = isDetail;
        isSlipType = PrinterConst.SlipType_Receipt;
        isTransResultUnFinish = false;

        isDemo();
        showPrintingDialog();

        new Thread(new Runnable() {
            @Override
            public void run() {
                PrinterProc printerProc = PrinterProc.getInstance();
                if (AppPreference.isPosTransaction()) {
                    printerProc.printReceipt(isSlipDataId, isReceiptDetail);
                }

                if (AppPreference.isTicketTransaction()) {
                    printerProc.printTicketReceipt(isSlipDataId, isReceiptDetail);
                }
            }
        }).start();

    }

    // QR券印刷命令
    // view：ダイアログ表示用
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void print_QRTicket(View view) {
        // 印刷命令を１回のみ受令許可
        if(isPrintStatus != PrinterConst.PrintStatus_IDLE)return;
        changePrintStatus(PrinterConst.PrintStatus_PRINTING);
        _view = view;
        isSlipType = PrinterConst.SlipType_QRTicket;
        isTransResultUnFinish = false;

        isDemo();
        showPrintingDialog();

        new Thread(new Runnable() {
            @Override
            public void run() {
                PrinterProc printerProc = PrinterProc.getInstance();
                printerProc.printQRTicketReceipt();
            }
        }).start();
    }

    // 自動釣銭機機内残高印刷
    // view：ダイアログ表示用　
    // CheckResult：デバイスチェック結果
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void print_cash_history(View view, MenuCashChangerViewModel.AmountValue AmountValue){
        // 印刷命令を１回のみ受令許可
        if(isPrintStatus != PrinterConst.PrintStatus_IDLE)return;
        changePrintStatus(PrinterConst.PrintStatus_PRINTING);
        _view = view;
        isSlipType = PrinterConst.SlipType_CashHistory;
        isAmountValue = AmountValue;

        isDemo();
        showPrintingDialog();

        new Thread(new Runnable() {
            @Override
            public void run() {
                PrinterProc printerProc = PrinterProc.getInstance();
                printerProc.CashHistory(isAmountValue);
            }
        }).start();

    }

    // デバイスチェック結果
    // view：ダイアログ表示用　
    // CheckResult：デバイスチェック結果
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void setPrintData_DeviceCheckResult(View view, String CheckResult){
        // 印刷命令を１回のみ受令許可
        if(isPrintStatus != PrinterConst.PrintStatus_IDLE)return;
        changePrintStatus(PrinterConst.PrintStatus_PRINTING);
        _view = view;
        isSlipType = PrinterConst.SlipType_DeviceCheck;
        isTransResultUnFinish = false;
        isDeviceCheckResult = CheckResult;

        isDemo();
        showPrintingDialog();

        new Thread(new Runnable() {
            @Override
            public void run() {
                PrinterProc printerProc = PrinterProc.getInstance();
                printerProc.PrintDeviceCheckResult(isDeviceCheckResult);
            }
        }).start();

    }

    //ADD-S BMT S.Oyama 2024/11/14 フタバ双方向向け改修
    public void showPrintingDialogExt(){
        showPrintingDialog();
    }
    //ADD-E BMT S.Oyama 2024/11/14 フタバ双方向向け改修

    // 伝票印刷用のダイアログ表示
    @SuppressWarnings("deprecation")
    private void showPrintingDialog(){
        _progressDialog = new ProgressDialog(_view.getContext());
        _progressDialog.setMessage("伝票印刷中 ・・・ ");                   // 内容(メッセージ)設定
        _progressDialog.setCancelable(false);                              // キャンセル無効
        _progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);    // スタイル設定
        _progressDialog.show();                                            // ダイアログを表示

        Timber.tag("Printer").i("ダイヤログ表示「伝票印刷中 ・・・ 」");
    }

    //ADD-S BMT S.Oyama 2024/11/06 フタバ双方向向け改修
    public void dismissPrintingDialogExt() {
        dismissPrintingDialog();
    }
    //ADD-E BMT S.Oyama 2024/11/06 フタバ双方向向け改修

    // 伝票印刷用のダイアログ閉じる
    private void dismissPrintingDialog(){
        Timber.tag("Printer").i("ダイヤログ閉じる「伝票印刷中 ・・・ 」");
        //ADD-S BMT S.Oyama 2024/10/22 フタバ双方向向け改修
        if (_progressDialog != null) {
            _progressDialog.dismiss();
            _progressDialog = null;
        }
        //ADD-E BMT S.Oyama 2024/10/22 フタバ双方向向け改修
    }

    //ADD-S BMT S.Oyama 2024/11/11 フタバ双方向向け改修
    /******************************************************************************/
    /*!
     * @brief  次印刷確認系のダイアログを閉じる（フタバ双方向用）
     * @note   次印刷確認系のダイアログを閉じる
     * @param [in] なし
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    private void dismissNextPrintDialog() {
        Timber.i("[FUTABA-D]印刷確認系ダイアログを閉じる");
        if (_alertDialogCtrl != null) {
            _alertDialogCtrl.dismiss();
            _alertDialogCtrl = null;
        }
        _alertDialog = null;
    }
    //ADD-E BMT S.Oyama 2024/11/11 フタバ双方向向け改修

    // お客様控えの伝票印刷確認（ダイアログ）
    private void showCheckDialog(){
        _alertDialog = new AlertDialog.Builder(_view.getContext());
        isSlipCopy = PrinterConst.SlipCopy_Customer;

        // ダイアログの設定
        _alertDialog.setTitle("お客様控え");                 // タイトル設定
        _alertDialog.setMessage("伝票印刷を開始しますか？");  // 内容(メッセージ)設定
        _alertDialog.setCancelable(false);                  // キャンセル無効

        // はいボタンの設定
        _alertDialog.setPositiveButton("はい", new DialogInterface.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            public void onClick(DialogInterface dialog, int which) {
                // 印刷命令を１回のみ受令許可
                if(isPrintStatus != PrinterConst.PrintStatus_PRINTWAITING)return;
                changePrintStatus(PrinterConst.PrintStatus_PRINTING);
                CommonClickEvent.RecordClickOperation("はい", "お客様控え印刷", true);
                showPrintingDialog();

                //ADD-S BMT S.Oyama 2024/11/11 フタバ双方向向け改修
                _alertDialog = null;
                _alertDialogCtrl = null;
                //ADD-E BMT S.Oyama 2024/11/11 フタバ双方向向け改修

                // はいボタン押下時に、お客様控えを印刷開始
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        PrinterProc printerProc = PrinterProc.getInstance();
                        if (isSlipType == PrinterConst.SlipType_Prepaid) {
                            printerProc.printPrepaidTrans(isSlipDataIds, PrinterConst.SlipCopy_Customer);
                        } else {
                            printerProc.printTrans(isSlipDataId, PrinterConst.SlipCopy_Customer);
                        }
                    }
                }).start();
            }
        });

        // ダイアログを表示
        _alertDialogCtrl = _alertDialog.show();

        Timber.tag("Printer").i("ダイヤログ表示「お客様控え 伝票印刷を開始しますか？」");
    }

    // 取引明細書の伝票印刷確認（ダイアログ）
    private void showCheckDetailStatementDialog(Integer count, Integer max){
        _alertDialog = new AlertDialog.Builder(_view.getContext());

        // ダイアログの設定
        _alertDialog.setTitle("取引明細書");                 // タイトル設定
        _alertDialog.setMessage("伝票印刷を開始しますか？(" + count.toString() + "/" + max.toString() + ")");  // 内容(メッセージ)設定
        _alertDialog.setCancelable(false);                  // キャンセル無効

        // はいボタンの設定
        _alertDialog.setPositiveButton("はい", new DialogInterface.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            public void onClick(DialogInterface dialog, int which) {
                // 印刷命令を１回のみ受令許可
                if(isPrintStatus != PrinterConst.PrintStatus_PRINTWAITING)return;
                changePrintStatus(PrinterConst.PrintStatus_PRINTING);
                CommonClickEvent.RecordClickOperation("はい", "取引明細書印刷(" + count.toString() + "/" + max.toString()+ ")", true);
                showPrintingDialog();

                //ADD-S BMT S.Oyama 2024/11/11 フタバ双方向向け改修
                _alertDialog = null;
                _alertDialogCtrl = null;
                //ADD-E BMT S.Oyama 2024/11/11 フタバ双方向向け改修

                // はいボタン押下時に、取引明細書を印刷開始
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        PrinterProc printerProc = PrinterProc.getInstance();
                        if (AppPreference.isPosTransaction()) {
                            printerProc.printDetailStatement(isSlipDataId);
                        }
                        if (AppPreference.isTicketTransaction()) {
                            printerProc.printTicketDetailStatement(isSlipDataId);
                        }
                    }
                }).start();
            }
        });

        // ダイアログを表示
        _alertDialogCtrl = _alertDialog.show();

        Timber.tag("Printer").i("ダイヤログ表示「取引明細書 伝票印刷を開始しますか？」");
    }

    // 取消の伝票印刷確認（ダイアログ）
    private void showCheckCancelTicketDialog(Integer count, Integer max){
        _alertDialog = new AlertDialog.Builder(_view.getContext());

        // ダイアログの設定
        _alertDialog.setTitle("取消票");                 // タイトル設定
        _alertDialog.setMessage("伝票印刷を開始しますか？(" + count.toString() + "/" + max.toString() + ")");  // 内容(メッセージ)設定
        _alertDialog.setCancelable(false);                  // キャンセル無効

        // はいボタンの設定
        _alertDialog.setPositiveButton("はい", new DialogInterface.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            public void onClick(DialogInterface dialog, int which) {
                // 印刷命令を１回のみ受令許可
                if(isPrintStatus != PrinterConst.PrintStatus_PRINTWAITING)return;
                changePrintStatus(PrinterConst.PrintStatus_PRINTING);
                CommonClickEvent.RecordClickOperation("はい", "取消票印刷(" + count.toString() + "/" + max.toString()+ ")", true);
                showPrintingDialog();
                //ADD-S BMT S.Oyama 2024/11/11 フタバ双方向向け改修
                _alertDialog = null;
                _alertDialogCtrl = null;
                //ADD-E BMT S.Oyama 2024/11/11 フタバ双方向向け改修

                // はいボタン押下時に、取消票を印刷開始
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        PrinterProc printerProc = PrinterProc.getInstance();
                        if (AppPreference.isPosTransaction()) {
                            printerProc.printCancelTicket(isSlipDataId);
                        }
                        if (AppPreference.isTicketTransaction()) {
                            printerProc.printTicketCancel(isSlipDataId);
                        }
                    }
                }).start();
            }
        });

        // ダイアログを表示
        _alertDialogCtrl = _alertDialog.show();

        Timber.tag("Printer").i("ダイヤログ表示「取消票 伝票印刷を開始しますか？」");
    }

    // 伝票印刷継続確認（ダイアログ）
    private void showCheckContinueDialog(){
        _alertDialog = new AlertDialog.Builder(_view.getContext());
        isSlipCopy = PrinterConst.SlipCopy_Customer;

        // ダイアログの設定
        _alertDialog.setTitle("伝票印刷");                  // タイトル設定
        _alertDialog.setMessage("次の伝票を印刷しますか？"); // 内容(メッセージ)設定
        _alertDialog.setCancelable(false);                 // キャンセル無効

        // はいボタンの設定
        _alertDialog.setPositiveButton("はい", new DialogInterface.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            public void onClick(DialogInterface dialog, int which) {
                //CHG-S BMT S.Oyama 2024/09/24 フタバ双方向向け改修
                //if (IFBoxAppModels.isMatch(IFBoxAppModels.OKABE_MS70_D)) {
                if (IFBoxAppModels.isMatch(IFBoxAppModels.OKABE_MS70_D) ) {
                    //CHG-E BMT S.Oyama 2024/09/24 フタバ双方向向け改修
                    _handler.removeCallbacks(_printNext);
                }
                // 印刷命令を１回のみ受令許可
                CommonClickEvent.RecordClickOperation("はい", "伝票印刷", true);

                showPrintingDialog();
                printNext();

                //ADD-S BMT S.Oyama 2024/11/11 フタバ双方向向け改修
                _alertDialog = null;
                _alertDialogCtrl = null;
                //ADD-E BMT S.Oyama 2024/11/11 フタバ双方向向け改修
            }
        });

        // ダイアログを表示
        _alertDialogCtrl = _alertDialog.show();

        Timber.tag("Printer").i("ダイヤログ表示「伝票印刷 次の伝票を印刷しますか？」");

        //CHG-S BMT S.Oyama 2024/09/24 フタバ双方向向け改修
        //if (IFBoxAppModels.isMatch(IFBoxAppModels.OKABE_MS70_D)) {
        if (IFBoxAppModels.isMatch(IFBoxAppModels.OKABE_MS70_D) ) {
            //CHG-E BMT S.Oyama 2024/09/24 フタバ双方向向け改修
            // 印刷確認画面で一定時間経過後、自動で次の伝票印刷へ進む
            _printNext = new Runnable() {
                @RequiresApi(api = Build.VERSION_CODES.N)
                @Override
                public void run() {
                    _alertDialogCtrl.dismiss();
                    showPrintingDialog();
                    printNext();

                    //ADD-S BMT S.Oyama 2024/11/11 フタバ双方向向け改修
                    _alertDialog = null;
                    _alertDialogCtrl = null;
                    //ADD-E BMT S.Oyama 2024/11/11 フタバ双方向向け改修
                }
            };

            // TODO M.Kodama 時間を調整
            _handler.postDelayed(_printNext, 18000);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void printNext() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // 09.22 t.wada 印刷可能状態に戻す
                changePrintStatus(PrinterConst.PrintStatus_PRINTING);

                //ADD-S BMT S.Oyama 2025/01/10 フタバ双方向向け改修
                if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == true ) {
                    PrinterProc printerProc = PrinterProc.getInstance();
                    if (isSlipType == PrinterConst.SlipType_Prepaid) {          //SlipTypeがプリペイドのときは，別処理となる
                        printerProc.printPrepaidTrans(isSlipDataIds, PrinterConst.SlipCopy_Customer);     //PrinterConst.SlipCopy_Merchant
                    } else {
                        printerProc.printTrans(isSlipDataId, PrinterConst.SlipCopy_Customer);
                    }
                } else {
                    PrinterProc printerProc = PrinterProc.getInstance();
                    printerProc.printTrans(isSlipDataId, PrinterConst.SlipCopy_Customer);
                }
                //ADD-E BMT S.Oyama 2025/01/10 フタバ双方向向け改修
            }
        }).start();
    }

    // 印刷データなし
    public void NoPrintData(){
        // 伝票印刷用のダイアログ閉じる
        dismissPrintingDialog();
        if(isSlipType == PrinterConst.SlipType_Aggregate && isAggregateOrder == 0){
            // 業務終了時
            deleteNowAggregateDate();
        }
        changePrintStatus(PrinterConst.PrintStatus_IDLE);
    }

    // 印刷データ異常（即時終了）
    public void PrintDataError(){
        // 伝票印刷用のダイアログ閉じる
        dismissPrintingDialog();
        changePrintStatus(PrinterConst.PrintStatus_IDLE);
    }

    // 印刷終了
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void PrintEnd(String MaskCardId, Integer TransResult, int PrintResult, Integer transType, boolean rePrinter){
        isMaskCardId = MaskCardId;
        isTransType = transType;
        isTransResult = TransResult;
        isRePrinter = rePrinter;

        //Timber.d("isSlipType = %s isTransType = %s isTransResult = %s isRePrinter = %s", isSlipType, isTransType, isTransResult, isRePrinter);

        // 伝票印刷用のダイアログ閉じる
        dismissPrintingDialog();
        if (PrintResult == Printer.PRINTER_OK) {
            // 正常に印刷終了
            switch (isSlipType) {
                case PrinterConst.SlipType_Trans:
                case PrinterConst.SlipType_Prepaid:
                    // 取引
                    if (isTransResult != null) {
                        if (isTransResult == PrinterConst.TransResult_OK) {
                            if (isSlipCopy == PrinterConst.SlipCopy_Merchant) {
                                changePrintStatus(PrinterConst.PrintStatus_PRINTWAITING);
                                // 取引伝票（加盟店控え）の印刷完了時
                                /* お客様控え伝票印刷の確認画面ダイアログを表示 */
                                showCheckDialog();
                            } else {
                                changePrintStatus(PrinterConst.PrintStatus_UPDATING);
                                // 取引伝票（お客様控え）の印刷完了時
                                if (!AppPreference.isServicePos() && !AppPreference.isServiceTicket()) {
                                    /* 印刷回数をカウント(+1) */
                                    updatePrintCnt();
                                    changePrintStatus(PrinterConst.PrintStatus_IDLE);
                                }

                                // お客様控え印刷の後に取引明細書を印刷する（POS用）
                                if(AppPreference.isPosTransaction()){
                                    detailStatementPrintMax = getDetailStatementPrintMax();
                                    if(0 < detailStatementPrintMax) {
                                        detailStatementPrintCount = 1;
                                        if(transType.equals(TransMap.TYPE_SALES)) {
                                            // お客様控え印刷の後に取引明細書を印刷する
                                            changePrintStatus(PrinterConst.PrintStatus_PRINTWAITING);
                                            isSlipType = PrinterConst.SlipType_DetailStatement;
                                            showCheckDetailStatementDialog(detailStatementPrintCount, detailStatementPrintMax);
                                        } else if(transType.equals(TransMap.TYPE_CANCEL)) {
                                            // お客様控え印刷の後に取消票を印刷する
                                            changePrintStatus(PrinterConst.PrintStatus_PRINTWAITING);
                                            isSlipType = PrinterConst.SlipType_CancelTicket;
                                            showCheckCancelTicketDialog(detailStatementPrintCount, detailStatementPrintMax);
                                        } else {
                                            // その他の場合
                                            /* 印刷回数をカウント(+1) */
                                            updatePrintCnt();
                                            changePrintStatus(PrinterConst.PrintStatus_IDLE);
                                        }
                                    }
                                }

                                // お客様控え印刷の後に取引明細書または取消票を印刷する（チケット用）
                                if (AppPreference.isTicketTransaction()) {
                                    detailStatementPrintMax = 1;
                                    detailStatementPrintCount = 1;
                                    changePrintStatus(PrinterConst.PrintStatus_PRINTWAITING);
                                    if (transType.equals(TransMap.TYPE_SALES)) {
                                        // 取引明細書を印刷する
                                        isSlipType = PrinterConst.SlipType_DetailStatement;
                                        showCheckDetailStatementDialog(detailStatementPrintCount, detailStatementPrintMax);
                                    } else if(transType.equals(TransMap.TYPE_CANCEL)) {
                                        // 取消票を印刷する
                                        isSlipType = PrinterConst.SlipType_CancelTicket;
                                        showCheckCancelTicketDialog(detailStatementPrintCount, detailStatementPrintMax);
                                    } else {
                                        // その他の場合
                                        /* 印刷回数をカウント(+1) */
                                        updatePrintCnt();
                                        changePrintStatus(PrinterConst.PrintStatus_IDLE);
                                    }
                                }
                                // フタバ双方向手動モードから双方向モードへの変更
                                //_ifBoxManager.printEndManual(true);
                            }
                        } else if (isTransResult == PrinterConst.TransResult_UnFinished) {
                            changePrintStatus(PrinterConst.PrintStatus_UPDATING);
                            // 処理未了伝票（加盟店控え）の印刷完了時
                            /* カード番号マスク更新 */
                            updateMaskCardId();
                            if (!AppPreference.isServicePos() && !AppPreference.isServiceTicket()) {
                                /* 印刷回数をカウント(+1) */
                                updatePrintCnt();
                                changePrintStatus(PrinterConst.PrintStatus_IDLE);
                            }

                            // 処理未了伝票印刷の後に取引明細書を印刷する（POS用）
                            if(AppPreference.isPosTransaction()){
                                detailStatementPrintMax = getDetailStatementPrintMax();
                                if(0 < detailStatementPrintMax) {
                                    detailStatementPrintCount = 1;
                                    if(transType.equals(TransMap.TYPE_SALES)) {
                                        // 未了伝票印刷の後に取引明細書を印刷する
                                        changePrintStatus(PrinterConst.PrintStatus_PRINTWAITING);
                                        isSlipType = PrinterConst.SlipType_DetailStatement;
                                        showCheckDetailStatementDialog(detailStatementPrintCount, detailStatementPrintMax);
                                    } else if(transType.equals(TransMap.TYPE_CANCEL)) {
                                        // 未了伝票印刷の後に取消票を印刷する
                                        changePrintStatus(PrinterConst.PrintStatus_PRINTWAITING);
                                        isSlipType = PrinterConst.SlipType_CancelTicket;
                                        showCheckCancelTicketDialog(detailStatementPrintCount, detailStatementPrintMax);
                                    } else {
                                        // その他の場合
                                        /* 印刷回数をカウント(+1) */
                                        updatePrintCnt();
                                        changePrintStatus(PrinterConst.PrintStatus_IDLE);
                                    }
                                }
                            }

                            // 処理未了伝票印刷の後に取引明細書を印刷する（チケット用）
                            if (AppPreference.isTicketTransaction()) {
                                detailStatementPrintMax = 1;
                                detailStatementPrintCount = 1;
                                changePrintStatus(PrinterConst.PrintStatus_PRINTWAITING);
                                isTransResultUnFinish = true;
                                if (transType.equals(TransMap.TYPE_SALES)) {
                                    // 取引明細書を印刷する
                                    isSlipType = PrinterConst.SlipType_DetailStatement;
                                    showCheckDetailStatementDialog(detailStatementPrintCount, detailStatementPrintMax);
                                } else if (transType.equals(TransMap.TYPE_CANCEL)) {
                                    // 取消票を印刷する
                                    isSlipType = PrinterConst.SlipType_CancelTicket;
                                    showCheckCancelTicketDialog(detailStatementPrintCount, detailStatementPrintMax);
                                } else {
                                    // その他の場合、印刷なし
                                    /* 印刷回数をカウント(+1) */
                                    updatePrintCnt();
                                    changePrintStatus(PrinterConst.PrintStatus_IDLE);
                                }
                            }
                        } else {
                            // その他
                            changePrintStatus(PrinterConst.PrintStatus_IDLE);
                        }
                    } else {
                        // その他
                        changePrintStatus(PrinterConst.PrintStatus_IDLE);
                    }
                    break;
                case PrinterConst.SlipType_Aggregate:
                    // 集計
                    if (isAggregateOrder == 0) {
                        changePrintStatus(PrinterConst.PrintStatus_UPDATING);
                        /* 業務終了のみ、集計印刷回数をカウント更新（5：過去の最古集計削除し、0から各カウント+1） */
                        /* updatePrintAggregateOrder()により削除されたslip_idに紐づくデータをpos_receiptから削除 */
                        updatePrintAggregateOrder();
                    } else {
                        // 集計履歴印刷時は、集計印刷回数をカウント更新なし
                    }
                    changePrintStatus(PrinterConst.PrintStatus_IDLE);
                    break;
                case PrinterConst.SlipType_DetailStatement:
                    // 取引明細
                    detailStatementPrintCount++;
                    // 取引明細書の印字最大回数まで取引明細書を印字する
                    if (detailStatementPrintCount <= detailStatementPrintMax) {
                        changePrintStatus(PrinterConst.PrintStatus_PRINTWAITING);
                        showCheckDetailStatementDialog(detailStatementPrintCount, detailStatementPrintMax);
                    }else{
                        /* 印刷回数をカウント(+1) */
                        updatePrintCnt();
                        changePrintStatus(PrinterConst.PrintStatus_IDLE);
                        if (AppPreference.isTicketTransaction() && !isRePrinter && !isTransResultUnFinish) {
                            // チケット販売時の初回取引明細書印刷完了
                            // QR発行画面に遷移する
                            NavigationWrapper.navigate(_view, R.id.action_navigation_menu_to_fragment_ticket_issue);
                        }
                    }
                    break;
                case PrinterConst.SlipType_TransCash_DetailStatement:
                    // 取引明細（現金決済）
                    detailStatementPrintCount++;
                    // 取引明細書の印字最大回数まで取引明細書を印字する
                    if (detailStatementPrintCount <= detailStatementPrintMax) {
                        changePrintStatus(PrinterConst.PrintStatus_PRINTWAITING);
                        showCheckDetailStatementDialog(detailStatementPrintCount, detailStatementPrintMax);
                    }else{
                        /* 印刷回数をカウント(+1) */
                        updatePrintCnt();
                        changePrintStatus(PrinterConst.PrintStatus_IDLE);
                    }
                    break;
                case PrinterConst.SlipType_CancelTicket:
                    // 取消票
                    detailStatementPrintCount++;
                    // 取引明細書の印字最大回数まで取引明細書を印字する
                    if (detailStatementPrintCount <= detailStatementPrintMax) {
                        changePrintStatus(PrinterConst.PrintStatus_PRINTWAITING);
                        showCheckCancelTicketDialog(detailStatementPrintCount, detailStatementPrintMax);
                    }else{
                        /* 印刷回数をカウント(+1) */
                        updatePrintCnt();
                        changePrintStatus(PrinterConst.PrintStatus_IDLE);
                    }
                    break;
                case PrinterConst.SlipType_Receipt:
                    // 領収書
                    updateReceiptPrintCnt();
                    changePrintStatus(PrinterConst.PrintStatus_IDLE);
                    break;
                case PrinterConst.SlipType_TransHistory_Waon:
                    // WAON履歴照会
                case PrinterConst.SlipType_TransHistory_Okica:
                    // OKICA残高履歴
                case PrinterConst.SlipType_DeviceCheck:
                    // デバイスチェック
                case PrinterConst.SlipType_QRTicket:
                    // QR券
                case PrinterConst.SlipType_CashHistory:
                    // 自動釣銭機機内残高印刷
                default:
                    // その他
                    changePrintStatus(PrinterConst.PrintStatus_IDLE);
                    break;
            }
        } else {
            if (AppPreference.getIsExternalPrinter()) {
                // 自動つり銭機連動
                if (PrintResult == Epos2CallbackCode.CODE_ERR_COVER_OPEN || PrintResult == Epos2CallbackCode.CODE_ERR_EMPTY) {
                    changePrintStatus(PrinterConst.PrintStatus_PAPERLACKING);
                } else {
                    changePrintStatus(PrinterConst.PrintStatus_ERROR);
                }
                // プリンターエラー発生
                PrinterCashChangerError(PrintResult);
            } else {
                if (PrintResult == Printer.PRINTER_STATUS_PAPER_LACK) {
                    changePrintStatus(PrinterConst.PrintStatus_PAPERLACKING);
                } else {
                    changePrintStatus(PrinterConst.PrintStatus_ERROR);
                }
                // プリンターエラー発生
                PrinterError(PrintResult);
            }
        }
    }

    // プリンターエラー表示
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void PrinterError(int error_code){

        _commonErrorDialog = new CommonErrorDialog();
        _commonErrorDialog.setCommonErrorEventHandlers(this);

        switch (error_code){
            case Printer.PRINTER_STATUS_BUSY: // ビジー状態（-1）
                _commonErrorDialog.ShowErrorMessage(_view.getContext(), MainApplication.getInstance().getString(R.string.error_type_printer_sts_busy));
                break;
            case Printer.PRINTER_STATUS_HIGHT_TEMP: // 高温状態（-2）
                _commonErrorDialog.ShowErrorMessage(_view.getContext(), MainApplication.getInstance().getString(R.string.error_type_printer_sts_hight_temp));
                break;
            case Printer.PRINTER_STATUS_PAPER_LACK: // 用紙切れ状態 (-3)
                _commonErrorDialog.ShowErrorMessage(_view.getContext(), MainApplication.getInstance().getString(R.string.error_type_printer_sts_paper_lack));
                break;
            case Printer.PRINTER_STATUS_NO_BATTERY: // バッテリー残量不足状態（-4）
                _commonErrorDialog.ShowErrorMessage(_view.getContext(), MainApplication.getInstance().getString(R.string.error_type_printer_sts_no_battery));
                break;
            case Printer.PRINTER_STATUS_FEED: // 用紙送り状態（-5）
                _commonErrorDialog.ShowErrorMessage(_view.getContext(), MainApplication.getInstance().getString(R.string.error_type_printer_sts_feed));
                break;
            case Printer.PRINTER_STATUS_PRINT: // 印刷状態（-6）
                _commonErrorDialog.ShowErrorMessage(_view.getContext(), MainApplication.getInstance().getString(R.string.error_type_printer_sts_print));
                break;
            case Printer.PRINTER_STATUS_FORCE_FEED: // 強制用紙送り状態（-7）
                _commonErrorDialog.ShowErrorMessage(_view.getContext(), MainApplication.getInstance().getString(R.string.error_type_printer_sts_force_feed));
                break;
            case Printer.PRINTER_STATUS_POWER_ON: // 電源ON処理中状態（-8）
                _commonErrorDialog.ShowErrorMessage(_view.getContext(), MainApplication.getInstance().getString(R.string.error_type_printer_sts_power_on));
                break;
            case Printer.PRINTER_TASKS_FULL: // 処理満載状態（-9）
                _commonErrorDialog.ShowErrorMessage(_view.getContext(), MainApplication.getInstance().getString(R.string.error_type_printer_tasks_full));
                break;
            default:
                // プリンター異常状態（未定義）
                _commonErrorDialog.ShowErrorMessage(_view.getContext(), MainApplication.getInstance().getString(R.string.error_type_printer_sts_undefined) + "@@@" + error_code + "@@@");
                break;
        }

        TimerTask task = new TimerTask() {
            public void run() {
                try {
                    Timber.tag("Printer").i("ダイヤログ消去");
                    _commonErrorDialog.dismiss();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        Timer timer = new Timer();
        timer.schedule(task, 180000);   // 3分でダイアログを消す
    }

    // 印刷終了
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void PrintEndDuplex(String MaskCardId, Integer TransResult, int PrintResult,int ContinueFlag) {

        //ADD-S BMT S.Oyama 2024/12/24 フタバ双方向向け改修
        Timber.i("[FUTABA-D]PrintManager::PrintEndDuplex  MaskCardId:%s TransResult:%d PrintResult:%d ContinueFlag:%d",
                MaskCardId, TransResult, PrintResult, ContinueFlag);
        //ADD-E BMT S.Oyama 2024/12/24 フタバ双方向向け改修

        isMaskCardId = MaskCardId;
        isTransResult = TransResult;

        // 伝票印刷用のダイアログ閉じる
        dismissPrintingDialog();
        //ADD-S BMT S.Oyama 2024/11/11 フタバ双方向向け改修
        if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == true ) {
            dismissNextPrintDialog();
        }
        //ADD-E BMT S.Oyama 2024/11/11 フタバ双方向向け改修
        // 正常に印刷終了
        if (PrintResult == Printer.PRINTER_OK) {
            switch (isSlipType) {
                case PrinterConst.SlipType_Trans:
                //ADD-S BMT S.Oyama 2025/01/10 フタバ双方向向け改修
                case PrinterConst.SlipType_Prepaid:
                //ADD-E BMT S.Oyama 2024/01/10 フタバ双方向向け改修
                    // 取引
                    if (isTransResult == PrinterConst.TransResult_OK) {
                        // 継続フラグ確認
                        if (0 < ContinueFlag) {
                            /* 継続印刷の確認画面ダイアログを表示 */
                            showCheckContinueDialog();
                        } else {

                            changePrintStatus(PrinterConst.PrintStatus_UPDATING);
                            // 取引伝票（お客様控え）の印刷完了時
                            /* 印刷回数をカウント(+1) */
                            updatePrintCnt();
                            changePrintStatus(PrinterConst.PrintStatus_IDLE);
                        }
                    } else if (isTransResult == PrinterConst.TransResult_UnFinished) {
                        changePrintStatus(PrinterConst.PrintStatus_UPDATING);
                        // 処理未了伝票（加盟店控え）の印刷完了時
                        /* カード番号マスク更新 */
                        updateMaskCardId();
                        /* 印刷回数をカウント(+1) */
                        updatePrintCnt();
                        changePrintStatus(PrinterConst.PrintStatus_IDLE);
                    } else {
                        // その他
                        changePrintStatus(PrinterConst.PrintStatus_IDLE);
                    }
                    break;
                case PrinterConst.SlipType_Aggregate:
                    // 集計
                    if (isAggregateOrder == 0) {
                        changePrintStatus(PrinterConst.PrintStatus_UPDATING);
                        /* 業務終了のみ、集計印刷回数をカウント更新（5：過去の最古集計削除し、0から各カウント+1） */
                        updatePrintAggregateOrder();
                    } else {
                        // その他（想定外）時は、集計印刷回数をカウント更新なし
                    }
                    changePrintStatus(PrinterConst.PrintStatus_IDLE);
                    break;
                default:
                    // その他(WAON印字含む)
                    changePrintStatus(PrinterConst.PrintStatus_IDLE);
                    break;
            }
        } else if (PrinterConst.DuplexPrintStatus_IFBOX_PRINTERROR == PrintResult || PrinterConst.DuplexPrintStatus_IFBOXERROR == PrintResult || PrinterConst.DuplexPrintStatus_IFBOXERROR_TIMEOUT == PrintResult) {
            // IM-A820通信エラー発生時
            printerDuplexErrorCode = PrintResult;
            changePrintStatus(PrinterConst.PrintStatus_ERROR);
            /* 伝票印刷再開の確認画面ダイアログを表示 */
            PrinterDuplexError(PrinterConst.DuplexPrintStatus_IFBOX_PRINTERROR);
        } else {
            if (isTransResult == PrinterConst.TransResult_UnFinished) {
                // 処理未了伝票（加盟店控え）の印刷完了時
                /* カード番号マスク更新 */
                updateMaskCardId();
            }
            if (PrintResult == PrinterConst.DuplexPrintStatus_PAPERLACKING) {
                changePrintStatus(PrinterConst.PrintStatus_PAPERLACKING);
            } else {
                changePrintStatus(PrinterConst.PrintStatus_ERROR);
            }

            //ADD-S BMT S.Oyama 2025/02/17 フタバ双方向向け改修
            if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == true ) {
                if ((isSlipType == PrinterConst.SlipType_TransHistory_Waon) &&
                        (PrintResult == PrinterConst.DuplexPrintStatus_PAPERLACKING)) {     //WAON歴　かつ用紙切れの場合は，エラーコードを差し替える
                    //ADD-S BMT S.Oyama 2025/03/11 フタバ双方向向け改修
                    //PrintResult = PrinterConst.DuplexPrintStatus_OUTOFPAPER_NORESTART;
                    PrintResult = PrinterConst.DuplexPrintStatus_PAPERLACKING;
                    //ADD-E BMT S.Oyama 2025/03/11 フタバ双方向向け改修
                }
            }
            //ADD-E BMT S.Oyama 2025/02/17 フタバ双方向向け改修

            // プリンターエラー発生
            PrinterDuplexError(PrintResult);
        }
    }

    // プリンターエラー表示
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void PrinterDuplexError(int error_code){

        //ADD-S BMT S.Oyama 2024/10/22 フタバ双方向向け改修
        if (_view == null)
        {
            Timber.e("[FUTABA-D]PrintManager::PrinterDuplexError -> _view is NULL Arg:ErrorCD %d", error_code);
            return;
        }
        else
        {
            Timber.i("[FUTABA-D]PrintManager::PrinterDuplexError -> dialog out put Arg:ErrorCD %d", error_code);
        }
        //ADD-E BMT S.Oyama 2024/10/22 フタバ双方向向け改修

        _commonErrorDialog = new CommonErrorDialog();
        _commonErrorDialog.setCommonErrorEventHandlers(this);

        switch (error_code){
            case PrinterConst.DuplexPrintStatus_PAPERLACKING:       // 用紙切れ状態(1)
                //ADD-S BMT S.Oyama 2025/02/21 フタバ双方向向け改修
                if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == true ) {
                    _commonErrorDialog.ShowErrorMessage(_view.getContext(), MainApplication.getInstance().getString(R.string.error_type_FutabaD_Outofpaper_Norestart_withSetkey));          //フタバD時の紙切れは，セットキーで再開のため，別のダイアログを表示させる
                }
                else {
                    _commonErrorDialog.ShowErrorMessage(_view.getContext(), MainApplication.getInstance().getString(R.string.error_type_printer_duplex_sts_paper_lack));
                }
                //ADD-E BMT S.Oyama 2025/02/21 フタバ双方向向け改修
                //ADD-S BMT S.Oyama 2025/01/28 フタバ双方向向け改修
                if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == true ) {
                    //ADD-S BMT S.Oyama 2025/04/24 フタバ双方向向け改修
                    PrinterProc printerProc = PrinterProc.getInstance();
                    String tmpBlandName = printerProc.getDuplexPrint_BlandName();
                    if (tmpBlandName.equals(MainApplication.getInstance().getString(R.string.money_brand_credit)) != true) {          // ブランド名がクレジットでない場合170秒でダイアログを閉じるを有効化
                        // 用紙切れ状態(1)の場合、長時間表示時エラーダイアログを非表示化
                        _printErrorDialog = new Runnable() {
                            @RequiresApi(api = Build.VERSION_CODES.N)
                            @Override
                            public void run() {
                                DissmissPrinterDuplexError();
                                Timber.i("[FUTABA-D]PrintManager::PrinterDuplexError  PAPERLACKING Error Long view. So, dissmissed dialog.");
                            }
                        };
                        _handlerErrorDialog.postDelayed(_printErrorDialog, 170 * 1000);     //170秒で非表示
                    }
                    //ADD-E BMT S.Oyama 2025/04/24 フタバ双方向向け改修
                }
                //ADD-E BMT S.Oyama 2025/01/28 フタバ双方向向け改修
                break;
            case PrinterConst.DuplexPrintStatus_PRINTCHECK:           // プリンタ確認
                _commonErrorDialog.ShowErrorMessage(_view.getContext(), MainApplication.getInstance().getString(R.string.error_type_printer_duplex_sts_check));
                break;
            case PrinterConst.DuplexPrintStatus_PRINTBUSY:             // 印字中
                _commonErrorDialog.ShowErrorMessage(_view.getContext(), MainApplication.getInstance().getString(R.string.error_type_printer_duplex_sts_busy));
                break;
            case PrinterConst.DuplexPrintStatus_DENY:                 // 印字拒否
                _commonErrorDialog.ShowErrorMessage(_view.getContext(), MainApplication.getInstance().getString(R.string.error_type_printer_duplex_sts_deny));
                break;
            case PrinterConst.DuplexPrintStatus_ERROR:                // 印字不能
                _commonErrorDialog.ShowErrorMessage(_view.getContext(), MainApplication.getInstance().getString(R.string.error_type_printer_duplex_sts_error));
                break;
            case PrinterConst.DuplexPrintStatus_CMDSTOP:             // データ送信不可
                _commonErrorDialog.ShowErrorMessage(_view.getContext(), MainApplication.getInstance().getString(R.string.error_type_printer_duplex_sts_cmdstop));
                break;
            case PrinterConst.DuplexPrintStatus_CMDERR:              // データ送信中止
                _commonErrorDialog.ShowErrorMessage(_view.getContext(), MainApplication.getInstance().getString(R.string.error_type_printer_duplex_sts_cmderr));
                break;
            case PrinterConst.DuplexPrintStatus_DATAERROR:           // 受信データ異常
                _commonErrorDialog.ShowErrorMessage(_view.getContext(), MainApplication.getInstance().getString(R.string.error_type_printer_duplex_sts_dataerr));
                break;
            case PrinterConst.DuplexPrintStatus_DISCON:              // メーター通信不可
                _commonErrorDialog.ShowErrorMessage(_view.getContext(), MainApplication.getInstance().getString(R.string.error_type_printer_duplex_sts_discon));
                break;
            case PrinterConst.DuplexPrintStatus_TIMEOUT:             // メーター通信タイムアウト
                _commonErrorDialog.ShowErrorMessage(_view.getContext(), MainApplication.getInstance().getString(R.string.error_type_printer_duplex_sts_timeout));
                break;
            case PrinterConst.DuplexPrintStatus_IFBOX_PRINTERROR:    // 印刷失敗、再開用
                _commonErrorDialog.ShowErrorMessage(_view.getContext(), MainApplication.getInstance().getString(R.string.error_type_ifbox_print_error));
                break;
            case PrinterConst.DuplexPrintStatus_METERSTSERROR:      // メーター状態異常
                _commonErrorDialog.ShowErrorMessage(_view.getContext(), MainApplication.getInstance().getString(R.string.error_type_printer_duplex_sts_ng_status));
                break;
            case PrinterConst.DuplexPrintStatus_IFBOXERROR:          // IM-A820通信エラー
                _commonErrorDialog.ShowErrorMessage(_view.getContext(), MainApplication.getInstance().getString(R.string.error_type_ifbox_transmission_error));
                break;
            case PrinterConst.DuplexPrintStatus_IFBOXERROR_TIMEOUT:  // IM-A820通信エラー（応答待ちタイムアウト）
                _commonErrorDialog.ShowErrorMessage(_view.getContext(), MainApplication.getInstance().getString(R.string.error_type_ifbox_transmission_timeout_error));
                break;
            case PrinterConst.DuplexPrintStatus_SDCARD_NOTFOUND:     // SDカード未挿入
                _commonErrorDialog.ShowErrorMessage(_view.getContext(), MainApplication.getInstance().getString(R.string.error_type_FutabaD_SDCardNotFound));
                break;
            case PrinterConst.DuplexPrintStatus_DISCOUNT_REJECT:     // 割引処理拒否
                _commonErrorDialog.ShowErrorMessage(_view.getContext(),MainApplication.getInstance().getString(R.string.error_type_FutabaD_DiscountReject));
                break;
            case PrinterConst.DuplexPrintStatus_MONEYRECEIPTKEY_ERR :   // 領収書発行エラー
            case PrinterConst.DuplexPrintStatus_TICKETRECEIPTKEY_ERR :  // チケット伝票発行エラー
                _commonErrorDialog.ShowErrorMessage(_view.getContext(),MainApplication.getInstance().getString(R.string.error_type_FutabaD_MoneyTicketReceipt_NotJob));
                break;
            case PrinterConst.DuplexPrintStatus_OUTOFPAPER_NORESTART:  // 用紙切れ（再開不可）
                _commonErrorDialog.ShowErrorMessage(_view.getContext(),MainApplication.getInstance().getString(R.string.error_type_FutabaD_Outofpaper_Norestart));
                break;
            case PrinterConst.DuplexPrintStatus_PROCESSCODE_TEISEIREQ:    // 処理コード要求によるエラー表示　訂正キーをリクエストしているエラー
                _commonErrorDialog.ShowErrorMessage(_view.getContext(),MainApplication.getInstance().getString(R.string.error_type_FutabaD_MeterErrorMes_Cancel_T));
                break;
            case PrinterConst.DuplexPrintStatus_PROCESSCODE_ERRRETURN:    // 処理コード要求によるエラー表示　異常処理コード画面戻り要求キーをリクエストしているエラー
                _commonErrorDialog.ShowErrorMessage(_view.getContext(),MainApplication.getInstance().getString(R.string.error_type_FutabaD_MeterErrorMes_Cancel_R));
                break;
            default:
                // プリンター異常状態（未定義）
                _commonErrorDialog.ShowErrorMessage(_view.getContext(), MainApplication.getInstance().getString(R.string.error_type_printer_duplex_sts_error_other) + "@@@" + error_code + "@@@");
                break;
        }


        boolean tmpTimeoutStartFL = false;
        if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) != true ) {
            tmpTimeoutStartFL = true;
        }
        else
        {
            if (error_code != PrinterConst.DuplexPrintStatus_PAPERLACKING)
            {
                tmpTimeoutStartFL = true;
            }
        }

        if (tmpTimeoutStartFL == true) {
            TimerTask task = new TimerTask() {
                public void run() {
                    try {
                        Timber.tag("Printer").i("ダイヤログ消去");
                        _commonErrorDialog.dismiss();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
            Timer timer = new Timer();
            timer.schedule(task, 180000);   // 3分でダイアログを消す
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void DissmissPrinterDuplexError() {

        if (_commonErrorDialog != null) {
            _commonErrorDialog.dismissAlertDialog();
            _commonErrorDialog = null;
        }
    }


    // プリンターエラー表示（つり銭機連動）
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void PrinterCashChangerError(int error_code){

        _commonErrorDialog = new CommonErrorDialog();
        _commonErrorDialog.setCommonErrorEventHandlers(this);

        switch (error_code){
            case Epos2CallbackCode.CODE_ERR_COVER_OPEN:  // カバーオープン
            case Epos2CallbackCode.CODE_ERR_EMPTY:  // 用紙なし
                _commonErrorDialog.ShowErrorMessage(_view.getContext(), MainApplication.getInstance().getString(R.string.error_type_printer_sts_paper_lack));
                break;
            default:
                _commonErrorDialog.ShowErrorMessage(_view.getContext(), MainApplication.getInstance().getString(R.string.error_type_cashchanger_printer_connection_error));
                break;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onPositiveClick(String errorCode) {
        // はいボタン押下時
        /* 用紙切れ⇒伝票印刷再開 */
        if(MainApplication.getInstance().getString(R.string.error_type_printer_sts_paper_lack).equals(errorCode) ||
           MainApplication.getInstance().getString(R.string.error_type_printer_duplex_sts_paper_lack).equals(errorCode) ||
           MainApplication.getInstance().getString(R.string.error_type_printer_duplex_sts_cmdstop).equals(errorCode) ||
           MainApplication.getInstance().getString(R.string.error_type_printer_duplex_sts_ng_status).equals(errorCode) ||
           MainApplication.getInstance().getString(R.string.error_type_ifbox_print_error).equals(errorCode) ||
           //ADD-S BMT S.Oyama 2025/02/21 フタバ双方向向け改修
           MainApplication.getInstance().getString(R.string.error_type_FutabaD_Outofpaper_Norestart_withSetkey).equals(errorCode)
           //ADD-E BMT S.Oyama 2025/02/21 フタバ双方向向け改修
        ) {
            if(!IFBoxAppModels.isMatch(IFBoxAppModels.YAZAKI_LT27_D) &&
            //ADDCHG-S BMT S.Oyama 2024/09/24 フタバ双方向向け改修
               !IFBoxAppModels.isMatch(IFBoxAppModels.OKABE_MS70_D) &&
               !IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D))
            //ADDCHG-S BMT S.Oyama 2024/09/24 フタバ双方向向け改修
            {
                if (isPrintStatus == PrinterConst.PrintStatus_PAPERLACKING) {
                    changePrintStatus(PrinterConst.PrintStatus_PRINTING);
                } else {
                    return;
                }
            }
            Timber.tag("Printer").i("%s", MainApplication.getInstance().getResources().getString(R.string.printLog_RePrint));
            // 伝票印刷用のダイアログ表示
            showPrintingDialog();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    SlipData slipData = DBManager.getSlipDao().getOneById(isSlipDataId);

                    //用紙切れエラーがスタックされている場合は削除
                    ErrorStackingDao errorStackingDao = DBManager.getErrorStackingDao();
                    ErrorStackingData errorStackingData = errorStackingDao.getErrorStackingData(errorCode);
                    if (errorStackingData != null) {
                        errorStackingDao.deleteErrorStackingData(errorStackingData.id);
                    }

                    PrinterProc printerProc = PrinterProc.getInstance();
                    switch (isSlipType) {
                        case PrinterConst.SlipType_Prepaid:
                            if(IFBoxAppModels.isMatch(IFBoxAppModels.YAZAKI_LT27_D) ||
                                    //ADDCHG-S BMT S.Oyama 2024/09/24 フタバ双方向向け改修
                                    IFBoxAppModels.isMatch(IFBoxAppModels.OKABE_MS70_D) ||
                                    (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) && (slipData.transTypeCode == null || !slipData.transTypeCode.equals(MANUALMODE_TRANS_TYPE_CODE_SALES)) && (slipData.transTypeCode == null || !slipData.transTypeCode.equals(MANUALMODE_TRANS_TYPE_CODE_CANCEL))) )
                            {
                                //ADDCHG-E BMT S.Oyama 2024/09/24 フタバ双方向向け改修
                                //ADD-S BMT S.Oyama 2024/12/23 フタバ双方向向け改修
                                if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == true){
                                    if (isPrintStatus == PrinterConst.PrintStatus_IDLE) {           //idle時は再印字を実施しない
                                        dismissPrintingDialog();                    //ダイアログを閉じる
                                    } else {
                                        //ADD-S BMT S.Oyama 2025/02/18 フタバ双方向向け改修
                                        //printerProc.printPrepaidTrans(isSlipDataIds, PrinterConst.SlipCopy_Merchant);
                                        //_ifBoxManager.send820_Reprint_KeyCode();                //SLIP組み立て止めて，セットキーを送るようにする
                                        //ADD-E BMT S.Oyama 2025/02/18 フタバ双方向向け改修
                                    }
                                }
                                else {
                                    printerProc.printPrepaidTrans(isSlipDataIds, PrinterConst.SlipCopy_Merchant);
                                }
                                //ADD-E BMT S.Oyama 2024/12/12 フタバ双方向向け改修
                            }else {
                                printerProc.printPrepaidTrans(isSlipDataIds, isSlipCopy);
                            }
                            break;
                        case PrinterConst.SlipType_Trans:
                            if(IFBoxAppModels.isMatch(IFBoxAppModels.YAZAKI_LT27_D) ||
                            //ADDCHG-S BMT S.Oyama 2024/09/24 フタバ双方向向け改修
                               IFBoxAppModels.isMatch(IFBoxAppModels.OKABE_MS70_D) ||
                              (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) && (slipData.transTypeCode == null || !slipData.transTypeCode.equals(MANUALMODE_TRANS_TYPE_CODE_SALES)) && (slipData.transTypeCode == null || !slipData.transTypeCode.equals(MANUALMODE_TRANS_TYPE_CODE_CANCEL))) )
                            {
                            //ADDCHG-E BMT S.Oyama 2024/09/24 フタバ双方向向け改修
                                //ADD-S BMT S.Oyama 2024/12/23 フタバ双方向向け改修
                                if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == true){
                                    if (isPrintStatus == PrinterConst.PrintStatus_IDLE) {           //idle時は再印字を実施しない
                                        dismissPrintingDialog();                    //ダイアログを閉じる
                                    } else {
                                        //ADD-S BMT S.Oyama 2025/02/18 フタバ双方向向け改修
                                        //printerProc.printTrans(isSlipDataId, PrinterConst.SlipCopy_Merchant);
                                        //_ifBoxManager.send820_Reprint_KeyCode();            //SLIP組み立て止めて，セットキーを送るようにする
                                        //ADD-E BMT S.Oyama 2025/02/18 フタバ双方向向け改修
                                    }
                                }
                                else {
                                    printerProc.printTrans(isSlipDataId, PrinterConst.SlipCopy_Merchant);
                                }
                                //ADD-E BMT S.Oyama 2024/12/12 フタバ双方向向け改修
                            }else {
                                printerProc.printTrans(isSlipDataId, isSlipCopy);
                            }
                            break;
                        case PrinterConst.SlipType_TransHistory_Waon:
                            //ADD-S BMT S.Oyama 2025/02/18 フタバ双方向向け改修
                            if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == true){
                                //ADD-S BMT S.Oyama 2025/03/11 フタバ双方向向け改修
                                //_ifBoxManager.send820_Reprint_KeyCode();                            //SLIP組み立て止めて，セットキーを送るようにする
                                //printerProc.sendWsPrintHistryFutabaDCoreErrorAck();                   //フタバDの場合，JSONでエラー応答を送る
                                //ADD-E BMT S.Oyama 2025/03/11 フタバ双方向向け改修
                            } else {
                                printerProc.printTransHistory_WAON(isResultWAON);
                            }
                            //ADD-E BMT S.Oyama 2025/02/18 フタバ双方向向け改修
                            break;
                        case PrinterConst.SlipType_TransHistory_Okica:
                            printerProc.printTransHistory_OKICA(isHistoryDataOKICA, isOkicaHistoryPrintDateTime);
                            break;
                        case PrinterConst.SlipType_DetailStatement:
                        case PrinterConst.SlipType_TransCash_DetailStatement:
                            if (AppPreference.isPosTransaction()) {
                                printerProc.printDetailStatement(isSlipDataId);
                            }

                            if (AppPreference.isTicketTransaction()) {
                                printerProc.printTicketDetailStatement(isSlipDataId);
                            }
                            break;
                        case PrinterConst.SlipType_CancelTicket:
                            if (AppPreference.isPosTransaction()) {
                                printerProc.printCancelTicket(isSlipDataId);
                            }

                            if (AppPreference.isTicketTransaction()) {
                                printerProc.printTicketCancel(isSlipDataId);
                            }
                            break;
                        case PrinterConst.SlipType_Receipt:
                            if (AppPreference.isPosTransaction()) {
                                printerProc.printReceipt(isSlipDataId, isReceiptDetail);
                            }

                            if (AppPreference.isTicketTransaction()) {
                                printerProc.printTicketReceipt(isSlipDataId, isReceiptDetail);
                            }
                            break;
                        case PrinterConst.SlipType_Aggregate:
                            printerProc.printAggregate(isAggregateOrder, isAggregateType);
                            break;
                        case PrinterConst.SlipType_DeviceCheck:
                            printerProc.PrintDeviceCheckResult(isDeviceCheckResult);
                            break;
                        case PrinterConst.SlipType_QRTicket:
                            printerProc.printQRTicketReceipt();
                        case PrinterConst.SlipType_CashHistory:
                            printerProc.CashHistory(isAmountValue);
                            break;
                        //ADD-S BMT S.Oyama 2025/03/11 フタバ双方向向け改修
                        case PrinterConst.SlipType_AggregateFutabaD:
                            if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == true) {
                                //_ifBoxManager.send820_Reprint_KeyCode();                            //SLIP組み立て止めて，セットキーを送るようにする
                                TimerTask timerTask = new TimerTask() {
                                    @Override
                                    public void run() {
                                        dismissPrintingDialog();                    //ダイアログを閉じる
                                    }
                                };

                                Timer timer = new Timer();
                                timer.schedule(timerTask, 5000);
                            }
                            break;
                        //ADD-E BMT S.Oyama 2025/03/11 フタバ双方向向け改修
                        default:
                            break;
                    }
                }
            }).start();
        } else if (MainApplication.getInstance().getString(R.string.error_type_ifbox_transmission_error).equals(errorCode) ||
                MainApplication.getInstance().getString(R.string.error_type_ifbox_transmission_timeout_error).equals(errorCode)) {
            /* 伝票印刷失敗(6033,6034)※LT27ヤザキ双方向専用 */
            // 何もしない
        //ADD-S BMT S.Oyama 2025/02/26 フタバ双方向向け改修
//        } else if (MainApplication.getInstance().getString(R.string.error_type_FutabaD_MeterErrorMes_Cancel_T).equals(errorCode) == true) {                 // 処理コード要求によるエラー表示　訂正キーをリクエストしているエラー
//            if ( IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == true) {     //フタバDのみ実施
//                _ifBoxManager.send820_TeiseiKeyNonAck();                        //訂正キーを送信 ACKを必要としないモードで
//            }
//        } else if (MainApplication.getInstance().getString(R.string.error_type_FutabaD_MeterErrorMes_Cancel_R).equals(errorCode) == true) {                 // 処理コード要求によるエラー表示　異常処理コード画面戻り要求キーをリクエストしているエラー
//            if ( IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == true) {     //フタバDのみ実施
//                _ifBoxManager.send820_GenericProcessCodeErrReturn();                        //異常処理コード画面戻り要求キーを送信 ACKを必要としないモードで
//            }
        //ADD-E BMT S.Oyama 2025/02/26 フタバ双方向向け改修
        }else{
            /* 用紙切れ以外のエラー */
            ReprintStop(errorCode);
        }
    }

    @Override
    public void onNegativeClick(String errorCode) {
        // いいえボタン押下時
        ReprintStop(errorCode);
    }
    @Override
    public void onNeutralClick(String errorCode) {
        // キャンセルボタン押下時
    }
    @Override
    public void onDismissClick(String errorCode) {
        // ボタンを押さずダイアログを閉じた時
        ReprintStop(errorCode);
    }

    // 印刷再開中止
    private void ReprintStop(String errorCode){

        //CHG-S BMT S.Oyama 2024/09/24 フタバ双方向向け改修
        //if (IFBoxAppModels.isMatch(IFBoxAppModels.YAZAKI_LT27_D) || IFBoxAppModels.isMatch(IFBoxAppModels.OKABE_MS70_D)) {
        if (IFBoxAppModels.isMatch(IFBoxAppModels.YAZAKI_LT27_D) || IFBoxAppModels.isMatch(IFBoxAppModels.OKABE_MS70_D) || IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D)) {
        //CHG-E BMT S.Oyama 2024/09/24 フタバ双方向向け改修
            /* LT27ヤザキ双方向の場合 */
            if (MainApplication.getInstance().getString(R.string.error_type_ifbox_print_error).equals(errorCode)) {
                if (PrinterConst.SlipType_Trans == isSlipType && PrinterConst.TransResult_OK == isTransResult) {
                    // 印刷失敗のメッセージ表示、取引詳細履歴画面から再印刷を案内
                    if (PrinterConst.DuplexPrintStatus_IFBOX_PRINTERROR == printerDuplexErrorCode) {
                        printerDuplexErrorCode = PrinterConst.DuplexPrintStatus_IFBOXERROR;
                    }
                    PrinterDuplexError(printerDuplexErrorCode);
                }
            }
        }

        // 印刷再開中止命令を１回のみ受令許可
        if(isPrintStatus == PrinterConst.PrintStatus_PAPERLACKING){
            changePrintStatus(PrinterConst.PrintStatus_UPDATING);
            Timber.tag("Printer").i("%s",MainApplication.getInstance().getResources().getString(R.string.printLog_StopRePrint));
        }else if(isPrintStatus == PrinterConst.PrintStatus_ERROR){
            changePrintStatus(PrinterConst.PrintStatus_UPDATING);
            Timber.tag("Printer").i("%s",MainApplication.getInstance().getResources().getString(R.string.printLog_printerErrorCheck));
        }else{
            return;
        }

        switch (isSlipType){
            case PrinterConst.SlipType_Trans:
                if(isTransResult == PrinterConst.TransResult_UnFinished){
                    isTransResultUnFinish = true;
                    /* 処理未了の場合、カード番号マスク更新 */
                    updateMaskCardId();
                }
                /* 印刷回数をカウント(+1) */
                updatePrintCnt();
                break;
            case PrinterConst.SlipType_DetailStatement:
            case PrinterConst.SlipType_TransCash_DetailStatement:
            case PrinterConst.SlipType_CancelTicket:
                /* 印刷回数をカウント(+1) */
                updatePrintCnt();
                break;
            case PrinterConst.SlipType_Receipt:
                /* 領収書の印刷回数をカウント(+1) */
                updateReceiptPrintCnt();
                break;
            case PrinterConst.SlipType_Aggregate:
                if(isAggregateOrder == 0){
                    /* 業務終了のみ、集計印刷回数をカウント更新（5：過去の最古集計削除し、0から各カウント+1） */
                    /* updatePrintAggregateOrder()により削除されたslip_idに紐づくデータをpos_receiptから削除 */
                    updatePrintAggregateOrder();
                }else{
                    // 集計履歴印刷時は、集計印刷回数をカウント更新なし
                }
                break;
            case PrinterConst.SlipType_TransHistory_Waon:
            case PrinterConst.SlipType_TransHistory_Okica:
            case PrinterConst.SlipType_DeviceCheck:
            case PrinterConst.SlipType_QRTicket:
            default:
                break;
        }
        changePrintStatus(PrinterConst.PrintStatus_IDLE);

        if (AppPreference.isTicketTransaction()) {
            //Timber.d("isSlipType = %s isTransType = %s isTransResult = %s isRePrinter = %s", isSlipType, isTransType, isTransResult, isRePrinter);
            if (isTransType != null &&
                    isTransType == PrinterConst.SlipType_Trans &&
                    isTransResult != null &&
                    isTransResult == PrinterConst.TransResult_OK &&
                    !isRePrinter) {

                // チケット販売時の初回売上票を印刷再開中止時、QR発行画面に遷移する
                NavigationWrapper.navigate(_view, R.id.action_navigation_menu_to_fragment_ticket_issue);
            } else if (isSlipType == PrinterConst.SlipType_DetailStatement && !isRePrinter && !isTransResultUnFinish) {
                // チケット販売時の初回取引明細書を印刷再開中止時、QR発行画面に遷移する※未了以外
                NavigationWrapper.navigate(_view, R.id.action_navigation_menu_to_fragment_ticket_issue);
            } else {
                // 何もしない
            }
        }
    }

    /* 伝票印刷回数をカウント(+1) */
    private void updatePrintCnt() {
        Thread updatePrintCnt = new Thread(new Runnable() {
            @Override
            public void run() {
                if (isSlipType == PrinterConst.SlipType_Prepaid && isSlipDataIds != null && isSlipDataIds.length > 0) {
                    // プリペイドは複数取引をまとめて印刷することがあるので対象取引全ての印字回数を増やす
                    for (int slipDataId : isSlipDataIds) {
                        DBManager.getSlipDao().updatePrintCnt(slipDataId);
                    }
                } else {
                    DBManager.getSlipDao().updatePrintCnt(isSlipDataId);
                }
            }
        });
        updatePrintCnt.start();

        try {
            updatePrintCnt.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 領収書の印刷回数をカウントアップ
    private void updateReceiptPrintCnt() {
        Thread updateReceiptPrintCnt = new Thread(() -> {
            if (AppPreference.isPosTransaction()) {
                DBManager.getReceiptDao().updatePrintCnt(isSlipDataId);
            }

            if (AppPreference.isTicketTransaction()) {
                DBManager.getTicketReceiptDao().updatePrintCnt(isSlipDataId);
            }
        });
        updateReceiptPrintCnt.start();

        try {
            updateReceiptPrintCnt.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* 業務終了時に集計印刷データなしの場合、現在の業務開始日時と業務終了日時を削除 */
    private void deleteNowAggregateDate() {
        Thread deleteNowAggregateDate = new Thread(new Runnable() {
            @Override
            public void run() {
                DBManager.getAggregateDao().deleteNowHistory();
            }
        });
        deleteNowAggregateDate.start();

        try {
            deleteNowAggregateDate.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* 集計印刷回数をカウント更新（5：過去の最古集計削除し、0から各カウント+1） */
    /* updatePrintAggregateOrder()により削除されたslip_idに紐づくデータをpos_receiptから削除 */
    private void updatePrintAggregateOrder() {
        Thread updatePrintAggregateOrder = new Thread(new Runnable() {
            @Override
            public void run() {
                DBManager.getSlipDao().updateTableAfterAggregate();
                DBManager.getAggregateDao().updateAggregateHistory();
                _historySlipIds = DBManager.getSlipDao().getIds();
                // POS機能有効時
                if (AppPreference.isServicePos()) DBManager.getReceiptDao().deleteBySlipId(_historySlipIds);
                // チケット販売機能有効時
                if (AppPreference.isServiceTicket()) DBManager.getTicketReceiptDao().deleteBySlipId(_historySlipIds);
            }
        });
        updatePrintAggregateOrder.start();

        try {
            updatePrintAggregateOrder.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /*Add-S k.Fukumitsu  2024/1/30 オカベ双_ホーム画面で入庫または業務終了時、集計をクリア*/
    /* オカベ双方向入庫時集計クリア */
    public void OkabeTotallingClearJudge(){
        updatePrintAggregateOrder();
    }
    /*Add-E k.Fukumitsu  2024/1/30 オカベ双_ホーム画面で入庫または業務終了時、集計をクリア*/

    //ADD-S BMT S.Oyama 2024/09/24 フタバ双方向向け改修
    /******************************************************************************/
    /*!
     * @brief  入庫時集計クリア（フタバ双方向用）
     * @note   入庫時集計クリア
     * @param [in] なし
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    public void FutabaDTotallingClearJudge() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Callable<Boolean> task = () -> {
            // 集計処理の対象にならなかったデータがないか確認
            // フタバ双方向で一度も手動決済モードにならずメーター連動してるときの取引は集計処理の対象にならない
            List<SlipData> slipData = DBManager.getSlipDao().getAggregate();
            return !slipData.isEmpty();
        };

        Future<Boolean> future = executor.submit(task);
        try {
            Boolean result = future.get();
            if(result) {
                // メーターから印刷するからと集計処理をされなかったSlipDataのクリア
                updatePrintAggregateOrder();
            }
        } catch (Exception e) {
            // とくになにもしない
            e.printStackTrace();
        }
    }


    /******************************************************************************/
    /*!
     * @brief  820に対して情報通知:ファンクション通知 ステータスエラー：一般処理向け（フタバ双方向用）
     * @note   820に対して情報通知:ファンクション通知 ステータスエラー：一般処理向け[本メソッドを起動時用には使用しないこと]
     * @param int tmpPhase 作業フェーズ
     * @param boolean isACKResult ACK結果を返すかどうか
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    public void send820_FunctionCodeErrorResult(View view, int tmpPhase, boolean isACKResult, int tmpSettlementMode ) {
        if ((IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == true)) {
            //条件が成り立つときは通過させる
        } else {
            return;         //成り立たないときは処理中止
        }

//        if (_ifBoxManager == null) {
//            return;
//        }
//
//        if (_ifBoxManager.getIsConnected820() == false) {
//            setView(view);
//            PrinterDuplexError(PrinterConst.DuplexPrintStatus_IFBOXERROR);
//            return;
//        }

        //ADD-S BMT S.Oyama 2025/02/10 フタバ双方向向け改修
        //_ifBoxManager.send820_FunctionCodeErrorResult(tmpPhase, isACKResult);             //取り消しキー送付のやり方は辞める

        PrinterProc printerProc = PrinterProc.getInstance();                                //print_startのx59を送付させる　print_endは無し
        printerProc.printTransFutabaD_SettlementAbort(tmpPhase, tmpSettlementMode);
        //ADD-E BMT S.Oyama 2025/02/10 フタバ双方向向け改修
    }
    //ADD-E BMT S.Oyama 2024/09/24 フタバ双方向向け改修


    /* カード番号マスク更新 */
    private void updateMaskCardId(){
        if(isMaskCardId != null){
            Thread updateMaskCardId = new Thread(new Runnable() {
                @Override
                public void run() {
                    DBManager.getSlipDao().updateMaskCardId(isSlipDataId, isMaskCardId);
                }
            });
            updateMaskCardId.start();

            try {
                updateMaskCardId.join();
                isMaskCardId = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // 印刷状態変更
    private void changePrintStatus(int PrintSlipSts){
        isPrintStatus = PrintSlipSts;
    }
    //ADD-S BMT S.Oyama 2024/10/23 フタバ双方向向け改修
    /******************************************************************************/
    /*!
     * @brief  印刷状態変更(外部開放用)（フタバ双方向用）
     * @note   印刷状態変更(外部開放用)
     * @param [in] int PrintSlipSts 印刷状態値
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    public void changePrintStatusEx(int PrintSlipSts)
    {
        isPrintStatus = PrintSlipSts;
    }
    //ADD-E BMT S.Oyama 2024/10/23 フタバ双方向向け改修

    //ADD-S BMT S.Oyama 2025/03/11 フタバ双方向向け改修
    /******************************************************************************/
    /*!
     * @brief  issliptypeの変更（フタバ双方向用）
     * @note   issliptypeの変更(外部開放用)
     * @param [in] int tmpIsSlipType  sliptype値
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    public void changeIsSlipTypeEx(int tmpIsSlipType)
    {
        isSlipType = tmpIsSlipType;
    }
    //ADD-E BMT S.Oyama 2025/03/11 フタバ双方向向け改修


    // 印刷状態取得
    public int getPrintStatus(){
        return isPrintStatus;
    }

    private void isDemo() {
        if (AppPreference.isDemoMode()) {
            Timber.tag("Printer").i("【デモモード】");
        }
    }

    // 取引明細書印刷枚数をマスタから取得
    private Integer getDetailStatementPrintMax() {
        Integer receiptCount = 0;

        Thread thread = new Thread(() -> {
            _serviceFunctionData = DBManager.getServiceFunctionDao().getServiceFunction();
        });
        thread.start();

        try {
            thread.join();
            if(_serviceFunctionData != null){
                receiptCount = _serviceFunctionData.receipt_count;
                Timber.tag("Printer").i("取引明細書印刷枚数：" + receiptCount);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return receiptCount;
    }
}
