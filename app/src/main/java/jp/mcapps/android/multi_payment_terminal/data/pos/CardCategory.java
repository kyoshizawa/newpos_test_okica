package jp.mcapps.android.multi_payment_terminal.data.pos;

// カードカテゴリの定数 (5:valuedesign対応ハウスマネー)
public enum CardCategory {
    CASH(0),
    CREDIT(1),
    EMONEY_COMMERCIAL(2),
    EMONEY_TRANSPORTATION(3),
    QR(4),
    POSTAL_ORDER(6),
    ;

    private final int _val;

    CardCategory(final int val) {
        _val = val;
    }

    public int getInt() {
        return _val;
    }
}
