package jp.mcapps.android.multi_payment_terminal.data;

public class AdditionalSettingKeys {
    private AdditionalSettingKeys() {
        // プライベートコンストラクタでインスタンス化を防ぐ
    }

    public static final String EMV_PINLESS_ENABLE = "EMV.PINLESS_ENABLE.ALLBRAND.CONTACT";
    public static final String EMV_PINLESS_LIMIT_FARE = "EMV.PINLESS_LIMIT_FARE.ALLBRAND.CONTACT";
}
