package jp.mcapps.android.multi_payment_terminal.data.okica;

/**
 * OKICAカード処理でのレスポンスデータのクラスです
 * カード処理正常終了時はdataに値が入りerrorはnullとなります
 * カード処理異常終了時はdataはnullとなりerrorに値が入ります
 *
 * @param <T> レスポンスデータのクラス
 */
public class OkicaCardResponse<T> {
    private T data;
    public T getData() {
        return data;
    }
    public OkicaCardResponse<T> setData(T data) {
        this.data = data;

        return this;
    }

    private Throwable error;
    public Throwable getError() {
        return error;
    }
    public boolean hasError() {
        return error != null;
    }
    public OkicaCardResponse<T> setError(Throwable error) {
        this.error = error;

        return this;
    }
}
