package jp.mcapps.android.multi_payment_terminal.database.history.uri;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.data.EmoneyOpeningInfo;
import jp.mcapps.android.multi_payment_terminal.data.trans_param.AmountParam;
import jp.mcapps.android.multi_payment_terminal.data.trans_param.RefundParam;
import jp.mcapps.android.multi_payment_terminal.data.trans_param.ResultParam;
import jp.mcapps.android.multi_payment_terminal.data.trans_param.SurveyParam;
import jp.mcapps.android.multi_payment_terminal.database.Converters;
import jp.mcapps.android.multi_payment_terminal.database.DeviceClient;
import jp.mcapps.android.multi_payment_terminal.model.QRSettlement;
// import jp.mcapps.android.multi_payment_terminal.thread.credit.data.CreditResult;

@Entity(tableName = "history_uri")
public class UriData {
    @PrimaryKey(autoGenerate = true)
    public int id;

    //共通項目

    @ColumnInfo(name = "pos_send")
    public Integer posSend;

    @ColumnInfo(name = "car_id")
    public Integer carId;

    @ColumnInfo(name = "driver_id")
    public Integer driverId;

    @ColumnInfo(name = "term_id")
    public String termId;

    @ColumnInfo(name = "common_name")
    public String commonName;

    @ColumnInfo(name = "trans_brand")
    public String transBrand;

    @ColumnInfo(name = "trans_date", index = true)
    public String transDate;

    @ColumnInfo(name = "old_trans_date")
    public String oldTransDate;

    @ColumnInfo(name = "trans_type")
    public Integer transType;

    @ColumnInfo(name = "trans_result")
    public Integer transResult;

    @ColumnInfo(name = "trans_result_detail")
    public Integer transResultDetail;

    @ColumnInfo(name = "term_sequence")
    public Integer termSequence;

    @ColumnInfo(name = "old_term_sequence")
    public Integer oldTermSequence;

    @ColumnInfo(name = "trans_id")
    public String transId;

    @ColumnInfo(name = "old_trans_id")
    public String oldTransId;

    @ColumnInfo(name = "card_id")
    public String cardId;

    @ColumnInfo
    public String installment;

    @ColumnInfo(name = "trans_amount")
    public Integer transAmount;

    @ColumnInfo(name = "trans_specified_amount")
    public Integer transSpecifiedAmount;

    @ColumnInfo(name = "trans_meter_amount")
    public Integer transMeterAmount;

    @ColumnInfo(name = "trans_adj_amount")
    public Integer transAdjAmount;

    @ColumnInfo(name = "trans_cash_together_amount")
    public Integer transCashTogetherAmount;

    @ColumnInfo(name = "trans_other_amount_one_type")
    public Integer transOtherAmountOneType;

    @ColumnInfo(name = "trans_other_amount_one")
    public Integer transOtherAmountOne;

    @ColumnInfo(name = "trans_other_amount_two_type")
    public Integer transOtherAmountTwoType;

    @ColumnInfo(name = "trans_other_amount_two")
    public Integer transOtherAmountTwo;

    @ColumnInfo(name = "trans_before_balance")
    public Long transBeforeBalance;

    @ColumnInfo(name = "trans_after_balance")
    public Long transAfterBalance;

    @ColumnInfo(name = "trans_time")
    public Integer transTime;

    @ColumnInfo(name = "trans_input_pin_time")
    public Integer transInputPinTime;

    @ColumnInfo(name = "term_latitude")
    public String termLatitude;

    @ColumnInfo(name = "term_longitude")
    public String termLongitude;

    @ColumnInfo(name = "term_network_type")
    public String termNetworkType;

    @ColumnInfo(name = "term_radio_level")
    public Integer termRadioLevel;

    @ColumnInfo(name = "encrypt_type")
    public Integer encryptType;

    //銀聯

    @ColumnInfo(name = "unionpay_send_date")
    public String unionpaySendDate;

    @ColumnInfo(name = "unionpay_proc_number")
    public Integer unionpayProcNumber;

    @ColumnInfo(name = "old_unionpay_send_date")
    public String oldUnionpaySendDate;

    @ColumnInfo(name = "old_unionpay_proc_number")
    public Integer oldUnionpayProcNumber;

    //クレジット

    @ColumnInfo(name = "credit_acq_id")
    public Integer creditAcqId;

    @ColumnInfo(name = "credit_ms_ic")
    public Integer creditMsIc;

    @ColumnInfo(name = "credit_on_off")
    public Integer creditOnOff;

    @ColumnInfo(name = "credit_chip_cc")
    public String creditChipCc;

    @ColumnInfo(name = "credit_forced_online")
    public Integer creditForcedOnline;

    @ColumnInfo(name = "credit_forced_approval")
    public Integer creditForcedApproval;

    @ColumnInfo(name = "credit_commodity_code")
    public String creditCommodityCode;

