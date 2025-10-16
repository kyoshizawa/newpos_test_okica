package jp.mcapps.android.multi_payment_terminal.thread.credit;

/**
 * Created by zhouqiang on 2017/12/12.
 * @author zhouqiang
 * card type
 */
public enum CardType {
    /**
     * mag-stripe mode
     */
    INMODE_MAG(0x02),

    /**
     * insert card mode
     */
    INMODE_IC(0x08),

    /**
     * contactless card mode
     */
    INMODE_NFC(0x10);

    private int val ;

    public int getVal(){
        return val ;
    }

    private CardType(int val){
        this.val = val ;
    }
}
