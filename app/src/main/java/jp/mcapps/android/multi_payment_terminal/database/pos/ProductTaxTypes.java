package jp.mcapps.android.multi_payment_terminal.database.pos;

import org.jetbrains.annotations.NotNull;

/**
 * 以下のURLの定義を参照
 * https://github.com/MobileCreate/pay_pf_grpc/blob/main/proto/paypf/pos/product/product-data.proto
 */
public enum ProductTaxTypes {
    UNKNOWN(0, null),
    TAX(1, "tax"),
    EXEMPTION(2, "exenption"),
    ;

    ProductTaxTypes(int value, String key) {
        this.value = value;
        this.key = key;
    }

    public final int value;
    public final String key;

    public static ProductTaxTypes valueOf(int value) {
        for (ProductTaxTypes it: values()) {
            if (it.value == value) {
                return it;
            }
        }
        return UNKNOWN;
    }

    public static ProductTaxTypes fromKey(@NotNull String key) {
        String s = key.toLowerCase();
        for (ProductTaxTypes it: values()) {
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
