package jp.mcapps.android.multi_payment_terminal.database.pos;

public enum GenerationIDs {
    // ダウンロード中レコード
    DOWNLOADING(0),
    // 現在アクティブなレコード
    CURRENTLY_ACTIVE(1),
    ;

    GenerationIDs(int value) {
        this.value = value;
    }

    public final int value;
}
