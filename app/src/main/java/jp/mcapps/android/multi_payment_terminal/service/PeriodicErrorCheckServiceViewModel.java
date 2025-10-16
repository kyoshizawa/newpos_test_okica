package jp.mcapps.android.multi_payment_terminal.service;

import androidx.lifecycle.ViewModel;

import com.google.gson.Gson;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.data.Amount;
import jp.mcapps.android.multi_payment_terminal.data.IFBoxAppModels;
import jp.mcapps.android.multi_payment_terminal.logger.EventLogger;
import jp.mcapps.android.multi_payment_terminal.model.IFBoxManager;
import jp.mcapps.android.multi_payment_terminal.model.McTerminal;
import jp.mcapps.android.multi_payment_terminal.ui.Converters;
import timber.log.Timber;

public class PeriodicErrorCheckServiceViewModel  extends ViewModel {
    private final IFBoxManager _ifBoxManager;
    private final EventLogger _eventLogger;

    public PeriodicErrorCheckServiceViewModel(IFBoxManager ifBoxManager, EventLogger eventLogger) {
        _ifBoxManager = ifBoxManager;
        _eventLogger = eventLogger;
    }

    protected boolean isIFBoxConnected() {
        return _ifBoxManager.isConnected();
    }

    protected boolean isAccOn_IMA820() {
        return _ifBoxManager.isAccOn_IMA820();
    }

    private String _meterStatus = "";
    protected Disposable getMeterInfo() {
        return _ifBoxManager.getMeterInfo()
                .subscribeOn(Schedulers.io())
                .subscribe(meter -> {

                    if (_meterStatus.equals("KUUSYA") && meter.status.equals("GEISYA")
                            || _meterStatus.equals("KUUSYA") && meter.status.equals("JISSYA")
                            || _meterStatus.equals("KUUSYA") && meter.status.equals("WARIMASI")) {
                        /* 以下のメータータリフ変化が発生した場合、売上送信する
                            1：空車 -> 迎車
                            2：空車 -> 実車
                            3：空車 -> 割増
                         */
                        new McTerminal().postPayment();

                        // OKICA売上送信
                        if (AppPreference.isOkicaCommunicationAvailable()) {
                            new McTerminal().postOkicaPayment();
                        }
                    }

                    if (_meterStatus.equals("KUUSYA") && meter.status.equals("GEISYA")
                            || _meterStatus.equals("KUUSYA") && meter.status.equals("JISSYA")
                            || _meterStatus.equals("KUUSYA") && meter.status.equals("WARIMASI")
                            || _meterStatus.equals("KUUSYA") && meter.status.equals("SIHARAI")) {
                        /* 以下のメータータリフ変化が発生した場合、増減額と定額をクリアする
                            1：空車 -> 迎車
                            2：空車 -> 実車
                            3：空車 -> 割増
                            4：空車 -> 支払
                         */
                        Amount.setFlatRateAmount(0);
                        Amount.setTotalChangeAmount(0);
                    }

//CHG-S BMT S.Oyama 2024/09/24 フタバ双方向向け改修
                    //if (IFBoxAppModels.isMatch(IFBoxAppModels.YAZAKI_LT27_D) || IFBoxAppModels.isMatch(IFBoxAppModels.OKABE_MS70_D)) {
                    if (IFBoxAppModels.isMatch(IFBoxAppModels.YAZAKI_LT27_D) || IFBoxAppModels.isMatch(IFBoxAppModels.OKABE_MS70_D) ) {
//CHG-E BMT S.Oyama 2024/09/24 フタバ双方向向け改修
                        /* LT-27双方向、岡部メーター双方向で以下のタリフ変化が発生した場合、それ以前の取引は再印字不可となる
                            1：空車 -> 実車
                            2：空車 -> 支払
                            3：空車 -> 割増
                            4：迎車 -> 実車
                            5：迎車 -> 支払
                            6：迎車 -> 割増
                         */
                        boolean beforeStatus = _meterStatus.equals("KUUSYA") || _meterStatus.equals("GEISYA");
                        boolean currentStatus = meter.status.equals("JISSYA") || meter.status.equals("SIHARAI") || meter.status.equals("WARIMASI");
                        if (beforeStatus && currentStatus) {
                            String datetime = Converters.dateToString(new Date());
                            AppPreference.setDatetimeLt27Printable(datetime);
                        }
//CHG-S BMT S.Oyama 2024/11/14 フタバ双方向向け改修
                    } else if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == true)
                    {
                        //FUTABA双方向で以下のタリフ変化が発生した場合、それ以前の取引は再印字不可となる
                        /*
                        1：支払 -> 空車
                        2：支払 -> 実車 20250328 t.wada 空車のみに変更
                        */

                        Timber.i("[FUTABA-D] Tarifu Change Check : BeforeStatus = %s, CurrentStatus = %s", _meterStatus, meter.status);
                        boolean beforeStatus =  _meterStatus.equals("SIHARAI");
                        //boolean currentStatus = meter.status.equals("JISSYA") || meter.status.equals("KUUSYA");
                        boolean currentStatus = meter.status.equals("KUUSYA");
                        if (beforeStatus && currentStatus) {
                            String datetime = Converters.dateToString(new Date());
                            AppPreference.setDatetimeLt27Printable(datetime);
                        }

                        AppPreference.setMeterStatus(meter.status);
                    }
//CHG-E BMT S.Oyama 2024/11/14 フタバ双方向向け改修
                    _meterStatus = meter.status;
                });
    }

    protected void fetchMeter() {
        //メータ状態の初期値取得 取得出来た場合はonNextが呼ばれるのでgetMeterInfoのsubscribeで処理される
        Disposable d = _ifBoxManager.fetchMeter()
                .timeout(5, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .subscribe(() -> {}, error -> {});
    }
}
