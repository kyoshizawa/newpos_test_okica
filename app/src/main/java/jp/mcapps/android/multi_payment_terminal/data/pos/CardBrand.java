package jp.mcapps.android.multi_payment_terminal.data.pos;

import jp.mcapps.android.multi_payment_terminal.data.QRPayTypeCodes;

public class CardBrand {
    private String _brand = null;
    public void set(Credit brand) {
        _brand = brand.name();
    }
    public void set(EMoneyCommercial brand) {
        _brand = brand.name();
    }
    public void set(EMoneyTransportation brand) {
        _brand = brand.name();
    }
    public void set(QR brand) {
        _brand = brand.name();
    }
    public String get(){
        return _brand;
    }

    public static final String VISA = Credit.VISA.name();
    public static final String MASTER = Credit.MASTER.name();
    public static final String JCB = Credit.JCB.name();
    public static final String AMEX = Credit.AMEX.name();
    public static final String DINERS = Credit.DINERS.name();
    public static final String DISCOVER = Credit.DISCOVER.name();
    public static final String GINREN = Credit.GINREN.name();
    public static final String ID = EMoneyCommercial.ID.name();
    public static final String QUICKPAY = EMoneyCommercial.QUICKPAY.name();
    public static final String EDY = EMoneyCommercial.EDY.name();
    public static final String NANACO = EMoneyCommercial.NANACO.name();
    public static final String WAON = EMoneyCommercial.WAON.name();
    public static final String SUICA = EMoneyTransportation.SUICA.name();
    public static final String SUGOCA = EMoneyTransportation.SUGOCA.name();
    public static final String KITACA = EMoneyTransportation.KITACA.name();
    public static final String PASMO = EMoneyTransportation.PASMO.name();
    public static final String MANACA = EMoneyTransportation.MANACA.name();
    public static final String TOICA = EMoneyTransportation.TOICA.name();
    public static final String PITAPA = EMoneyTransportation.PITAPA.name();
    public static final String ICOCA = EMoneyTransportation.ICOCA.name();
    public static final String HAYAKAKEN = EMoneyTransportation.HAYAKAKEN.name();
    public static final String NIMOCA = EMoneyTransportation.NIMOCA.name();
    public static final String OKICA = EMoneyTransportation.OKICA.name();
    public static final String WECHAT = QR.WECHAT.name();
    public static final String ALIPAY = QR.ALIPAY.name();
    public static final String DOCOMO = QR.DOCOMO.name();
    public static final String AUPAY = QR.AUPAY.name();
    public static final String PAYPAY = QR.PAYPAY.name();
    public static final String LINEPAY = QR.LINEPAY.name();
    public static final String RAKUTENPAY = QR.RAKUTENPAY.name();
    public static final String GINKOPAY = QR.GINKOPAY.name();
    public static final String MERPAY = QR.MERPAY.name();
    public static final String QUOPAY = QR.QUOPAY.name();
    public static final String ALIPAYPLUS = QR.ALIPAYPLUS.name();
    public static final String AEONPAY = QR.AEONPAY.name();

