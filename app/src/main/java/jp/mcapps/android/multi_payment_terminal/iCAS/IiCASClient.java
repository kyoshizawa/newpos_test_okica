package jp.mcapps.android.multi_payment_terminal.iCAS;

import jp.mcapps.android.multi_payment_terminal.iCAS.data.DeviceClient;

public interface IiCASClient {
    void OnUIUpdate(DeviceClient.RWParam rwParam);
    void OnStatusChanged(DeviceClient.Status status);
    void OnDisplay(DeviceClient.Display display);
    void OnOperation(DeviceClient.Operation operation);
    void OnCancelDisable(boolean bDisable);
    void OnResultSuica(DeviceClient.Result resultSuica);
    void OnResultID(DeviceClient.ResultID resultID);
    void OnResultWAON(DeviceClient.ResultWAON resultWAON);
    void OnResultQUICPay(DeviceClient.ResultQUICPay resultQUICPay);
    void OnResultEdy(DeviceClient.ResultEdy resultEdy);
    void OnResultnanaco(DeviceClient.Resultnanaco resultnanaco);
    void OnJournalEdy(String daTermTo);
    void OnJournalnanaco(String daTermTo);
    void OnJournalQUICPay(String daTermTo);
    void OnFinished(int statusCode);
    void OnErrorOccurred(long lErrorType, final String errorMessage);
    void OnRecovery(Object result);

    /**
     * RWトランスミット実行
     * RWトランスミットを実行します。
     * 本ハンドラはサブスレッド内で動作します。
     *
     * @param command       コマンドデータ
     * @param timeout       タイムアウト時間（ミリ秒）
     * @param response      RWレスポンスデータ
     *
     * @return エラーコード（enum errorType 参照）
     *         成功時はレスポンスデータレングス
     */
    long OnTransmitRW(byte[] command, long timeout, byte[] response);
}