    @ColumnInfo(name = "credit_aid")
    public String creditAid;

    @ColumnInfo(name = "credit_entry_mode")
    public String creditEntryMode;

    @ColumnInfo(name = "credit_pan_sequence_number")
    public Integer creditPanSequenceNumber;

    @ColumnInfo(name = "credit_icterm_flag")
    public Integer creditIctermFlag;

    @ColumnInfo(name = "credit_brand_id")
    public String creditBrandId;

    @ColumnInfo(name = "credit_key_type")
    public Integer creditKeyType;

    @ColumnInfo(name = "credit_key_ver")
    public Integer creditKeyVer;

    @ColumnInfo(name = "credit_encryption_data")
    public String creditEncryptionData;

    //交通系

    @ColumnInfo(name = "ic_err_code")
    public Integer icErrCode;

    @ColumnInfo(name = "ic_idm")
    public String icIdm;

    @ColumnInfo(name = "ic_sprwid")
    public String icSprwid;

    @ColumnInfo(name = "ic_statement_id")
    public Integer icStatementId;

    @ColumnInfo(name = "ic_sequence")
    public Integer icSequence;

    @ColumnInfo(name = "ic_sflog_id")
    public Integer icSflogId;

    @ColumnInfo(name = "ic_old_statement_id")
    public Integer icOldStatementId;

    @ColumnInfo(name = "ic_old_sflog_id")
    public Integer icOldSflogId;

    //iD

    @ColumnInfo(name = "id_slip_number")
    public Integer idSlipNumber;

    @ColumnInfo(name = "id_old_slip_number")
    public Integer idOldSlipNumber;

    @ColumnInfo(name = "id_error_code")
    public Integer idErrorCode;

    @ColumnInfo(name = "id_term_ident_id")
    public String idTermIdentId;

    @ColumnInfo(name = "id_sequence_number")
    public Integer idSequenceNumber;

    @ColumnInfo(name = "id_recognition_number")
    public String idRecognitionNumber;

    //WAON

    @ColumnInfo(name = "waon_slip_number")
    public Integer waonSlipNumber;

    @ColumnInfo(name = "waon_old_slip_number")
    public Integer waonOldSlipNumber;

    @ColumnInfo(name = "waon_err_code")
    public Integer waonErrCode;

    @ColumnInfo(name = "waon_idm")
    public String waonIdm;

    @ColumnInfo(name = "waon_term_ident_id")
    public String waonTermIdentId;

    @ColumnInfo(name = "waon_card_through_num")
    public Integer waonCardThroughNum;

    @ColumnInfo(name = "waon_point_trade_value")
    public Long waonPointTradeValue;

    @ColumnInfo(name = "waon_point_grant_type")
    public Integer waonPointGrantType;

    @ColumnInfo(name = "waon_add_point_total")
    public Long waonAddPointTotal;

    @ColumnInfo(name = "waon_total_point")
    public Long waonTotalPoint;

    //nanaco

    @ColumnInfo(name = "nanaco_slip_number")
    public Long nanacoSlipNumber;

    @ColumnInfo(name = "nanaco_err_code")
    public Integer nanacoErrCode;

    @ColumnInfo(name = "nanaco_card_trans_number")
    public Integer nanacoCardTransNumber;

    @ColumnInfo(name = "nanaco_term_ident_id")
    public String nanacoTermIdentId;

    //Edy

    @ColumnInfo(name = "rakuten_edy_err_code")
    public Integer rakutenEdyErrCode;

    @ColumnInfo(name = "rakuten_edy_trans_number")
    public Long rakutenEdyTransNumber;

    @ColumnInfo(name = "rakuten_edy_card_trans_number")
    public Integer rakutenEdyCardTransNumber;

    @ColumnInfo(name = "rakuten_edy_term_ident_id")
    public String rakutenEdyTermIdentId;

    //QUICPay

    @ColumnInfo(name = "quicpay_slip_number")
    public Integer quicpaySlipNumber;

    @ColumnInfo(name = "quicpay_old_slip_number")
    public Integer quicpayOldSlipNumber;

    @ColumnInfo(name = "quicpay_err_code")
    public Integer quicpayErrCode;

    @ColumnInfo(name = "quicpay_term_ident_id")
    public String quicpayTermIdentId;

    @ColumnInfo(name = "quicpay_dealings_through_number")
    public Integer quicpayDealingsThroughNumber;

    //QR

    @ColumnInfo(name= "codetrans_order_id")
    public String codetransOrderId;

    @ColumnInfo(name= "codetrans_old_order_id")
    public String codetransOldOrderId;

    @ColumnInfo(name= "codetrans_pay_type_code")
    public String codetransPayTypeCode;

    @ColumnInfo
    public String wallet;

    public UriData() {}

