package jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.FeliCaClient;

public interface IState {
    /**
     * 処理実行
     * 現在のステータスに応じた処理を実行する
     *
     * @param clientStateMachine 動作させるステータス
     *
     * @return エラーコード
     */
    long DoExec(ClientStateMachine clientStateMachine);

    /**
     * オブジェクトの破棄
     * オブジェクトを破棄します
     */
    void Release();

    void SetEventListener(IFeliCaClientEventListener listener);
}
