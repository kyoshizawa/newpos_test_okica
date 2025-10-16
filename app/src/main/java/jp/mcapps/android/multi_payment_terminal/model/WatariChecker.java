package jp.mcapps.android.multi_payment_terminal.model;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.view.View;

import com.pos.device.printer.Printer;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.data.Amount;
import jp.mcapps.android.multi_payment_terminal.data.BusinessType;
import jp.mcapps.android.multi_payment_terminal.data.CurrentRadio;
import jp.mcapps.android.multi_payment_terminal.data.IFBoxAppModels;
import jp.mcapps.android.multi_payment_terminal.service.GetRadioService;
import jp.mcapps.android.multi_payment_terminal.thread.credit.CreditSettlement;
import timber.log.Timber;

public class WatariChecker {
    private static final int BATTERY_LOWER_LIMIT = 10;  // パーセント
    private static WatariChecker _instance = null;
    private WatariCheckListener _listener;
    public interface WatariCheckListener {
        void onFinished(String errCode);
    }

    public static WatariChecker getInstance() {
        if(_instance == null){
            _instance = new WatariChecker();
        }
        return _instance;
    }
//    private static IFBoxManager _ifBoxManager;
//    public void setIFBoxManager( IFBoxManager ifBoxManager) {
//        _ifBoxManager = ifBoxManager ;
//    }

    public void setListener(WatariCheckListener listener) {
        _listener = listener;
    }

    public void check(View view, BusinessType type) {
        if (_listener == null) {
            Timber.e("リスナー未設定");
            return;
        }

        new Thread(() -> {
            Timber.d("和多利ポイント起動前チェック開始");
            final MainApplication app = MainApplication.getInstance();

            //通常モードの場合のみチェックする項目
            if (!AppPreference.isDemoMode()) {
                //電波強度が強or中じゃない場合は使わせない、エラーコード1001
                if (CurrentRadio.getImageLevel() == 0) {
                    _listener.onFinished(MainApplication.getInstance().getString(R.string.error_type_comm_reception));
                    return;
                } else if (CurrentRadio.getImageLevel() == GetRadioService.AIRPLANE_MODE) {
                    //機内モード状態の場合は使わせない、エラーコード1003
                    _listener.onFinished(MainApplication.getInstance().getString(R.string.error_type_airplane_mode));
                    return;
                }

                //係員設定チェック
                if (AppPreference.isDriverCodeInput() && AppPreference.getDriverCode().equals("")) {
                    _listener.onFinished(app.getString(R.string.error_type_payment_system_2013));
                    return;
                }

                // ストライプ情報を読むので一部クレジットの処理を使う
                CreditSettlement creditSettlement = CreditSettlement.getInstance();
                // TODO 読み込みがなんか遅い気がする　こんなもん？　キャッシュとかいる？
                /* カードデータ保護用公開鍵取得 */
                if (CreditSettlement.k_OK != creditSettlement._mcCenterCommManager.creditGetKey()) {
                    if (creditSettlement.getCreditErrorCode().equals("3008")) {
                        _listener.onFinished(app.getString(R.string.error_type_option_service_5992));
                    } else {
                        _listener.onFinished(app.getString(R.string.error_type_option_service_5993));
                    }
                    return;
                }
            }

            //通常モード・デモモード共通でチェックする項目
            if (type == BusinessType.POINT_ADD) {
                // 決済金額下限チェック
                if (Amount.getFixedAmount() <= 0) {
                    _listener.onFinished("2001");
                    return;
                }
                Timber.d("決済金額下限値チェックOK");

                // 決済金額上限チェック
                if (Amount.getFixedAmount() > 999999) {
                    _listener.onFinished(app.getString(R.string.error_type_option_service_5990));
                    return;
                }
                Timber.d("決済金額上限値チェックOK");
            }

//CHG-S BMT S.Oyama 2024/09/24 フタバ双方向向け改修
            //if (!IFBoxAppModels.isMatch(IFBoxAppModels.YAZAKI_LT27_D) && (!IFBoxAppModels.isMatch(IFBoxAppModels.OKABE_MS70_D))) {
            if (!IFBoxAppModels.isMatch(IFBoxAppModels.YAZAKI_LT27_D) && (!IFBoxAppModels.isMatch(IFBoxAppModels.OKABE_MS70_D)) && (!IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D))) {
//CHG-E BMT S.Oyama 2024/09/24 フタバ双方向向け改修
                // プリンター状態のチェック
                int isPrinterSts = Printer.getInstance().getStatus();
                switch (isPrinterSts) {
                    case Printer.PRINTER_OK: // 正常状態（0）
                        break;
                    case Printer.PRINTER_STATUS_BUSY: // ビジー状態（-1）
                        _listener.onFinished(app.getString(R.string.error_type_printer_sts_busy));
                        return;
                    case Printer.PRINTER_STATUS_HIGHT_TEMP: // 高温状態（-2）
                        _listener.onFinished(app.getString(R.string.error_type_printer_sts_hight_temp));
                        return;
                    case Printer.PRINTER_STATUS_PAPER_LACK: // 用紙切れ状態 (-3)
                        _listener.onFinished(app.getString(R.string.error_type_printer_sts_paper_lack));
                        return;
                    case Printer.PRINTER_STATUS_NO_BATTERY: // バッテリー残量不足状態（-4）
                        _listener.onFinished(app.getString(R.string.error_type_printer_sts_no_battery));
                        return;
                    case Printer.PRINTER_STATUS_FEED: // 用紙送り状態（-5）
                        _listener.onFinished(app.getString(R.string.error_type_printer_sts_feed));
                        return;
                    case Printer.PRINTER_STATUS_PRINT: // 印刷状態（-6）
                        _listener.onFinished(app.getString(R.string.error_type_printer_sts_print));
                        return;
                    case Printer.PRINTER_STATUS_FORCE_FEED: // 強制用紙送り状態（-7）
                        _listener.onFinished(app.getString(R.string.error_type_printer_sts_force_feed));
                        return;
                    case Printer.PRINTER_STATUS_POWER_ON: // 電源ON処理中状態（-8）
                        _listener.onFinished(app.getString(R.string.error_type_printer_sts_power_on));
                        return;
                    case Printer.PRINTER_TASKS_FULL: // 処理満載状態（-9）
                        _listener.onFinished(app.getString(R.string.error_type_printer_tasks_full));
                        return;
                    default:
                        // プリンター異常状態（未定義）
                        _listener.onFinished(app.getString(R.string.error_type_printer_sts_undefined) + "@@@" + isPrinterSts + "@@@");
                        return;
                }
                Timber.d("プリンターチェックOK");
            }else{
                // LT27の場合IM-A820未接続状態であれば使わせない
//                if(!_ifBoxManager.isConnected())
//                {
//                    _listener.onFinished(app.getString(R.string.error_type_ifbox_connection_error));
//                    return;
//                }
            }

            // バッテリー状態のチェック
            IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = view.getContext().registerReceiver(null, intentFilter);
            int batteryLevel = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);

            if (batteryLevel <= BATTERY_LOWER_LIMIT) {
                _listener.onFinished(app.getString(R.string.error_type_low_battery_error));
                return;
            }
            Timber.d("バッテリーチェックOK");

            _listener.onFinished(null);
        }).start();
    }
}