    public static final String VISA_NAME = CardBrand.Credit.ConvertToName(CardBrand.VISA);
    public static final String MASTER_NAME = CardBrand.Credit.ConvertToName(CardBrand.MASTER);
    public static final String JCB_NAME = CardBrand.Credit.ConvertToName(CardBrand.JCB);
    public static final String AMEX_NAME = CardBrand.Credit.ConvertToName(CardBrand.AMEX);
    public static final String DINERS_NAME = CardBrand.Credit.ConvertToName(CardBrand.DINERS);
    public static final String DISCOVER_NAME = CardBrand.Credit.ConvertToName(CardBrand.DISCOVER);
    public static final String GINREN_NAME = CardBrand.Credit.ConvertToName(CardBrand.GINREN);
    public static final String ID_NAME = CardBrand.EMoneyCommercial.ConvertToName(CardBrand.ID);
    public static final String QUICKPAY_NAME = CardBrand.EMoneyCommercial.ConvertToName(CardBrand.QUICKPAY);
    public static final String EDY_NAME = CardBrand.EMoneyCommercial.ConvertToName(CardBrand.EDY);
    public static final String NANACO_NAME = CardBrand.EMoneyCommercial.ConvertToName(CardBrand.NANACO);
    public static final String WAON_NAME = CardBrand.EMoneyCommercial.ConvertToName(CardBrand.WAON);
    public static final String SUICA_NAME = CardBrand.EMoneyTransportation.ConvertToName(CardBrand.SUICA);
    public static final String SUGOCA_NAME = CardBrand.EMoneyTransportation.ConvertToName(CardBrand.SUGOCA);
    public static final String KITACA_NAME = CardBrand.EMoneyTransportation.ConvertToName(CardBrand.KITACA);
    public static final String PASMO_NAME = CardBrand.EMoneyTransportation.ConvertToName(CardBrand.PASMO);
    public static final String MANACA_NAME = CardBrand.EMoneyTransportation.ConvertToName(CardBrand.MANACA);
    public static final String TOICA_NAME = CardBrand.EMoneyTransportation.ConvertToName(CardBrand.TOICA);
    public static final String PITAPA_NAME = CardBrand.EMoneyTransportation.ConvertToName(CardBrand.PITAPA);
    public static final String ICOCA_NAME = CardBrand.EMoneyTransportation.ConvertToName(CardBrand.ICOCA);
    public static final String HAYAKAKEN_NAME = CardBrand.EMoneyTransportation.ConvertToName(CardBrand.HAYAKAKEN);
    public static final String NIMOCA_NAME = CardBrand.EMoneyTransportation.ConvertToName(CardBrand.NIMOCA);
    public static final String PAYPAY_NAME = CardBrand.QR.ConvertToName(CardBrand.PAYPAY);
    public static final String DOCOMO_NAME = CardBrand.QR.ConvertToName(CardBrand.DOCOMO);
    public static final String ALIPAY_NAME = CardBrand.QR.ConvertToName(CardBrand.ALIPAY);
    public static final String ALIPAYPLUS_NAME = CardBrand.QR.ConvertToName(CardBrand.ALIPAYPLUS);
    public static final String WECHATPAY_NAME = CardBrand.QR.ConvertToName(CardBrand.WECHAT);
    public static final String MERUPAY_NAME = CardBrand.QR.ConvertToName(CardBrand.MERPAY);
    public static final String RAKUTENPAY_NAME = CardBrand.QR.ConvertToName(CardBrand.RAKUTENPAY);
    public static final String AUPAY_NAME = CardBrand.QR.ConvertToName(CardBrand.AUPAY);
    public static final String GINKOUPAY_NAME = CardBrand.QR.ConvertToName(CardBrand.GINKOPAY);
    public static final String AEONPAY_NAME = CardBrand.QR.ConvertToName(CardBrand.AEONPAY);

    // クレジット
    public enum Credit {
        VISA,
        MASTER,
        JCB,
        AMEX,
        DINERS,
        DISCOVER,
        GINREN,
        UNKNOWN;

        public static Credit Convert(String brandSign) {

            if (brandSign.equals("35")) {
                return Credit.JCB;
            } else if (brandSign.equals("36")) {
                return Credit.DINERS;
            } else if (brandSign.equals("37")) {
                return Credit.AMEX;
            } else if (brandSign.equals("40")) {
                return Credit.VISA;
            } else if (brandSign.equals("50")) {
                return Credit.MASTER;
            } else if (brandSign.equals("65")) { // TODO Discoverのブランドサインを調査
                return Credit.DISCOVER;
            } else {
                return Credit.UNKNOWN;
            }
            // 銀聯はブランドサインが無い
        }

        public static String ConvertToName(String credit) {

            if (credit.equals(CardBrand.VISA)) {
                return "VISAカード";
            } else if (credit.equals(CardBrand.MASTER)) {
                return "マスターカード";
            } else if (credit.equals(CardBrand.JCB)) {
                return "JCBカード";
            } else if (credit.equals(CardBrand.AMEX)) {
                return "アメリカンエクスプレスカード";
            } else if (credit.equals(CardBrand.DINERS)) {
                return "ダイナースカード";
            } else if (credit.equals(CardBrand.DISCOVER)) {
                return "ディスカバーカード";
            } else if (credit.equals(CardBrand.GINREN)) {
                return "銀聯カード";
            } else {
                return "";
            }
        }
    }

    // 商業電マネ
    public enum EMoneyCommercial {
        ID,
        QUICKPAY,
        EDY,
        NANACO,
        WAON,
        UNKNOWN;

        public static String ConvertToName(String money) {

            if (money.equals(CardBrand.ID)) {
                return "iD";
            } else if (money.equals(CardBrand.QUICKPAY)) {
                return "QUICPay";
            } else if (money.equals(CardBrand.EDY)) {
                return "楽天Edy";
            } else if (money.equals(CardBrand.NANACO)) {
                return "nanaco";
            } else if (money.equals(CardBrand.WAON)) {
                return "WAON";
            } else {
                return "";
            }
        }
    }

    // 交通電マネ
    public enum EMoneyTransportation {
        SUICA,
        SUGOCA,
        KITACA,
        PASMO,
        MANACA,
        TOICA,
        PITAPA,
        ICOCA,
        HAYAKAKEN,
        NIMOCA,
        OKICA,
        UNKNOWN;

