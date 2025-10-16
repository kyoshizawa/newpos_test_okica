package jp.mcapps.android.multi_payment_terminal.thread.credit;

import jp.mcapps.android.multi_payment_terminal.thread.emv.Constants.*;
import jp.mcapps.android.multi_payment_terminal.thread.emv.EmvCLProcess;
import jp.mcapps.android.multi_payment_terminal.thread.emv.EmvProcessing;
import timber.log.Timber;

public class StateDemoCLProc implements CreditState {
    private final String LOGTAG = "StateDemoCLProc";
    private CreditSettlement _creditSettlement;
    private EmvCLProcess _emvCLProc;

    public StateDemoCLProc(CreditSettlement creditSettlement, EmvCLProcess emvCLProc) {
        _creditSettlement = creditSettlement;
        _emvCLProc = emvCLProc;
    }

    public int stateMethod(){
        int ret = 0;

        Timber.tag(LOGTAG).d("stateMethod");

        new Thread(() -> {
            try {
                _creditSettlement.getCLState().onNext(CLState.CardHold);
                Thread.sleep(1000);
                _creditSettlement.getCLState().onNext(CLState.RemoveCard);
                Thread.sleep(1000);
                _creditSettlement.getCLState().onNext(CLState.OnlineRequest);
                Thread.sleep(2000);
                _creditSettlement.startDemoAuth();
            } catch (InterruptedException e) {
                Timber.e(e);
            }
        }).start();

        ret = CreditSettlement.k_OK;

        return ret;
    }
}
