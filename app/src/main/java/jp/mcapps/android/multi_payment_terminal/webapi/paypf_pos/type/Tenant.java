package jp.mcapps.android.multi_payment_terminal.webapi.paypf_pos.type;

import com.google.gson.annotations.Expose;

/**
 * 以下のURLに定義されているTenantProductを参照してください
 * https://github.com/MobileCreate/pay_pf_grpc/blob/main/proto/paypf/pos/product/tenant-data.proto
 */
public class Tenant {

    @Expose
    public long id;

    @Expose
    public String tenant_code;

    @Expose
    public long merchant_id;

    @Expose
    public String customer_code;

    @Expose
    public String name;

    @Expose
    public String name_kana;

    @Expose
    public String zipcode;

    @Expose
    public int pref_cd;

    @Expose
    public String city;

    @Expose
    public String address_line1;

    @Expose
    public String address_line2;

    @Expose
    public String address_line3;

    @Expose
    public String kana_city;

    @Expose
    public String address_kana_line1;

    @Expose
    public String address_kana_line2;

    @Expose
    public String address_kana_line3;

    @Expose
    public String phone_number;

    @Expose
    public String fax;

    @Expose
    public String houjin_bangou;

    @Expose
    public String alphabet_name;

    @Expose
    public boolean use_qr;

    @Expose
    public ParentTenantAndMerchatInfo parentInfo;

    public class ParentTenantAndMerchatInfo {
        @Expose
        public long id;

        @Expose
        public String tenant_code;

        @Expose
        public long merchant_id;

        @Expose
        public String customer_code;

        @Expose
        public String name;

        @Expose
        public String name_kana;

        @Expose
        public String zipcode;

        @Expose
        public int pref_cd;

        @Expose
        public String city;

        @Expose
        public String address_line1;

        @Expose
        public String address_line2;

        @Expose
        public String address_line3;

        @Expose
        public String kana_city;

        @Expose
        public String address_kana_line1;

        @Expose
        public String address_kana_line2;

        @Expose
        public String address_kana_line3;

        @Expose
        public String phone_number;

        @Expose
        public String fax;

        @Expose
        public String houjin_bangou;

        @Expose
        public String alphabet_name;
    }
}
