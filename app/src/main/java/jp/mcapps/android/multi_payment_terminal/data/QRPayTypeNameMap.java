package jp.mcapps.android.multi_payment_terminal.data;

import java.util.HashMap;

public class QRPayTypeNameMap {
    private static HashMap<String, String> _map = new HashMap<>();

    public static String get(String payType) {
        String value = _map.get(payType);
        return value;
    }

    public static void set(String payType, String payTypeName) {
        String value = _map.get(payType);
        if (value != null) {
            _map.put(payType, payTypeName);
        }
    }

    static {
        _map.put(QRPayTypeCodes.Wechat, "Wechat");
        _map.put(QRPayTypeCodes.Alipay, "Alipay");
        _map.put(QRPayTypeCodes.Docomo, "d 払い");
        _map.put(QRPayTypeCodes.auPAY, "au PAY");
        _map.put(QRPayTypeCodes.PayPay, "PayPay");
        _map.put(QRPayTypeCodes.LINEPay, "LINE Pay");
        _map.put(QRPayTypeCodes.RakutenPay, "楽天ペイ");
        _map.put(QRPayTypeCodes.GinkoPay, "銀行 Pay");
        _map.put(QRPayTypeCodes.merpay, "メルペイ");
        _map.put(QRPayTypeCodes.QUOPay, "QUO カード Pay");
        _map.put(QRPayTypeCodes.AlipayPlus, "Alipay＋");
        _map.put(QRPayTypeCodes.AEONPay, "AEONPay");
        _map.put(QRPayTypeCodes.JCoinPay, "JCoinPay");
    }
}
