package jp.mcapps.android.multi_payment_terminal.database.history.slip;

import static jp.mcapps.android.multi_payment_terminal.thread.printer.PrinterProc.MANUALMODE_TRANS_TYPE_CODE_CANCEL;
import static jp.mcapps.android.multi_payment_terminal.thread.printer.PrinterProc.MANUALMODE_TRANS_TYPE_CODE_SALES;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.data.EmoneyOpeningInfo;
import jp.mcapps.android.multi_payment_terminal.data.IFBoxAppModels;
import jp.mcapps.android.multi_payment_terminal.data.QRPayTypeCodes;
import jp.mcapps.android.multi_payment_terminal.data.QRPayTypeNameMap;
import jp.mcapps.android.multi_payment_terminal.data.TransMap;
import jp.mcapps.android.multi_payment_terminal.data.trans_param.AmountParam;
import jp.mcapps.android.multi_payment_terminal.data.trans_param.RefundParam;
import jp.mcapps.android.multi_payment_terminal.data.trans_param.ResultParam;
import jp.mcapps.android.multi_payment_terminal.database.Converters;
import jp.mcapps.android.multi_payment_terminal.database.DBManager;
//import jp.mcapps.android.multi_payment_terminal.iCAS.data.DeviceClient;
import jp.mcapps.android.multi_payment_terminal.database.DeviceClient;
import jp.mcapps.android.multi_payment_terminal.model.QRSettlement;
//import jp.mcapps.android.multi_payment_terminal.model.WatariSettlement;
//import jp.mcapps.android.multi_payment_terminal.thread.credit.CreditSettlement;
//import jp.mcapps.android.multi_payment_terminal.thread.credit.data.CreditResult;
import jp.mcapps.android.multi_payment_terminal.ui.emoney.okica.EMoneyOkicaViewModel;

@Entity(tableName = "history_slip")
public class SlipData implements Serializable {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "trans_brand")
    public String transBrand;

    @ColumnInfo(name = "trans_type")
    public Integer transType;

    @ColumnInfo(name = "trans_type_code")
    public String transTypeCode;

    @ColumnInfo(name = "trans_result")
    public Integer transResult;

    @ColumnInfo(name = "trans_result_detail")
    public Integer transResultDetail;

    @ColumnInfo(name = "print_cnt")
    public Integer printCnt;

    @ColumnInfo(name = "old_aggregate_order")
    public Integer oldAggregateOrder;

    @ColumnInfo(name = "encrypt_type")
    public Integer encryptType;

    @ColumnInfo(name = "cancel_flg")
    public Integer cancelFlg;

    @ColumnInfo(name = "trans_id")
    public String transId;

    @ColumnInfo(name = "merchant_name")
    public String merchantName;

    @ColumnInfo(name = "merchant_office")
    public String merchantOffice;

    @ColumnInfo(name = "merchant_telnumber")
    public String merchantTelnumber;

    @ColumnInfo(name = "car_id")
    public Integer carId;

    @ColumnInfo(name = "driver_id")
    public Integer driverId;

    @ColumnInfo(name = "term_id")
    public String termId;

    @ColumnInfo(name = "term_sequence")
    public Integer termSequence;

    @ColumnInfo(name = "trans_date", index = true)
    public String transDate;

    @ColumnInfo(name = "card_company")
    public String cardCompany;

    @ColumnInfo(name = "card_id_merchant")
    public String cardIdMerchant;

    @ColumnInfo(name = "card_id_customer")
    public String cardIdCustomer;

    @ColumnInfo(name = "card_exp_date")
    public String cardExpDate;

    @ColumnInfo(name = "card_trans_number")
    public Integer cardTransNumber;

    @ColumnInfo(name = "edy_trans_number")
    public Long edyTransNumber;

    @ColumnInfo(name = "nanaco_slip_number")
    public Long nanacoSlipNumber;

    @ColumnInfo(name = "slip_number")
    public Integer slipNumber;

    @ColumnInfo(name = "old_slip_number")
    public Integer oldSlipNumber;

    @ColumnInfo(name = "auth_id")
    public Integer authId;  // アルファベット対応でprintingAuthIdに変更したため使わなくなったが、対応前のDBから読み込んだ場合はこちらが有効

    @ColumnInfo(name = "auth_sequence_number")
    public Integer authSequenceNumber;

    @ColumnInfo(name = "commodity_code")
    public String commodityCode;

    @ColumnInfo
    public String installment;

    @ColumnInfo
    public Integer point;

    @ColumnInfo(name = "point_grant_type")
    public Integer pointGrantType;

    @ColumnInfo(name = "point_grant_msg_one")
    public String pointGrantMsgOne;

    @ColumnInfo(name = "point_grant_msg_two")
    public String pointGrantMsgTwo;

    @ColumnInfo(name = "unique_id")
    public String uniqueId;

    @ColumnInfo(name = "term_ident_id")
    public String termIdentId;

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

    @ColumnInfo(name = "common_name")
    public String commonName;

    @ColumnInfo(name = "credit_type")
    public String creditType;

    @ColumnInfo(name = "credit_arc")
    public String creditArc;

    @ColumnInfo(name = "credit_aid")
    public String creditAid;

    @ColumnInfo(name = "credit_apl")
    public String creditApl;

    @ColumnInfo(name = "credit_signature_flg")
    public Integer creditSignatureFlg;

    @ColumnInfo(name= "codetrans_order_id")
    public String codetransOrderId;

    @ColumnInfo(name= "codetrans_pay_type_name")
    public String codetransPayTypeName;

    @ColumnInfo(name = "free_count_one")
    public Integer freeCountOne;

    @ColumnInfo(name = "free_count_two")
    public Integer freeCountTwo;

    @ColumnInfo(name= "credit_kid")
    public String creditKid;

    @ColumnInfo(name = "trans_complete_amount")
    public Integer transCompleteAmount;

    @ColumnInfo(name= "printing_authid")
    public String printingAuthId;

    @ColumnInfo(name = "transaction_terminal_type")
    public Integer transactionTerminalType;

    @ColumnInfo(name= "card_category")
    public String cardCategory;

    @ColumnInfo(name= "card_brand_code")
    public String cardBrandCode;

    @ColumnInfo(name= "card_brand_name")
    public String cardBrandName;

    @ColumnInfo(name= "purchased_ticket_deal_id")
    public String purchasedTicketDealId;

    @ColumnInfo(name= "trip_reservation_id")
    public String tripReservationId;

    @ColumnInfo(name= "send_cancel_purchased_ticket")
    public Integer sendCancelPurchasedTicket;

	@ColumnInfo(name = "watari_point")
    public Integer watariPoint;

    @ColumnInfo(name = "watari_sum_point")
    public Integer watariSumPoint;

    @ColumnInfo(name = "watari_calidity_period")
    public String watariCalidityPeriod;

//ADD-S BMT S.Oyama 2024/09/3 フタバ双方向向け改修
    @ColumnInfo(name= "trans_type_name")
    public String trans_type_name;

    @ColumnInfo(name= "status")
    public String status;

    @ColumnInfo(name= "slip_kind")
    public Integer slip_kind;

    @ColumnInfo(name= "pay_separation")
    public Integer pay_separation;

    @ColumnInfo(name= "card_id_card_company")
    public String card_id_card_company;

    @ColumnInfo(name= "card_exp_date_merchant")
    public String card_exp_date_merchant;

    @ColumnInfo(name= "card_exp_date_card_company")
    public String card_exp_date_card_company;

    @ColumnInfo(name= "auth_id_str")
    public String auth_id_str;                              //auth_id_strは使用しない　元から定義してある「printingAuthId」を使用のこと

    //@ColumnInfo(name= "trans_complete_amount")
    //public Integer trans_complete_amount;

    //@ColumnInfo(name= "credit_type")
    //public String credit_type;

    //@ColumnInfo(name= "credit_kid")
    //public String credit_kid;

    @ColumnInfo(name= "cat_dual_type")
    public Integer cat_dual_type;

    @ColumnInfo(name= "card_seq_no")
    public String card_seq_no;

    @ColumnInfo(name= "atc")
    public String atc;

    @ColumnInfo(name= "rw_id")
    public String rw_id;

    @ColumnInfo(name= "sprw_id")
    public String sprw_id;

    @ColumnInfo(name= "off_on_type")
    public String off_on_type;

    @ColumnInfo(name= "card_type")
    public String card_type;

    @ColumnInfo(name= "card_id")
    public String card_id;

    @ColumnInfo(name= "point_yuko_msg")
    public String point_yuko_msg;

    @ColumnInfo(name= "point_marchant")
    public Integer point_marchant;

    @ColumnInfo(name= "point_total")
    public String point_total;

    @ColumnInfo(name= "point_exp_date")
    public String point_exp_date;

    @ColumnInfo(name= "point_exp")
    public Integer point_exp;

    @ColumnInfo(name= "waon_trans_type_code")
    public Integer waon_trans_type_code;

    @ColumnInfo(name= "card_slip_no")
    public Integer card_slip_no;

    @ColumnInfo(name= "lid")
    public Integer lid;

    @ColumnInfo(name= "service_name")
    public String service_name;

    @ColumnInfo(name= "card_trans_number_str")
    public String card_trans_number_str;

    @ColumnInfo(name= "pay_id")
    public Integer pay_id;

    @ColumnInfo(name= "ic_no")
    public String ic_no;

    @ColumnInfo(name= "old_ic_no")
    public String old_ic_no;

    @ColumnInfo(name= "terminal_no")
    public String terminal_no;

    @ColumnInfo(name= "terminal_seq_no")
    public String terminal_seq_no;

    //@ColumnInfo(name= "unique_id")
    //public String unique_id;

    @ColumnInfo(name= "terminal_id")
    public String terminal_id;

    @ColumnInfo(name= "edy_seq_no")
    public String edy_seq_no;

    @ColumnInfo(name= "input_kingaku")
    public Integer input_kingaku;
//ADD-E BMT S.Oyama 2024/09/3 フタバ双方向向け改修

	@ColumnInfo(name = "prepaid_add_point")
    public Integer prepaidAddPoint;

    @ColumnInfo(name = "prepaid_total_point")
    public Integer prepaidTotalPoint;

    @ColumnInfo(name = "prepaid_next_expired")
    public String prepaidNextExpired;

    @ColumnInfo(name = "prepaid_next_expired_point")
    public Integer prepaidNextExpiredPoint;

    @ColumnInfo(name = "prepaid_service_name")
    public String prepaidServiceName;

    public SlipData() {}

    /**
     * クレジット印刷データ
     */
