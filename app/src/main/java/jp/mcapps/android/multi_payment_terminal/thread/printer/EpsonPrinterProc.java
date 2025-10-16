package jp.mcapps.android.multi_payment_terminal.thread.printer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.ConnectivityManager;
import android.net.Network;

import com.epson.epos2.Epos2CallbackCode;
import com.epson.epos2.Epos2Exception;
import com.epson.epos2.printer.Printer;
import com.epson.epos2.printer.PrinterStatusInfo;
import com.epson.epos2.printer.ReceiveListener;
import com.pos.device.printer.PrintCanvas;

import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Single;
import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.devices.DiscoverDevice;
import jp.mcapps.android.multi_payment_terminal.model.DeviceConnectivityManager;
import timber.log.Timber;

public class EpsonPrinterProc {
    private static EpsonPrinterProc _instance = null;
    private Printer _printer = null;

    private final String LOGTAG = "EpsonPrinter";

    public static EpsonPrinterProc getInstance() {
        if (_instance == null) {
            _instance = new EpsonPrinterProc();
        }
        return _instance;
    }

    private Printer getPrinter() {
        if (_printer == null) {
            try {
                int printerSeries = -1;
                if (AppPreference.getIsCashChanger()) {
                    printerSeries = Printer.TM_T70;
                }
                else if (AppPreference.getIsCashDrawerTypePonly() || AppPreference.getIsCashDrawerTypeAll()) {
                    printerSeries = Printer.TM_M30III;   // TM-m30II用
                }
                _printer = new Printer(printerSeries, Printer.MODEL_JAPANESE, MainApplication.getInstance().getApplicationContext());
            } catch (Epos2Exception e) {
                _printer = null;
            }
        }
        return _printer;
    }

    public int print(PrintCanvas _printCanvas) {
        String epsonPrinterTarget = DiscoverDevice.getEpsonPrinterTarget();
        if (epsonPrinterTarget == null) {
            return Epos2CallbackCode.CODE_ERR_SYSTEM;
        }
        Printer printer = getPrinter();
        if (printer == null) {
            return Epos2CallbackCode.CODE_ERR_SYSTEM;
        }
        // プロセスのデフォルトがモバイルネットワークになっていると有線LANのプリンタ接続に失敗するので、
        // 一時的にデフォルトなしにして有線LANを有効化する
        // 最後にプロセスのデフォルトネットワークを戻すため、現在のデフォルトネットワークを覚えておく
        Context appContext = MainApplication.getInstance();
        ConnectivityManager connectivityManager = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network defaultNetwork = connectivityManager.getBoundNetworkForProcess();
        connectivityManager.bindProcessToNetwork((Network) null);
        int ret = Epos2CallbackCode.CODE_SUCCESS;
        try {
            if (printer.getStatus().getConnection() != Printer.TRUE) {
                printer.connect(epsonPrinterTarget, Printer.PARAM_DEFAULT);
            }
            printer.clearCommandBuffer();
            Bitmap orgBitmap = _printCanvas.getBitmap();
            // 中央に寄せるため左側に16ドット追加する
            int margin = 16;
//            int margin = 96;    // TM-m30II 80mm用紙用
            Bitmap newBitmap = Bitmap.createBitmap(orgBitmap.getWidth() + margin, orgBitmap.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(newBitmap);
            canvas.drawBitmap(orgBitmap, margin, 0, null);
            // 印刷
            printer.addImage(newBitmap, 0, 0, newBitmap.getWidth(), newBitmap.getHeight(), Printer.PARAM_DEFAULT, Printer.PARAM_DEFAULT, Printer.PARAM_DEFAULT, Printer.PARAM_DEFAULT, Printer.PARAM_DEFAULT);
            printer.addCut(Printer.PARAM_DEFAULT);
            Single<Object> single = Single.create(emitter -> {
                printer.setReceiveEventListener(new ReceiveListener() {
                    @Override
                    public void onPtrReceive(Printer printer, int code, PrinterStatusInfo status, String printJobId) {
                        if (code != Epos2CallbackCode.CODE_PRINTING) {
                            emitter.onSuccess(code);
                        }
                    }
                });
            })
            .timeout(60, TimeUnit.SECONDS)
            .onErrorReturnItem(Epos2CallbackCode.CODE_ERR_TIMEOUT);

            printer.sendData(Printer.PARAM_DEFAULT);
            ret = (int) single.blockingGet();
            Timber.tag(LOGTAG).i("Result sendData result:%d", ret);
        } catch (Epos2Exception e) {
            ret = Epos2CallbackCode.CODE_ERR_SYSTEM;
            Timber.tag(LOGTAG).e(e, "Exception sendData: status %d", e.getErrorStatus());
        } finally {
            if (printer != null && printer.getStatus().getConnection() == Printer.TRUE) {
                try {
                    printer.disconnect();
                } catch (Epos2Exception e) {
                    Timber.tag(LOGTAG).i("Exception disconnect");
                    // 何もしない
                    // 印刷に失敗していれば、どこかでfalseになっている
                    // disconnectのみ失敗した場合、印刷は成功しているのでtrueを返すべき
                    // (たぶん次の印刷時にconnectで失敗してfalseを返すことになるはず)
                }
            }
//            // プロセスのデフォルトネットワークを元に戻す
//            ConnectivityManager.setProcessDefaultNetwork(defaultNetwork);
            DeviceConnectivityManager.setProcessMobileNetwork(defaultNetwork);
        }
        return ret;
    }

    public boolean checkConnect()
    {
        boolean ret = true;
        Printer printer = null;

        String epsonPrinterTarget = DiscoverDevice.getEpsonPrinterTarget();
        if (epsonPrinterTarget == null) {
            ret = false;
        } else {
            printer = getPrinter();
            if (printer == null) {
                ret = false;
            }
        }

        // プロセスのデフォルトがモバイルネットワークになっていると有線LANのプリンタ接続に失敗するので、
        // 一時的にデフォルトなしにして有線LANを有効化する
        // 最後にプロセスのデフォルトネットワークを戻すため、現在のデフォルトネットワークを覚えておく
        Context appContext = MainApplication.getInstance();
        ConnectivityManager connectivityManager = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network defaultNetwork = connectivityManager.getBoundNetworkForProcess();
        connectivityManager.bindProcessToNetwork((Network) null);

        if (ret) {
            try {
                if (printer.getStatus().getConnection() != Printer.TRUE) {
                    printer.connect(epsonPrinterTarget, Printer.PARAM_DEFAULT);
                }
            } catch (Epos2Exception e) {
                ret = false;
            } finally {
                if (printer != null && printer.getStatus().getConnection() == Printer.TRUE) {
                    try {
                        printer.disconnect();
                    } catch (Epos2Exception e) {
                    }
                }
            }
        }

        DeviceConnectivityManager.setProcessMobileNetwork(defaultNetwork);
        return ret;
    }
}
