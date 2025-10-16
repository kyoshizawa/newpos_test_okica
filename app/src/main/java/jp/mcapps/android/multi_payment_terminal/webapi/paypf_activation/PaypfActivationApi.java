package jp.mcapps.android.multi_payment_terminal.webapi.paypf_activation;


import java.io.IOException;

import jp.mcapps.android.multi_payment_terminal.webapi.HttpStatusException;
import jp.mcapps.android.multi_payment_terminal.webapi.paypf_activation.data.GetToken;
import jp.mcapps.android.multi_payment_terminal.webapi.paypf_activation.data.Activate;
import jp.mcapps.android.multi_payment_terminal.webapi.paypf_activation.data.RefreshToken;

public interface PaypfActivationApi {
    /**
     * アクティベーション要求を行います
     *
     * @param modelCode  製品型式コード
     * @param serialNo   製品シリアル番号
     * @param unitId     サブスク契約の式の単位
     * @param usePayment 決済端末かどうか
     * @param tid        決済端末番号
     * @param supplierCd 取引先コード
     * @return
     * @throws IOException
     * @throws HttpStatusException
     */
    Activate.Response activate(String modelCode, String serialNo, String unitId, Boolean usePayment, String tid, String supplierCd) throws IOException, HttpStatusException;

    /**
     * アクセストークンを取得する
     *
     * @param modelCode 製品型式コード
     * @param serialNo  製品シリアル番号
     * @param useInTest テストモードかどうか
     * @return
     * @throws IOException
     * @throws HttpStatusException
     */
    GetToken.Response getToken(String modelCode, String serialNo, Boolean useInTest) throws IOException, HttpStatusException;

    /**
     * アクセストークンを更新する
     *
     * @param refreshToken リフレッシュトークン
     * @param useInTest テストモードかどうか
     * @return
     * @throws IOException
     * @throws HttpStatusException
     */
    RefreshToken.Response refreshToken(String refreshToken, Boolean useInTest) throws IOException, HttpStatusException;
}
