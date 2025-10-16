package jp.mcapps.android.multi_payment_terminal.thread.credit;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.newpos.emvl2.EMV_ENUM;
import com.newpos.emvl2.EMV_ERROR_VALUE;
import com.pos.device.SDKException;

import jp.mcapps.android.multi_payment_terminal.error.CreditErrorCodes;
import jp.mcapps.android.multi_payment_terminal.thread.emv.Constants.*;
import jp.mcapps.android.multi_payment_terminal.thread.emv.EmvCLProcess;
import timber.log.Timber;
import jp.mcapps.android.multi_payment_terminal.thread.emv.EmvProcessing;

public class StateMsOnlineAuth implements CreditState {
    private final String LOGTAG = "StateMsOnlineAuth";
    private CreditSettlement _creditSettlement;
    private EmvProcessing _emvProc;

    private final EmvCLProcess _emvCLProc;

    public StateMsOnlineAuth(CreditSettlement creditSettlement, EmvProcessing emvProc, EmvCLProcess emvCLProc) {
        _creditSettlement = creditSettlement;
        _emvProc = emvProc;
        _emvCLProc = emvCLProc;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public int stateMethod() throws SDKException {
        int ret;

        Timber.tag(LOGTAG).d("stateMethod");

        if (_creditSettlement.isIC()) {
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
        }
        else if (_creditSettlement.isCL()) {
            ret = _emvCLProc.startTransaction(
                    CardManager.getInstance(_creditSettlement.getActivateIF().getMode()),
                    _creditSettlement.getSlipData().transAmount,
                    EMV_ENUM.EMV_TRANS_REFUND);

            if (!verifyRefundOutcome(ret)) {
                _creditSettlement.setCreditError(CreditErrorCodes.T16);
                return CreditSettlement.k_ERROR_UNKNOWN;
            }

            // CVM検証が実施されていない場合は元取引と同じ検証を行う
            if (_emvCLProc.getCVM() == OutcomeCVM.CVM_NA) {
                _creditSettlement.setSignatureFlag(_creditSettlement.getSlipData().creditSignatureFlg);
            }
            else if (_emvCLProc.getCVM() != OutcomeCVM.OBTAIN_SIGNATURE) {
                _creditSettlement.setSignatureFlag(CreditSettlement.k_SIGNATURE_SPACE_NONE);
            }
        }

        /* カード判定 */
        ret = _creditSettlement._mcCenterCommManager.cardAnalyze();
        if (0 != ret) {
            Timber.tag(LOGTAG).d("Fail cardAnalyze %d", ret);
            return CreditSettlement.k_ERROR_UNKNOWN;
        }

        /* オンラインオーソリ */
        if (CreditSettlement.k_CREDIT_STAT_PROCESSING == _creditSettlement.getCreditProcStatus()) {
            ret = _creditSettlement._mcCenterCommManager.onlineAuth();
            if (CreditSettlement.k_OK == ret) {
                // 正常終了
                _creditSettlement.setCreditProcStatus(_creditSettlement.k_CREDIT_STAT_ONLINE_OK);
                ret = CreditSettlement.k_OK;
            } else {
                // エラー
                Timber.tag(LOGTAG).d("Fail onlineAuth %d", ret);
                _creditSettlement.setCreditProcStatus(_creditSettlement.k_CREDIT_STAT_ONLINE_NG);
                ret = CreditSettlement.k_ERROR_UNKNOWN;
            }
        } else {
            ret = _creditSettlement._mcCenterCommManager.onlineAuthCancel(_creditSettlement.getSlipData());
            if (CreditSettlement.k_OK == ret) {
                // 正常終了
                _creditSettlement.setCreditProcStatus(_creditSettlement.k_CREDIT_STAT_ONLINE_CANCEL_OK);
                ret = CreditSettlement.k_OK;
            } else {
                // エラー
                Timber.tag(LOGTAG).d("Fail onlineAuthCancel %d", ret);
                _creditSettlement.setCreditProcStatus(_creditSettlement.k_CREDIT_STAT_ONLINE_CANCEL_NG);
                ret = CreditSettlement.k_ERROR_UNKNOWN;
            }
        }

        return ret;
    }

    /**
     * 非接触EMVの取消Outcomeを検証します
     *
     * @param outcome 取引結果
     * @return 成功/失敗,
     */
    private boolean verifyRefundOutcome(int outcome) {
        int kernelId = _emvCLProc.getKernelId();

        if (kernelId == KernelID.Mastercard) {
            /*
             * POSガイドラインよりEND APPLICATIONとなった場合､カード会社判定処理を行う
             * 取消の場合はTAC DenialがオールFになるためそれでEnd Applicationとなる
             * END_APPLICATIONの定義がないがOutcome Parameter Set('DF8129')から
             * EMV_TERMINATEDがEND APPLICATIONに相当する
             * 端末アクション分析が実行されるが取消のTAC DenialはオールFで設定されるため
             * CIDはAACとなる
             */
            return isCLCommonSuccessOutcome(outcome)
                    || outcome == EMV_ERROR_VALUE.EMV_TERMINATED;
        }
        else if (kernelId == KernelID.VISA) {
            /*
             * POSガイドラインではDECLINEDになるとの事だが
             * CIDがAACの場合でもカーネルからはEMV_OKが返ってくる
             * メーカーに確認したところ取消の場合はAACでもEMV_OKが返る仕様とのこと
             * 端末アクション分析は行われない
             */
            return isCLCommonSuccessOutcome(outcome);
        }
        else if (kernelId == KernelID.Amex) {
            /*
             * POSガイドラインよりAPPROVED､ONLINE REQUESTの取引結果の場合､カード会社判定処理を行う
             */
            return isCLCommonSuccessOutcome(outcome);
        }
        else if (kernelId == KernelID.JCB) {
            /*
             * Kernel取引結果の判定でDECLINEDとなった場合､カード会社判定処理を行う
             * 端末アクション分析は行われない
             */
            return isCLCommonSuccessOutcome(outcome)
                    || outcome == EMV_ERROR_VALUE.EMV_DECLIEND;
        }
        else if (kernelId == KernelID.Diners) {
            // Todo 非接触 EMV 対応 POS ガイドライン 取引処理編によるとDECLINEDで終了のはずがOnline Requestで終了する
            return isCLCommonSuccessOutcome(outcome)
                    || outcome == EMV_ERROR_VALUE.EMV_DECLIEND;
        }
        return false;
    }

    /**
     * 非接触カード処理の取消結果の評価を行う
     * 仕様と異なりVISAでオンライン要求になるカードがあったため
     * 全ブランドTCとARCになった場合もOKにする
     *
     * @param outcome 結果
     * @return 成否
     */
    private boolean isCLCommonSuccessOutcome(int outcome) {
        return outcome == EMV_ERROR_VALUE.EMV_OK
                || outcome == EMV_ERROR_VALUE.EMV_REQUIRE_ONLINE
                || outcome == EMV_ERROR_VALUE.EMV_REQUIRE_ONLINE_LONG_TAP
                || outcome == EMV_ERROR_VALUE.EMV_REQUIRE_ONLINE_2END_TAP
                || outcome == EMV_ERROR_VALUE.EMV_REQUIRE_ONLINE_2END_TAP_IF_HAS_SCRIPT
                || outcome == EMV_ERROR_VALUE.EMV_REQUIRE_ONLINE_2END_TAP_IF_HAS_SCRIPT_OR_IAD;
    }
}
