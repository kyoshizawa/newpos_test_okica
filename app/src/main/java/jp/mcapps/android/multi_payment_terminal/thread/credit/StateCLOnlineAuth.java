package jp.mcapps.android.multi_payment_terminal.thread.credit;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.newpos.emvl2.EMV_ERROR_VALUE;

import jp.mcapps.android.multi_payment_terminal.error.CreditErrorCodes;
import jp.mcapps.android.multi_payment_terminal.thread.emv.Constants.*;
import jp.mcapps.android.multi_payment_terminal.thread.emv.EmvCLProcess;
import jp.mcapps.android.multi_payment_terminal.thread.emv.utils.ISOUtil;
import timber.log.Timber;

public class StateCLOnlineAuth implements CreditState {
    private final String LOGTAG = "StateCLOnlineAuth";
    private CreditSettlement _creditSettlement;
    private EmvCLProcess _emvCLProc;

    public StateCLOnlineAuth(CreditSettlement creditSettlement, EmvCLProcess emvCLProc) {
        _creditSettlement = creditSettlement;
        _emvCLProc = emvCLProc;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public int stateMethod(){
        int ret;

        Timber.tag(LOGTAG).d("stateMethod");


        /* オンラインオーソリ */
        ret = _creditSettlement._mcCenterCommManager.onlineAuth();

        // オンラインオーソリ結果設定
        _creditSettlement.setAuthResCode();

        if (_creditSettlement.getOnlineAuthErrorReason() != CreditSettlement.AuthErrorReason.NONE) {
            final int outcome = _emvCLProc.getNotApprovalOutcomeFromARC();

            if (outcome == EMV_ERROR_VALUE.EMV_OTHER_INTERFACE) _creditSettlement.setCreditError(CreditErrorCodes.T31);
            _creditSettlement.setCreditProcStatus(CreditSettlement.k_CREDIT_STAT_ONLINE_NG);

            /*
             * 接触ICの実装より端末からは売上データのICCデータをセンターに送信する
             * センターは拒否売上と拒否アドバイスの送信を同時に実行し同一のICC関連データを使用
             * 売上の方が重要なので端末からは売上の方のICCデータを送信する
             * 送らないといけないICCデータの内容が異なるのでアドバイス送信でエラーになるかも
             */
            // String iccData = ISOUtil.byte2hex(_emvCLProc.getTagForCancel());
            String iccData = ISOUtil.byte2hex(_emvCLProc.getICCDataForSales());

            iccData = _creditSettlement._mcCenterCommManager.getEncryptData(String.format("%06X%s", iccData.length()/2, iccData));
            _creditSettlement.setRsaDataForPayment(iccData);
            ret = CreditSettlement.k_ERROR_UNKNOWN;
        } else {
            // 正常終了
            _creditSettlement.setCreditProcStatus(CreditSettlement.k_CREDIT_STAT_ONLINE_OK);

            String iccData = ISOUtil.byte2hex(_emvCLProc.getICCDataForSales());
            iccData = _creditSettlement._mcCenterCommManager.getEncryptData(String.format("%06X%s", iccData.length()/2, iccData));
            _creditSettlement.setRsaDataForPayment(iccData);

            ret = CreditSettlement.k_OK;
        }

        return ret;
    }
}