    /**
     * クレジット売上データ
     */
//    public UriData(CreditResult.Result result, int encryptType, int termSequence, ResultParam resultParam, RefundParam refundParam, AmountParam amountParam, SurveyParam surveyParam) {
//        //共通項目
//        posSend = 0;    //POS送信状態
//        carId = AppPreference.getMcCarId(); //号機番号（車番）
//        driverId = AppPreference.getMcDriverId();    //乗務員コード
//        termId = AppPreference.getMcTermId();   //端末番号
//        transBrand = MainApplication.getInstance().getString(R.string.money_brand_credit);   //ブランド名
//        transDate = Converters.dateFormat(result.authDateTime);  //取引日時
//        oldTransDate = refundParam.oldTransDate;  //元取引日時
//        transType = resultParam.transType;    //取引種別
//        transResult = resultParam.transResult;    //取引結果
//        transResultDetail = resultParam.transResultDetail;    //取引結果詳細
//        this.termSequence = termSequence;   //端末通番
//        oldTermSequence = refundParam.oldTermSequence;    //元端末通番
//        cardId = result.maskedMemberNo;   //カード番号
//        installment = "10"; //分割回数
//        transAmount = amountParam.transAmount;  //取引金額
//        transSpecifiedAmount = amountParam.transSpecifiedAmount;    //定額
//        transMeterAmount = amountParam.transMeterAmount;    //メータ金額
//        transAdjAmount = amountParam.transAdjAmount;    //増減額
//        transCashTogetherAmount = amountParam.transCashTogetherAmount;   //現金併用金額 0固定
//        transOtherAmountOne = amountParam.transTicketAmount;    //チケット金額
//        if(transOtherAmountOne != null && 0 < transOtherAmountOne) {
//            transOtherAmountOneType = 1;   //その他金額1種別（チケット）
//        } else {
//            transOtherAmountOneType = 0;   //その他金額1種別（予備）
//        }
//        transOtherAmountTwoType = 0;   //その他金額2種別（予備）
//        transOtherAmountTwo = 0;   //その他金額2（予備）
//        transTime = surveyParam.transTime;  //取引処理時間 このタイミングではNULLの場合あり
//        transInputPinTime = surveyParam.transInputPinTime;  //暗証番号入力時間 このタイミングではNULLの場合あり
//        termLatitude = surveyParam.termLatitude; //位置情報（緯度）
//        termLongitude = surveyParam.termLongitude;    //位置情報（経度）
//        termNetworkType = surveyParam.termNetworkType;  //ネットワーク種別
//        termRadioLevel = surveyParam.termRadioLevel;    //電波状況（レベル）
//        this.encryptType = encryptType;   //暗号化パターン
//
//        //クレジット固有項目
//        creditAcqId = result.acquirerId;    //クレジットアクワイアラID
//        creditMsIc = result.msICKbn;    //磁気・IC区分
//        creditOnOff = result.onOffKbn;   //オンオフ区分
//        creditChipCc = result.chipCC;   //チップコンディションコード
//        creditForcedOnline = result.forcedOnline;    //強制オンライン
//        creditForcedApproval = result.forcedApproval;  //強制承認
//        creditCommodityCode = result.productCd; //商品コード
//        creditAid = result.aid; //AID
//        creditEntryMode = result.posEntryMode;  //POSエントリーモード
//        creditPanSequenceNumber = result.panSeqNo;    //PANシーケンスナンバー
//        creditIctermFlag = result.icTerminalFlg;    //IC対応端末フラグ
//        creditBrandId = result.brandSign;   //ブランド識別
//        creditKeyType = result.keyType; //鍵種別
//        creditKeyVer = result.keyVersion;   //鍵バージョン
//        creditEncryptionData = result.rsaData;  //暗号化データ
//    }

