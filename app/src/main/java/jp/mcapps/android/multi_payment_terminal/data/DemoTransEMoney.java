package jp.mcapps.android.multi_payment_terminal.data;

import jp.mcapps.android.multi_payment_terminal.data.trans_param.SurveyParam;
import jp.mcapps.android.multi_payment_terminal.database.history.slip.SlipData;
import jp.mcapps.android.multi_payment_terminal.database.history.uri.UriData;

public class DemoTransEMoney {
    private final SlipData _slipData;
    private final UriData _uriData;

    public DemoTransEMoney() {
        _slipData = new SlipData();
        _uriData = new UriData();

        _slipData.transBrand = "交通系電子マネー";
        _slipData.transType = TransMap.TYPE_SALES;
        _slipData.transResult = TransMap.RESULT_SUCCESS;
        _slipData.encryptType = 1;
        _slipData.carId = 123;
        _slipData.driverId = 999;
        _slipData.termId = "3080600099999";
        _slipData.transAmount = 600;
        _slipData.transAdjAmount = 0;
        _slipData.transCashTogetherAmount = 500;
        _slipData.transAfterBalance = 0L;
        _slipData.printCnt = 1;
        _slipData.oldAggregateOrder = 0;
        _slipData.merchantName = "モバイルクリエイト株式会社";
        _slipData.merchantOffice = "大分本社";
        _slipData.merchantTelnumber = "097-576-8181";
        _slipData.cardIdMerchant = "JE***********2345";
        _slipData.cardIdCustomer = "JE***********2345";
        _slipData.cardTransNumber = 9999;

        _uriData.transBrand = "交通系電子マネー";
        _uriData.transType = TransMap.TYPE_SALES;
        _uriData.transResult = TransMap.RESULT_SUCCESS;
        _uriData.encryptType = 1;
        _uriData.carId = 123;
        _uriData.driverId = 999;
        _uriData.termId = "3080600099999";
        _uriData.transAmount = 600;
        _uriData.transAdjAmount = 0;
        _uriData.transCashTogetherAmount = 500;
        _uriData.transAfterBalance = 0L;
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
        _uriData.cardId = "JE***********2345";  //カード番号
        _uriData.icSprwid = "1234567890123";
    }

    public SlipData getSlipData() {
        return _slipData;
    }

    public UriData getUriData() {
        return _uriData;
    }
}
