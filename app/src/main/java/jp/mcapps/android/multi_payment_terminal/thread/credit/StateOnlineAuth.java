package jp.mcapps.android.multi_payment_terminal.thread.credit;

import android.os.Build;

import androidx.annotation.RequiresApi;

import jp.mcapps.android.multi_payment_terminal.error.CreditErrorCodes;
import jp.mcapps.android.multi_payment_terminal.thread.emv.ParamConfig;
import timber.log.Timber;
import jp.mcapps.android.multi_payment_terminal.thread.emv.EmvProcessing;

public class StateOnlineAuth implements CreditState {
    private final String LOGTAG = "StateOnlineAuth";
    private CreditSettlement _creditSettlement;
    private EmvProcessing _emvProc;

    public StateOnlineAuth(CreditSettlement creditSettlement, EmvProcessing emvProc) {
        _creditSettlement = creditSettlement;
        _emvProc = emvProc;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public int stateMethod(){
        int ret;

        Timber.tag(LOGTAG).d("stateMethod");

        /* PIN入力後のICカード処理 */
        ret = _emvProc.emvAfterInputPin();
        if (0 != ret) {
            // オンライン判定でなければ終了
            _creditSettlement.setIcProcError(ret);
            return CreditSettlement.k_ERROR_UNKNOWN;
        }

        /* オンラインオーソリ */
        ret = _creditSettlement._mcCenterCommManager.onlineAuth();

        // オンラインオーソリ結果設定
        _creditSettlement.setAuthResCode();

        // オンラインオーソリ応答検証
        ret = _emvProc.emvOnlineAuthVerification();
        String iccData = _emvProc.emvGetSomeTagValues(ParamConfig.TagsOnlineAuthVerification);
        iccData = _creditSettlement._mcCenterCommManager.getEncryptData(iccData);
        _creditSettlement.setRsaDataForPayment(iccData);
        if (0 == ret) {
            // 決済正常終了
            _creditSettlement.setCreditProcStatus(_creditSettlement.k_CREDIT_STAT_ONLINE_OK);
            ret = CreditSettlement.k_OK;
        } else {
            // 決済エラー
            if (CreditSettlement.AuthErrorReason.NONE == _creditSettlement.getOnlineAuthErrorReason()) {
                // オーソリNG要因設定（既に設定されている場合は上書きしない）
                _creditSettlement.setOnlineAuthErrorReason(CreditSettlement.AuthErrorReason.RES_VERI_NG);
            }
            _creditSettlement.setCreditError(CreditErrorCodes.T24);
            _creditSettlement.setCreditProcStatus(_creditSettlement.k_CREDIT_STAT_ONLINE_NG);
            ret = CreditSettlement.k_ERROR_UNKNOWN;
        }

        return ret;
    }
}