    /**
     * 交通系売上データ
     */
    public UriData(DeviceClient.Result result, int encryptType, int termSequence, ResultParam resultParam, RefundParam refundParam, AmountParam amountParam, SurveyParam surveyParam) {
        //共通項目
        posSend = 0;    //POS送信状態
        carId = AppPreference.getMcCarId(); //号機番号（車番）
        driverId = AppPreference.getMcDriverId();    //乗務員コード
        termId = AppPreference.getMcTermId();   //端末番号
        commonName = AppPreference.getJremUniqueId();   //ユニークID
        transBrand = MainApplication.getInstance().getString(R.string.money_brand_suica);   //ブランド名
        transDate = Converters.dateFormat(result.time);  //取引日時
        oldTransDate = refundParam.oldTransDate;  //元取引日時
        transType = resultParam.transType;    //取引種別
        transResult = resultParam.transResult;    //取引結果
        transResultDetail = resultParam.transResultDetail;    //取引結果詳細
        this.termSequence = termSequence;   //端末通番
        oldTermSequence = refundParam.oldTermSequence;    //元端末通番
        transId = result.sid;//決済ID
        oldTransId = refundParam.oldTransId;  //元決済ID
        cardId = Converters.tenantMaskSuica(result.IDi);//カード番号
        installment = "10"; //分割回数
        transAmount = amountParam.transAmount;  //取引金額
        transSpecifiedAmount = amountParam.transSpecifiedAmount;    //定額
        transMeterAmount = amountParam.transMeterAmount;    //メータ金額
        transAdjAmount = amountParam.transAdjAmount;    //増減額
        transCashTogetherAmount = amountParam.transCashTogetherAmount;   //現金併用金額
        transOtherAmountOne = amountParam.transTicketAmount;    //チケット金額
        if(transOtherAmountOne != null && 0 < transOtherAmountOne) {
            transOtherAmountOneType = 1;   //その他金額1種別（チケット）
        } else {
            transOtherAmountOneType = 0;   //その他金額1種別（予備）
        }
        transOtherAmountTwoType = 0;   //その他金額2種別（予備）
        transOtherAmountTwo = 0;   //その他金額2（予備）
        transBeforeBalance = Converters.stringToLong(result.bRem); //取引前残高
        transAfterBalance = Converters.stringToLong(result.rem);   //取引後残高
        transTime = surveyParam.transTime;  //取引処理時間 このタイミングではNULLの場合あり
        transInputPinTime = surveyParam.transInputPinTime;  //暗証番号入力時間 このタイミングではNULLの場合あり
        termLatitude = surveyParam.termLatitude; //位置情報（緯度）
        termLongitude = surveyParam.termLongitude;    //位置情報（経度）
        termNetworkType = surveyParam.termNetworkType;  //ネットワーク種別
        termRadioLevel = surveyParam.termRadioLevel;    //電波状況（レベル）
        this.encryptType = encryptType;   //暗号化パターン

        //交通系固有項目
        icErrCode = Converters.stringToInteger(result.code);   //エラーコード
        icIdm = result.IDm;  //IDm
        icSprwid = result.sprwid != null
                 ? result.sprwid
                 : EmoneyOpeningInfo.getSuica().sprwid;  //SPRWID
        icStatementId = Converters.stringToInteger(result.statementID);   //一件明細ID
        icSequence = Converters.stringToInteger(result.ICsequence);   //IC取扱通番
        icSflogId = Converters.stringToInteger(result.SFLogID);   //SFログID
        icOldStatementId = Converters.stringToInteger(result.oldstatementID); //旧一件明細ID
        icOldSflogId = Converters.stringToInteger(result.oldSFLogID); //旧SFログID
    }

    /**
     * Edy売上データ
     */
    public UriData(DeviceClient.ResultEdy result, int encryptType, int termSequence, ResultParam resultParam, AmountParam amountParam, SurveyParam surveyParam) {
        //共通項目
        posSend = 0;    //POS送信状態
        carId = AppPreference.getMcCarId(); //号機番号（車番）
        driverId = AppPreference.getMcDriverId();    //乗務員コード
        termId = AppPreference.getMcTermId();   //端末番号
        commonName = AppPreference.getJremUniqueId();   //ユニークID
        transBrand = MainApplication.getInstance().getString(R.string.money_brand_edy);   //ブランド名
        transDate = Converters.dateFormat(result.time);  //取引日時
        transType = resultParam.transType;    //取引種別
        transResult = resultParam.transResult;    //取引結果
        transResultDetail = resultParam.transResultDetail;    //取引結果詳細
        this.termSequence = termSequence;   //端末通番
        transId = result.sid;//決済ID
        //カード番号 saleHistoriesを確認
        installment = "10"; //分割回数
        transAmount = amountParam.transAmount;  //取引金額
        transSpecifiedAmount = amountParam.transSpecifiedAmount;    //定額
        transMeterAmount = amountParam.transMeterAmount;    //メータ金額
        transAdjAmount = amountParam.transAdjAmount;    //増減額
        transCashTogetherAmount = amountParam.transCashTogetherAmount;   //現金併用金額
        transOtherAmountOne = amountParam.transTicketAmount;    //チケット金額
        if(transOtherAmountOne != null && 0 < transOtherAmountOne) {
            transOtherAmountOneType = 1;   //その他金額1種別（チケット）
        } else {
            transOtherAmountOneType = 0;   //その他金額1種別（予備）
        }
        transOtherAmountTwoType = 0;   //その他金額2種別（予備）
        transOtherAmountTwo = 0;   //その他金額2（予備）
        //取引前残高 saleHistoriesを確認
        //取引後残高 saleHistoriesを確認
        transTime = surveyParam.transTime;  //取引処理時間 このタイミングではNULLの場合あり
        transInputPinTime = surveyParam.transInputPinTime;  //暗証番号入力時間 このタイミングではNULLの場合あり
        termLatitude = surveyParam.termLatitude; //位置情報（緯度）
        termLongitude = surveyParam.termLongitude;    //位置情報（経度）
        termNetworkType = surveyParam.termNetworkType;  //ネットワーク種別
        termRadioLevel = surveyParam.termRadioLevel;    //電波状況（レベル）
        this.encryptType = encryptType;   //暗号化パターン

        //Edy固有項目
        rakutenEdyErrCode = Converters.stringToInteger(result.code);    //エラーコード
        //Edy取引通番 saleHistoriesを確認
        //カード取引通番 saleHistoriesを確認
        rakutenEdyTermIdentId = result.termIdentId != null
                ? result.termIdentId
                : EmoneyOpeningInfo.getEdy().termIdentId;  // 上位端末ID

        if(result.saleHistories != null) {
            cardId = result.saleHistories[0].userMaskMembershipNum;  //カード番号（お客様控え用のマスクカード番号をセット）
            transBeforeBalance = Converters.stringToLong(result.saleHistories[0].beforeBalance); //取引前残高
            transAfterBalance = Converters.stringToLong(result.saleHistories[0].afterBalance);    //取引後残高
            rakutenEdyTransNumber = Converters.stringToLong(result.saleHistories[0].edyTransactionNo);  // Edy取引通番
            rakutenEdyCardTransNumber = Converters.stringToInteger(result.saleHistories[0].cardTransactionNo);  // カード取引通番
        }
    }

