package jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.payment;

import com.google.gson.annotations.Expose;

import jp.mcapps.android.multi_payment_terminal.database.history.uri.UriData;

public class DataJr {
    @Expose
    public Integer errCd; //エラーコード。異常終了の場合のみ設定される。正常時はnull

    @Expose
    public String idm;  //Idm。R/WにかざされたICカードのIdm、端末タイムアウト(通信異常)の場合はnull

    @Expose
    public String sprwid;   //物販端末ID

    //以下　一件明細作成業務の正常終了の場合のみ設定

    @Expose
    public Integer statementId; //一件明細ID

    @Expose
    public Integer sequenceNo;  //IC取扱通番

    @Expose
    public Integer sfLogId; //SFログID

    @Expose
    public Integer oldStatementId; //旧一件明細ID

    @Expose
    public Integer oldSfLogId; //旧SFログID

    public DataJr(UriData data) {
        errCd = data.icErrCode;
        idm = data.icIdm;
        sprwid = data.icSprwid;
        statementId = data.icStatementId;
        sequenceNo = data.icSequence;
        sfLogId = data.icSflogId;
        oldStatementId = data.icOldStatementId;
        oldSfLogId = data.icOldSflogId;
    }
}
