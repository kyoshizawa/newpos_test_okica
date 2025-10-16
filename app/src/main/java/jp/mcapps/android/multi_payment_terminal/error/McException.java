package jp.mcapps.android.multi_payment_terminal.error;

import org.jetbrains.annotations.NotNull;

public class McException extends RuntimeException {
    // MC定義のエラーコード
    private final String _mcErrorCode;
    public String getMcErrorCode() {
        return _mcErrorCode;
    }

    // 元々のエラーコード
    private String _originErrorCode = "";
    public String getOriginErrorCode() {
        return _originErrorCode;
    }

    public McException(@NotNull String mcErrorCode) {
        _mcErrorCode = mcErrorCode;
    }

    public McException(@NotNull String mcErrorCode, @NotNull String originErrorCode) {
        _mcErrorCode = mcErrorCode;
        _originErrorCode = originErrorCode;
    }
}
