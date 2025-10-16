package jp.mcapps.android.multi_payment_terminal.data.sam;

import androidx.annotation.Nullable;

import com.pos.device.SDKException;

public class SamExceptions {
    /**
     * シンタックスエラー
     */
    public static class SyntaxErrorException extends Throwable {
    }

    /**
     * シーケンス不一致エラー
     */
    public static class SequenceMisMatchException extends Throwable {
    }

    /**
     * ステータスワード異常
     */
    public static class IllegalStatusWordException extends Throwable {
    }

    /**
     * SAM通信で発生したSDKExceptionのラップクラス
     * SDKExceptionにエラーコードを取得するメソッドがないためラップする
     */
    public static class SamSDKException extends Throwable {
        private final int code;
        public int getCode() {
            return code;
        }

        /**
         * コンストラクタ
         *
         * @param e SDKException
         */
        public SamSDKException(SDKException e) {
            int code = 0;

            try {
                code = Integer.parseInt(e.getMessage().split("=")[1]);
            } catch (Exception ignore) {
            }

            this.code = code;
        }
    }

    /**
     * ReadおよびWriteコマンドでステータスフラグが非0の場合に発生する例外
     */
    public static class RWStatusException extends Throwable {
        private final int statusFlg1;
        private final int statusFlg2;

        /**
         * コンストラクタ
         *
         * @param statusFlg1
         * @param statusFlg2
         */
        public RWStatusException(byte statusFlg1, byte statusFlg2) {
            this.statusFlg1 = 0xFF & statusFlg1;
            this.statusFlg2 = 0xFF & statusFlg2;
        }

        @Nullable
        @Override
        public String getMessage() {
            return String.format("ステータスフラグ1=%02X ステータスフラグ2=%02X", statusFlg1, statusFlg2);
        }
    }
}
