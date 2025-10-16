package jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.payment;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import jp.mcapps.android.multi_payment_terminal.database.history.uri.UriData;

public class DataEd {
    @Expose
    public Integer errCd; //エラー詳細コード、未設定の場合はnull

    @Expose
    public Long edyProcNo;    //Edy用の取引通番

    @Expose
    public Integer procNo;  //カード内で保持する通番

    @Expose
    public String htid;  //上位端末ID

    public DataEd(UriData data) {
        errCd = data.rakutenEdyErrCode;
        edyProcNo = data.rakutenEdyTransNumber;
        procNo = data.rakutenEdyCardTransNumber;
        htid = data.rakutenEdyTermIdentId;
    }
}
