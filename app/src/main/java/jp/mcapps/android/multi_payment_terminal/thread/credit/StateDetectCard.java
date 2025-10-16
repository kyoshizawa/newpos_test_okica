package jp.mcapps.android.multi_payment_terminal.thread.credit;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.data.BusinessType;
import jp.mcapps.android.multi_payment_terminal.database.history.slip.SlipData;
import timber.log.Timber;

import static jp.mcapps.android.multi_payment_terminal.thread.credit.CardType.INMODE_IC;
import static jp.mcapps.android.multi_payment_terminal.thread.credit.CardType.INMODE_MAG;
import static jp.mcapps.android.multi_payment_terminal.thread.credit.CardType.INMODE_NFC;

public class StateDetectCard implements CreditState {
    private final String LOGTAG = "StateDetectCard";
    private CardListener _cardListener;

    private final CreditSettlement _creditSettlement;

    public StateDetectCard(CardListener cardListener, CreditSettlement creditSettlement) {
        _cardListener = cardListener;
        _creditSettlement = creditSettlement;
    }

    public int stateMethod(){
        Timber.tag(LOGTAG).d("stateMethod");

        final BusinessType type = MainApplication.getInstance().getBusinessType();



       final CardManager cm = CardManager.getInstance(_creditSettlement.getActivateIF().getMode());

        /* IC、または、磁気カードの検出を開始 */
        cm.getCard(CreditSettlement.k_WAIT_CARD_TIMEOUT, _cardListener);

        return CreditSettlement.k_OK;
    }
}
