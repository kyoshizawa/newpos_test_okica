package jp.mcapps.android.multi_payment_terminal.data.trans_param;

public class AmountParam {
    public Integer transAmount; //取引金額
    public Integer transSpecifiedAmount;  //定額料金
    public Integer transMeterAmount;  //メーター料金
    public Integer transAdjAmount;  //増減額
    public Integer transCashTogetherAmount = 0; //現金併用金額
    public Integer transTicketAmount;   //チケット金額
    public Integer transEigyoCount;     //営業回数（双方向メーター）
    public Integer transCompleteAmount; //支払済み金額（双方向メーター）
}
