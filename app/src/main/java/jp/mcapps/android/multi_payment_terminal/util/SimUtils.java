package jp.mcapps.android.multi_payment_terminal.util;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.pos.device.sys.SystemManager;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class SimUtils {

    private static boolean _init = false;
    public static void SetInitSDK(boolean flg) {
        _init = flg;
    }

    /**
     * ICCIDを取得します。Android Version により処理が変わります
     * @param context
     * @return
     */
    @SuppressLint("MissingPermission")
    public static String getIccId(Context context) {
        // OS5 or 権限有の場合  SIM未挿入等で取れない場合は空文字
        // Android 13 で取得できなくなっている（アプリが落ちる）ため別の対応
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                if (!_init) return "";

                String iccid = "";
                for (int slot : getSlots()) {
                    iccid = SystemManager.getIccid(slot); // スロット
                    if (!TextUtils.isEmpty(iccid)) {
                        break;
                    }
                }
                if (TextUtils.isEmpty(iccid)) {
                    Timber.i("Android 10以降 && iccId が取得できないため、iccIdは空文字に設定");
                    return "";
                }
                return iccid;
            } catch (Exception x) {
                // SDKエラー等
                Timber.e(x, "getIccId でエラー");
                return "";
            }
        } else {
            // 従来
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            return tm.getSimSerialNumber() == null ? "" : tm.getSimSerialNumber();
        }
    }

    /**
     * Imsiを取得します。Android Version により処理が変わります
     * @param context
     * @return
     */
    @SuppressLint("MissingPermission")
    public static String getImsi(Context context) {
        // OS5 or 権限有の場合  SIM未挿入等で取れない場合は空文字
        // Android 13 で取得できなくなっている（アプリが落ちる）ため別の対応
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                if (!_init) return "";

                String imsi = "";
                for (int slot : getSlots()) {
                    imsi = SystemManager.getIMSI(slot); // スロット
                    if (!TextUtils.isEmpty(imsi)) {
                        break;
                    }
                }
                if (TextUtils.isEmpty(imsi)) {
                    Timber.i("Android 10以降 && imsi が取得できないため、imsiは空文字に設定");
                    return "";
                }
                return imsi;
            } catch (Exception x) {
                // SDKエラー等
                Timber.e(x, "getImsi でエラー");
                return "";
            }
        } else {
            // 従来
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            return tm.getSubscriberId() == null ? "" : tm.getSubscriberId();
        }
    }

    /**
     * Imeiを取得します。Android Version により処理が変わります
     * @param context
     * @return
     */
    @SuppressLint("MissingPermission")
    public static String getImei(Context context) {
        // OS5 or 権限有の場合  SIM未挿入等で取れない場合は空文字
        // Android 13 で取得できなくなっている（アプリが落ちる）ため別の対応
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                if (!_init) return "";

                String imei = "";
                for (int slot : getSlots()) {
                        imei = SystemManager.getImei(slot); // スロット
                        if (!TextUtils.isEmpty(imei)) {
                            break;
                        }
                }
                if (TextUtils.isEmpty(imei)) {
                    Timber.i("Android 10以降 && imei が取得できないため、imeiは空文字に設定");
                    return "";
                }
                return imei;
            } catch (Exception x) {
                // SDKエラー等
                Timber.e(x, "getImei でエラー");
                return "";
            }
        } else {
            // 従来
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            return tm.getDeviceId() == null ? "" : tm.getDeviceId();
        }
    }

    /**
     * 一度 getImsi して SIMが刺さっているスロットをとる
     * なければ SIM_CARD_1 を返す
     * @return
     */
    private static int[] getSlots() {
        int[] slots = new int[] { SystemManager.SIM_CARD_1, SystemManager.SIM_CARD_2 };

        if (!_init) return slots;

        List<Integer> valids = new ArrayList();

        for (int slot : slots) {
            String imsi = SystemManager.getIMSI(slot); // スロット
            if (!TextUtils.isEmpty(imsi)) {
                valids.add(slot);
            }
        }

        if (valids.isEmpty()) {
            return new int[] { SystemManager.SIM_CARD_1 };
        }

        int[] result = new int[valids.size()];
        for (int i = 0; i < valids.size(); i++) {
            result[i] = valids.get(i);
        }
        return slots;
    }

}
