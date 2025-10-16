package jp.mcapps.android.multi_payment_terminal.thread.credit;

import com.newpos.emvl2.EMV_ENUM;
import com.newpos.emvl2.EMV_ERROR_VALUE;

import jp.mcapps.android.multi_payment_terminal.data.Amount;
import jp.mcapps.android.multi_payment_terminal.error.CreditErrorCodes;
import jp.mcapps.android.multi_payment_terminal.thread.emv.Constants.*;
import jp.mcapps.android.multi_payment_terminal.thread.emv.EmvCLProcess;
import timber.log.Timber;

public class StateCLProc implements CreditState {
    private final String LOGTAG = "StateIcProc";
    private CreditSettlement _creditSettlement;
    private EmvCLProcess _emvCLProc;

    public StateCLProc(CreditSettlement creditSettlement, EmvCLProcess emvCLProc) {
        _creditSettlement = creditSettlement;
        _emvCLProc = emvCLProc;
    }

    public int stateMethod(){
        int ret = 0;

        Timber.tag(LOGTAG).d("stateMethod");

        _creditSettlement.getCLState().onNext(CLState.CardHold);

        int outcome = _emvCLProc.startTransaction(
                CardManager.getInstance(_creditSettlement.getActivateIF().getMode()),
                Amount.getFixedAmount(),
                EMV_ENUM.EMV_TRANS_PURCHASE);

        if (!isOnlineOutcome(outcome)) {
            // 読取エラー
            if (outcome == EMV_ERROR_VALUE.EMV_TRY_AGAIN || outcome == EMV_ERROR_VALUE.EMV_PRESENT_CARD_AGAIN) {
                return CreditSettlement.k_ERROR_READ_AGAIN;
            }
            else if (outcome == EMV_ERROR_VALUE.EMV_TRY_AGAIN_SEEPHONE) {
                return CreditSettlement.k_ERROR_READ_AGAIN_AND_SEEPHONE;
            }

            _creditSettlement.setCreditError(outcome == EMV_ERROR_VALUE.EMV_OTHER_INTERFACE ? CreditErrorCodes.T31
                    : outcome == EMV_ERROR_VALUE.EMV_NO_APP_SUPPORTED ? CreditErrorCodes.T32
                    // : outcome == EMV_ERROR_VALUE.EMV_TRY_AGAIN_SEEPHONE ? CreditErrorCodes.T33
                    : outcome == EMV_ERROR_VALUE.EMV_TERMINATED ? CreditErrorCodes.T34
                    : CreditErrorCodes.T18);

            return CreditSettlement.k_ERROR_UNKNOWN;
        }

        if (_emvCLProc.getCVM() != OutcomeCVM.OBTAIN_SIGNATURE) {
            _creditSettlement.setSignatureFlag(CreditSettlement.k_SIGNATURE_SPACE_NONE);
        }

        _creditSettlement.getCLState().onNext(CLState.OnlineRequest);

        // カード判定
        ret = _creditSettlement._mcCenterCommManager.cardAnalyze();
        if (CreditSettlement.k_OK != ret) {
            return CreditSettlement.k_ERROR_UNKNOWN;
        }

        return ret;
    }

    private boolean isOnlineOutcome(int outcome) {
        return outcome == EMV_ERROR_VALUE.EMV_REQUIRE_ONLINE
                || outcome == EMV_ERROR_VALUE.EMV_REQUIRE_ONLINE_LONG_TAP
                || outcome == EMV_ERROR_VALUE.EMV_REQUIRE_ONLINE_2END_TAP
                || outcome == EMV_ERROR_VALUE.EMV_REQUIRE_ONLINE_2END_TAP_IF_HAS_SCRIPT
                || outcome == EMV_ERROR_VALUE.EMV_REQUIRE_ONLINE_2END_TAP_IF_HAS_SCRIPT_OR_IAD;
    }
}
