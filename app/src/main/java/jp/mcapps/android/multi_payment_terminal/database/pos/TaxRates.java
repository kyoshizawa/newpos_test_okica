package jp.mcapps.android.multi_payment_terminal.database.pos;

public enum TaxRates {
    // 標準税率
    STANDARD_TAX_RATE(0),
    // 軽減税率
    REDUCED_TAX_RATE(1),
    ;

    TaxRates(int value) {
        this.value = value;
    }

    public final int value;
}