    /**
     * iD売上データ
     */
    public UriData(DeviceClient.ResultID result, int encryptType, int termSequence, ResultParam resultParam, RefundParam refundParam, AmountParam amountParam, SurveyParam surveyParam) {
        //共通項目
        posSend = 0;    //POS送信状態
        carId = AppPreference.getMcCarId(); //号機番号（車番）
        driverId = AppPreference.getMcDriverId();    //乗務員コード
        termId = AppPreference.getMcTermId();   //端末番号
        commonName = AppPreference.getJremUniqueId();   //ユニークID
        transBrand = MainApplication.getInstance().getString(R.string.money_brand_id);   //ブランド名
        transDate = Converters.dateFormat(result.time);  //取引日時
        oldTransDate = refundParam.oldTransDate;  //元取引日時
        transType = resultParam.transType;    //取引種別
        transResult = resultParam.transResult;    //取引結果
        transResultDetail = resultParam.transResultDetail;    //取引結果詳細
        this.termSequence = termSequence;   //端末通番
        oldTermSequence = refundParam.oldTermSequence;    //元端末通番
        transId = result.sid;//決済ID
        oldTransId = refundParam.oldTransId;  //元決済ID
        cardId = result.tenantMaskMembershipNum;//カード番号
        installment = "10"; //分割回数
        transAmount = amountParam.transAmount;  //取引金額
        transSpecifiedAmount = amountParam.transSpecifiedAmount;    //定額
        transMeterAmount = amountParam.transMeterAmount;    //メータ金額
        transAdjAmount = amountParam.transAdjAmount;    //増減額
        transCashTogetherAmount = amountParam.transCashTogetherAmount;   //現金併用金額
        transOtherAmountOne = amountParam.transTicketAmount;    //チケット金額
        if(transOtherAmountOne != null && 0 < transOtherAmountOne) {
            transOtherAmountOneType = 1;   //その他金額1種別（チケット）
        } else {
            transOtherAmountOneType = 0;   //その他金額1種別（予備）
        }
        transOtherAmountTwoType = 0;   //その他金額2種別（予備）
        transOtherAmountTwo = 0;   //その他金額2（予備）
//        transBeforeBalance = Converters.stringToInteger(result.bRem); //取引前残高
//        transAfterBalance = Converters.stringToInteger(result.rem);   //取引後残高
        transTime = surveyParam.transTime;  //取引処理時間 このタイミングではNULLの場合あり
        transInputPinTime = surveyParam.transInputPinTime;  //暗証番号入力時間 このタイミングではNULLの場合あり
        termLatitude = surveyParam.termLatitude; //位置情報（緯度）
        termLongitude = surveyParam.termLongitude;    //位置情報（経度）
        termNetworkType = surveyParam.termNetworkType;  //ネットワーク種別
        termRadioLevel = surveyParam.termRadioLevel;    //電波状況（レベル）
        this.encryptType = encryptType;   //暗号化パターン

        //iD固有項目
        idSlipNumber = Converters.stringToInteger(result.slipNo);  //伝票番号
        idOldSlipNumber = result.oldSlipNo != null
                ? Converters.stringToInteger(result.oldSlipNo)
                : refundParam.oldSlipNumber;  //元伝票番号 RASの応答に設定されていない場合は印刷履歴から取得
        idErrorCode = Converters.stringToInteger(result.code);  //エラーコード
        idTermIdentId = result.termIdentId != null
                ? result.termIdentId
                : EmoneyOpeningInfo.getId().termIdentId;    //上位端末ID
        idSequenceNumber = Converters.stringToInteger(result.sequenceNo);
        idRecognitionNumber = result.recognitionNum;
    }

