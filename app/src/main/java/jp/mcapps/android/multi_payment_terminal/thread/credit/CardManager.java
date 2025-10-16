package jp.mcapps.android.multi_payment_terminal.thread.credit;

import android.os.ConditionVariable;

import com.pos.device.SDKException;
import com.pos.device.icc.ContactCard;
import com.pos.device.icc.IccReader;
import com.pos.device.icc.IccReaderCallback;
import com.pos.device.icc.OperatorMode;
import com.pos.device.icc.SlotType;
import com.pos.device.icc.VCC;
import com.pos.device.magcard.MagCardCallback;
import com.pos.device.magcard.MagCardReader;
import com.pos.device.magcard.MagneticCard;
import com.pos.device.magcard.TrackInfo;
import com.pos.device.picc.EmvContactlessCard;
import com.pos.device.picc.PiccReader;
import com.pos.device.picc.PiccReaderCallback;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.error.CreditErrorCodes;
import timber.log.Timber;

//import com.newpos.bypay.EmvL2;
//import com.newpos.bypay.EmvL2App;
//import com.newpos.libpay.Logger;
//import com.newpos.libpay.presenter.TransView;
//import com.newpos.libpay.trans.Tcode;

/**
 * Created by zhouqiang on 2017/3/14.
 *
 * @author zhouqiang
 * card manage
 */

public class CardManager {
    private final String LOGTAG = "CardManager";

    public static final int TYPE_MAG = 1;
    public static final int TYPE_ICC = 2;
    public static final int TYPE_NFC = 3;
    private static CardManager instance;

    private static int mode;

    private CardManager() {
    }

    public static CardManager getInstance(int m) {
        mode = m;
        if (null == instance) {
            instance = new CardManager();
        }
        return instance;
    }

    private boolean isUpiCl = false;

    public boolean isUpiClAid() {
        return isUpiCl;
    }

    public void setisUpiClAid(boolean isupi) {
        isUpiCl = isupi;
    }

    public int GetMode() {
        return mode;
    }

    public void SetMode(int pmode) {
        mode = pmode;
    }

    public void PiccReset() {
        try {
            piccReader.reset();
        } catch (SDKException e) {
            e.printStackTrace();
        }
    }

    private MagCardReader magCardReader;
    private IccReader iccReader;
    private PiccReader piccReader;
    public EmvContactlessCard mEmvContactlessCard;

    private void init() {
        //Log.d("yuan", "1-->"+(mode & CardType.INMODE_MAG.getVal()));
        if ((mode & CardType.INMODE_MAG.getVal()) != 0) {
            //Log.d("yuan", "4-->");
            magCardReader = MagCardReader.getInstance();
            //Log.d("yuan", magCardReader.getClass().getName());
        }
        //Log.d("yuan", "2-->"+(mode & CardType.INMODE_IC.getVal()));
        if ((mode & CardType.INMODE_IC.getVal()) != 0) {
            iccReader = IccReader.getInstance(SlotType.USER_CARD);
        }
        //Log.d("yuan", "3-->"+(mode & CardType.INMODE_NFC.getVal()));
        if ((mode & CardType.INMODE_NFC.getVal()) != 0) {
            piccReader = PiccReader.getInstance();
        }
        isEnd = false;
        CreditSettlement.getInstance().setIcCardExistFlg(false);
    }

    private void stopMAG() {
        try {
            if (magCardReader != null) {
                magCardReader.stopSearchCard();
            }
        } catch (SDKException e) {
            e.printStackTrace();
        }
    }

    private void stopICC() {
        if (iccReader != null) {
            try {
                iccReader.stopSearchCard();
            } catch (SDKException e) {
                e.printStackTrace();
            }
        }
    }

    public void stopPICC() {
        if (piccReader != null) {
            piccReader.stopSearchCard();
            try {
                piccReader.release();
            } catch (SDKException e) {
                e.printStackTrace();
            }
        }
    }

    public void releaseAll() {
        isEnd = true;
        try {
            if (magCardReader != null) {
                magCardReader.stopSearchCard();
                //Logger.debug("mag stop");
            }
            if (iccReader != null) {
                iccReader.stopSearchCard();
                iccReader.release();
                //Logger.debug("icc stop");
            }
            if (piccReader != null) {
                piccReader.stopSearchCard();
                piccReader.release();
                //Logger.debug("picc stop");
            }
        } catch (SDKException e) {
            e.printStackTrace();
        }
    }

