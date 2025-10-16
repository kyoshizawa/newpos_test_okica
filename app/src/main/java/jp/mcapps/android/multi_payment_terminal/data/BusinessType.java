package jp.mcapps.android.multi_payment_terminal.data;

public enum BusinessType {
    PAYMENT,           // 支払
    BALANCE,           // 残高照会
    REFUND,            // 取消
    RECOVERY_PAYMENT,  // 支払処理未了リカバリ
    RECOVERY_REFUND,   // 取消処理未了リカバリ
    CHARGE,            // チャージ
    POINT_ADD,         // ポイント付与
    POINT_REFUND,      // ポイント取消
}
