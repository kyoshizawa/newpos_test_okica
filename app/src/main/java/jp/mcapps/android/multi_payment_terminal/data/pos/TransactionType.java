package jp.mcapps.android.multi_payment_terminal.data.pos;

// 取引種別の定数
public enum TransactionType {
    PROCEEDS(1),
    CANCEL(2),
    ;

    private final int _val;

    TransactionType(final int val) {
        _val = val;
    }

    public int getInt() {
        return _val;
    }
}
