package jp.mcapps.android.multi_payment_terminal.ui.emoney.okica;

import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.data.TransMap;
import jp.mcapps.android.multi_payment_terminal.data.okica.SFBalanceInfo;
import jp.mcapps.android.multi_payment_terminal.data.okica.SFLogInfo;

/**
 * SF判定を行うクラスです
 */
public class SfJudge {
    /**
     * SF判定の結果
     */
    public enum Result {
        Success(null, "成功"),
        PreSfOperationError(_app.getString(R.string.error_type_okica_sf_judge_cancel_error), "前SF操作判定異常"),
        SfLogIDError(_app.getString(R.string.error_type_okica_sf_judge_cancel_error), "SFログID判定エラー");
        ;

        private final String errorCode;

        public String getErrorCode() {
            return errorCode;
        }

        private final String message;

        public String getMessage() {
            return message;
        }

        Result(String errorCode, String message) {
            this.errorCode = errorCode;
            this.message = message;
        }
    }

    private static final MainApplication _app = MainApplication.getInstance();

    /**
     * カードのSF判定を行います
     * 最初に異常を検出した時点で総合判定NGとし判定処理を終了します
     *
     * @return 総合判定結果
     */
    public static Result execute(
            int transType,
            SFBalanceInfo sfBalanceInfo,
            SFLogInfo sfLogInfo
    ) {
        Result result = Result.Success;

        if (!judgePreSfOperation(transType, sfLogInfo.getProcessingType())) {  // 前SF操作判定
            result = Result.PreSfOperationError;
        }
        else if (!judgeSfLogID(sfLogInfo.getSFLogId(), sfBalanceInfo.getExecId())) {  // SFログID判定
            result = Result.SfLogIDError;
        }

        return result;
    }

    /**
     * 前SF操作判定を行います
     *
     * @param transType 取引種別
     * @param processingType SFログ情報の処理種別
     *
     * @return 判定結果 true: 正常 false: 異常
     */
    private static boolean judgePreSfOperation(int transType, int processingType) {
        if (transType == TransMap.TYPE_CANCEL) {  // 取消
            return (processingType == 70) ? true : false;  // 70：物販利用であればOK
        } else {
            return false;
        }
    }

    /**
     * SFログID判定を行います
     *
     * @param sfLogID SFログ情報のSFログID
     * @param execId SF残額情報の実行ID
     *
     * @return 判定結果 true: 正常 false: 異常
     */
    private static boolean judgeSfLogID(int sfLogID, int execId) {
        if (sfLogID == execId) {
            return true;
        } else {
            return false;
        }
    }
}
