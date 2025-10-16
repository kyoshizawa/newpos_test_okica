package jp.mcapps.android.multi_payment_terminal.webapi.gmo.data;

import com.google.gson.annotations.Expose;

public class PostLogin {
    public static class Request {
        @Expose
        public String loginId;

        @Expose
        public String userPassword;

        @Expose
        public String osName;

        @Expose
        public String osVersion;

        @Expose
        public String serialNo;
    }

    public static class Response {
        @Expose
        public String returnCode;

        @Expose
        public String returnMessage;

        @Expose
        public String msgSummaryCode;

        @Expose
        public String msgSummary;

        @Expose
        public Result result;

        @Expose
        public String balanceAmount;
    }

    public static class Result {
        @Expose
        public String credentialKey;

        @Expose
        public String partnerFullName;

        @Expose
        public String description;

        @Expose
        public String adminPassword;

        @Expose
        public String authForRefund;

        @Expose
        public String cashNumber;

        @Expose
        public String warningWord;

        @Expose
        public String checkSnFlag;

        @Expose
        public String merchantFullName;

        @Expose
        public String merchantName;

        @Expose
        public String merchantKanaName;

        @Expose
        public String prefectures;

        @Expose
        public String city;

        @Expose
        public String street;

        @Expose
        public String address;

        @Expose
        public String contactPhoneNum;

        @Expose
        public String email;

        @Expose
        public String contactHomeUrl;

        @Expose
        public String cqrProvisionMethodashNumber;

        @Expose
        public String merchantId;

        @Expose
        public String pwChangedFlag;

        @Expose
        public PayTypeList[] payTypeList;
    }

    public static class PayTypeList {
        @Expose
        public String payTypeId;

        @Expose
        public String payTypeCode;

        @Expose
        public String payTypeName;

        @Expose
        public String qrcodeRegExr;

        @Expose
        public String dispQrcodeFlag;

        @Expose
        public String readQrcodeFlag;
    }
}
