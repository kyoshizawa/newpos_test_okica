package jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.payment;

import com.google.gson.annotations.Expose;

import jp.mcapps.android.multi_payment_terminal.database.history.uri.UriData;

public class DataWn {
    @Expose
    public Integer slipNo;  //取引時の伝票番号

    @Expose
    public Integer cancelSlipNo;    //取消対象の伝票番号、決済時はnull

    @Expose
    public Integer errCd;   //異常終了時のみ設定、正常時はnull

    @Expose
    public String idm;  //チップ製造番号、端末タイムアウト時はnull

    @Expose
    public String sprwid;  //物販端末ID

    @Expose
    public Integer cardProcNo;   //カード通番、端末タイムアウト時はnull

    @Expose
    public Long pointTradeValue;    //ポイント対象金額、端末タイムアウト時はnull

    @Expose
    public Integer pointGrantType;  //ポイント付与区分、端末タイムアウト時はnull

    @Expose
    public Long addPointTotal;  //付与されたポイントの絶対値、売上、取消、ポイントチャージで取得できれば設定、出来なければ0。それ以外の業務はnull

    @Expose
    public Long totalPoint; //現在~2年前に獲得したポイントの合計、端末タイムアウト時はnull

    public DataWn(UriData data) {
        slipNo = data.waonSlipNumber;
        cancelSlipNo = data.waonOldSlipNumber;
        errCd = data.waonErrCode;
        idm = data.waonIdm;
        sprwid = data.waonTermIdentId;
        cardProcNo = data.waonCardThroughNum;
        pointTradeValue = data.waonPointTradeValue;
        pointGrantType = data.waonPointGrantType;
        addPointTotal = data.waonAddPointTotal;
        totalPoint = data.waonTotalPoint;
    }
}
