package jp.mcapps.android.multi_payment_terminal.model;

import com.google.gson.Gson;

import org.checkerframework.checker.units.qual.C;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.error.CreditErrorCodes;
import jp.mcapps.android.multi_payment_terminal.error.McPosCenterErrorMap;
import jp.mcapps.android.multi_payment_terminal.thread.credit.CreditSettlement;
import jp.mcapps.android.multi_payment_terminal.thread.emv.CAPK;
import jp.mcapps.android.multi_payment_terminal.thread.emv.ParamManage;
import jp.mcapps.android.multi_payment_terminal.thread.emv.RiskManagementParameter;
import jp.mcapps.android.multi_payment_terminal.webapi.HttpStatusException;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.McPosCenterApi;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.McPosCenterApiImpl;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.CAKey;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.RiskParameterContactless;
import timber.log.Timber;

public class McCredit {
    private static final int MAX_RETRY_COUNT = 3;
    private static MainApplication _app = MainApplication.getInstance();
    private McPosCenterApi _apiClient = new McPosCenterApiImpl();
    private final String LOGTAG = "McCredit";

    public String getCAKey() {
        Timber.tag(LOGTAG).i("CA公開鍵取得要求");
        try {
            CAKey.Response response = _apiClient.getCAKey();

            if (!response.result) {
                Timber.tag(LOGTAG).e("CA公開鍵取得応答失敗（エラーコード:%s）", response.errorCode);
                //return response.errorCode;
                //return McPosCenterErrorMap.get(response.errorCode);
                CreditSettlement.getInstance().setCreditError(CreditErrorCodes.T12);
                return CreditSettlement.getInstance().getCreditErrorCode();
            }
            Timber.tag(LOGTAG).i("CA公開鍵取得応答成功");

            // CA公開鍵を設定
            ParamManage pm = new ParamManage();
            pm.saveCAPublicKey(response);

            if (AppPreference.isMoneyContactless()) {
                final CAPK[] capk = new CAPK[response.caKeys.length];

                for (int i = 0; i < response.caKeys.length; i++) {
                    final CAPK c = new CAPK();

                    c.setBlandSign(response.caKeys[i].blandSign);
                    c.setCaPublicKeyVersion(response.caKeys[i].caPublicKeyVersion);
                    c.setCaPublicKeyIndex(response.caKeys[i].caPublicKeyIndex);
                    c.setCaHashAlgorithmIndicator(response.caKeys[i].caHashAlgorithmIndicator);
                    c.setCaPublicKeyAlgorithmIndicator(response.caKeys[i].caPublicKeyAlgorithmIndicator);
                    c.setCaPublicKeyModulus(response.caKeys[i].caPublicKeyModulus);
                    c.setCaPublicKeyExponent(response.caKeys[i].caPublicKeyExponent);
                    c.setCaPublicKeyCheckSum(response.caKeys[i].caPublicKeyCheckSum);

                    capk[i] = c;
                }

                _app.setCAPK(capk);
            }

            return null;

        } catch (UnknownHostException | SocketTimeoutException e) {
            Timber.e(e);
            CreditSettlement.getInstance().setCreditError(CreditErrorCodes.T07);
            return CreditSettlement.getInstance().getCreditErrorCode();
        } catch (IOException | IllegalStateException e) {
            Timber.e(e);
            return McPosCenterErrorMap.INTERNAL_ERROR_CODE;
        } catch (HttpStatusException e) {
            Timber.e("getCAKey HttpStatusError %d", e.getStatusCode());
            return McPosCenterErrorMap.INTERNAL_ERROR_CODE;
        }
    }