    private CardListener listener;
    private boolean getNfc = false;

    public void DelaySecond(final int msecond, CardListener l, int nfci, int nfci1) {
        getNfc = true;
        final int i = nfci;
        final int i1 = nfci1;
        final CardInfo info = new CardInfo();
        //      final  CountDownLatch cdt=new CountDownLatch(1);
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    Thread.sleep(msecond);//休眠3秒

                    if (!isEnd) {
                        //Logger.debug("CardManager>>getCard>>NFC>>i=" + i);
                        isEnd = true;
                        stopICC();
                        stopMAG();

                        if (0 == i) {
                            //           try {
                            //           Beeper.getInstance().beep(3000 , 500);
                            //          } catch (SDKException e) {
                            //          e.printStackTrace();
                            //          }
                            try {
                                listener.callback(handlePICC(i1));
                            } catch (SDKException e) {
                                Timber.e(e);
                            }
                        } else {
                            info.setResultFalg(false);
                            //info.setErrno(Tcode.SEARCH_CARD_FAIL);
                            listener.callback(info);
                        }
                    }

                    //             cdt.countDown();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();
        /***
         try {
         cdt.await();
         } catch (InterruptedException e) {
         e.printStackTrace();
         }
         ***/
    }

    private boolean isEnd = false;

    public void getCard(final int timeout, CardListener l) {
        init();
        final CardInfo info = new CardInfo();
        if (null == l) {
            Timber.tag(LOGTAG).d("Fail getCard arg2");
            CreditSettlement.getInstance().setCreditError(CreditErrorCodes.T99);
            info.setResultFalg(false);
            listener.callback(info);
        } else {
            this.listener = l;
            try {
                if ((mode & CardType.INMODE_MAG.getVal()) != 0) {
                    magCardReader.startSearchCard(timeout, new MagCardCallback() {
                        @Override
                        public void onSearchResult(int i, MagneticCard magneticCard) {
                            if (!isEnd) {
                                CreditSettlement.getInstance().setMSICKbn(CreditSettlement.k_MSIC_KBN_MS);
                                isEnd = true;
                                stopICC();
                                stopPICC();
                                if (0 == i) {
                                    listener.callback(handleMAG(magneticCard));
                                } else {
                                    Timber.tag(LOGTAG).d("Fail onSearchResult in MAG");
                                    if(TIMEOUT_ERROR == i) {
                                        /* 読み込み待ちタイムアウト */
                                        CreditSettlement.getInstance().setWaitCardTimeout();
                                    } else {
                                        /* 磁気カードの読み込みが不十分だった場合、再度カード読み込み待ちへ移行する為、ここでエラーは設定しない */
                                    }
                                    info.setResultFalg(false);
                                    listener.callback(info);
                                }
                            }
                        }
                    });
                }
                if ((mode & CardType.INMODE_IC.getVal()) != 0) {
                    iccReader.startSearchCard(timeout, new IccReaderCallback() {
                        @Override
                        public void onSearchResult(int i) {
                            if (!isEnd) {
                                CreditSettlement.getInstance().setMSICKbn(CreditSettlement.k_MSIC_KBN_IC);
                                isEnd = true;
                                stopMAG();
                                stopPICC();
                                if (0 == i) {
                                    try {
                                        listener.callback(handleICC());
                                    } catch (SDKException e) {
                                        Timber.tag(LOGTAG).d("Fail SDKException callback in IC");
                                        CreditSettlement.getInstance().setCreditError(CreditErrorCodes.T28);
                                        info.setResultFalg(false);
                                        listener.callback(info);
                                    }
                                } else {
                                    Timber.tag(LOGTAG).d("Fail onSearchResult in IC");
                                    if(TIMEOUT_ERROR == i) {
                                        /* 読み込み待ちタイムアウト */
                                        CreditSettlement.getInstance().setWaitCardTimeout();
                                    } else {
                                        CreditSettlement.getInstance().setCreditError(CreditErrorCodes.T03);
                                    }
                                    info.setResultFalg(false);
                                    listener.callback(info);
                                }
                            }
                        }
                    });
                }
                if ((mode & CardType.INMODE_NFC.getVal()) != 0) {
                    piccReader.startSearchCard(timeout, (i, nfcType) -> {
                        if (!isEnd) {
                            isEnd = true;
                            stopICC();
                            stopMAG();
                            if (AppPreference.isDemoMode()) {
                                // デモモード
                                stopPICC();
                            }

                            if (0 == i) {
                                try {
                                    listener.callback(handlePICC(nfcType));
                                } catch (SDKException e) {
                                    Timber.tag(LOGTAG).d("Fail SDKException callback in IC");
                                    CreditSettlement.getInstance().setCreditError(CreditErrorCodes.T28);
                                    info.setResultFalg(false);
                                    listener.callback(info);
                                }
                            } else {
                                Timber.tag(LOGTAG).d("Fail onSearchResult in NFC");
                                if(2 == i) {
                                    /* 読み込み待ちタイムアウト */
                                    CreditSettlement.getInstance().setWaitCardTimeout();
                                } else {
                                    CreditSettlement.getInstance().setCreditError(CreditErrorCodes.T99);
                                }

                                info.setResultFalg(false);
                                listener.callback(info);
                            }
                        }
                    });
                }
            }
            catch (SDKException sdk) {
                Timber.tag(LOGTAG).d("SDKException startSearchCard");
                CreditSettlement.getInstance().setCreditError(CreditErrorCodes.T99);
                releaseAll();
                info.setResultFalg(false);
                listener.callback(info);
            }
        }
    }

