package jp.mcapps.android.multi_payment_terminal.webapi.paypf_pos.data;

import com.google.gson.annotations.Expose;
import org.jetbrains.annotations.Nullable;

public class AuthTest {

    public static class Response {

        /**
         * 端末ID
         */
        @Expose
        public String sub;

        /**
         * トークン有効期限
         */
        @Expose
        public String exp;

        /**
         * 顧客ID
         */
        @Expose
        public String customer_id;

        /**
         * 決済端末識別番号
         */
        @Expose
        public String terminal_no;

        /**
         * ABTサービスインスタンスID
         */
        @Expose
        public String service_instance_abt;

        /**
         * POSサービスインスタンスID
         */
        @Expose
        public String service_instance_pos;

        /**
         * HTTPステータスがエラーの時、APIGatewayのエラーメッセージが入る
         */
        @Expose
        @Nullable
        public String message;
    }
}
