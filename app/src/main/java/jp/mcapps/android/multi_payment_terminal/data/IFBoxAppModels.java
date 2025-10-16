package jp.mcapps.android.multi_payment_terminal.data;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.webapi.ifbox.data.Version;
import timber.log.Timber;

public class IFBoxAppModels {
    public static final String FUTABA = "FUTABA";
    public static final String NISHIBE = "NISHIBE";
    public static final String YAZAKI_LT24 = "YAZAKI/LT24";
    public static final String YAZAKI_LT26 = "YAZAKI/LT26";
    public static final String YAZAKI_LT27_D = "YAZAKI/LT27-D";
    public static final String OKABE_MS70_D = "OKABE/MS70-D";
//ADD-S BMT S.Oyama 2024/09/03 フタバ双方向向け改修
    public static final String FUTABA_D = "FUTABA-R9-6-D";              //"FUTABA/D"
//ADD-E BMT S.Oyama 2024/09/03 フタバ双方向向け改修
    public static final String FUTABA_D_MANUAL = "FUTABA-R9-6-D-MANUAL";

    public static boolean isMatch(String appModel) {
        Version.Response ifboxVersionInfo = AppPreference.getIFBoxVersionInfo();
        if (null == ifboxVersionInfo) {
//            Timber.i("ifboxVersionInfo = null");
        } else if (null == ifboxVersionInfo.appModel) {
            Timber.e("ifboxVersionInfo.appModel = null");
        } else {
//            Timber.i("ifboxVersionInfo = %s", ifboxVersionInfo);
        }

        return null != ifboxVersionInfo && null != ifboxVersionInfo.appModel && ifboxVersionInfo.appModel.equals(appModel);
    }

    // フタバ双方向か判定（手動決済も込み）
    public static boolean isFutaba() {
        Version.Response ifboxVersionInfo = AppPreference.getIFBoxVersionInfo();
        if (ifboxVersionInfo == null) return false;

        return ifboxVersionInfo.appModel.equals(FUTABA_D) || ifboxVersionInfo.appModel.equals(FUTABA_D_MANUAL);
    }

    //双方向か判定
    public static boolean isDuplex() {
        Version.Response ifboxVersionInfo = AppPreference.getIFBoxVersionInfo();
        if (ifboxVersionInfo == null) return false;

        return ifboxVersionInfo.appModel.equals(OKABE_MS70_D) || ifboxVersionInfo.appModel.equals(YAZAKI_LT27_D) || ifboxVersionInfo.appModel.equals(FUTABA_D);
    }
}