        public static EMoneyTransportation Convert(String cardPrefix) {
            if (cardPrefix.equals("JE")) {
                return EMoneyTransportation.SUICA;
            } else if (cardPrefix.equals("JK")) {
                return EMoneyTransportation.SUGOCA;
            } else if (cardPrefix.equals("NR")) {
                return EMoneyTransportation.NIMOCA;
            } else if (cardPrefix.equals("FC")) {
                return EMoneyTransportation.HAYAKAKEN;
            } else if (cardPrefix.equals("JC")) {
                return EMoneyTransportation.TOICA;
            } else if (cardPrefix.equals("JW")) {
                return EMoneyTransportation.ICOCA;
            } else if (cardPrefix.equals("JH")) {
                return EMoneyTransportation.KITACA;
            } else if (cardPrefix.equals("PB")) {
                return EMoneyTransportation.PASMO;
            } else if (cardPrefix.equals("TP")) {
                return EMoneyTransportation.MANACA;
            } else if (cardPrefix.equals("SU")) { // TODO PITAPAのカード番号を調査
                return EMoneyTransportation.PITAPA;
            } else {
                return EMoneyTransportation.UNKNOWN;
            }
        }

        public static String ConvertToName(String money) {

            if (money.equals(CardBrand.SUICA)) {
                return "Suica";
            } else if (money.equals(CardBrand.SUGOCA)) {
                return "SUGOCA";
            } else if (money.equals(CardBrand.KITACA)) {
                return "Kitaca";
            } else if (money.equals(CardBrand.PASMO)) {
                return "PASMO";
            } else if (money.equals(CardBrand.MANACA)) {
                return "manaca";
            } else if (money.equals(CardBrand.TOICA)) {
                return "TOICA";
            } else if (money.equals(CardBrand.PITAPA)) {
                return "PiTaPa";
            } else if (money.equals(CardBrand.ICOCA)) {
                return "ICOCA";
            } else if (money.equals(CardBrand.HAYAKAKEN)) {
                return "はやかけん";
            } else if (money.equals(CardBrand.NIMOCA)) {
                return "nimoca";
            } else {
                return "";
            }
        }
    }

    // QR
    public enum QR {
        WECHAT,
        ALIPAY,
        DOCOMO,
        AUPAY,
        PAYPAY,
        LINEPAY,
        RAKUTENPAY,
        GINKOPAY,
        MERPAY,
        QUOPAY,
        ALIPAYPLUS,
        AEONPAY,
        UNKNOWN;

        public static QR Convert(String brand) {
            brand = brand == null ? "" : brand; // 未了時に種別が未確定パターンのエラー回避 -> この時はUNKNOWN
            if (brand.equals(QRPayTypeCodes.Wechat)) {
                return QR.WECHAT;
            } else if (brand.equals(QRPayTypeCodes.Alipay)) {
                return QR.ALIPAY;
            } else if (brand.equals(QRPayTypeCodes.Docomo)) {
                return QR.DOCOMO;
            } else if (brand.equals(QRPayTypeCodes.auPAY)) {
                return QR.AUPAY;
            } else if (brand.equals(QRPayTypeCodes.PayPay)) {
                return QR.PAYPAY;
            } else if (brand.equals(QRPayTypeCodes.LINEPay)) {
                return QR.LINEPAY;
            } else if (brand.equals(QRPayTypeCodes.RakutenPay)) {
                return QR.RAKUTENPAY;
            } else if (brand.equals(QRPayTypeCodes.GinkoPay)) {
                return QR.GINKOPAY;
            } else if (brand.equals(QRPayTypeCodes.merpay)) {
                return QR.MERPAY;
            } else if (brand.equals(QRPayTypeCodes.QUOPay)) {
                return QR.QUOPAY;
            } else if (brand.equals(QRPayTypeCodes.AlipayPlus)) {
                return QR.ALIPAYPLUS;
            } else if (brand.equals(QRPayTypeCodes.AEONPay)) {
                return QR.AEONPAY;
            } else {
                return QR.UNKNOWN;
            }
        }

        public static String ConvertToName(String money) {

            if (money.equals(CardBrand.PAYPAY)) {
                return "PayPay";
            } else if (money.equals(CardBrand.DOCOMO)) {
                return "d払い";
            } else if (money.equals(CardBrand.ALIPAY)) {
                return "Alipay";
            } else if (money.equals(CardBrand.ALIPAYPLUS)) {
                return "Alipay +";
            } else if (money.equals(CardBrand.WECHAT)) {
                return "WeChatPay";
            } else if (money.equals(CardBrand.MERPAY)) {
                return "メルペイ";
            } else if (money.equals(CardBrand.RAKUTENPAY)) {
                return "楽天ペイ";
            } else if (money.equals(CardBrand.AUPAY)) {
                return "auPay";
            } else if (money.equals(CardBrand.GINKOPAY)) {
                return "銀行Pay";
            } else if (money.equals(CardBrand.AEONPAY)) {
                return "AEON Pay";
            //ADD-S BMT S.Oyama 2025/03/17 フタバ双方向向け改修
            } else if (money.equals(CardBrand.LINEPAY)) {
                return "LINE Pay";
            //ADD-E BMT S.Oyama 2025/03/17 フタバ双方向向け改修
            } else {
                return "不明";
            }
        }
    }
}
