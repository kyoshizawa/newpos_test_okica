package jp.mcapps.android.multi_payment_terminal.thread.credit;

/**
 * Created by zhouqiang on 2017/3/14.
 * @author zhouqiang
 * card detail information
 */

public class CardInfo {

    private boolean resultFalg ;

    /** card info */
    private CardType cardType ;
    private byte[] cardAtr ;
    private String[] trackNo ;
    private int nfcType ;

    /**
     * failed information
     */
    private int errno ;

    public CardInfo(){}

    public CardType getCardType() {
        return cardType;
    }

    public void setCardType(CardType cardType) {
        this.cardType = cardType;
    }

    public byte[] getCardAtr() {
        return cardAtr;
    }

    public void setCardAtr(byte[] cardAtr) {
        this.cardAtr = cardAtr;
    }

    public String[] getTrackNo() {
        return trackNo;
    }

    public void setTrackNo(String[] trackNo) {
        this.trackNo = trackNo;
    }

    public int getNfcType() {
        return nfcType;
    }

    public void setNfcType(int nfcType) {
        this.nfcType = nfcType;
    }

    public boolean isResultFalg() {
        return resultFalg;
    }

    public void setResultFalg(boolean resultFalg) {
        this.resultFalg = resultFalg;
    }

    public int getErrno() {
        return errno;
    }

    public void setErrno(int errno) {
        this.errno = errno;
    }
}