    /**
     * waon売上データ
     */
    public UriData(DeviceClient.ResultWAON result, int encryptType, int termSequence, ResultParam resultParam, RefundParam refundParam, AmountParam amountParam, SurveyParam surveyParam) {
        //共通項目
        posSend = 0;    //POS送信状態
        carId = AppPreference.getMcCarId(); //号機番号（車番）
        driverId = AppPreference.getMcDriverId();    //乗務員コード
        termId = AppPreference.getMcTermId();   //端末番号
        commonName = AppPreference.getJremUniqueId();   //ユニークID
        transBrand = MainApplication.getInstance().getString(R.string.money_brand_waon);   //ブランド名
        transDate = Converters.dateFormat(result.time);  //取引日時
        oldTransDate = refundParam.oldTransDate;  //元取引日時
        transType = resultParam.transType;    //取引種別
        transResult = resultParam.transResult;    //取引結果
        transResultDetail = resultParam.transResultDetail;    //取引結果詳細
        this.termSequence = termSequence;   //端末通番
        oldTermSequence = refundParam.oldTermSequence;    //元端末通番
        transId = result.sid;//決済ID
        oldTransId = refundParam.oldTransId;  //元決済ID
        cardId = result.tenantMaskWaonNum();    //カード番号
        installment = "10"; //分割回数
        transAmount = amountParam.transAmount;  //取引金額
        transSpecifiedAmount = amountParam.transSpecifiedAmount;    //定額
        transMeterAmount = amountParam.transMeterAmount;    //メータ金額
        transAdjAmount = amountParam.transAdjAmount;    //増減額
        transCashTogetherAmount = amountParam.transCashTogetherAmount;   //現金併用金額
        transOtherAmountOne = amountParam.transTicketAmount;    //チケット金額
        if(transOtherAmountOne != null && 0 < transOtherAmountOne) {
            transOtherAmountOneType = 1;   //その他金額1種別（チケット）
        } else {
            transOtherAmountOneType = 0;   //その他金額1種別（予備）
        }
        transOtherAmountTwoType = 0;   //その他金額2種別（予備）
        transOtherAmountTwo = 0;   //その他金額2（予備）
        transBeforeBalance = Converters.stringToLong(result.beforeBalance); //取引前残高
        transAfterBalance = Converters.stringToLong(result.balance);   //取引後残高
        transTime = surveyParam.transTime;  //取引処理時間 このタイミングではNULLの場合あり
        transInputPinTime = surveyParam.transInputPinTime;  //暗証番号入力時間 このタイミングではNULLの場合あり
        termLatitude = surveyParam.termLatitude; //位置情報（緯度）
        termLongitude = surveyParam.termLongitude;    //位置情報（経度）
        termNetworkType = surveyParam.termNetworkType;  //ネットワーク種別
        termRadioLevel = surveyParam.termRadioLevel;    //電波状況（レベル）
        this.encryptType = encryptType;   //暗号化パターン

        //waon固有項目
        waonSlipNumber = Converters.stringToInteger(result.slipNo);  //伝票番号
        waonOldSlipNumber = result.oldSlipNo != null
                ? Converters.stringToInteger(result.oldSlipNo)
                : refundParam.oldSlipNumber;  //元伝票番号 RASの応答に設定されていない場合は印刷履歴から取得
        waonErrCode = Converters.stringToInteger(result.code);  //エラーコード
        waonIdm = result.idm;  //製造番号
        waonTermIdentId = result.termIdentId != null
                ? result.termIdentId
                : EmoneyOpeningInfo.getWaon().termIdentId;  // 物販端末R/Wコード
        waonCardThroughNum = Converters.stringToInteger(result.cardThroughNum);  //カード通番
        waonPointTradeValue = Converters.stringToLong(result.pointTradeValue);  //ポイント対象金額
        waonPointGrantType = Converters.stringToInteger(result.pointGrantType);  //ポイント付与区分
        waonAddPointTotal = Converters.stringToLong(result.addPointTotal);  //付与ポイント合計
        waonTotalPoint = Converters.stringToLong(result.totalPoint);  //累計ポイント(現在～2年前)
    }

