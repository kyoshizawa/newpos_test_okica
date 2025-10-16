package jp.mcapps.android.multi_payment_terminal.data;

public class QRPayTypeCodes {
    public static final String Wechat     = "Wechat";
    public static final String Alipay     = "Alipay";
    public static final String Docomo     = "Docomo";
    public static final String auPAY      = "auPAY";
    public static final String PayPay     = "PayPay";
    public static final String LINEPay    = "LINEPay";
    public static final String RakutenPay = "RakutenPay";
    public static final String GinkoPay   = "GinkoPay";
    public static final String merpay     = "merpay";
    public static final String QUOPay     = "QUOPay";
    public static final String AlipayPlus = "Alipay+";
    public static final String JCoinPay   = "JCoinPay";
    public static final String AEONPay    = "AEONPay";

    public static class Flags {
        public static final int WECHAT     = 1 << 0;
        public static final int ALIPAY     = 1 << 1;
        public static final int DOCOMO     = 1 << 2;
        public static final int AUPAY      = 1 << 3;
        public static final int PAYPAY     = 1 << 4;
        public static final int LINEPAY    = 1 << 5;
        public static final int RAKUTENPAY = 1 << 6;
        public static final int GINKOPAY   = 1 << 7;
        public static final int MERPAY     = 1 << 8;
        public static final int QUOPAY     = 1 << 9;
        public static final int ALIPAYPLUS = 1 << 11;
        public static final int JCOINPAY   = 1 << 12; // 2025/03/08 ADD
        public static final int AEONPAY    = 1 << 13; // 2025/03/08 ADD
    }
}
