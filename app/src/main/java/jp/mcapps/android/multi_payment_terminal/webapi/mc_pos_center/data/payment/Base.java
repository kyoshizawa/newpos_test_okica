package jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.payment;

import com.google.gson.annotations.Expose;

import jp.mcapps.android.multi_payment_terminal.database.history.uri.UriData;

public class Base {
    @Expose
    public String terminalNo; //端末番号

    @Expose
    public Integer carNo;   //係員(乗務員)が入力した号機番号（車番）

    @Expose
    public Integer driverCd;    //係員(乗務員)が入力した係員番号

    @Expose
    public String payDateTime;  //操作を行った日時。YYYYMMDDHHMMSS。

    @Expose
    public String cancelDateTime;  //取消元の操作を行った日時。YYYYMMDDHHMMSS。取消でない場合はnull

    @Expose
    public Integer payType; // 0：売上、1：取消

    @Expose
    public Integer payResult;   //決済操作の成否を示す。引かれたかひかれていないか。 0：成功、1：失敗、2：未了

    @Expose
    public Integer payResultReason; //取引結果の詳細コード。

    @Expose
    public Integer procNo;  //レシート番号

    @Expose
    public Integer cancelProcNo;    //取消元のレシート番号。取消でない場合はnull

    @Expose
    public String maskedMemberNo;   //先頭6桁、末尾4桁以外をマスクした会員番号

    @Expose
    public String payMethod;    //"10"：一括 固定

    @Expose
    public Integer fare;    //取引された金額。必ず設定される。

    @Expose
    public Integer specifiedFare;   //手入力の金額。補足情報。使わない場合は0。

    @Expose
    public Integer meterFare;   //メーターから連動された金額。補足情報。使わない場合は0。

    @Expose
    public Integer adjFare; //手入力の増減額。補足情報。使わない場合は0。

    @Expose
    public Integer cashTogetherFare;    //現金併用にて、現金払いを含めた取引の総額。補足情報。使わない場合は0。

    @Expose
    public Integer otherFare1;  //予備1。使わない場合は0。

    @Expose
    public Integer otherFare2;  //予備2。使わない場合は0。

    @Expose
    public Long beforeBalance;   //取引前の残高。電マネで使用。使わない場合は0。取れなかった場合はnull

    @Expose
    public Long afterBalance;    //取引後の残高。電マネで使用。使わない場合は0。取れなかった場合はnull

    @Expose
    public Integer processingTime;  //タッチから完了までの時間。ミリ秒。電マネでない場合は0。

    @Expose
    public String locationLat;  //緯度。世界測地(1/1000分)

    @Expose
    public String locationLng;  //経度。世界測地(1/1000分)

    @Expose
    public Integer antenna; //アンテナレベル。0～4

    @Expose
    public String networkType;  //"3G"、"LTE"、"Wifi"

    @Expose
    public Integer pinEntryTime;    //PIN画面表示から入力完了までの時間。ミリ秒。PIN入力を伴わない場合は0。

    @Expose
    public String transId;  //電子マネーの決済ID。電子マネーでないときはnull。

    @Expose
    public String cancelTransId;    //電子マネーの取消元決済ID。電子マネーでない、または、取消でないときはnull。

    @Expose
    public String commonName;   //クライアント証明書のクライアント毎に付与されたID。存在しない場合は null。

    public Base(UriData data) {

        terminalNo = data.termId;
        carNo = data.carId;
        driverCd = data.driverId;
        payDateTime = data.transDate.replaceAll("[/: ]", "");
        cancelDateTime = data.oldTransDate != null ? data.oldTransDate.replaceAll("[/: ]", "") : null;
        payType = data.transType;
        payResult = data.transResult;
        payResultReason = data.transResultDetail;
        procNo = data.termSequence;
        cancelProcNo = data.oldTermSequence;
        maskedMemberNo = data.cardId;
        payMethod = data.installment;
        fare = data.transAmount;
        specifiedFare = data.transSpecifiedAmount != null ? data.transSpecifiedAmount : 0;
        meterFare = data.transMeterAmount != null ? data.transMeterAmount : 0;
        adjFare = data.transAdjAmount != null ? data.transAdjAmount : 0;
        cashTogetherFare = data.transCashTogetherAmount != null ? data.transCashTogetherAmount : 0;
        otherFare1 = data.transOtherAmountOne != null ? data.transOtherAmountOne : 0;
        otherFare2 = data.transOtherAmountTwo != null ? data.transOtherAmountTwo : 0;
        beforeBalance = data.transBeforeBalance;
        afterBalance = data.transAfterBalance;
        processingTime = data.transTime != null ? data.transTime : 0;
        locationLat = data.termLatitude;
        locationLng = data.termLongitude;
        antenna = data.termRadioLevel;
        networkType = data.termNetworkType;
        pinEntryTime = data.transInputPinTime != null ? data.transInputPinTime : 0;
        transId = data.transId;
        cancelTransId = data.oldTransId;
        commonName = data.commonName;
    }
}
