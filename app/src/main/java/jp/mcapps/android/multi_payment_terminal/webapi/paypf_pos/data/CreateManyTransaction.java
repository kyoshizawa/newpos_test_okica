package jp.mcapps.android.multi_payment_terminal.webapi.paypf_pos.data;

import com.google.gson.annotations.Expose;

public class CreateManyTransaction {

    public static class Request {

        /**
         * 必須. 取引データ（複数）
         */
        @Expose
        public RequestTransactionItem[] items;
    }

    public static class RequestTransactionItem {

        /**
         * 必須. サービスインスタンスID
         */
        @Expose
        public String service_instance_id;

        /**
         * 必須. 1:支払 2:取消
         */
        @Expose
        public Integer transaction_type;

        /**
         * 必須. 決済時刻 yyyy-MM-ddTHH:mm:ss.sssZ
         */
        @Expose
        public String transaction_at;

        /**
         * 必須. 端末識別番号(terminal_no)
         */
        @Expose
        public String tid;

        /**
         * 必須. 端末ID (jwtのsub)
         */
        @Expose
        public Long terminal_id;

        /**
         * 必須. 端末シリアル番号
         */
        @Expose
        public String terminal_name;

        /**
         * 必須. 加盟店ID
         */
        @Expose
        public Long merchant_id;

        /**
         * 必須. 店舗ID
         */
        @Expose
        public Long tenant_id;

        /**
         * 必須. 店舗コード
         */
        @Expose
        public String tenant_code;

        /**
         * 必須. 店舗名
         */
        @Expose
        public String tenant_name;

        /**
         * 必須. 伝票番号
         */
        @Expose
        public String transaction_no;

        /**
         * 必須. カード種別 1:CREDIT 2:EMONEY_COMMERCIAL 3:EMONEY_TRANSPORTATION 4:QR 6:POSTAL_ORDER
         */
        @Expose
        public Integer card_category;

        /**
         * 必須. VISA, JCB, MASTER, etc...
         */
        @Expose
        public String card_brand;

        /**
         * 必須. 取引金額
         */
        @Expose
        public Long amount;

        /**
         * 必須. 取引金額のうち現金支払分
         */
        @Expose
        public Long amount_cash;

        /**
         * 必須. スタッフコード
         */
        @Expose
        public String staff_code;

        /**
         * 必須. スタッフ名
         */
        @Expose
        public String staff_name;

        /**
         * 取消し対象のtransaction_at ※transaction_typeが2の時は必須
         */
        @Expose
        public String org_transaction_at;

        /**
         * 取消し対象のtransaction_no ※transaction_typeが2の時は必須
         */
        @Expose
        public String org_transaction_no;

        /**
         * 必須. 未了フラグ
         */
        @Expose
        public boolean is_unexecuted;

        /**
         * 必須. 取引データおよび、取消しデータの明細
         */
        @Expose
        public RequestTransactionDetail[] transaction_details;
    }

    public static class RequestTransactionDetail {

        /**
         * 商品ID
         */
        @Expose
        public Long product_id;

        /**
         * 商品コード
         */
        @Expose
        public String product_code;

        /**
         * 商品名
         */
        @Expose
        public String product_name;

        /**
         * 税区分
         */
        @Expose
        public String product_tax_type;

        /**
         * 軽減税率区分
         */
        @Expose
        public String reduced_tax_type;

        /**
         * 内税／外税
         */
        @Expose
        public String included_tax_type;

        /**
         * 単価
         */
        @Expose
        public Integer unit_price;

        /**
         * 変更前単価
         */
        @Expose
        public Integer org_unit_price;

        /**
         * 個数
         */
        @Expose
        public Integer count;

        /**
         * 手動明細かどうか
         */
        @Expose
        public Boolean is_manual;

        /**
         * バーコード情報
         */
        @Expose
        public RequestBarcodeInformation barcode_info;
    }

    public static class RequestBarcodeInformation {

        /**
         * バーコードの種類: GS1-128, EL-QR
         */
        @Expose
        public String barcode_type;

        /**
         * バーコードの内容
         */
        @Expose
        public String scanned_content;
    }

    public static class Response {

        /**
         * 各取引データに対する結果（成功・失敗）
         */
        @Expose
        public ResponseTransactionResult[] items;
    }

    public static class ResponseTransactionResult {

        /**
         * 取消しデータの伝票番号
         */
        @Expose
        public String transaction_no;

        /**
         * エラー情報
         */
        @Expose
        public Status error;
    }
}
