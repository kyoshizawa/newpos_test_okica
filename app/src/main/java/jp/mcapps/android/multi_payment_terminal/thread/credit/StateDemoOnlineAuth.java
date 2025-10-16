package jp.mcapps.android.multi_payment_terminal.thread.credit;

import jp.mcapps.android.multi_payment_terminal.error.CreditErrorCodes;
import jp.mcapps.android.multi_payment_terminal.thread.emv.EmvCLProcess;
import timber.log.Timber;
import jp.mcapps.android.multi_payment_terminal.thread.emv.EmvProcessing;

public class StateDemoOnlineAuth implements CreditState {
    private final String LOGTAG = "StateDemoOnlineAuth";
    private CreditSettlement _creditSettlement;
    private EmvProcessing _emvProc;
    private EmvCLProcess _emvCLProc;

    public StateDemoOnlineAuth(CreditSettlement creditSettlement, EmvProcessing emvProc, EmvCLProcess emvLCProc) {
        _creditSettlement = creditSettlement;
        _emvProc = emvProc;
        _emvCLProc = emvLCProc;
    }

    public int stateMethod(){
        int ret = 0;

        Timber.tag(LOGTAG).d("stateMethod");

        // オンライン処理は行わず、処理結果OKとする
        if (CreditSettlement.k_CREDIT_STAT_PROCESSING == _creditSettlement.getCreditProcStatus()) {
            _creditSettlement.setCreditProcStatus(CreditSettlement.k_CREDIT_STAT_ONLINE_OK);
        } else {
            _creditSettlement.setCreditProcStatus(CreditSettlement.k_CREDIT_STAT_ONLINE_CANCEL_OK);
        }

        ret = CreditSettlement.k_OK;

        return ret;
    }
}
