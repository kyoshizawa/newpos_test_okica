package jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.payment;

import com.google.gson.annotations.Expose;

import jp.mcapps.android.multi_payment_terminal.database.history.uri.UriData;

public class DataCr {
    @Expose
    public Integer acquireId;   //クレジットアクワイアラID

    @Expose
    public Integer msIcKbn; //0：磁気、1：接触IC、2：非接触IC

    @Expose
    public Integer onOffKbn;    //オンオフ区分 1：オンライン

    @Expose
    public String chipCc;   //チップコンディションコード

    @Expose
    public Integer forcedOnline;    //強制オンライン 固定：0

    @Expose
    public Integer forcedApproval;  //強制認証 固定：0

    @Expose
    public String productCd;    //センター情報取得で取得した商品コード

    @Expose
    public String aid;  //AID

    @Expose
    public String posEntryMode; //POSエントリーモード

    @Expose
    public String panSeqNo;     //PANシーケンス番号

    @Expose
    public Integer icTerminalFlg;   //端末の対応可能な機能。1：接触IC対応、3：非接触対応(予約)

    @Expose
    public String brandSign;    //ブランドサイン

    @Expose
    public Integer keyType; //使用された鍵の種別

    @Expose
    public Integer keyVersion;  //使用された鍵のバージョン

    @Expose
    public String rsaData;  //暗号化されたICCデータ、ストライプ情報(HEX)

    public DataCr(UriData data) {

        acquireId = data.creditAcqId;
        msIcKbn = data.creditMsIc;
        onOffKbn = data.creditOnOff;
        chipCc = data.creditChipCc;
        forcedOnline = data.creditForcedOnline;
        forcedApproval = data.creditForcedApproval;
        productCd = data.creditCommodityCode;
        aid = data.creditAid;
        posEntryMode = data.creditEntryMode;
        panSeqNo = String.format("%08x", data.creditPanSequenceNumber);
        icTerminalFlg = data.creditIctermFlag;
        brandSign = data.creditBrandId;
        keyType = data.creditKeyType;
        keyVersion = data.creditKeyVer;
        rsaData = data.creditEncryptionData;
    }
}
