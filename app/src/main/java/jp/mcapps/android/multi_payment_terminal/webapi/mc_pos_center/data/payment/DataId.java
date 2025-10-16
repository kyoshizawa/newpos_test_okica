package jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.payment;

import com.google.gson.annotations.Expose;

import jp.mcapps.android.multi_payment_terminal.database.history.uri.UriData;

public class DataId {
    @Expose
    public Integer slipNo;  //取引時の伝票番号

    @Expose
    public Integer cancelSlipNo;    //取消対象の伝票番号、決済時はnull

    @Expose
    public Integer errCd;   //エラー詳細コード、未設定の場合はnull

    @Expose
    public String tid;  //物販端末ID

    @Expose
    public Integer secNo;   //オンライン処理時の報告電文にせってされているCAFIS処理通番、未設定の場合はnull

    @Expose
    public String recognitionNo;   //オンライン処理時の報告電文にセットされている承認番号、未設定の場合はnull

    public DataId(UriData data) {
        slipNo = data.idSlipNumber;
        cancelSlipNo = data.idOldSlipNumber;
        errCd = data.idErrorCode;
        tid = data.idTermIdentId;
        secNo = data.idSequenceNumber;
        recognitionNo = data.idRecognitionNumber;
    }
}
