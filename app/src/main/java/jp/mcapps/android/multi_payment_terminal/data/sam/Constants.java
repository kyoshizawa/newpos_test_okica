package jp.mcapps.android.multi_payment_terminal.data.sam;

public class Constants {
    /**
     * OKICAシステムコード
     */
    public static final byte[] SYSTEM_CODE = new byte[] {(byte)0x8F, (byte)0xC1 };

    /**
     * 工場出荷状態のキーバージョン
     */
    public static final byte[] INITIAL_KEY_VERSION = new byte[] { (byte)0x01, (byte)0x00 };

    /**
     * MCキーのバージョン
     */
    public static final byte[] MC_KEY_VERSION = new byte[] { (byte)0x02, (byte)0x00 };

    /**
     * 工場出荷状態のAdmin鍵
     */
    public static final byte[] INITIAL_ADMIN_KEY = new byte[] {
            (byte)0x11, (byte)0x11, (byte)0x11, (byte)0x11, (byte)0x11, (byte)0x11, (byte)0x11, (byte)0x11,
            (byte)0x22, (byte)0x22, (byte)0x22, (byte)0x22, (byte)0x22, (byte)0x22, (byte)0x22, (byte)0x22,
            (byte)0x33, (byte)0x33, (byte)0x33, (byte)0x33, (byte)0x33, (byte)0x33, (byte)0x33, (byte)0x33,
    };

    /**
     * MC設定のAdmin鍵
     */
    public static final byte[] MC_ADMIN_KEY = new byte[] {
            (byte)0x79, (byte)0x76, (byte)0x09, (byte)0x91, (byte)0x54, (byte)0x24, (byte)0x95, (byte)0x88,
            (byte)0x19, (byte)0x61, (byte)0x00, (byte)0x52, (byte)0x45, (byte)0x66, (byte)0x67, (byte)0x14,
            (byte)0x78, (byte)0x00, (byte)0x21, (byte)0x66, (byte)0x68, (byte)0x11, (byte)0x43, (byte)0x00,
    };

    /**
     * 工場出荷状態のNormal鍵
     */
    public static final byte[] INITIAL_NORMAL_KEY = new byte[] {
            (byte)0x66, (byte)0x66, (byte)0x66, (byte)0x66, (byte)0x66, (byte)0x66, (byte)0x66, (byte)0x66,
            (byte)0x77, (byte)0x77, (byte)0x77, (byte)0x77, (byte)0x77, (byte)0x77, (byte)0x77, (byte)0x77,
            (byte)0x88, (byte)0x88, (byte)0x88, (byte)0x88, (byte)0x88, (byte)0x88, (byte)0x88, (byte)0x88,
    };

    /**
     * MC設定のNormal鍵
     */
    public static final byte[] MC_NORMAL_KEY = new byte[] {
            (byte)0x89, (byte)0x96, (byte)0x02, (byte)0x52, (byte)0x40, (byte)0x54, (byte)0x51, (byte)0x15,
            (byte)0x37, (byte)0x38, (byte)0x95, (byte)0x15, (byte)0x43, (byte)0x62, (byte)0x62, (byte)0x52,
            (byte)0x24, (byte)0x34, (byte)0x31, (byte)0x67, (byte)0x74, (byte)0x44, (byte)0x91, (byte)0x55,
    };

    /**
     * 工場出荷状態のパッケージ生成鍵
     */
    public static final byte[] INITIAL_USER_PACKAGE_KEY = new byte[] {
            (byte)0xBB, (byte)0xBB, (byte)0xBB, (byte)0xBB, (byte)0xBB, (byte)0xBB, (byte)0xBB, (byte)0xBB,
            (byte)0xCC, (byte)0xCC, (byte)0xCC, (byte)0xCC, (byte)0xCC, (byte)0xCC, (byte)0xCC, (byte)0xCC,
    };

    /**
     * MC設定のパッケージ生成鍵
     */
    public static final byte[] MC_USER_PACKAGE_KEY = new byte[] {
            (byte)0xCC, (byte)0xDE, (byte)0xBF, (byte)0xFF, (byte)0xAC, (byte)0xAB, (byte)0xCF, (byte)0xAD,
            (byte)0xAA, (byte)0xDF, (byte)0xAE, (byte)0xBA, (byte)0xCB, (byte)0xED, (byte)0xBB, (byte)0xFE,
    };

    /**
     * GSKコード
     */
    public static final byte[] GSK_CODE = new byte[] { (byte)0x00, (byte)0x01 };

    /**
     * GSKバージョン
     */
    public static final byte[] GSK_VERSION = new byte[] { (byte)0x01, (byte)0x00 };

    /**
     * USKコード
     */
    public static final byte[] USK_CODE = new byte[] { (byte)0x00, (byte)0x02 };

    /**
     * USKバージョン
     */
    public static final byte[] USK_VERSION = new byte[] { (byte)0x02, (byte)0x00 };

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
}
