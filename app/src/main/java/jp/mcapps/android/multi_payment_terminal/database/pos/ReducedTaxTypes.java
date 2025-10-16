package jp.mcapps.android.multi_payment_terminal.database.pos;

import org.jetbrains.annotations.NotNull;

public enum ReducedTaxTypes {
    UNKNOWN(0, null),
    EXEMPTION(1, "exenption"),
    GENERAL(2, "general"),
    REDUCED(3, "reduced"),
    ;

    ReducedTaxTypes(int value, String key) {
        this.value = value;
        this.key = key;
    }

    public final int value;
    public final String key;

    public static ReducedTaxTypes valueOf(int value) {
        for (ReducedTaxTypes it: values()) {
            if (it.value == value) {
                return it;
            }
        }
        return UNKNOWN;
    }

    public static ReducedTaxTypes fromKey(@NotNull String key) {
        String s = key.toLowerCase();
        for (ReducedTaxTypes it: values()) {
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
