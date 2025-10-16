package jp.mcapps.android.multi_payment_terminal.data.okica;

import com.pos.device.SDKException;

public class OkicaExceptions {
    /**
     * R/Wタイムアウトエラー
     */
    public static class RWTimeoutException extends Throwable {
    }

    /**
     * SAMエラー
     */
    public static class SamCommandException extends Throwable {
        public SamCommandException() {
        }

        public SamCommandException(Throwable innerException)
        {
            this.innerException = innerException;
        }
        private Throwable innerException;

        public Throwable getInnerException() {
            return innerException;
        }
    }

    /**
     * 中断エラー
     */
    public static class TerminationException extends Throwable {
    }

    /**
     * OKICAカードR/Wで発生したSDKExceptionのラップクラス
     * SDKExceptionにエラーコードを取得するメソッドがないためラップする
     */
    public static class OkicaSDKException extends Throwable {
        private final int code;
        public int getCode() {
            return code;
        }

        /**
         * コンストラクタ
         *
         * @param e SDKException
         */
        public OkicaSDKException(SDKException e) {
            int code = 0;

            try {
                code = Integer.parseInt(e.getMessage().split("=")[1]);
            } catch (Exception ignore) {
            }

            this.code = code;
        }
    }
}
