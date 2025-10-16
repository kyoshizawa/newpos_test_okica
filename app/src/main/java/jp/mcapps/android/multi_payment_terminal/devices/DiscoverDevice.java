package jp.mcapps.android.multi_payment_terminal.devices;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;

import com.epson.epos2.Epos2Exception;
import com.epson.epos2.discovery.DeviceInfo;
import com.epson.epos2.discovery.Discovery;
import com.epson.epos2.discovery.DiscoveryListener;
import com.epson.epos2.discovery.FilterOption;

import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Completable;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.model.DeviceConnectivityManager;
import timber.log.Timber;

public class DiscoverDevice {

    private static String mEpsonPrinterTarget = null;
    private static String mCashChangerTarget = null;

    //private static boolean mDiscoveryDevice = false;

    public static void discovery() {
        FilterOption filterOption = new FilterOption();
        filterOption.setPortType(Discovery.PORTTYPE_ALL);

        Context appContext = MainApplication.getInstance();
        ConnectivityManager connectivityManager = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network defaultNetwork = connectivityManager.getBoundNetworkForProcess();
        connectivityManager.bindProcessToNetwork(null);

        if (mEpsonPrinterTarget == null) {
            // Epson Printerの検索
            filterOption.setDeviceType(Discovery.TYPE_PRINTER);
            Completable completable = Completable.create(emitter -> {
                try {
                    Discovery.start(MainApplication.getInstance().getApplicationContext(), filterOption, new DiscoveryListener() {
                        @Override
                        public void onDiscovery(DeviceInfo deviceInfo) {
                            if (deviceInfo.getDeviceType() == Discovery.TYPE_PRINTER) {
                                Timber.d("Printer Name: " + deviceInfo.getDeviceName());
                                mEpsonPrinterTarget = deviceInfo.getTarget();
                            }
                            emitter.onComplete();
                        }
                    });
                } catch (Epos2Exception e) {
                    emitter.onError(e);
                }
            });
            try {
                completable.blockingAwait(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                // 何もしない
            }
            try {
                Discovery.stop();
            } catch (Epos2Exception e) {
                // 何もしない
            }
        }

        if (mCashChangerTarget == null) {
            // CacheChargerの検索
            filterOption.setDeviceType(Discovery.TYPE_CCHANGER);
            Completable completable = Completable.create(emitter -> {
                try {
                    Discovery.start(MainApplication.getInstance().getApplicationContext(), filterOption, new DiscoveryListener() {
                        @Override
                        public void onDiscovery(DeviceInfo deviceInfo) {
                            if (deviceInfo.getDeviceType() == Discovery.TYPE_CCHANGER) {
                                mCashChangerTarget = deviceInfo.getTarget();
                            }
                            emitter.onComplete();
                        }
                    });
                } catch (Epos2Exception e) {
                    emitter.onError(e);
                }
            });
            try {
                completable.blockingAwait(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                // 何もしない
            }
            try {
                Discovery.stop();
            } catch (Epos2Exception e) {
                // 何もしない
            }
        }

        DeviceConnectivityManager.setProcessMobileNetwork(defaultNetwork);
    }

//    public static boolean checkDiscovery(int type) {
//        FilterOption filterOption = new FilterOption();
//        filterOption.setPortType(Discovery.PORTTYPE_TCP);
//        filterOption.setDeviceType(type);
//
//        Network defaultNetwork = ConnectivityManager.getProcessDefaultNetwork();
//        ConnectivityManager.setProcessDefaultNetwork((Network)null);
//
//        mDiscoveryDevice = false;
//        Completable completable = Completable.create(emitter -> {
//            try {
//                Discovery.start(MainApplication.getInstance().getApplicationContext(), filterOption, new DiscoveryListener() {
//                    @Override
//                    public void onDiscovery(DeviceInfo deviceInfo) {
//                        if (deviceInfo.getDeviceType() == type) {
//                            // 指定されたデバイスを検出した
//                            mDiscoveryDevice = true;
//                        }
//                        emitter.onComplete();
//                    }
//                });
//            } catch (Epos2Exception e) {
//                mDiscoveryDevice = false;
//                emitter.onError(e);
//            }
//        });
//        try {
//            completable.blockingAwait(5, TimeUnit.SECONDS);
//        } catch (Exception e) {
//            // 何もしない
//        }
//        try {
//            Discovery.stop();
//        } catch (Epos2Exception e) {
//            // 何もしない
//        }
//
//        DeviceConnectivityManager.setProcessMobileNetwork(defaultNetwork);
//        return mDiscoveryDevice;
//    }

    public static String getEpsonPrinterTarget() {
        if (mEpsonPrinterTarget == null) {
            discovery();
        }
        return mEpsonPrinterTarget;
    }

    public static String getCashChangerTarget() {
        if (mCashChangerTarget == null) {
            discovery();
        }
        return mCashChangerTarget;
    }
}
