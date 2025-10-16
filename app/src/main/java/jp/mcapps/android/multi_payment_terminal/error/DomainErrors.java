package jp.mcapps.android.multi_payment_terminal.error;

import org.jetbrains.annotations.NotNull;

public enum DomainErrors {
    //
    // エラー定義
    //

    // 汎用的なエラー (意味はgRPCのエラーコードを参照)
    // https://qiita.com/Hiraku/items/0549e4cf7079d22b27e8
    // TODO ... 汎用的なエラーは必要かどうかわからんけど一応記述してみた
    CANCELLED(1),
    UNKNOWN(2),
    INVALID_ARGUMENT(3),
    DEADLINE_EXCEEDED(4),
    NOT_FOUND(5),
    ALREADY_EXISTS(6),
    PERMISSION_DENIED(7),
    RESOURCE_EXHAUSTED(8),
    FAILED_PRECONDITION(9),
    ABORTED(10),
    OUT_OF_RANGE(11),
    UNIMPLEMENTED(12),
    INTERNAL(13),
    UNAVAILABLE(14),
    DATA_LOSS(15),
    UNAUTHENTICATED(16),

    // POS固有のドメインエラー（エラーメッセージ用）
    POS_DOMAIN_ERRORS(7000),
    POS_SERVICE_INSTANCE_IS_NOT_ASSIGNED(7001), // POSサービスインスタンスIDが空白
    POS_SERVICE_RESPONSE_ERROR(7097), // POSサービスの応答エラー
    POS_SERVICE_NETWORK_ERROR(7098), // POSサービスの通信エラー
    POS_SERVICE_UNKNOWN_ERROR(7099), // POSサービス状態が不正

    TICKET_SERVICE_INSTANCE_IS_NOT_ASSIGNED(8001), // チケット販売サービスインスタンスIDが空白
    TICKET_SALES_SERVICE_RESPONSE_ERROR(8097), // チケット販売サービスの応答エラー
    TICKET_SALES_SERVICE_NETWORK_ERROR(8098), // チケット販売サービスの通信エラー
    TICKET_SALES_SERVICE_UNKNOWN_ERROR(8099), // チケット販売サービス状態が不正

    // TODO ... ドメイン固有のエラー定義を追加
    ;

    //
    // 以降は、例外の実装部分
    //

    DomainErrors(int code) {
        this.code = code;
    }

    public final int code;

    public class Exception extends java.lang.Exception {
        Exception() {
            super();
        }

        Exception(String msg) {
            super(msg);
        }

        public DomainErrors getError() {
            return DomainErrors.this;
        }
    }

    // 例外をthrowする
    public void raise() throws Exception {
        throw new Exception();
    }

    // 例外をthrowする
    public void raise(@NotNull String msg) throws Exception {
        throw new Exception(msg);
    }
}
