package jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.payment;

import com.google.gson.annotations.Expose;

import jp.mcapps.android.multi_payment_terminal.database.history.uri.UriData;

public class DataQp {
    @Expose
    public Integer slipNo;  //伝票番号、確定前にエラー終了の場合はnull

    @Expose
    public Integer cancelSlipNo;    //取消元伝票番号、値が取得できない場合はnull

    @Expose
    public Integer errCd;   //エラー詳細コード、未設定の場合はnull

    @Expose
    public String tid;  //物販端末ID

    @Expose
    public Integer procNo;  //ICチップの取引通番

    public DataQp(UriData data) {
        slipNo = data.quicpaySlipNumber;
        cancelSlipNo = data.quicpayOldSlipNumber;
        errCd = data.quicpayErrCode;
        tid = data.quicpayTermIdentId;
        procNo = data.quicpayDealingsThroughNumber;
    }
}
