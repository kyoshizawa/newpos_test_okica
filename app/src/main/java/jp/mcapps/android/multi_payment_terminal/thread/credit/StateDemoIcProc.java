package jp.mcapps.android.multi_payment_terminal.thread.credit;

import jp.mcapps.android.multi_payment_terminal.error.CreditErrorCodes;
import timber.log.Timber;
import jp.mcapps.android.multi_payment_terminal.thread.emv.EmvProcessing;

public class StateDemoIcProc implements CreditState {
    private final String LOGTAG = "StateDemoIcProc";
    private CreditSettlement _creditSettlement;
    private EmvProcessing _emvProc;

    public StateDemoIcProc(CreditSettlement creditSettlement, EmvProcessing emvProc) {
        _creditSettlement = creditSettlement;
        _emvProc = emvProc;
    }

    public int stateMethod(){
        int ret = 0;

        Timber.tag(LOGTAG).d("stateMethod");

        if (CreditSettlement.k_CHIPCC_IC == _creditSettlement.getChipCC()) {
            // 暗証番号入力
            if (CreditSettlement.k_CREDIT_STAT_PROCESSING == _creditSettlement.getCreditProcStatus()) {
                _creditSettlement.onPinInputRequired();
            }
        }

        ret = CreditSettlement.k_OK;

        return ret;
    }
}
