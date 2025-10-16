package jp.mcapps.android.multi_payment_terminal.database.pos;

import java.util.Dictionary;

public enum TaxRoundings {
    // 切り捨て
    FLOOR(0),
    // 切り上げ
    CEILING(1),
    // 四捨五入
    ROUND(2),
    ;

    TaxRoundings(int value) {
        this.value = value;
    }

    public final int value;

    public static TaxRoundings valueOf(int value) {
        for (TaxRoundings it: values()) {
            if (it.value == value) {
                return it;
            }
        }
        return FLOOR;
    }
}