    private CardInfo handleMAG(MagneticCard card) {
        CardInfo info = new CardInfo();
        info.setResultFalg(true);
        info.setCardType(CardType.INMODE_MAG);
        TrackInfo ti_1 = card.getTrackInfos(MagneticCard.TRACK_1);
        TrackInfo ti_2 = card.getTrackInfos(MagneticCard.TRACK_2);
        TrackInfo ti_3 = card.getTrackInfos(MagneticCard.TRACK_3);
        info.setTrackNo(new String[]{ti_1.getData(), ti_2.getData(), ti_3.getData()});
        return info;
    }

    private CardInfo handleICC() throws SDKException {
        CardInfo info = new CardInfo();
        info.setCardType(CardType.INMODE_IC);
        if (iccReader.isCardPresent()) {
            ContactCard contactCard = iccReader.connectCard(VCC.VOLT_5, OperatorMode.EMV_MODE);
            byte[] atr = contactCard.getATR();
            if (atr.length != 0) {
                info.setResultFalg(true);
                info.setCardAtr(atr);
                // スロットのカード有無を監視
                CreditSettlement.getInstance().setIcCardExistFlg(true);
                final Runnable run = () -> {
                    boolean exist = true;
                    while (exist) {
                        exist = iccReader.isCardPresent();
                    }
                    CreditSettlement.getInstance().setIcCardExistFlg(false);
                };
                new Thread(run).start();
            } else {
                info.setResultFalg(false);
            }
        } else {
            info.setResultFalg(false);
        }
        return info;
    }

    private CardInfo handlePICC(int nfcType) throws SDKException {
        CardInfo info = new CardInfo();
        info.setResultFalg(true);
        info.setCardType(CardType.INMODE_NFC);
        info.setNfcType(nfcType);

        mEmvContactlessCard = EmvContactlessCard.connect();
        return info;
    }

    private ConditionVariable mVariable;
    private int timeout = 60 * 1000;

    /*
    public int DetectCards(EmvL2App param) {
        mVariable = new ConditionVariable();
        final int[] ret = {0};
        releaseAll();
        if (param.DetectMagStripe) {
            //Not support
        }
        if (param.DetectContact) {
            //Not Support
        }
        if (param.DetectContactLess) {
            piccReader = PiccReader.getInstance();
            Logger.debug("use contactless card");
            piccReader.startSearchCard(timeout, new PiccReaderCallback() {
                @Override
                public void onSearchResult(int i, int i1) {
                    Logger.debug("get contactless card i = " + i + " i1 = " + i1);
                    if (i == 0) {
                        ret[0] = EmvL2.L2_CS_PRESENT;
                        try {
                            mEmvContactlessCard = EmvContactlessCard.connect();
                        } catch (SDKException e) {
                            e.printStackTrace();
                        }
                        mode = TYPE_NFC;
                    } else {
                        // ret[0] = EmvL2.L2_CS_TIMEOUT;
                        Logger.debug("get picc error error");

                    }
                    stopICC();
                    stopMAG();
                    mVariable.open();
                }
            });
        }

        mVariable.block();
        return ret[0];
    }
    */
}
