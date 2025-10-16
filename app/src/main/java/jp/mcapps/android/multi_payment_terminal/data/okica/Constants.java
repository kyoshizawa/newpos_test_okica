package jp.mcapps.android.multi_payment_terminal.data.okica;

import android.annotation.SuppressLint;
import android.os.Build;

import com.pos.device.config.DevConfig;

import jp.mcapps.android.multi_payment_terminal.util.DeviceUtils;

public class Constants {


    @SuppressLint("HardwareIds")
    //public static final String TERMINAL_INSTALL_ID = "M200000" + DeviceUtils.getSerial();

    // テスト用に固定で
    public static final String TERMINAL_INSTALL_ID = "M2000009320001597";

    // 事業者コード：沖縄ＩＣカード（物販）
    public static final int COMPANY_CODE_BUPPAN = 0x0C02;

    /**
     * アクセスキー暗号化データの複合鍵(2-Key Triple DES)
     */
    public static final byte[] ACCESS_KEY_3DES_KEY = new byte[] {
            (byte)0x10, (byte)0x49, (byte)0x28, (byte)0x60, (byte)0x00, (byte)0x8f, (byte)0x45, (byte)0x42,
            (byte)0xfd, (byte)0x56, (byte)0x49, (byte)0xf9, (byte)0xe1, (byte)0x9c, (byte)0xf7, (byte)0x26,
            (byte)0x10, (byte)0x49, (byte)0x28, (byte)0x60, (byte)0x00, (byte)0x8f, (byte)0x45, (byte)0x42
    };

    /**
     * アクセスキー暗号化データの初期ベクター
     */
    public static byte[] ACCESS_KEY_3DES_IV = new byte[] {
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00
    };

    public static class TicketTypeCodes {
        public static final int ADULT = 0;
        public static final int CHILD = 1;
        public static final int ADULT_DISCOUNT = 2;
        public static final int CHILD_DISCOUNT = 3;
    }
}
