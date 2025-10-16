package jp.mcapps.android.multi_payment_terminal.webapi.grpc.data;

import com.google.gson.annotations.Expose;

public class GetMasterFile {
    public static class Response {
        @Expose
        public Boolean result;  // 処理の成否 正常：true, 異常：false

        @Expose
        public String errorCode;  // 処理が異常な場合のコード Result: true の場合、本項目は"" となる

        /*** 以降は、Result: true の場合のみ付与 ***/
        @Expose
        public int version;    // バージョン

        @Expose
        public String fileName;    // ファイル名

        @Expose
        public byte[] file;    // ファイル(バイナリ形式)
    }
}
