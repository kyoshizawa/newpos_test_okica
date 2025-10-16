package jp.mcapps.android.multi_payment_terminal.ui.info;

import android.content.pm.PackageInfo;

import androidx.lifecycle.ViewModel;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import timber.log.Timber;

public class InfoViewModel extends ViewModel {
    public String getVersionName() {
        try {
            PackageInfo packageInfo = MainApplication.getInstance().getPackageManager()
                    .getPackageInfo(MainApplication.getInstance().getPackageName(), 0);
            return "version: " + packageInfo.versionName;
        } catch (Exception e) {
            Timber.e(e);
            return "";
        }
    }

    private final String _apkVersionName = getVersionName();
    public String getApkVersionName() {
        return _apkVersionName;
    }

    public boolean getTerminalAuth() {
        return MainApplication.getInstance().isMcAuthSuccess();
    }

    //会社情報を設定
    public String getMerchantName() {
        if(AppPreference.isServicePos()) {
            // POSアクティベート状態の時、決済システム側のデータではなくPOSのデータを表示する
            String merchantName = AppPreference.getPosMerchantName() == null ? "－" : AppPreference.getPosMerchantName();
            return merchantName;
        } else {
            return AppPreference.getMerchantName();
        }
    }
    public String getMerchantOffice() {
        if(AppPreference.isServicePos()) {
            // POSアクティベート状態の時、決済システム側のデータではなくPOSのデータを表示する
            String merchantOffice = AppPreference.getPosMerchantOffice() == null ? "－" : AppPreference.getPosMerchantOffice();
            return merchantOffice;
        } else {
            return AppPreference.getMerchantOffice();
        }
    }
    public String getMerchantTelNumber() {
        if(AppPreference.isServicePos()) {
            // POSアクティベート状態の時、決済システム側のデータではなくPOSのデータを表示する
            String merchantTelnumber = AppPreference.getPosMerchantTelnumber() == null ? "－" : AppPreference.getPosMerchantTelnumber();
            return merchantTelnumber;
        } else {
            return AppPreference.getMerchantTelnumber();
        }
    }
    public String getTabletVersion() {
        return AppPreference.getTabletVersionInfo().versionName;
    }
    public String getTabletVersionCode() { return AppPreference.getTabletVersionInfo().versionCode.toString(); }


    //各種マネーの有効/無効表示を設定
    public boolean useCredit() {
        return AppPreference.isMoneyCredit();
    }
    public boolean useContactLess() {
        return AppPreference.isMoneyContactless();
    }
    public boolean useSuica() {
        return AppPreference.isMoneySuica();
    }
    public boolean useId() {
        return AppPreference.isMoneyId();
    }
    public boolean useWaon() {
        return AppPreference.isMoneyWaon();
    }
    public boolean useNanaco() {
        return AppPreference.isMoneyNanaco();
    }
    public boolean useEdy() {
        return AppPreference.isMoneyEdy();
    }
    public boolean useQuicpay() {
        return AppPreference.isMoneyQuicpay();
    }
    public boolean useQr() {
        return AppPreference.isMoneyQr();
    }
    public boolean useUnionpay() {
        return AppPreference.isMoneyUnionpay();
    }
    public boolean useOkica() {
        return AppPreference.isMoneyOkica();
    }
    public boolean useOkicaCharge() {
        return AppPreference.getOkicaTerminalInfo().isCharge;
    }
    //各種機能の有効/無効表示を設定
    public boolean useServicePos() {
        return AppPreference.isServicePos();
    }

}
