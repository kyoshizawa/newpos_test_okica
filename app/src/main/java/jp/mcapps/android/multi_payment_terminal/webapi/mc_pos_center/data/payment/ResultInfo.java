package jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.payment;

import com.google.gson.annotations.Expose;

public class ResultInfo {
    @Expose
    public Boolean tranResult; //一件毎の処理結果 true：成功、false：失敗

    @Expose
    public String terminalNo;   //対象の売り上げの端末番号。

    @Expose
    public String paymentDateTime;  //対象の売り上げの取引日時。YYYYMMDDHHHMMSS

    @Expose
    public Integer procNo;  //対象の売り上げの伝票番号
}
