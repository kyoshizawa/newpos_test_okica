package jp.mcapps.android.multi_payment_terminal.util;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.pos.device.config.DevConfig;
import com.pos.device.sys.SystemManager;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class DeviceUtils {

    private static boolean _init = false;
    public static void SetInitSDK(boolean flg) {
        _init = flg;
    }

    /**
     * シリアルを取得します。
     * @return
     */
    public static String getSerial() {
        // Android 13 対応
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                if (!_init) return "";

                return DevConfig.getSN();
            } catch (Exception x) {
                // SDKエラー等
                Timber.e(x, "getSerial でエラー");
                return "";
            }
        } else {
            // 従来
            return Build.SERIAL;
        }
    }
}
