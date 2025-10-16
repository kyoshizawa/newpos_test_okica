package jp.mcapps.android.multi_payment_terminal.ui.aggregate;

public class AggregateListItem {
    public int settlementCnt;

    public int cancelCnt;

    public int unfinishedCnt;

    public int subtotal;

    public int unfinishedSubtotal;

    public AggregateListItem() {
        settlementCnt = 0;
        cancelCnt = 0;
        unfinishedCnt = 0;
        subtotal = 0;
        unfinishedSubtotal = 0;
    }
}
