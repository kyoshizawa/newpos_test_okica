package jp.mcapps.android.multi_payment_terminal.database.pos;

import org.jetbrains.annotations.NotNull;

/**
 * 以下のURLの定義を参照
 * https://github.com/MobileCreate/pay_pf_grpc/blob/main/proto/paypf/pos/product/product-data.proto
 */
public enum IncludedTaxTypes {
    UNKNOWN(0, null),
    INCLUDED(1, "included"),
    EXCLUDED(2, "outsided"),
    EMPTY(3, "empty"),
    ;

    IncludedTaxTypes(int value, String key) {
        this.value = value;
        this.key = key;
    }

    public final int value;
    public final String key;

    public static IncludedTaxTypes valueOf(int value) {
        for (IncludedTaxTypes it: values()) {
            if (it.value == value) {
                return it;
            }
        }
        return UNKNOWN;
    }

    public static IncludedTaxTypes fromKey(@NotNull String key) {
        String s = key.toLowerCase();
        for (IncludedTaxTypes it: values()) {
            if (it.key != null) {
                String k = it.key.toLowerCase();
                if (k.equals(s)) {
                    return it;
                }
            }
        }
        return UNKNOWN;
    }
}
