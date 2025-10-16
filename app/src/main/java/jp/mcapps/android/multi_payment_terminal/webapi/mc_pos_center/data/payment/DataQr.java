package jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.payment;

import com.google.gson.annotations.Expose;

import jp.mcapps.android.multi_payment_terminal.database.history.uri.UriData;

public class DataQr {
    @Expose
    public String slipNo;       // 伝票番号
    @Expose
    public String cancelSlipNo; // 払戻元伝票番号
    @Expose
    public String qrService;    // 決済種別
    @Expose
    public String wallet;       // ウォレットコード Alipay+以外はnull*/

    public DataQr(UriData data) {
        slipNo = data.codetransOrderId;
        cancelSlipNo = data.codetransOldOrderId;
        qrService = data.codetransPayTypeCode;
        wallet = data.wallet;
    }
}
