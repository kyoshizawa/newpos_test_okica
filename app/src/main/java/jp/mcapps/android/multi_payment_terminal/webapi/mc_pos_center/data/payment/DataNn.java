package jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.payment;

import com.google.gson.annotations.Expose;

import jp.mcapps.android.multi_payment_terminal.database.history.uri.UriData;

public class DataNn {
    @Expose
    public Long slipNo; //上位端末通番

    @Expose
    public Integer errCd;   //異常時に設定、正常時はnull

    @Expose
    public Integer procNo;  //カード内で保持している通番

    @Expose
    public String tid;  //物販端末ID

    public DataNn(UriData data) {
        slipNo = data.nanacoSlipNumber;
        errCd = data.nanacoErrCode;
        procNo = data.nanacoCardTransNumber;
        tid = data.nanacoTermIdentId;
    }
}
