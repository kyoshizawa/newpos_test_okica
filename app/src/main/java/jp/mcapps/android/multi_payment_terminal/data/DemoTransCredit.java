package jp.mcapps.android.multi_payment_terminal.data;

import jp.mcapps.android.multi_payment_terminal.data.trans_param.SurveyParam;
import jp.mcapps.android.multi_payment_terminal.database.history.slip.SlipData;
import jp.mcapps.android.multi_payment_terminal.database.history.uri.UriData;

public class DemoTransCredit {
    private final SlipData _slipData;
    private final UriData _uriData;

    public DemoTransCredit(){
        _slipData = new SlipData();
        _uriData = new UriData();

        _slipData.transBrand = "クレジット";
        _slipData.transType = TransMap.TYPE_SALES;
        _slipData.transResult = TransMap.RESULT_SUCCESS;
        _slipData.encryptType = 1;
        _slipData.carId = 123;
        _slipData.driverId = 999;
        _slipData.termId = "3080600099999";
        _slipData.transAmount = 1000;
        _slipData.transAdjAmount = 1000;
        _slipData.printCnt = 1;
        _slipData.oldAggregateOrder = 0;
        _slipData.merchantName = "モバイルクリエイト株式会社";
        _slipData.merchantOffice = "大分本社";
        _slipData.merchantTelnumber = "097-576-8181";
        _slipData.cardCompany = "AMEX CARD";
        _slipData.cardIdMerchant = "123456******3456";
        _slipData.cardIdCustomer = "123456******3456";
        _slipData.cardExpDate = "XX/XX";
        _slipData.commodityCode = "240";
        _slipData.installment = "10";
        _slipData.creditType = "IC";
        _slipData.creditArc = "00";
        _slipData.creditAid = "A1234567890123456";
        _slipData.creditApl = "AMERICAN EXPRESS";
        _slipData.creditSignatureFlg = 1;

        _uriData.posSend = 1;
        _uriData.transResultDetail = TransMap.DETAIL_NORMAL;
        _uriData.installment = "10";
        _uriData.transTime = 0;  //取引処理時間
        _uriData.transInputPinTime = 0;  //暗証番号入力時間
        SurveyParam surveyParam = new SurveyParam();
        surveyParam.setAntennaLevel();
        surveyParam.setLocation();
        _uriData.termLatitude = surveyParam.termLatitude;   //緯度
        _uriData.termLongitude = surveyParam.termLongitude;  //経度
        _uriData.termRadioLevel = surveyParam.termRadioLevel;   //電波レベル
        _uriData.termNetworkType = surveyParam.termNetworkType;  //ネットワーク種別
        _uriData.transBrand = "クレジット";
        _uriData.transType = TransMap.TYPE_SALES;
        _uriData.transResult = TransMap.RESULT_SUCCESS;
        _uriData.encryptType = 1;
        _uriData.carId = 123;
        _uriData.driverId = 999;
        _uriData.termId = "3080600099999";
        _uriData.transAmount = 1000;
        _uriData.transAdjAmount = 1000;
        _uriData.creditAcqId = 1;
        _uriData.creditMsIc = 1;
        _uriData.creditOnOff = 0;
        _uriData.creditChipCc = "00";
        _uriData.creditForcedOnline = 0;
        _uriData.creditCommodityCode = "240";
        _uriData.creditAid = "A1234567890123456";
        _uriData.creditEntryMode = "0501";
        _uriData.creditPanSequenceNumber = 1;
        _uriData.creditIctermFlag = 0;
        _uriData.creditBrandId = "35";
        _uriData.creditKeyType = 0;
        _uriData.creditKeyVer = 1;
        _uriData.creditEncryptionData = "XXX・・・";
    }

    public SlipData getSlipData() {
        return _slipData;
    }

    public UriData getUriData() {
        return _uriData;
    }
}