    /**
     * QUICPay売上データ
     */
    public UriData(DeviceClient.ResultQUICPay result, int encryptType, int termSequence, ResultParam resultParam, RefundParam refundParam, AmountParam amountParam, SurveyParam surveyParam) {
        //共通項目
        posSend = 0;    //POS送信状態
        carId = AppPreference.getMcCarId(); //号機番号（車番）
        driverId = AppPreference.getMcDriverId();    //乗務員コード
        termId = AppPreference.getMcTermId();   //端末番号
        commonName = AppPreference.getJremUniqueId();   //ユニークID
        transBrand = MainApplication.getInstance().getString(R.string.money_brand_qp);   //ブランド名
        transDate = Converters.dateFormat(result.time);  //取引日時
        oldTransDate = refundParam.oldTransDate;  //元取引日時
        transType = resultParam.transType;    //取引種別
        transResult = resultParam.transResult;    //取引結果
        transResultDetail = resultParam.transResultDetail;    //取引結果詳細
        this.termSequence = termSequence;   //端末通番
        oldTermSequence = refundParam.oldTermSequence;    //元端末通番
        transId = result.sid;//決済ID
        oldTransId = refundParam.oldTransId;  //元決済ID
        cardId = result.tenantMaskMembershipNum;//カード番号
        installment = "10"; //分割回数
        transAmount = amountParam.transAmount;  //取引金額
        transSpecifiedAmount = amountParam.transSpecifiedAmount;    //定額
        transMeterAmount = amountParam.transMeterAmount;    //メータ金額
        transAdjAmount = amountParam.transAdjAmount;    //増減額
        transCashTogetherAmount = amountParam.transCashTogetherAmount;   //現金併用金額
        transOtherAmountOne = amountParam.transTicketAmount;    //チケット金額
        if(transOtherAmountOne != null && 0 < transOtherAmountOne) {
            transOtherAmountOneType = 1;   //その他金額1種別（チケット）
        } else {
            transOtherAmountOneType = 0;   //その他金額1種別（予備）
        }
        transOtherAmountTwoType = 0;   //その他金額2種別（予備）
        transOtherAmountTwo = 0;   //その他金額2（予備）
        // transBeforeBalance = Converters.stringToInteger(result.beforeBalance); //取引前残高
        // transAfterBalance = Converters.stringToInteger(result.balance);   //取引後残高
        transTime = surveyParam.transTime;  //取引処理時間 このタイミングではNULLの場合あり
        transInputPinTime = surveyParam.transInputPinTime;  //暗証番号入力時間 このタイミングではNULLの場合あり
        termLatitude = surveyParam.termLatitude; //位置情報（緯度）
        termLongitude = surveyParam.termLongitude;    //位置情報（経度）
        termNetworkType = surveyParam.termNetworkType;  //ネットワーク種別
        termRadioLevel = surveyParam.termRadioLevel;    //電波状況（レベル）
        this.encryptType = encryptType;   //暗号化パターン

        //QUICPay固有項目
        quicpaySlipNumber = Converters.stringToInteger(result.slipNo);
        quicpayOldSlipNumber = result.oldSlipNo != null
                ? Converters.stringToInteger(result.oldSlipNo)
                : refundParam.oldSlipNumber;  //元伝票番号 RASの応答に設定されていない場合は印刷履歴から取得
        quicpayErrCode = Converters.stringToInteger(result.code);
        quicpayTermIdentId = result.termIdentId != null
                ? result.termIdentId
                : EmoneyOpeningInfo.getQuicpay().termIdentId;   // 物販端末R/Wコード
        quicpayDealingsThroughNumber = Converters.stringToInteger(result.dealingsThroughNum);
    }

    /**
     * nanaco売上データ
     */
    public UriData(DeviceClient.Resultnanaco result, int encryptType, int termSequence, ResultParam resultParam, RefundParam refundParam, AmountParam amountParam, SurveyParam surveyParam) {
        //共通項目
        posSend = 0;    //POS送信状態
        carId = AppPreference.getMcCarId(); //号機番号（車番）
        driverId = AppPreference.getMcDriverId();    //乗務員コード
        termId = AppPreference.getMcTermId();   //端末番号
        commonName = AppPreference.getJremUniqueId();   //ユニークID
        transBrand = MainApplication.getInstance().getString(R.string.money_brand_nanaco);   //ブランド名
        transDate = Converters.dateFormat(result.time);  //取引日時
        oldTransDate = refundParam.oldTransDate;  //元取引日時
        transType = resultParam.transType;    //取引種別
        transResult = resultParam.transResult;    //取引結果
        transResultDetail = resultParam.transResultDetail;    //取引結果詳細
        this.termSequence = termSequence;   //端末通番
        oldTermSequence = refundParam.oldTermSequence;    //元端末通番
        transId = result.sid;//決済ID
        oldTransId = refundParam.oldTransId;  //元決済ID
        //カード番号 saleHistoriesを確認
        installment = "10"; //分割回数
        transAmount = amountParam.transAmount;  //取引金額
        transSpecifiedAmount = amountParam.transSpecifiedAmount;    //定額
        transMeterAmount = amountParam.transMeterAmount;    //メータ金額
        transAdjAmount = amountParam.transAdjAmount;    //増減額
        transCashTogetherAmount = amountParam.transCashTogetherAmount;   //現金併用金額
        transOtherAmountOne = amountParam.transTicketAmount;    //チケット金額
        if(transOtherAmountOne != null && 0 < transOtherAmountOne) {
            transOtherAmountOneType = 1;   //その他金額1種別（チケット）
        } else {
            transOtherAmountOneType = 0;   //その他金額1種別（予備）
        }
        transOtherAmountTwoType = 0;   //その他金額2種別（予備）
        transOtherAmountTwo = 0;   //その他金額2（予備）
        //取引前残高 saleHistoriesを確認
        //取引後残高 saseHistoriesを確認
        transTime = surveyParam.transTime;  //取引処理時間 このタイミングではNULLの場合あり
        transInputPinTime = surveyParam.transInputPinTime;  //暗証番号入力時間 このタイミングではNULLの場合あり
        termLatitude = surveyParam.termLatitude; //位置情報（緯度）
        termLongitude = surveyParam.termLongitude;    //位置情報（経度）
        termNetworkType = surveyParam.termNetworkType;  //ネットワーク種別
        termRadioLevel = surveyParam.termRadioLevel;    //電波状況（レベル）
        this.encryptType = encryptType;   //暗号化パターン

        //nanaco固有項目
        //伝票番号 saseHistoriesを確認
        nanacoErrCode = Converters.stringToInteger(result.code);  //エラーコード
        //カード取引通番 saseHistoriesを確認
        nanacoTermIdentId = result.termIdentId != null
                ? result.termIdentId
                : EmoneyOpeningInfo.getNanaco().termIdentId;  //上位端末ID

        if(result.saleHistories != null) {
            cardId = result.saleHistories[0].tenantMaskNanacoNum;  //カード番号
            transBeforeBalance = Converters.stringToLong(result.saleHistories[0].beforeBalance); //取引前残高
            transAfterBalance = Converters.stringToLong(result.saleHistories[0].afterBalance);    //取引後残高
            nanacoSlipNumber = Converters.stringToLong(result.saleHistories[0].slipNo);
            nanacoCardTransNumber = Converters.stringToInteger(result.saleHistories[0].cardTransactionNo);  // 取引通番
        }
    }

