package jp.mcapps.android.multi_payment_terminal.webapi.grpc.data;

import com.google.gson.annotations.Expose;

public class TerminalInfo {
    public static class Response {
        @Expose
        public Boolean result;  // 処理の成否 正常：true, 異常：false

        @Expose
        public String errorCode;  // 処理が異常な場合のコード Result: true の場合、本項目は"" となる

        /*** 以降は、Result: true の場合のみ付与 ***/
        @Expose
        public String terminalId;   // 端末識別番号

        @Expose
        public  String shopCd;  // 店舗識別コード

        @Expose
        public String machineModelCd;   // 機種コード

        @Expose
        public String machineId;    // 機器ID(= 物販端末ID)

        @Expose
        public boolean isCharge;    // チャージフラグ

        @Expose
        public boolean isInstalled;    // 端末設置フラグ
    }
}