    public String getRiskParameterContactless() {
        try {

            RiskParameterContactless.Request request = new RiskParameterContactless.Request();
            RiskParameterContactless.Response response = _apiClient.getRiskParameterContactless(request);

            if (!response.result) {
                CreditSettlement.getInstance().setCreditError(CreditErrorCodes.T12);
                return CreditSettlement.getInstance().getCreditErrorCode();
            }

            final int enables = 0;

            List<RiskManagementParameter> rmpList = new ArrayList<>();

            int size = 0;
            for (int i = 0; i < response.riskParams.length; i++) {
                // ブランドが有効になっているものだけ保存する
                if (!response.riskParams[i].brandEnableFlg) continue;
                size++;

                final RiskManagementParameter r = new RiskManagementParameter();

                r.setBrandSign(response.riskParams[i].brandSign);
                r.setAid(response.riskParams[i].aid);
                r.setFloorLimit(response.riskParams[i].floorLimit);
                r.setMaxTargetPercentage(response.riskParams[i].maxTargetPercentage);
                r.setTargetParcentage(response.riskParams[i].targetPercentage);
                r.setThreshold(response.riskParams[i].threshold);
                r.setBrandEnableFlg(response.riskParams[i].brandEnableFlg);
                r.setDefaultDDOL(response.riskParams[i].defaultDDOL);
                r.setExecPriority(response.riskParams[i].execPriority);
                r.setTerminalActionCdDenial(response.riskParams[i].terminalActionCdDenial);
                r.setTerminalActionCdDefault(response.riskParams[i].terminalActionCdDefault);
                r.setTerminalActionCdOnline(response.riskParams[i].terminalActionCdOnline);
                r.setTerminalActionCdDenialRefund(response.riskParams[i].terminalActionCdDenialRefund);
                r.setTerminalActionCdDefaultRefund(response.riskParams[i].terminalActionCdDefaultRefund);
                r.setTerminalActionCdOnlineRefund(response.riskParams[i].terminalActionCdOnlineRefund);
                r.setAcquirerOnlinePayType(response.riskParams[i].acquirerOnlinePayType);
                r.setKameitenClassCd(response.riskParams[i].kameitenClassCd);
                r.setTxnTypeCd(response.riskParams[i].txnTypeCd);
                r.setChargeTypeCd(response.riskParams[i].chargeTypeCd);
                r.setAppPersonalInfo(response.riskParams[i].appPersonalInfo);
                r.setKernelId(response.riskParams[i].kernelId);
                r.setAcquirerContactlessId(response.riskParams[i].acquirerContactlessId);
                r.setCombinationOptions(response.riskParams[i].combinationOptions);
                r.setContactlessTransactionLimit(response.riskParams[i].contactlessTransactionLimit);
                r.setCvmRequiredLimit(response.riskParams[i].cvmRequiredLimit);
                r.setMerchantNameAndLocation(response.riskParams[i].merchantNameAndLocation);
                r.setRemovableTimeout(response.riskParams[i].removableTimeout);
                r.setTerminalCountryCode(response.riskParams[i].terminalCountryCode);
                r.setTerminalInterchangeProfile(response.riskParams[i].terminalInterchangeProfile);
                r.setTerminalType(response.riskParams[i].terminalType);
                r.setTransactionCurrencyCode(response.riskParams[i].transactionCurrencyCode);
                r.setTransactionCurrencyExponent(response.riskParams[i].transactionCurrencyExponent);
                r.setDefaultMdol(response.riskParams[i].defaultMdol);

                rmpList.add(r);
            }

            final RiskManagementParameter[] rmp = rmpList.toArray(new RiskManagementParameter[size]);
            _app.setRiskManagementParameter(rmp);

            return null;

        } catch (UnknownHostException | SocketTimeoutException e) {
            Timber.e(e);
            CreditSettlement.getInstance().setCreditError(CreditErrorCodes.T07);
            return CreditSettlement.getInstance().getCreditErrorCode();
        } catch (IOException | IllegalStateException e) {
            Timber.e(e);
            return McPosCenterErrorMap.INTERNAL_ERROR_CODE;
        } catch (HttpStatusException e) {
            Timber.e("getRiskParameterContactless HttpStatusError %d", e.getStatusCode());
            return McPosCenterErrorMap.INTERNAL_ERROR_CODE;
        }
    }
}