    /**
     * QR売上データ
     */
    public UriData(QRSettlement.ResultSummary result, int encryptType, int termSequence, ResultParam resultParam, RefundParam refundParam, AmountParam amountParam, SurveyParam surveyParam) {
        //共通項目
        posSend = 0;    //POS送信状態
        carId = AppPreference.getMcCarId(); //号機番号（車番）
        driverId = AppPreference.getMcDriverId();    //乗務員コード
        termId = AppPreference.getMcTermId();   //端末番号
        transBrand = MainApplication.getInstance().getString(R.string.money_brand_codetrans);   //ブランド名
        transDate = result.payTime != null ? result.payTime : "";  //取引日時
        oldTransDate = refundParam.oldTransDate;  //元取引日時
        transType = resultParam.transType;    //取引種別
        transResult = resultParam.transResult;    //取引結果
        transResultDetail = resultParam.transResultDetail;    //取引結果詳細
        this.termSequence = termSequence;   //端末通番
        oldTermSequence = refundParam.oldTermSequence;    //元端末通番
        //カード番号 saleHistoriesを確認
        installment = "10"; //分割回数
        transAmount = amountParam.transAmount;  //取引金額
        transSpecifiedAmount = amountParam.transSpecifiedAmount;    //定額
        transMeterAmount = amountParam.transMeterAmount;    //メータ金額
        transAdjAmount = amountParam.transAdjAmount;    //増減額
        transCashTogetherAmount = amountParam.transCashTogetherAmount;   //現金併用金額
        transOtherAmountOne = amountParam.transTicketAmount;    //チケット金額
        if(transOtherAmountOne != null && 0 < transOtherAmountOne) {
            transOtherAmountOneType = 1;   //その他金額1種別（チケット）
        } else {
            transOtherAmountOneType = 0;   //その他金額1種別（予備）
        }
        transOtherAmountTwoType = 0;   //その他金額2種別（予備）
        transOtherAmountTwo = 0;   //その他金額2（予備）
        transBeforeBalance = null; // 取引前残高
        transAfterBalance = Converters.stringToLong(result.balanceAmount);  // 取引後残高
        transTime = surveyParam.transTime;  //取引処理時間 このタイミングではNULLの場合あり
        transInputPinTime = 0;  //暗証番号入力時間
        termLatitude = surveyParam.termLatitude; //位置情報（緯度）
        termLongitude = surveyParam.termLongitude;    //位置情報（経度）
        termNetworkType = surveyParam.termNetworkType;  //ネットワーク種別
        termRadioLevel = surveyParam.termRadioLevel;    //電波状況（レベル）
        this.encryptType = encryptType;   //暗号化パターン

        // QR固有項目
        if (result.refundId != null) {
            codetransOrderId = result.refundId;   // 伝票番号
            codetransOldOrderId = result.orderId; // 元伝票番号
        } else {
            codetransOrderId = result.orderId;    // 伝票番号
            codetransOldOrderId = null;
        }
        codetransPayTypeCode = result.payType;    // 決済種別コード
        wallet = result.wallet; // ウォレットコード Alipay+のみ
    }
}
