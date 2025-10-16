package jp.mcapps.android.multi_payment_terminal.data;

// 取引結果
public enum TransactionResults {
    None,
    SUCCESS,  // 成功
    FAILURE,  // 失敗
    UNKNOWN,  // 状態不明(通信タイムアウト等により)
}
