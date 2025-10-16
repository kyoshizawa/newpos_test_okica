package jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.payment;

import com.google.gson.annotations.Expose;

import jp.mcapps.android.multi_payment_terminal.database.history.uri.UriData;

public class DataUp {
    @Expose
    public String ginrenSendDate;   //銀聯送信日時

    @Expose
    public Integer ginrenProcNo;    //銀聯処理通番

    @Expose
    public String oldGinrenSendData;    //取消対象の銀聯送信日時

    @Expose
    public Integer oldGinrenProcNo; //取消対象の銀聯番号

    public DataUp(UriData data) {
        ginrenSendDate = data.unionpaySendDate;
        ginrenProcNo = data.unionpayProcNumber;
        oldGinrenSendData = data.oldUnionpaySendDate;
        oldGinrenProcNo = data.oldUnionpayProcNumber;
    }
}
