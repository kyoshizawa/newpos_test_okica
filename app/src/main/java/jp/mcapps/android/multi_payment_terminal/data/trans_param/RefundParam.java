package jp.mcapps.android.multi_payment_terminal.data.trans_param;

public class RefundParam {
    public Integer oldSlipId;
    public String oldTransDate; //元取引日時
    public Integer oldTermSequence; //元端末通番
    public String oldTransId;   //元決済ID
    public Integer oldSlipNumber;   //元伝票番号
    public Integer oldTransAmount;  //取引金額
}