//    public SlipData(CreditResult.Result result, int encryptType, int termSequence, ResultParam resultParam, RefundParam refundParam, AmountParam amountParam) {
//        transBrand = MainApplication.getInstance().getString(R.string.money_brand_credit);   //ブランド名
//        transType = resultParam.transType;    //取引種別
//        transTypeCode = "0";
//        if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D_MANUAL) && resultParam.transResult == TransMap.RESULT_SUCCESS) {
//            // 手動決済モードで正常終了の場合
//            transTypeCode = transType == TransMap.TYPE_SALES ? MANUALMODE_TRANS_TYPE_CODE_SALES : MANUALMODE_TRANS_TYPE_CODE_CANCEL;
//        }
//        transResult = resultParam.transResult;    //取引結果
//        transResultDetail = resultParam.transResultDetail;  //取引結果詳細
//        printCnt = 0; //印刷回数
//        oldAggregateOrder = 0;    //集計印刷順
//        this.encryptType = encryptType;    //暗号化パターン
//        if (transType == TransMap.TYPE_SALES && transResult == TransMap.RESULT_SUCCESS) {
//            cancelFlg = 1;  //取消可否 売上かつ成功の場合のみ
//        }
//        if (AppPreference.isPosTransaction()) {
//            merchantName = AppPreference.getPosMerchantName();   //加盟店名
//            merchantOffice = AppPreference.getPosMerchantOffice();  //加盟店営業所名
//            merchantTelnumber = AppPreference.getPosMerchantTelnumber();    //加盟店電話番号
//        } else {
//            merchantName = AppPreference.getMerchantName();   //加盟店名
//            merchantOffice = AppPreference.getMerchantOffice();  //加盟店営業所名
//            merchantTelnumber = AppPreference.getMerchantTelnumber();    //加盟店電話番号
//        }
//        carId = AppPreference.getMcCarId(); //号機番号（車番）
//        driverId = AppPreference.getMcDriverId();    //乗務員コード
//        termId = AppPreference.getMcTermId();   //機器番号
//        this.termSequence = termSequence; //機器通番
//        transDate = Converters.dateFormat(result.authDateTime);  //取引日時
//        cardCompany = result.printText; //カード発行会社
//        cardIdMerchant = result.maskedMemberNo;   //カード番号（加盟店控）
//        cardIdCustomer = result.maskedMemberNo;   //カード番号（お客様控）
//        cardExpDate = result.cardExpDate;   //カード有効期限
//        slipNumber = termSequence;  //伝票番号
//        oldSlipNumber = refundParam.oldSlipNumber; //元伝票番号
//        printingAuthId = result.cafisApprovalNo.replaceFirst("^0+", "");    //承認番号 (先頭の0を消す)
//        commodityCode = result.productCd;   //商品コード
//        installment = "一括"; //分割回数
//        transAmount = amountParam.transAmount;   //取引金額
//        transSpecifiedAmount = amountParam.transSpecifiedAmount;    //定額
//        transMeterAmount = amountParam.transMeterAmount;    //メーター金額
//        transAdjAmount = amountParam.transAdjAmount;    // 増減額
//        transCashTogetherAmount = amountParam.transCashTogetherAmount;  // 現金併用額
//        transOtherAmountOne = amountParam.transTicketAmount;    //チケット金額
//        if (transOtherAmountOne != null && 0 < transOtherAmountOne) {
//            transOtherAmountOneType = 1;   //その他金額1種別（チケット）
//        } else {
//            transOtherAmountOneType = 0;   //その他金額1種別（予備）
//        }
//        transOtherAmountTwoType = 0;   //その他金額2種別（予備）
//        transOtherAmountTwo = 0;    //その他金額2（予備）
//        freeCountOne = amountParam.transEigyoCount;   //フリーカウント1
//        freeCountTwo = 0;   //フリーカウント2
////        creditType = result.msICKbn == CreditSettlement.k_MSIC_KBN_MS ? "MS"
////                : result.msICKbn == CreditSettlement.k_MSIC_KBN_IC ? "IC"
////                : result.msICKbn == CreditSettlement.k_MSIC_KBN_CONTACTLESS_IC ? "CL"
////                : "";
//        //ARC ICの場合のみ設定
//        //AID ICの場合のみ設定
//        //APL ICの場合のみ設定
////        creditSignatureFlg = result.signatureFlag; //サイン
//
////        if (result.msICKbn != 0) {
////            //IC
////            if (transType == TransMap.TYPE_SALES) {
////                creditArc = result.resCd; //ARC
////            } else {
////                creditArc = "00"; //ARC（取消時はオーソリ応答のARCが設定されない　00固定とする）
////            }
////            creditAid = result.aid;     //AID
////            creditApl = result.applicationLabel;  //APL
////        }
////        else
////        {
////            creditArc = "00"; //ARC（取消時はオーソリ応答のARCが設定されない　00固定とする）
////            creditAid = Converters.repeatSpace(32);     //AID  32個の空白
////            creditApl = Converters.repeatSpace(16);     //APL  16個の空白
////        }
//
////        creditKid = result.kid;  //KID
////        //ADD-S BMT S.Oyama 2025/03/05 フタバ双方向向け改修
////        if (creditKid == null || creditKid.isEmpty() == true) {
////            creditKid = "000";  //KID
////        }
//        //ADD-E BMT S.Oyama 2025/03/05 フタバ双方向向け改修
//        transCompleteAmount = amountParam.transCompleteAmount;  //支払済み金額
//
//        if (AppPreference.isPosTransaction()) {
//            transactionTerminalType = AppPreference.TerminalType.Pos.ordinal();
//        } else if (AppPreference.isTicketTransaction()) {
//            transactionTerminalType = AppPreference.TerminalType.Ticket.ordinal();
//        } else {
//            transactionTerminalType = AppPreference.TerminalType.Taxi.ordinal();
//        }
//
//        //ADD-S BMT S.Oyama 2024/09/30 フタバ双方向向け改修
//        card_id_card_company    = cardIdMerchant;                                                   //カード番号（カード会社控）
//        card_exp_date_merchant  = cardExpDate;                                                      //カード有効期限（加盟店控）
//        card_exp_date_card_company = cardExpDate;                                                   //カード有効期限（カード会社控）
//
//        cat_dual_type           = 0;                                                                //0:CAT型、1:DUAL型   0:CAT固定
//        card_seq_no             = Converters.integerToString(result.panSeqNo);                      //カードシーケンス番号   PANシーケンスナンバー
//        atc                     = result.atc;                                                       //ATC
//        rw_id                   = "";                        //RWID
//        sprw_id                 = "";                        //SPRWID
//        off_on_type             = "";                        //0:オフライン、1:オンライン
//        card_type               = "";                        //カード区分
//        card_id                 = "";                        //カードID
//        point_yuko_msg          = "";                        //ポイント利用不可時のメッセージ
//        point_marchant          = 0;                         //加盟店ポイント
//        point_total             = "";                        //累計ポイント
//        point_exp_date          = "";                        //ポイント有効期限
//        point_exp               = 0;                         //期限ポイント
//        waon_trans_type_code    = 0;                         //WAON取引種別コード
//        card_slip_no            = 0;                         //カード通番
//        lid                     = 0;                         //端末シリアル番号
//        service_name            = "";                        //サービス名
//        card_trans_number_str   = "";                        //取引通番
//        pay_id                  = 0;                         //支払ID
//        ic_no                   = "";                        //IC通番
//        old_ic_no               = "";                        //元IC通番
//        terminal_no             = "";                        //端末番号
//        terminal_seq_no         = "";                        //端末通番
//        uniqueId                = "";                        //ユニークID
//        terminal_id             = "";                        //上位端末ID
//        edy_seq_no              = "";                        //Edy取引通番
//        input_kingaku           = 0;                         //入力金額  PrinterProcで設定
//        //ADD-E BMT S.Oyama 2024/09/30 フタバ双方向向け改修
//
//        //ADD-S BMT S.Oyama 2024/11/29 フタバ双方向向け改修
//        prepaidAddPoint               = 0;                   //プリペイドポイント付与
//        prepaidTotalPoint             = 0;                   //プリペイドポイント残高
//        prepaidNextExpired            = "";                  //次回ポイント失効日
//        prepaidNextExpiredPoint       = 0;                   //次回ポイント失効ポイント
//        //ADD-E BMT S.Oyama 2024/11/29 フタバ双方向向け改修
//
//        updateCancelFlg();
//        setSendCancelPurchasedTicket();
//    }

    /**
     * 交通系印刷データ
     */
    public SlipData(DeviceClient.Result result, int encryptType, int termSequence, ResultParam resultParam, AmountParam amountParam) {
        transBrand = MainApplication.getInstance().getString(R.string.money_brand_suica);   //ブランド名
        transType = resultParam.transType;    //取引種別
        transTypeCode = "0";
        if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D_MANUAL) && resultParam.transResult == TransMap.RESULT_SUCCESS) {
            // 手動決済モードで正常終了の場合
            transTypeCode = transType == TransMap.TYPE_SALES ? MANUALMODE_TRANS_TYPE_CODE_SALES : MANUALMODE_TRANS_TYPE_CODE_CANCEL;
        }
        transResult = resultParam.transResult;    //取引結果
        transResultDetail = resultParam.transResultDetail;  //取引結果詳細
        printCnt = 0; //印刷回数
        oldAggregateOrder = 0;    //集計印刷順
        this.encryptType = encryptType;    //暗号化パターン

        //ADD-S BMT S.Oyama 2024/12/16 フタバ双方向向け改修
        if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == true || IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D_MANUAL) == true) {
            //フタバD時は取り消しはない
        }
        else {
            if (transType == TransMap.TYPE_SALES && transResult == TransMap.RESULT_SUCCESS) {
                cancelFlg = 1;  //取消可否 売上かつ成功の場合のみ
            }
        }
        //ADD-E BMT S.Oyama 2024/12/16 フタバ双方向向け改修

        if (AppPreference.isPosTransaction()) {
            merchantName = AppPreference.getPosMerchantName();   //加盟店名
            merchantOffice = AppPreference.getPosMerchantOffice();  //加盟店営業所名
            merchantTelnumber = AppPreference.getPosMerchantTelnumber();    //加盟店電話番号
        } else {
            merchantName = AppPreference.getMerchantName();   //加盟店名
            merchantOffice = AppPreference.getMerchantOffice();  //加盟店営業所名
            merchantTelnumber = AppPreference.getMerchantTelnumber();    //加盟店電話番号
        }
        carId = AppPreference.getMcCarId(); //号機番号（車番）
        driverId = AppPreference.getMcDriverId();    //乗務員コード
        termId = AppPreference.getMcTermId();   //機器番号
        this.termSequence = termSequence; //機器通番
        transDate = Converters.dateFormat(result.time);  //取引日時
        cardIdMerchant = resultParam.transResult != TransMap.RESULT_UNFINISHED
                       ? Converters.tenantMaskSuica(result.IDi)
                       : result.IDi;   //カード番号（加盟店控）処理未了時はマスクなしで一時保存
        cardIdCustomer = Converters.tenantMaskSuica(result.IDi);   //カード番号（お客様控）
        cardTransNumber = Converters.stringToInteger(result.ICsequence);  //取引通番
        termIdentId = result.sprwid != null
                    ? result.sprwid
                    : EmoneyOpeningInfo.getSuica().sprwid;//端末番号
        transAmount = amountParam.transAmount;   //取引金額
        transSpecifiedAmount = amountParam.transSpecifiedAmount;    //定額
        transMeterAmount = amountParam.transMeterAmount;    //メーター金額
        transAdjAmount = amountParam.transAdjAmount;    // 増減額
        transOtherAmountOne = amountParam.transTicketAmount;    //チケット金額
        if (transOtherAmountOne != null && 0 < transOtherAmountOne) {
            transOtherAmountOneType = 1;   //その他金額1種別（チケット）
        } else {
            transOtherAmountOneType = 0;   //その他金額1種別（予備）
        }
        transOtherAmountTwoType = 0;   //その他金額2種別（予備）
        transOtherAmountTwo = 0;    //その他金額2（予備）
        freeCountOne = amountParam.transEigyoCount;   //フリーカウント1
        freeCountTwo = 0;   //フリーカウント2
        transCashTogetherAmount = amountParam.transCashTogetherAmount != null
                ? amountParam.transCashTogetherAmount
                : 0;  // 現金併用額
        transBeforeBalance = Converters.stringToLong(result.bRem); //取引前残高
        // 20250214 t.wada 処理未了の場合は取引前の残高をセットする
        if ( transResult == TransMap.RESULT_UNFINISHED) {
            transAfterBalance = transBeforeBalance;
        } else {
            transAfterBalance = Converters.stringToLong(result.rem);   //取引後残高
        }
        commonName = AppPreference.getJremUniqueId();   //ユニークID
        transCompleteAmount = amountParam.transCompleteAmount;  //支払済み金額

        if (AppPreference.isPosTransaction()) {
            transactionTerminalType = AppPreference.TerminalType.Pos.ordinal();
        } else if (AppPreference.isTicketTransaction()) {
            transactionTerminalType = AppPreference.TerminalType.Ticket.ordinal();
        } else {
            transactionTerminalType = AppPreference.TerminalType.Taxi.ordinal();
        }

        //ADD-S BMT S.Oyama 2024/09/30 フタバ双方向向け改修
        cat_dual_type           = 0;                         //0:CAT型、1:DUAL型
        card_seq_no             = "";                        //カードシーケンス番号
        atc                     = "";                        //ATC
        rw_id                   = "                    ";                                           //RWID 該当フィールドなしなので20個の空白
        sprw_id                 = result.sprwid != null                                             //SPRWID
                                ? result.sprwid
                                : EmoneyOpeningInfo.getSuica().sprwid;
        off_on_type             = "";                        //0:オフライン、1:オンライン
        card_type               = "";                        //カード区分
        card_id                 = "";                        //カードID
        point_yuko_msg          = "";                        //ポイント利用不可時のメッセージ
        point_marchant          = 0;                         //加盟店ポイント
        point_total             = "";                        //累計ポイント
        point_exp_date          = "";                        //ポイント有効期限
        point_exp               = 0;                         //期限ポイント
        waon_trans_type_code    = 0;                         //WAON取引種別コード
        card_slip_no            = 0;                         //カード通番
        lid                     = 0;                         //端末シリアル番号
        service_name            = "";                        //サービス名
        card_trans_number_str   = "";                        //取引通番
        pay_id                  = 0;                         //支払ID
        ic_no                   = "";                        //IC通番
        old_ic_no               = "";                        //元IC通番
        terminal_no             = "";                        //端末番号
        terminal_seq_no         = "";                        //端末通番
        uniqueId                = "";                        //ユニークID
        terminal_id             = "";                        //上位端末ID
        edy_seq_no              = "";                        //Edy取引通番
        input_kingaku           = 0;                         //入力金額
        //ADD-E BMT S.Oyama 2024/09/30 フタバ双方向向け改修

        //ADD-S BMT S.Oyama 2024/11/29 フタバ双方向向け改修
        prepaidAddPoint               = 0;                   //プリペイドポイント付与
        prepaidTotalPoint             = 0;                   //プリペイドポイント残高
        prepaidNextExpired            = "";                  //次回ポイント失効日
        prepaidNextExpiredPoint       = 0;                   //次回ポイント失効ポイント
        //ADD-E BMT S.Oyama 2024/11/29 フタバ双方向向け改修

        updateCancelFlg();
        setSendCancelPurchasedTicket();
    }

    /**
     * Edy印刷データ
     */
    public SlipData(DeviceClient.ResultEdy result, int encryptType, int termSequence, ResultParam resultParam, AmountParam amountParam) {
        transBrand = MainApplication.getInstance().getString(R.string.money_brand_edy);   //ブランド名
        transType = resultParam.transType;    //取引種別
        transTypeCode = "0";
        if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D_MANUAL) && resultParam.transResult == TransMap.RESULT_SUCCESS) {
            // 手動決済モードで正常終了の場合
            transTypeCode = transType == TransMap.TYPE_SALES ? MANUALMODE_TRANS_TYPE_CODE_SALES : MANUALMODE_TRANS_TYPE_CODE_CANCEL;
        }
        transResult = resultParam.transResult;    //取引結果
        transResultDetail = resultParam.transResultDetail;  //取引結果詳細
        printCnt = 0; //印刷回数
        oldAggregateOrder = 0;    //集計印刷順
        this.encryptType = encryptType;    //暗号化パターン
        //取消可否  Edyは取消なし
        transId = result.sid;   //決済ID
        if (AppPreference.isPosTransaction()) {
            merchantName = AppPreference.getPosMerchantName();   //加盟店名
            merchantOffice = AppPreference.getPosMerchantOffice();  //加盟店営業所名
            merchantTelnumber = AppPreference.getPosMerchantTelnumber();    //加盟店電話番号
        } else {
            merchantName = AppPreference.getMerchantName();   //加盟店名
            merchantOffice = AppPreference.getMerchantOffice();  //加盟店営業所名
            merchantTelnumber = AppPreference.getMerchantTelnumber();    //加盟店電話番号
        }
        carId = AppPreference.getMcCarId(); //号機番号（車番）
        driverId = AppPreference.getMcDriverId();    //乗務員コード
        termId = AppPreference.getMcTermId();   //機器番号
        this.termSequence = termSequence; //機器通番
        transDate = Converters.dateFormat(result.time);  //取引日時
        //カード番号（加盟店控）saleHistoriesを確認
        //カード番号（お客様控）saleHistoriesを確認
        //取引通番（カード取引通番）saleHistoriesを確認
        //Edy取引通番 saleHistoriesを確認
        termIdentId = result.termIdentId != null
                ? result.termIdentId
                : EmoneyOpeningInfo.getEdy().termIdentId;//端末番号
        transAmount = amountParam.transAmount;   //取引金額
        transSpecifiedAmount = amountParam.transSpecifiedAmount;    //定額
        transMeterAmount = amountParam.transMeterAmount;    //メーター金額
        transAdjAmount = amountParam.transAdjAmount;    // 増減額
        transCashTogetherAmount = amountParam.transCashTogetherAmount != null
                ? amountParam.transCashTogetherAmount
                : 0;  // 現金併用額
        transOtherAmountOne = amountParam.transTicketAmount;    //チケット金額
        if (transOtherAmountOne != null && 0 < transOtherAmountOne) {
            transOtherAmountOneType = 1;   //その他金額1種別（チケット）
        } else {
            transOtherAmountOneType = 0;   //その他金額1種別（予備）
        }
        transOtherAmountTwoType = 0;   //その他金額2種別（予備）
        transOtherAmountTwo = 0;    //その他金額2（予備）
        freeCountOne = amountParam.transEigyoCount;   //フリーカウント1
        freeCountTwo = 0;   //フリーカウント2
        //取引前残高 saleHistoriesを確認
        //取引後残高 saleHistoriesを確認
        commonName = AppPreference.getJremUniqueId();   //ユニークID

        if (result.saleHistories != null) {
            cardIdMerchant = resultParam.transResult != TransMap.RESULT_UNFINISHED
                    ? result.saleHistories[0].userMaskMembershipNum
                    : result.saleHistories[0].memberMaskMembershipNum;   //カード番号（加盟店控え用にお客様控え用のマスクカード番号をセット）※処理未了時は、加盟店控え用のカード番号をマスクなしで一時保存
            cardIdCustomer = result.saleHistories[0].userMaskMembershipNum; //カード番号(お客様控え用)
            cardTransNumber = Converters.stringToInteger(result.saleHistories[0].cardTransactionNo); // カード取引通番
            edyTransNumber = Converters.stringToLong(result.saleHistories[0].edyTransactionNo);  // Edy取引通番
            transBeforeBalance = Converters.stringToLong(result.saleHistories[0].beforeBalance); //取引前残高 saleHistoriesを確認
            transAfterBalance = Converters.stringToLong(result.saleHistories[0].afterBalance);   //取引後残高 saleHistoriesを確認
        }

        transCompleteAmount = amountParam.transCompleteAmount;  //支払済み金額

        if (AppPreference.isPosTransaction()) {
            transactionTerminalType = AppPreference.TerminalType.Pos.ordinal();
        } else if (AppPreference.isTicketTransaction()) {
            transactionTerminalType = AppPreference.TerminalType.Ticket.ordinal();
        } else {
            transactionTerminalType = AppPreference.TerminalType.Taxi.ordinal();
        }

        //ADD-S BMT S.Oyama 2024/09/30 フタバ双方向向け改修
        cat_dual_type           = 0;                         //0:CAT型、1:DUAL型
        card_seq_no             = Converters.integerToString(cardTransNumber);                      //カードシーケンス番号
        atc                     = "";                        //ATC
        rw_id                   = "";                        //RWID
        sprw_id                 = "";                        //SPRWID
        off_on_type             = "";                        //0:オフライン、1:オンライン
        card_type               = "";                        //カード区分
        card_id                 = "";                        //カードID
        point_yuko_msg          = "";                        //ポイント利用不可時のメッセージ
        point_marchant          = 0;                         //加盟店ポイント
        point_total             = "";                        //累計ポイント
        point_exp_date          = "";                        //ポイント有効期限
        point_exp               = 0;                         //期限ポイント
        waon_trans_type_code    = 0;                         //WAON取引種別コード
        card_slip_no            = 0;                         //カード通番
        lid                     = 0;                         //端末シリアル番号
        service_name            = "";                        //サービス名
        card_trans_number_str   = "";                        //取引通番
        pay_id                  = 0;                         //支払ID
        ic_no                   = "";                        //IC通番
        old_ic_no               = "";                        //元IC通番
        terminal_no             = "";                        //端末番号
        terminal_seq_no         = "";                        //端末通番
        uniqueId                = "";                        //ユニークID
        terminal_id             = termIdentId;                                                      //上位端末ID
        edy_seq_no              = Converters.longToString(edyTransNumber);                          //Edy取引通番
        input_kingaku           = 0;                         //入力金額
        //ADD-E BMT S.Oyama 2024/09/30 フタバ双方向向け改修

        //ADD-S BMT S.Oyama 2024/11/29 フタバ双方向向け改修
        prepaidAddPoint               = 0;                   //プリペイドポイント付与
        prepaidTotalPoint             = 0;                   //プリペイドポイント残高
        prepaidNextExpired            = "";                  //次回ポイント失効日
        prepaidNextExpiredPoint       = 0;                   //次回ポイント失効ポイント
        //ADD-E BMT S.Oyama 2024/11/29 フタバ双方向向け改修

        updateCancelFlg();
        setSendCancelPurchasedTicket();
    }

    /**
     * iD印刷データ
     */
    public SlipData(DeviceClient.ResultID result, int encryptType, int termSequence, ResultParam resultParam, AmountParam amountParam) {
        transBrand = MainApplication.getInstance().getString(R.string.money_brand_id);   //ブランド名
        transType = resultParam.transType;    //取引種別
        transTypeCode = "0";
        if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D_MANUAL) && resultParam.transResult == TransMap.RESULT_SUCCESS) {
            // 手動決済モードで正常終了の場合
            transTypeCode = transType == TransMap.TYPE_SALES ? MANUALMODE_TRANS_TYPE_CODE_SALES : MANUALMODE_TRANS_TYPE_CODE_CANCEL;
        }
        transResult = resultParam.transResult;    //取引結果
        transResultDetail = resultParam.transResultDetail;  //取引結果詳細
        printCnt = 0; //印刷回数
        oldAggregateOrder = 0;    //集計印刷順
        this.encryptType = encryptType;    //暗号化パターン

        if (transType == TransMap.TYPE_SALES && transResult == TransMap.RESULT_SUCCESS) {
            cancelFlg = 1;  //取消可否 売上かつ成功の場合のみ
        }

        transId = result.sid;   //決済ID
        if (AppPreference.isPosTransaction()) {
            merchantName = AppPreference.getPosMerchantName();   //加盟店名
            merchantOffice = AppPreference.getPosMerchantOffice();  //加盟店営業所名
            merchantTelnumber = AppPreference.getPosMerchantTelnumber();    //加盟店電話番号
        } else {
            merchantName = AppPreference.getMerchantName();   //加盟店名
            merchantOffice = AppPreference.getMerchantOffice();  //加盟店営業所名
            merchantTelnumber = AppPreference.getMerchantTelnumber();    //加盟店電話番号
        }
        carId = AppPreference.getMcCarId(); //号機番号（車番）
        driverId = AppPreference.getMcDriverId();    //乗務員コード
        termId = AppPreference.getMcTermId();   //機器番号
        this.termSequence = termSequence; //機器通番
        transDate = Converters.dateFormat(result.time);  //取引日時
        // iDは未了でもカード番号をマスクする
        cardIdMerchant = result.memberMaskMembershipNum;
        cardIdCustomer = result.userMaskMembershipNum;   //カード番号（お客様控）
        cardExpDate = "XX/XX";
        slipNumber = Converters.stringToInteger(result.slipNo);
        printingAuthId  = resultParam.transResult != TransMap.RESULT_UNFINISHED && result.recognitionNum != null
                          ? result.recognitionNum.replaceFirst("^0+", "")   //承認番号 (先頭の0を消す)
                          : null;   //処理未了や(非オンラインで)承認番号がない場合
        authSequenceNumber = Converters.stringToInteger(result.sequenceNo); //処理通番
        termIdentId = result.termIdentId != null
                ? result.termIdentId
                : EmoneyOpeningInfo.getId().termIdentId;//端末番号
        transAmount = amountParam.transAmount;   //取引金額
        transSpecifiedAmount = amountParam.transSpecifiedAmount;    //定額
        transMeterAmount = amountParam.transMeterAmount;    //メーター金額
        transAdjAmount = amountParam.transAdjAmount;    // 増減額
        transCashTogetherAmount = amountParam.transCashTogetherAmount != null
                ? amountParam.transCashTogetherAmount
                : 0;  // 現金併用額
        transOtherAmountOne = amountParam.transTicketAmount;    //チケット金額
        if (transOtherAmountOne != null && 0 < transOtherAmountOne) {
            transOtherAmountOneType = 1;   //その他金額1種別（チケット）
        } else {
            transOtherAmountOneType = 0;   //その他金額1種別（予備）
        }
        transOtherAmountTwoType = 0;   //その他金額2種別（予備）
        transOtherAmountTwo = 0;    //その他金額2（予備）
        freeCountOne = amountParam.transEigyoCount;   //フリーカウント1
        freeCountTwo = 0;   //フリーカウント2
        commonName = AppPreference.getJremUniqueId();   //ユニークID

        if (result.giftFlg != null && result.giftFlg.equals("true")) {
            transAfterBalance = Converters.stringToLong(result.rem);
        }

        transCompleteAmount = amountParam.transCompleteAmount;  //支払済み金額

        if (AppPreference.isPosTransaction()) {
            transactionTerminalType = AppPreference.TerminalType.Pos.ordinal();
        } else if (AppPreference.isTicketTransaction()) {
            transactionTerminalType = AppPreference.TerminalType.Ticket.ordinal();
        } else {
            transactionTerminalType = AppPreference.TerminalType.Taxi.ordinal();
        }

        //ADD-S BMT S.Oyama 2024/10/08 フタバ双方向向け改修
        String tmpoff_on_type;
        if (result.sequenceNo == null) {
            tmpoff_on_type = "0";
        } else {
            tmpoff_on_type = "1";
        }
        //ADD-E BMT S.Oyama 2024/10/08 フタバ双方向向け改修


        //ADD-S BMT S.Oyama 2024/09/30 フタバ双方向向け改修
        //card_id_card_company    = cardIdMerchant;                                                   //カード番号（カード会社控）
        card_exp_date_merchant  = "XX/XX";                                                            //カード有効期限（加盟店控）
        //card_exp_date_card_company = cardExpDate;                                                   //カード有効期限（カード会社控）


        cat_dual_type           = 0;                                                                //0:CAT型、1:DUAL型  0:固定
        card_seq_no             = "";                        //カードシーケンス番号
        atc                     = "";                        //ATC
        rw_id                   = "";                        //RWID
        sprw_id                 = "";                        //SPRWID
        off_on_type             = tmpoff_on_type;                                                   //0:オフライン、1:オンライン
        card_type               = "FE";                                                             //カード区分
        card_id                 = "";                        //カードID
        point_yuko_msg          = "";                        //ポイント利用不可時のメッセージ
        point_marchant          = 0;                         //加盟店ポイント
        point_total             = "";                        //累計ポイント
        point_exp_date          = "";                        //ポイント有効期限
        point_exp               = 0;                         //期限ポイント
        waon_trans_type_code    = 0;                         //WAON取引種別コード
        card_slip_no            = 0;                         //カード通番
        lid                     = 0;                         //端末シリアル番号
        service_name            = "";                        //サービス名
        card_trans_number_str   = "";                        //取引通番
        pay_id                  = 0;                         //支払ID
        ic_no                   = "";                        //IC通番
        old_ic_no               = "";                        //元IC通番
        terminal_no             = "";                        //端末番号
        terminal_seq_no         = "";                        //端末通番
        uniqueId                = "";                        //ユニークID
        terminal_id             = "";                        //上位端末ID
        edy_seq_no              = "";                        //Edy取引通番
        input_kingaku           = 0;                         //入力金額     PrinterProcで設定
        //ADD-E BMT S.Oyama 2024/09/30 フタバ双方向向け改修

        //ADD-S BMT S.Oyama 2024/11/29 フタバ双方向向け改修
        prepaidAddPoint               = 0;                   //プリペイドポイント付与
        prepaidTotalPoint             = 0;                   //プリペイドポイント残高
        prepaidNextExpired            = "";                  //次回ポイント失効日
        prepaidNextExpiredPoint       = 0;                   //次回ポイント失効ポイント
        //ADD-E BMT S.Oyama 2024/11/29 フタバ双方向向け改修

        updateCancelFlg();
        setSendCancelPurchasedTicket();
    }

    /**
     * waon印刷データ
     */
    public SlipData(DeviceClient.ResultWAON result, int encryptType, int termSequence, ResultParam resultParam, RefundParam refundParam, AmountParam amountParam) {
        transBrand = MainApplication.getInstance().getString(R.string.money_brand_waon);   //ブランド名
        transType = resultParam.transType;    //取引種別
        if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D_MANUAL) && resultParam.transResult == TransMap.RESULT_SUCCESS) {
            // 手動決済モードで正常終了の場合
            transTypeCode = transType == TransMap.TYPE_SALES ? MANUALMODE_TRANS_TYPE_CODE_SALES : MANUALMODE_TRANS_TYPE_CODE_CANCEL;
        } else {
            transTypeCode = result.tradeTypeCode;   //取引種別コード
        }
        transResult = resultParam.transResult;    //取引結果
        transResultDetail = resultParam.transResultDetail;  //取引結果詳細
        printCnt = 0; //印刷回数
        oldAggregateOrder = 0;    //集計印刷順
        this.encryptType = encryptType;    //暗号化パターン

        //ADD-S BMT S.Oyama 2024/12/16 フタバ双方向向け改修
        if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == true || IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D_MANUAL) == true) {
            //フタバD時は取り消しはない
        }
        else {
            if (transType == TransMap.TYPE_SALES && transResult == TransMap.RESULT_SUCCESS) {
                cancelFlg = 1;  //取消可否 売上かつ成功の場合のみ
            }
        }
        //ADD-E BMT S.Oyama 2024/12/16 フタバ双方向向け改修

        transId = result.sid;   //決済ID
        if (AppPreference.isPosTransaction()) {
            merchantName = AppPreference.getPosMerchantName();   //加盟店名
            merchantOffice = AppPreference.getPosMerchantOffice();  //加盟店営業所名
            merchantTelnumber = AppPreference.getPosMerchantTelnumber();    //加盟店電話番号
        } else {
            merchantName = AppPreference.getMerchantName();   //加盟店名
            merchantOffice = AppPreference.getMerchantOffice();  //加盟店営業所名
            merchantTelnumber = AppPreference.getMerchantTelnumber();    //加盟店電話番号
        }
        carId = AppPreference.getMcCarId(); //号機番号（車番）
        driverId = AppPreference.getMcDriverId();    //乗務員コード
        termId = AppPreference.getMcTermId();   //機器番号
        this.termSequence = termSequence; //機器通番
        transDate = Converters.dateFormat(result.time);  //取引日時
        cardIdMerchant = result.memberMaskWaonNum();    //カード番号（加盟店控）
        cardIdCustomer = result.userMaskWaonNum();  //カード番号（お客様控）
        cardTransNumber = Converters.stringToInteger(result.cardThroughNum);  //取引通番
        slipNumber = Converters.stringToInteger(result.slipNo);
        oldSlipNumber = result.oldSlipNo != null
                ? Converters.stringToInteger(result.oldSlipNo)
                : refundParam.oldSlipNumber;  //元伝票番号 RASの応答に設定されていない場合は印刷履歴から取得
        point = Converters.stringToInteger(result.point) != null
                ? Integer.valueOf(result.point)
                : 0;
        pointGrantType = Converters.stringToInteger(result.pointGrantType);
        pointGrantMsgOne = result.pointGrantMessage1;
        pointGrantMsgTwo = result.pointGrantMessage2;
        termIdentId = result.termIdentId != null
                ? result.termIdentId
                : EmoneyOpeningInfo.getWaon().termIdentId;//端末番号
        transAmount = amountParam.transAmount;   //取引金額
        transSpecifiedAmount = amountParam.transSpecifiedAmount;    //定額
        transMeterAmount = amountParam.transMeterAmount;    //メーター金額
        transAdjAmount = amountParam.transAdjAmount;    // 増減額
        transCashTogetherAmount = amountParam.transCashTogetherAmount != null
                ? amountParam.transCashTogetherAmount
                : 0;  // 現金併用額
        transOtherAmountOne = amountParam.transTicketAmount;    //チケット金額
        if (transOtherAmountOne != null && 0 < transOtherAmountOne) {
            transOtherAmountOneType = 1;   //その他金額1種別（チケット）
        } else {
            transOtherAmountOneType = 0;   //その他金額1種別（予備）
        }
        transOtherAmountTwoType = 0;   //その他金額2種別（予備）
        transOtherAmountTwo = 0;    //その他金額2（予備）
        freeCountOne = amountParam.transEigyoCount;   //フリーカウント1
        freeCountTwo = 0;   //フリーカウント2
        transBeforeBalance = Converters.stringToLong(result.beforeBalance);  // 取引前残高
        transAfterBalance = Converters.stringToLong(result.balance);  // 取引後残高
        commonName = AppPreference.getJremUniqueId();   //ユニークID
        transCompleteAmount = amountParam.transCompleteAmount;  //支払済み金額

        if (AppPreference.isPosTransaction()) {
            transactionTerminalType = AppPreference.TerminalType.Pos.ordinal();
        } else if (AppPreference.isTicketTransaction()) {
            transactionTerminalType = AppPreference.TerminalType.Ticket.ordinal();
        } else {
            transactionTerminalType = AppPreference.TerminalType.Taxi.ordinal();
        }

        //ADD-S BMT S.Oyama 2024/10/07 フタバ双方向向け改修
        String tmpsprw_id = "             ";
        if (result.addInfo != null) {
            if ((result.addInfo.historyData != null) && (result.addInfo.historyData.length > 0)) {
                if (result.addInfo.historyData[0].sprw_id != null) {
                    tmpsprw_id = result.addInfo.historyData[0].sprw_id;
                }
            }
        }

        String tmpPointExpDate = "";
        if (result.validDate2 != null) {
            tmpPointExpDate = result.validDate2;
        }
        else {
            if (result.validDate1 != null) {
                tmpPointExpDate = result.validDate1;
            }
        }

        if (tmpPointExpDate.length() == 4)          //ポイント有効日付が4桁の場合は末日を追加
        {
            String tmpYStr = tmpPointExpDate.substring(0, 2);
            String tmpMStr = tmpPointExpDate.substring(2);
            String tmpDStr = "";
            try
            {
                int tmpY = Converters.stringToInteger(tmpYStr);
                int tmpM = Converters.stringToInteger(tmpMStr);

                tmpY += 2000;           //2000～2099年対応

                if (tmpM == 2) {
                    if (((tmpY % 4 == 0) && (tmpY % 100 != 0)) || (tmpY % 400 == 0)) {
                        tmpDStr = "29";                               // うるう年の2月は29日
                    } else {
                        tmpDStr = "28";                               // 通常の2月は28日
                    }
                } else if (tmpM == 4 || tmpM == 6 || tmpM == 9 || tmpM == 11) {
                    tmpDStr = "30";                                   // 30日の月
                } else {
                    tmpDStr = "31";                                   // 31日の月
                }

                tmpPointExpDate = tmpYStr + tmpMStr + tmpDStr;
            }
            catch (Exception e)
            {
                tmpPointExpDate = "";
            }
        }


        int tmppointexp = 0;
        if (result.point2 != null) {
            tmppointexp = Converters.stringToInteger(result.point2);
        }
        else {
            if (result.point1 != null) {
                tmppointexp = Converters.stringToInteger(result.point1);
            }
        }

        long tmplidBCD = 0;
        long tmptermID = Converters.stringToLong(termId);
        tmplidBCD =  Converters.longToIntBCD(tmptermID, 8);           //lidは8桁BCD
        //ADD-E BMT S.Oyama 2024/10/07 フタバ双方向向け改修


        //ADD-S BMT S.Oyama 2024/09/30 フタバ双方向向け改修
        cat_dual_type           = 0;                         //0:CAT型、1:DUAL型
        card_seq_no             = "";                        //カードシーケンス番号
        atc                     = "";                        //ATC
        rw_id                   = "                    ";                                           //RWID 該当フィールドなしなので20個の空白
        sprw_id                 = tmpsprw_id;                                                       //SPRWID
        off_on_type             = "";                        //0:オフライン、1:オンライン
        card_type               = "";                        //カード区分
        card_id                 = result.waonNum;                                                   //カードID
        point_yuko_msg          = result.pointGrantType + "0";                                      //ポイント利用不可時のメッセージ
        point_marchant          = 0;                                                                //加盟店ポイント
        point_total             = result.totalPoint;                                                //累計ポイント
        point_exp_date          = tmpPointExpDate;                                                  //ポイント有効期限
        point_exp               = tmppointexp;                                                      //期限ポイント
        waon_trans_type_code    = Converters.stringToInteger(result.tradeTypeCode);                 //WAON取引種別コード
        card_slip_no            = Converters.stringToInteger(result.cardThroughNum);                //カード通番
        lid                     = (int)tmplidBCD;                                                   //端末シリアル番号
        service_name            = "";                        //サービス名
        card_trans_number_str   = "";                        //取引通番
        pay_id                  = 0;                         //支払ID
        ic_no                   = "";                        //IC通番
        old_ic_no               = "";                        //元IC通番
        terminal_no             = "";                        //端末番号
        terminal_seq_no         = "";                        //端末通番
        uniqueId                = "";                        //ユニークID
        terminal_id             = "";                        //上位端末ID
        edy_seq_no              = "";                        //Edy取引通番
        input_kingaku           = 0;                         //入力金額
        //ADD-E BMT S.Oyama 2024/09/30 フタバ双方向向け改修

        //ADD-S BMT S.Oyama 2024/11/29 フタバ双方向向け改修
        prepaidAddPoint               = 0;                   //プリペイドポイント付与
        prepaidTotalPoint             = 0;                   //プリペイドポイント残高
        prepaidNextExpired            = "";                  //次回ポイント失効日
        prepaidNextExpiredPoint       = 0;                   //次回ポイント失効ポイント
        //ADD-E BMT S.Oyama 2024/11/29 フタバ双方向向け改修

        updateCancelFlg();
        setSendCancelPurchasedTicket();
    }

    /**
     * QUICPay印刷データ
     */
    public SlipData(DeviceClient.ResultQUICPay result, int encryptType, int termSequence, ResultParam resultParam, AmountParam amountParam) {
        transBrand = MainApplication.getInstance().getString(R.string.money_brand_qp);   //ブランド名
        transType = resultParam.transType;    //取引種別
        transTypeCode = "0";
        if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D_MANUAL) && resultParam.transResult == TransMap.RESULT_SUCCESS) {
            // 手動決済モードで正常終了の場合
            transTypeCode = transType == TransMap.TYPE_SALES ? MANUALMODE_TRANS_TYPE_CODE_SALES : MANUALMODE_TRANS_TYPE_CODE_CANCEL;
        }
        transResult = resultParam.transResult;    //取引結果
        transResultDetail = resultParam.transResultDetail;  //取引結果詳細
        printCnt = 0; //印刷回数
        oldAggregateOrder = 0;    //集計印刷順
        this.encryptType = encryptType;    //暗号化パターン

        //ADD-S BMT S.Oyama 2024/12/16 フタバ双方向向け改修
        if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == true || IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D_MANUAL) == true) {
            //フタバD時は取り消しはない
        }
        else {
            if (transType == TransMap.TYPE_SALES && transResult == TransMap.RESULT_SUCCESS) {
                cancelFlg = 1;  //取消可否 売上かつ成功の場合のみ
            }
        }
        //ADD-E BMT S.Oyama 2024/12/16 フタバ双方向向け改修

        transId = result.sid;   //決済ID
        if (AppPreference.isPosTransaction()) {
            merchantName = AppPreference.getPosMerchantName();   //加盟店名
            merchantOffice = AppPreference.getPosMerchantOffice();  //加盟店営業所名
            merchantTelnumber = AppPreference.getPosMerchantTelnumber();    //加盟店電話番号
        } else {
            merchantName = AppPreference.getMerchantName();   //加盟店名
            merchantOffice = AppPreference.getMerchantOffice();  //加盟店営業所名
            merchantTelnumber = AppPreference.getMerchantTelnumber();    //加盟店電話番号
        }
        carId = AppPreference.getMcCarId(); //号機番号（車番）
        driverId = AppPreference.getMcDriverId();    //乗務員コード
        termId = AppPreference.getMcTermId();   //機器番号
        // QUICPayは機器通番未使用
        this.termSequence = termSequence; //機器通番
        transDate = Converters.dateFormat(result.time);  //取引日時
        cardIdMerchant = resultParam.transResult != TransMap.RESULT_UNFINISHED
                ? result.memberMaskMembershipNum
                : result.cardCompMaskMembershipNum;   //カード番号（加盟店控）処理未了時はマスクなしで一時保存
        cardIdCustomer = result.userMaskMembershipNum;   //カード番号（お客様控）
        slipNumber = Converters.stringToInteger(result.slipNo);
        termIdentId = result.termIdentId != null
                ? result.termIdentId
                : EmoneyOpeningInfo.getQuicpay().termIdentId;//端末番号
        transAmount = amountParam.transAmount;   //取引金額
        transSpecifiedAmount = amountParam.transSpecifiedAmount;    //定額
        transMeterAmount = amountParam.transMeterAmount;    //メーター金額
        transAdjAmount = amountParam.transAdjAmount;    // 増減額
        transCashTogetherAmount = amountParam.transCashTogetherAmount != null
                ? amountParam.transCashTogetherAmount
                : 0;  // 現金併用額
        transOtherAmountOne = amountParam.transTicketAmount;    //チケット金額
        if (transOtherAmountOne != null && 0 < transOtherAmountOne) {
            transOtherAmountOneType = 1;   //その他金額1種別（チケット）
        } else {
            transOtherAmountOneType = 0;   //その他金額1種別（予備）
        }
        transOtherAmountTwoType = 0;   //その他金額2種別（予備）
        transOtherAmountTwo = 0;    //その他金額2（予備）
        freeCountOne = amountParam.transEigyoCount;   //フリーカウント1
        freeCountTwo = 0;   //フリーカウント2
        commonName = AppPreference.getJremUniqueId();   //ユニークID
        transCompleteAmount = amountParam.transCompleteAmount;  //支払済み金額

        if (AppPreference.isPosTransaction()) {
            transactionTerminalType = AppPreference.TerminalType.Pos.ordinal();
        } else if (AppPreference.isTicketTransaction()) {
            transactionTerminalType = AppPreference.TerminalType.Ticket.ordinal();
        } else {
            transactionTerminalType = AppPreference.TerminalType.Taxi.ordinal();
        }

        //ADD-S BMT S.Oyama 2024/09/30 フタバ双方向向け改修
        cardExpDate = "XX/XX";    //カード有効期限

        if (result.acquierName != null) { //カード発行会社
            if (result.acquierName.equals("") == true)
            {
                cardCompany = "ＪＣＢグル";
            }
            else
            {
                cardCompany = result.acquierName;
            }
        } else {
            cardCompany = "";
        }

        //card_id_card_company    = cardIdMerchant;                                                   //カード番号（カード会社控）
        card_exp_date_merchant  = "XX/XX";                                                            //カード有効期限（加盟店控）
        //card_exp_date_card_company = cardExpDate;                                                   //カード有効期限（カード会社控）

        cat_dual_type           = 0;                         //0:CAT型、1:DUAL型
        card_seq_no             = "";                        //カードシーケンス番号
        atc                     = "";                        //ATC
        rw_id                   = "";                        //RWID
        sprw_id                 = "";                        //SPRWID
        off_on_type             = "";                        //0:オフライン、1:オンライン              //??
        card_type               = "FE";                                                             //カード区分
        card_id                 = "";                        //カードID
        point_yuko_msg          = "";                        //ポイント利用不可時のメッセージ
        point_marchant          = 0;                         //加盟店ポイント
        point_total             = "";                        //累計ポイント
        point_exp_date          = "";                        //ポイント有効期限
        point_exp               = 0;                         //期限ポイント
        waon_trans_type_code    = 0;                         //WAON取引種別コード
        card_slip_no            = 0;                         //カード通番
        lid                     = 0;                         //端末シリアル番号
        service_name            = "";                        //サービス名
        card_trans_number_str   = "";                        //取引通番
        pay_id                  = 0;                         //支払ID
        ic_no                   = result.slipNo;                                                    //IC通番
        old_ic_no               = result.oldSlipNo;                                                 //元IC通番
        terminal_no             = "";                        //端末番号
        terminal_seq_no         = "";                        //端末通番
        uniqueId                = "";                        //ユニークID
        terminal_id             = "";                        //上位端末ID
        edy_seq_no              = "";                        //Edy取引通番
        input_kingaku           = 0;                         //入力金額
        //ADD-E BMT S.Oyama 2024/09/30 フタバ双方向向け改修

        //ADD-S BMT S.Oyama 2024/11/29 フタバ双方向向け改修
        prepaidAddPoint               = 0;                   //プリペイドポイント付与
        prepaidTotalPoint             = 0;                   //プリペイドポイント残高
        prepaidNextExpired            = "";                  //次回ポイント失効日
        prepaidNextExpiredPoint       = 0;                   //次回ポイント失効ポイント
        //ADD-E BMT S.Oyama 2024/11/29 フタバ双方向向け改修

        updateCancelFlg();
        setSendCancelPurchasedTicket();
    }

    /**
     * nanaco印刷データ
     */
    public SlipData(DeviceClient.Resultnanaco result, int encryptType, int termSequence, ResultParam resultParam, AmountParam amountParam) {
        transBrand = MainApplication.getInstance().getString(R.string.money_brand_nanaco);   //ブランド名
        transType = resultParam.transType;    //取引種別
        transTypeCode = "0";
        if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D_MANUAL) && resultParam.transResult == TransMap.RESULT_SUCCESS) {
            // 手動決済モードで正常終了の場合
            transTypeCode = transType == TransMap.TYPE_SALES ? MANUALMODE_TRANS_TYPE_CODE_SALES : MANUALMODE_TRANS_TYPE_CODE_CANCEL;
        }
        transResult = resultParam.transResult;    //取引結果
        transResultDetail = resultParam.transResultDetail;  //取引結果詳細
        printCnt = 0; //印刷回数
        oldAggregateOrder = 0;    //集計印刷順
        this.encryptType = encryptType;    //暗号化パターン
        //取消可否  nanacoは取消なし
        transId = result.sid;   //決済ID
        if (AppPreference.isPosTransaction()) {
            merchantName = AppPreference.getPosMerchantName();   //加盟店名
            merchantOffice = AppPreference.getPosMerchantOffice();  //加盟店営業所名
            merchantTelnumber = AppPreference.getPosMerchantTelnumber();    //加盟店電話番号
        } else {
            merchantName = AppPreference.getMerchantName();   //加盟店名
            merchantOffice = AppPreference.getMerchantOffice();  //加盟店営業所名
            merchantTelnumber = AppPreference.getMerchantTelnumber();    //加盟店電話番号
        }
        carId = AppPreference.getMcCarId(); //号機番号（車番）
        driverId = AppPreference.getMcDriverId();    //乗務員コード
        termId = AppPreference.getMcTermId();   //機器番号
        this.termSequence = termSequence; //機器通番
        transDate = Converters.dateFormat(result.time);  //取引日時
        //cardTransNumber = Converters.stringToInteger(result.saleHistories[0].cardTransactionNo);
        termIdentId = result.termIdentId != null
                ? result.termIdentId
                : EmoneyOpeningInfo.getNanaco().termIdentId;//端末番号
        transAmount = amountParam.transAmount;   //取引金額
        transSpecifiedAmount = amountParam.transSpecifiedAmount;    //定額
        transMeterAmount = amountParam.transMeterAmount;    //メーター金額
        transAdjAmount = amountParam.transAdjAmount;    // 増減額
        transCashTogetherAmount = amountParam.transCashTogetherAmount != null
                ? amountParam.transCashTogetherAmount
                : 0;  // 現金併用額
        transOtherAmountOne = amountParam.transTicketAmount;    //チケット金額
        if (transOtherAmountOne != null && 0 < transOtherAmountOne) {
            transOtherAmountOneType = 1;   //その他金額1種別（チケット）
        } else {
            transOtherAmountOneType = 0;   //その他金額1種別（予備）
        }
        transOtherAmountTwoType = 0;   //その他金額2種別（予備）
        transOtherAmountTwo = 0;    //その他金額2（予備）
        freeCountOne = amountParam.transEigyoCount;   //フリーカウント1
        freeCountTwo = 0;   //フリーカウント2
        if (AppPreference.isDemoMode()) {
            // デモモードの場合、固定値を使用
            commonName = "H123456789012345";
        } else {
            commonName = AppPreference.getJremUniqueId();   //ユニークID
        }

        if (result.saleHistories != null) {
            cardIdMerchant = result.saleHistories[0].memberMaskNanacoNum; //カード番号（加盟店控）nanacoはマスクされていない番号がないので処理未了時もマスクされたものを保存
            cardIdCustomer = result.saleHistories[0].userMaskNanacoNum; //カード番号(お客様控え用)
            cardTransNumber = Converters.stringToInteger(result.saleHistories[0].cardTransactionNo); // カード取引通番
            nanacoSlipNumber = Converters.stringToLong(result.saleHistories[0].slipNo); // 伝票番号
            transBeforeBalance = Converters.stringToLong(result.saleHistories[0].beforeBalance); //取引前残高 saleHistoriesを確認
            transAfterBalance = Converters.stringToLong(result.saleHistories[0].afterBalance);   //取引後残高 saleHistoriesを確認
        }

        transCompleteAmount = amountParam.transCompleteAmount;  //支払済み金額

        if (AppPreference.isPosTransaction()) {
            transactionTerminalType = AppPreference.TerminalType.Pos.ordinal();
        } else if (AppPreference.isTicketTransaction()) {
            transactionTerminalType = AppPreference.TerminalType.Ticket.ordinal();
        } else {
            transactionTerminalType = AppPreference.TerminalType.Taxi.ordinal();
        }

        //ADD-S BMT S.Oyama 2024/09/30 フタバ双方向向け改修
        cat_dual_type           = 0;                         //0:CAT型、1:DUAL型
        card_seq_no             = "";                        //カードシーケンス番号
        atc                     = "";                        //ATC
        rw_id                   = "";                        //RWID
        sprw_id                 = "";                        //SPRWID
        off_on_type             = "";                        //0:オフライン、1:オンライン
        card_type               = "";                        //カード区分
        card_id                 = "";                        //カードID
        point_yuko_msg          = "";                        //ポイント利用不可時のメッセージ
        point_marchant          = 0;                         //加盟店ポイント
        point_total             = "";                        //累計ポイント
        point_exp_date          = "";                        //ポイント有効期限
        point_exp               = 0;                         //期限ポイント
        waon_trans_type_code    = 0;                         //WAON取引種別コード
        card_slip_no            = 0;                         //カード通番
        lid                     = 0;                         //端末シリアル番号
        service_name            = "";                        //サービス名
        card_trans_number_str   = "";                        //取引通番
        pay_id                  = 0;                         //支払ID
        ic_no                   = "";                        //IC通番
        old_ic_no               = "";                        //元IC通番
        terminal_no             = termIdentId;                                                      //端末番号
        terminal_seq_no         = String.valueOf(this.termSequence);                                //端末通番
        uniqueId                = commonName;                                                       //ユニークID
        terminal_id             = "";                        //上位端末ID
        edy_seq_no              = "";                        //Edy取引通番
        input_kingaku           = 0;                         //入力金額
        //ADD-E BMT S.Oyama 2024/09/30 フタバ双方向向け改修

        //ADD-S BMT S.Oyama 2024/11/29 フタバ双方向向け改修
        prepaidAddPoint               = 0;                   //プリペイドポイント付与
        prepaidTotalPoint             = 0;                   //プリペイドポイント残高
        prepaidNextExpired            = "";                  //次回ポイント失効日
        prepaidNextExpiredPoint       = 0;                   //次回ポイント失効ポイント
        //ADD-E BMT S.Oyama 2024/11/29 フタバ双方向向け改修

        updateCancelFlg();
        setSendCancelPurchasedTicket();
    }

    /**
     * okica印刷データ
     */
    public SlipData(EMoneyOkicaViewModel.TransactionData result, int encryptType, int termSequence, ResultParam resultParam, RefundParam refundParam, AmountParam amountParam) {
        transBrand = MainApplication.getInstance().getString(R.string.money_brand_okica);   //ブランド名
        transType = resultParam.transType;    //取引種別
        transTypeCode = "0";
        if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D_MANUAL) && resultParam.transResult == TransMap.RESULT_SUCCESS) {
            // 手動決済モードで正常終了の場合
            transTypeCode = transType == TransMap.TYPE_SALES ? MANUALMODE_TRANS_TYPE_CODE_SALES : MANUALMODE_TRANS_TYPE_CODE_CANCEL;
        }
        transResult = resultParam.transResult;    //取引結果
        transResultDetail = resultParam.transResultDetail;  //取引結果詳細
        printCnt = 0; //印刷回数
        oldAggregateOrder = 0;    //集計印刷順
        this.encryptType = encryptType;    //暗号化パターン
        if (transType == TransMap.TYPE_SALES && transResult == TransMap.RESULT_SUCCESS) {
            cancelFlg = 1;  //取消可否 売上かつ成功の場合のみ
        }
        transId = result.getIDi().toString();   //IDi(バイト文字列)
        if (AppPreference.isPosTransaction()) {
            merchantName = AppPreference.getPosMerchantName();   //加盟店名
            merchantOffice = AppPreference.getPosMerchantOffice();  //加盟店営業所名
            merchantTelnumber = AppPreference.getPosMerchantTelnumber();    //加盟店電話番号
        } else {
            merchantName = AppPreference.getMerchantName();   //加盟店名
            merchantOffice = AppPreference.getMerchantOffice();  //加盟店営業所名
            merchantTelnumber = AppPreference.getMerchantTelnumber();    //加盟店電話番号
        }
        carId = AppPreference.getMcCarId(); //号機番号（車番）
        driverId = AppPreference.getMcDriverId();    //乗務員コード
        termId = AppPreference.getMcTermId();   //機器番号
        this.termSequence = termSequence; //機器通番
        transDate = result.getTransactionDate();  //取引日時
        cardIdMerchant = Converters.tenantMaskOkica(result.getIDi().getCardNo());   //カード番号（加盟店控）処理未了時は同様
        cardIdCustomer = Converters.tenantMaskOkica(result.getIDi().getCardNo());   //カード番号（お客様控）
        slipNumber = termSequence;  //伝票番号
        oldSlipNumber = refundParam.oldSlipNumber; //元伝票番号

        //OKICA端末番号
        if (AppPreference.isDemoMode()) {
            // デモモードの場合、固定値を使用
            termIdentId = "12345678901234567";
        } else {
            termIdentId = AppPreference.getOkicaTerminalInfo() != null
                    ? AppPreference.getOkicaTerminalInfo().terminalId
                    : null;
        }

        transAmount = amountParam.transAmount;   //取引金額
        transSpecifiedAmount = amountParam.transSpecifiedAmount;    //定額
        transMeterAmount = amountParam.transMeterAmount;    //メーター金額
        transAdjAmount = amountParam.transAdjAmount;    // 増減額
        transOtherAmountOne = amountParam.transTicketAmount;    //チケット金額
        if (transOtherAmountOne != null && 0 < transOtherAmountOne) {
            transOtherAmountOneType = 1;   //その他金額1種別（チケット）
        } else {
            transOtherAmountOneType = 0;   //その他金額1種別（予備）
        }
        transOtherAmountTwoType = 0;   //その他金額2種別（予備）
        transOtherAmountTwo = 0;    //その他金額2（予備）
        freeCountOne = amountParam.transEigyoCount;   //フリーカウント1
        freeCountTwo = 0;   //フリーカウント2
        transCashTogetherAmount = amountParam.transCashTogetherAmount != null
                ? amountParam.transCashTogetherAmount
                : 0;  // 現金併用額
        transBeforeBalance = (long) result.getReadData().getAccessControlInfo().getPurseAmount(); //取引前残高
        if (transType == 0 || transType == 1) {
            // 支払または支払取消時
            transAfterBalance = (long) result.getWriteData().getAccessControlInfo().getPurseAmount(); //取引後残高
        } else if (transType == 4) {
            // チャージ時
            transAfterBalance = (long) result.getWriteChargeData().getAccessControlInfo().getPurseAmount(); //取引後残高
        }

        transCompleteAmount = amountParam.transCompleteAmount;  //支払済み金額

        if (AppPreference.isPosTransaction()) {
            transactionTerminalType = AppPreference.TerminalType.Pos.ordinal();
        } else if (AppPreference.isTicketTransaction()) {
            transactionTerminalType = AppPreference.TerminalType.Ticket.ordinal();
        } else {
            transactionTerminalType = AppPreference.TerminalType.Taxi.ordinal();
        }

        //ADD-S BMT S.Oyama 2024/09/30 フタバ双方向向け改修
        cat_dual_type           = 0;                         //0:CAT型、1:DUAL型
        card_seq_no             = "";                        //カードシーケンス番号
        atc                     = "";                        //ATC
        rw_id                   = "";                        //RWID
        sprw_id                 = "";                        //SPRWID
        off_on_type             = "";                        //0:オフライン、1:オンライン
        card_type               = "";                        //カード区分
        card_id                 = "";                        //カードID
        point_yuko_msg          = "";                        //ポイント利用不可時のメッセージ
        point_marchant          = 0;                         //加盟店ポイント
        point_total             = "";                        //累計ポイント
        point_exp_date          = "";                        //ポイント有効期限
        point_exp               = 0;                         //期限ポイント
        waon_trans_type_code    = 0;                         //WAON取引種別コード
        card_slip_no            = 0;                         //カード通番
        lid                     = 0;                         //端末シリアル番号
        service_name            = "";                        //サービス名
        card_trans_number_str   = "";                        //取引通番
        pay_id                  = 0;                         //支払ID
        ic_no                   = "";                        //IC通番
        old_ic_no               = "";                        //元IC通番
        terminal_no             = "";                        //端末番号
        terminal_seq_no         = "";                        //端末通番
        uniqueId                = "";                        //ユニークID
        terminal_id             = "";                        //上位端末ID
        edy_seq_no              = "";                        //Edy取引通番
        input_kingaku           = 0;                         //入力金額
        //ADD-E BMT S.Oyama 2024/09/30 フタバ双方向向け改修

        //ADD-S BMT S.Oyama 2024/11/29 フタバ双方向向け改修
        prepaidAddPoint               = 0;                   //プリペイドポイント付与
        prepaidTotalPoint             = 0;                   //プリペイドポイント残高
        prepaidNextExpired            = "";                  //次回ポイント失効日
        prepaidNextExpiredPoint       = 0;                   //次回ポイント失効ポイント
        //ADD-E BMT S.Oyama 2024/11/29 フタバ双方向向け改修

        updateCancelFlg();
        setSendCancelPurchasedTicket();
    }

    /**
     * QR印刷データ
     */
    public SlipData(QRSettlement.ResultSummary result, int encryptType, int termSequence, ResultParam resultParam, AmountParam amountParam) {
        transBrand = MainApplication.getInstance().getString(R.string.money_brand_codetrans);  //ブランド名
        transType = resultParam.transType;  //取引種別
        transTypeCode = "0";
        if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D_MANUAL) && resultParam.transResult == TransMap.RESULT_SUCCESS) {
            // 手動決済モードで正常終了の場合
            transTypeCode = transType == TransMap.TYPE_SALES ? MANUALMODE_TRANS_TYPE_CODE_SALES : MANUALMODE_TRANS_TYPE_CODE_CANCEL;
        }
        transResult = resultParam.transResult;  //取引結果
        transResultDetail = resultParam.transResultDetail;  //取引結果詳細
        printCnt = 0; //印刷回数
        oldAggregateOrder = 0;    //集計印刷順
        this.encryptType = encryptType;    //暗号化パターン
        //CHG-S BMT S.Oyama 2024/09/24 フタバ双方向向け改修
        //if (!IFBoxAppModels.isMatch(IFBoxAppModels.YAZAKI_LT27_D) && !IFBoxAppModels.isMatch(IFBoxAppModels.OKABE_MS70_D)) {
            // ヤザキ双方向と岡部双方向のQR以外は取消可能
        if (!IFBoxAppModels.isMatch(IFBoxAppModels.YAZAKI_LT27_D) && !IFBoxAppModels.isMatch(IFBoxAppModels.OKABE_MS70_D) && !IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D)  && !IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D_MANUAL) == true) {
            // ヤザキ双方向と岡部双方向とフタバ双方向のQR以外は取消可能
        //CHG-E BMT S.Oyama 2024/09/24 フタバ双方向向け改修
            if (transType == TransMap.TYPE_SALES && transResult == TransMap.RESULT_SUCCESS) {
                cancelFlg = 1;  //取消可否 売上かつ成功の場合のみ
            }
        }
        if (AppPreference.isPosTransaction()) {
            merchantName = AppPreference.getPosMerchantName();   //加盟店名
            merchantOffice = AppPreference.getPosMerchantOffice();  //加盟店営業所名
            merchantTelnumber = AppPreference.getPosMerchantTelnumber();    //加盟店電話番号
        } else {
            merchantName = AppPreference.getMerchantName();   //加盟店名
            merchantOffice = AppPreference.getMerchantOffice();  //加盟店営業所名
            merchantTelnumber = AppPreference.getMerchantTelnumber();    //加盟店電話番号
        }
        carId = AppPreference.getMcCarId(); //号機番号（車番）
        driverId = AppPreference.getMcDriverId();    //乗務員コード
        termId = AppPreference.getMcTermId();   //機器番号
        this.termSequence = termSequence; //機器通番
        transDate = result.payTime != null ? result.payTime : "";  //取引日時
        transAmount = amountParam.transAmount;  //取引金額
        transSpecifiedAmount = amountParam.transSpecifiedAmount;    //定額
        transMeterAmount = amountParam.transMeterAmount;    //メーター金額
        transAdjAmount = amountParam.transAdjAmount;    // 増減額
        transCashTogetherAmount = amountParam.transCashTogetherAmount != null
                ? amountParam.transCashTogetherAmount
                : 0;  // 現金併用額
        transOtherAmountOne = amountParam.transTicketAmount;    //チケット金額
        if (transOtherAmountOne != null && 0 < transOtherAmountOne) {
            transOtherAmountOneType = 1;   //その他金額1種別（チケット）
        } else {
            transOtherAmountOneType = 0;   //その他金額1種別（予備）
        }
        transOtherAmountTwoType = 0;   //その他金額2種別（予備）
        transOtherAmountTwo = 0;    //その他金額2（予備）
        freeCountOne = amountParam.transEigyoCount;   //フリーカウント1
        freeCountTwo = 0;   //フリーカウント2

        transBeforeBalance = null;  // 取引前残高
        transAfterBalance = Converters.stringToLong(result.balanceAmount);   // 取引後残高

        if (result.refundId != null) {
            codetransOrderId = result.refundId;   // 伝票番号
        } else {
            codetransOrderId = result.orderId;    // 伝票番号
        }
        codetransPayTypeName = QRPayTypeNameMap.get(result.payType); // 決済種別名称

        transCompleteAmount = amountParam.transCompleteAmount;  //支払済み金額

        if (AppPreference.isPosTransaction()) {
            transactionTerminalType = AppPreference.TerminalType.Pos.ordinal();
        } else if (AppPreference.isTicketTransaction()) {
            transactionTerminalType = AppPreference.TerminalType.Ticket.ordinal();
        } else {
            transactionTerminalType = AppPreference.TerminalType.Taxi.ordinal();
        }

        //ADD-S BMT S.Oyama 2024/10/08 フタバ双方向向け改修
        int tmpPayType = 0;
        //ADD-S BMT S.Oyama 2025/0317 フタバ双方向向け改修
        if (result.payType != null) {
        //ADD-E BMT S.Oyama 2025/0317 フタバ双方向向け改修
            switch (result.payType) {
                case QRPayTypeCodes.Wechat:         //Wechat
                    tmpPayType = 2;
                    break;
                case QRPayTypeCodes.Alipay:         //Alipay
                    tmpPayType = 1;
                    break;
                case QRPayTypeCodes.Docomo:         //d 払い
                    tmpPayType = 11;
                    break;
                case QRPayTypeCodes.auPAY:         //au PAY
                    tmpPayType = 16;
                    break;
                case QRPayTypeCodes.PayPay:         //PayPay
                    tmpPayType = 10;
                    break;
                case QRPayTypeCodes.LINEPay:         //LINE Pay
                    tmpPayType = 4;
                    break;
                case QRPayTypeCodes.RakutenPay:         //楽天ペイ
                    tmpPayType = 9;
                    break;
                case QRPayTypeCodes.GinkoPay:         //銀行 Pay
                    tmpPayType = 17;
                    break;
                case QRPayTypeCodes.merpay:         //メルペイ
                    tmpPayType = 15;
                    break;
                case QRPayTypeCodes.QUOPay:         //QUO カード Pay
                    tmpPayType = 8;
                    break;
                case QRPayTypeCodes.AlipayPlus:         //Alipay＋
                    if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == true || IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D_MANUAL) == true) {
                        tmpPayType = 3; //フタバD
                    } else {
                        tmpPayType = 18;// フタバD以外
                    }
                    break;
                case QRPayTypeCodes.AEONPay:         //AEONPay
                    tmpPayType = 7;
                    break;
                case QRPayTypeCodes.JCoinPay:         //JCoinPay
                    tmpPayType = 6;
                    break;
            }
        //ADD-S BMT S.Oyama 2025/0317 フタバ双方向向け改修
        }
        //ADD-E BMT S.Oyama 2025/0317 フタバ双方向向け改修
        //ADD-E BMT S.Oyama 2024/10/08 フタバ双方向向け改修

        //ADD-S BMT S.Oyama 2024/09/30 フタバ双方向向け改修
        cat_dual_type           = 0;                         //0:CAT型、1:DUAL型
        card_seq_no             = "";                        //カードシーケンス番号
        atc                     = "";                        //ATC
        rw_id                   = "";                        //RWID
        sprw_id                 = "";                        //SPRWID
        off_on_type             = "";                        //0:オフライン、1:オンライン
        card_type               = "";                        //カード区分
        card_id                 = "";                        //カードID
        point_yuko_msg          = "";                        //ポイント利用不可時のメッセージ
        point_marchant          = 0;                         //加盟店ポイント
        point_total             = "";                        //累計ポイント
        point_exp_date          = "";                        //ポイント有効期限
        point_exp               = 0;                         //期限ポイント
        waon_trans_type_code    = 0;                         //WAON取引種別コード
        card_slip_no            = 0;                         //カード通番
        lid                     = 0;                         //端末シリアル番号
        service_name            = "";                        //サービス名
        card_trans_number_str   = codetransOrderId;                                                 //取引通番
        pay_id                  = tmpPayType;                                                       //支払ID
        ic_no                   = "";                        //IC通番
        old_ic_no               = "";                        //元IC通番
        terminal_no             = "";                        //端末番号
        terminal_seq_no         = "";                        //端末通番
        uniqueId                = "";                        //ユニークID
        terminal_id             = "";                        //上位端末ID
        edy_seq_no              = "";                        //Edy取引通番
        input_kingaku           = 0;                         //入力金額
        //ADD-E BMT S.Oyama 2024/09/30 フタバ双方向向け改修

        //ADD-S BMT S.Oyama 2024/11/29 フタバ双方向向け改修
        prepaidAddPoint               = 0;                   //プリペイドポイント付与
        prepaidTotalPoint             = 0;                   //プリペイドポイント残高
        prepaidNextExpired            = "";                  //次回ポイント失効日
        prepaidNextExpiredPoint       = 0;                   //次回ポイント失効ポイント
        //ADD-E BMT S.Oyama 2024/11/29 フタバ双方向向け改修

        updateCancelFlg();
        setSendCancelPurchasedTicket();
    }

    /**
     * 現金決済データ
     */
    public SlipData(String payTime , int termSequence, ResultParam resultParam, RefundParam refundParam, AmountParam amountParam, Boolean isFixedAmountPostalOrder) {
        if (isFixedAmountPostalOrder) {
            transBrand = MainApplication.getInstance().getString(R.string.money_brand_postal_order);  //ブランド名　郵便小為替
        } else {
            transBrand = MainApplication.getInstance().getString(R.string.money_brand_cash);  //ブランド名  現金
        }
        transType = resultParam.transType;    //取引種別
        transResult = resultParam.transResult;    //取引結果
        transResultDetail = resultParam.transResultDetail;  //取引結果詳細
        printCnt = 0; //印刷回数
        oldAggregateOrder = 0;    //集計印刷順
        this.encryptType = -1;    //暗号化パターン
        if (transType == TransMap.TYPE_SALES && transResult == TransMap.RESULT_SUCCESS) {
            cancelFlg = 1;  //取消可否 売上かつ成功の場合のみ
        }
        merchantName = AppPreference.getMerchantName();   //加盟店名
        merchantOffice = AppPreference.getMerchantOffice();  //加盟店営業所名
        merchantTelnumber = AppPreference.getMerchantTelnumber();    //加盟店電話番号
        carId = AppPreference.getMcCarId(); //号機番号（車番）
        driverId = AppPreference.getMcDriverId();    //乗務員コード
        termId = AppPreference.getMcTermId();   //機器番号
        this.termSequence = termSequence; //機器通番
        transDate = payTime;  //取引日時
        transAmount = amountParam.transAmount;  //取引金額
        slipNumber = termSequence;  //伝票番号 使用されないが仮
        oldSlipNumber = refundParam.oldSlipNumber; //元伝票番号
        transSpecifiedAmount = amountParam.transSpecifiedAmount;    //定額
        transMeterAmount = amountParam.transMeterAmount;    //メーター金額
        transAdjAmount = amountParam.transAdjAmount;    // 増減額
        transCashTogetherAmount = amountParam.transCashTogetherAmount != null
                ? amountParam.transCashTogetherAmount
                : 0;  // 現金併用額
        transOtherAmountOne = amountParam.transTicketAmount;    //チケット金額
        if (transOtherAmountOne != null && 0 < transOtherAmountOne) {
            transOtherAmountOneType = 1;   //その他金額1種別（チケット）
        } else {
            transOtherAmountOneType = 0;   //その他金額1種別（予備）
        }
        transOtherAmountTwoType = 0;   //その他金額2種別（予備）
        transOtherAmountTwo = 0;    //その他金額2（予備）
        freeCountOne = amountParam.transEigyoCount;   //フリーカウント1
        freeCountTwo = 0;   //フリーカウント2
        transCompleteAmount = amountParam.transCompleteAmount;  //支払済み金額

        if (AppPreference.isPosTransaction()) {
            transactionTerminalType = AppPreference.TerminalType.Pos.ordinal();
        } else if (AppPreference.isTicketTransaction()) {
            transactionTerminalType = AppPreference.TerminalType.Ticket.ordinal();
        } else {
            transactionTerminalType = AppPreference.TerminalType.Taxi.ordinal();
        }

        //ADD-S BMT S.Oyama 2024/09/30 フタバ双方向向け改修
        cat_dual_type           = 0;                         //0:CAT型、1:DUAL型
        card_seq_no             = "";                        //カードシーケンス番号
        atc                     = "";                        //ATC
        rw_id                   = "";                        //RWID
        sprw_id                 = "";                        //SPRWID
        off_on_type             = "";                        //0:オフライン、1:オンライン
        card_type               = "";                        //カード区分
        card_id                 = "";                        //カードID
        point_yuko_msg          = "";                        //ポイント利用不可時のメッセージ
        point_marchant          = 0;                         //加盟店ポイント
        point_total             = "";                        //累計ポイント
        point_exp_date          = "";                        //ポイント有効期限
        point_exp               = 0;                         //期限ポイント
        waon_trans_type_code    = 0;                         //WAON取引種別コード
        card_slip_no            = 0;                         //カード通番
        lid                     = 0;                         //端末シリアル番号
        service_name            = "";                        //サービス名
        card_trans_number_str   = "";                        //取引通番
        pay_id                  = 0;                         //支払ID
        ic_no                   = "";                        //IC通番
        old_ic_no               = "";                        //元IC通番
        terminal_no             = "";                        //端末番号
        terminal_seq_no         = "";                        //端末通番
        uniqueId                = "";                        //ユニークID
        terminal_id             = "";                        //上位端末ID
        edy_seq_no              = "";                        //Edy取引通番
        input_kingaku           = 0;                         //入力金額
        //ADD-E BMT S.Oyama 2024/09/30 フタバ双方向向け改修

        //ADD-S BMT S.Oyama 2024/11/29 フタバ双方向向け改修
        prepaidAddPoint               = 0;                   //プリペイドポイント付与
        prepaidTotalPoint             = 0;                   //プリペイドポイント残高
        prepaidNextExpired            = "";                  //次回ポイント失効日
        prepaidNextExpiredPoint       = 0;                   //次回ポイント失効ポイント
        //ADD-E BMT S.Oyama 2024/11/29 フタバ双方向向け改修

        updateCancelFlg();
        setSendCancelPurchasedTicket();
    }

    private void updateCancelFlg() {
        if (transType == TransMap.TYPE_CANCEL) {
            // 取消の伝票データ生成時
            if (transResult == TransMap.RESULT_SUCCESS || transResult == TransMap.RESULT_UNFINISHED) {
                // 取引結果が成功または未了時、取消対象取引を取消不可にする
                int slipId = AppPreference.getCancelTargetSlipId();
                //Timber.d("updateCancelFlg:%s",slipId);
                Thread thread = new Thread(() -> {
                    DBManager.getSlipDao().updateCancelUriId(slipId);
                });
                thread.start();
            }
        }
    }

    private void setSendCancelPurchasedTicket() {
        sendCancelPurchasedTicket = 0;
        if (transactionTerminalType == AppPreference.TerminalType.Ticket.ordinal()
                && transType == TransMap.TYPE_CANCEL
                && transResult == TransMap.RESULT_SUCCESS) {
            // チケット購入の決済取消が成功した場合、チケット購入の取消を送信必要なため、値（1）をセット
            sendCancelPurchasedTicket = 1;
        } else if (transactionTerminalType == AppPreference.TerminalType.Ticket.ordinal()
                && transType == TransMap.TYPE_CANCEL
                && transResult == TransMap.RESULT_UNFINISHED) {
            // チケット購入の決済取消時に処理未了が発生した場合、チケット購入の取消を送信必要なため、値（1）をセット
            sendCancelPurchasedTicket = 1;
        }
    }

    //ADD-S BMT S.Oyama 2025/02/10 フタバ双方向向け改修
    /******************************************************************************/
    /*!
     * @brief  820への送信処理：決済取り消し情報発砲向け空電文(フタバD)
     * @note　 820への送信処理：決済取り消し情報発砲向け空電文
     * @param なし
     * @retval なし
     * @return　なし
     * @private
     */

    /******************************************************************************/
    public SlipData(String payTime) {
        transBrand                      = "xxx";  //ブランド名
        transType                       = TransMap.TYPE_SALES;      //取引種別
        transResult                     = TransMap.RESULT_SUCCESS;  //取引結果
        transResultDetail               = TransMap.DETAIL_NORMAL;   //取引結果詳細
        printCnt                        = 0; //印刷回数
        oldAggregateOrder               = 0;    //集計印刷順
        this.encryptType                = -1;    //暗号化パターン
        cancelFlg                       = 1;  //取消可否 売上かつ成功の場合のみ
        merchantName                    = AppPreference.getMerchantName();   //加盟店名
        merchantOffice                  = AppPreference.getMerchantOffice();  //加盟店営業所名
        merchantTelnumber               = AppPreference.getMerchantTelnumber();    //加盟店電話番号
        carId                           = AppPreference.getMcCarId(); //号機番号（車番）
        driverId                        = AppPreference.getMcDriverId();    //乗務員コード
        termId                          = AppPreference.getMcTermId();   //機器番号
        termIdentId                     = "";                       //端末番号
        this.termSequence               = 0; //機器通番
        transDate                       = payTime;  //取引日時
        transAmount                     = 0;  //取引金額
        slipNumber                      = termSequence;  //伝票番号 使用されないが仮
        oldSlipNumber                   = 0; //元伝票番号
        commodityCode                   = "";
        installment                     = "一括"; //分割回数
        transSpecifiedAmount            = 0;    //定額
        transMeterAmount                = 0;    //メーター金額
        transAdjAmount                  = 0;    // 増減額
        transCashTogetherAmount         = 0;    // 現金併用額
        transOtherAmountOne             = 0;    //チケット金額
        transOtherAmountOneType         = 0;    //その他金額1種別（予備）
        transOtherAmountTwoType         = 0;    //その他金額2種別（予備）
        transOtherAmountTwo             = 0;    //その他金額2（予備）
        freeCountOne                    = 0;    //フリーカウント1
        freeCountTwo                    = 0;    //フリーカウント2
        transCompleteAmount             = 0;    //支払済み金額
        cardExpDate                     = "";   //カード有効期限
        transBeforeBalance              = Long.valueOf(0);  // 取引前残高
        transAfterBalance               = Long.valueOf(0);  // 取引後残高
        cardIdMerchant                  = ""; //カード番号（加盟店控）nanacoはマスクされていない番号がないので処理未了時もマスクされたものを保存
        cardIdCustomer                  = ""; //カード番号(お客様控え用)


        transactionTerminalType         = AppPreference.TerminalType.Taxi.ordinal();

        //ADD-S BMT S.Oyama 2024/09/30 フタバ双方向向け改修
        card_exp_date_merchant          = "";                                                      //カード有効期限（加盟店控）
        card_exp_date_card_company      = "";                                                   //カード有効期限（カード会社控）

        cat_dual_type           = 0;                         //0:CAT型、1:DUAL型
        card_seq_no             = "";                        //カードシーケンス番号
        atc                     = "";                        //ATC
        rw_id                   = "";                        //RWID
        sprw_id                 = "";                        //SPRWID
        off_on_type             = "";                        //0:オフライン、1:オンライン
        card_type               = "";                        //カード区分
        card_id                 = "";                        //カードID
        point_yuko_msg          = "";                        //ポイント利用不可時のメッセージ
        point_marchant          = 0;                         //加盟店ポイント
        point_total             = "";                        //累計ポイント
        point_exp_date          = "";                        //ポイント有効期限
        point_exp               = 0;                         //期限ポイント
        waon_trans_type_code    = 0;                         //WAON取引種別コード
        card_slip_no            = 0;                         //カード通番
        lid                     = 0;                         //端末シリアル番号
        service_name            = "";                        //サービス名
        card_trans_number_str   = "";                        //取引通番
        pay_id                  = 0;                         //支払ID
        ic_no                   = "";                        //IC通番
        old_ic_no               = "";                        //元IC通番
        terminal_no             = "";                        //端末番号
        terminal_seq_no         = "";                        //端末通番
        uniqueId                = "";                        //ユニークID
        terminal_id             = "";                        //上位端末ID
        edy_seq_no              = "";                        //Edy取引通番
        input_kingaku           = 0;                         //入力金額
        //ADD-E BMT S.Oyama 2024/09/30 フタバ双方向向け改修

        //ADD-S BMT S.Oyama 2024/11/29 フタバ双方向向け改修
        prepaidAddPoint               = 0;                   //プリペイドポイント付与
        prepaidTotalPoint             = 0;                   //プリペイドポイント残高
        prepaidNextExpired            = "";                  //次回ポイント失効日
        prepaidNextExpiredPoint       = 0;                   //次回ポイント失効ポイント
        //ADD-E BMT S.Oyama 2024/11/29 フタバ双方向向け改修
    }
    //ADD-E BMT S.Oyama 2025/02/10 フタバ双方向向け改修

}
