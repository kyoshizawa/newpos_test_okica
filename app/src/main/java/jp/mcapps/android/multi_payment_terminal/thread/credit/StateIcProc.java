package jp.mcapps.android.multi_payment_terminal.thread.credit;

import com.pos.device.SDKException;

import jp.mcapps.android.multi_payment_terminal.error.CreditErrorCodes;
import timber.log.Timber;
import jp.mcapps.android.multi_payment_terminal.thread.emv.EmvProcessing;

public class StateIcProc implements CreditState {
    private final String LOGTAG = "StateIcProc";
    private CreditSettlement _creditSettlement;
    private EmvProcessing _emvProc;

    public StateIcProc(CreditSettlement creditSettlement, EmvProcessing emvProc) {
        _creditSettlement = creditSettlement;
        _emvProc = emvProc;
    }

    public int stateMethod() throws SDKException {
        int ret = 0;

        Timber.tag(LOGTAG).d("stateMethod");

        ret = _emvProc.init(_creditSettlement);
        if (0 != ret) {
            _creditSettlement.setCreditError(CreditErrorCodes.T16);
            return CreditSettlement.k_ERROR_UNKNOWN;
        }

        // アプリケーション選択
        ret = _emvProc.selectApp();
        if (0 != ret) {
            _creditSettlement.setCreditError(CreditErrorCodes.T17);
            return CreditSettlement.k_ERROR_UNKNOWN;
        }

        // ICカードデータ読み込み
        ret = _emvProc.emvReadData();
        if (0 != ret) {
            _creditSettlement.setCreditError(CreditErrorCodes.T18);
            return CreditSettlement.k_ERROR_UNKNOWN;
        }

        /* カード判定 */
        ret = _creditSettlement._mcCenterCommManager.cardAnalyze();
        if (CreditSettlement.k_OK != ret) {
            return CreditSettlement.k_ERROR_UNKNOWN;
        }

        /* ICカード処理（PIN入力まで） */
        ret = _emvProc.emvIcProc();
        if (0 != ret) {
            _creditSettlement.setIcProcError(ret);
            return CreditSettlement.k_ERROR_UNKNOWN;
        }

        return ret;
    }
}
