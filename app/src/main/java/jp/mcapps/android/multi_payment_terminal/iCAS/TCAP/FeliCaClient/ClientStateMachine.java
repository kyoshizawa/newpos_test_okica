package jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.FeliCaClient;

import jp.mcapps.android.multi_payment_terminal.iCAS.TCAPTest.iCASClientTest;

public abstract class ClientStateMachine {
    public enum status
    {
        STATUS_NONE(0),             // ステータスなし(終了を意味します)
        STATUS_HANDSHAKE(1),        // Handshakeステータス
        STATUS_NEUTRAL(2),          // Neutralステータス
        STATUS_FAREWELL(3),         // Farewellステータス
        ;

        private final int _val;

        status(final int val) {
            _val = val;
        }

        public int getInt() {
            return _val;
        }
    }

    private IState _iState;             // 現在のステータスオブジェクト
    private status _currentStatus;      // 現在のステータス
    private status _nextStatus;         // 次に遷移するステータス
    private final TCAPContext _tcapContext;   // TCAP通信コンテキスト
    private final TCAPCommunicationAgent _tcapCommunicationAgent;     // 通信エージェント

    public ClientStateMachine(TCAPContext tcapContext, TCAPCommunicationAgent tcapCommunicationAgent) {
        _iState = null;
        _currentStatus = status.STATUS_NONE;
        _nextStatus = status.STATUS_NONE;
        _tcapContext = tcapContext;
        _tcapCommunicationAgent = tcapCommunicationAgent;
    }

    /**
     * URLの設定
     * サーバURLを保持します。
     *
     * @param url サーバURL
     */
    public void SetUrl(String url) {
        _tcapCommunicationAgent.Initialize(url);
    }

    /**
     * ステータス変更
     * ステータスオブジェクトを変更します。
     *
     * @param nextStatus 設定するステータス
     */
    public void ChangeState(status nextStatus) {
        _nextStatus = nextStatus;
    }

    /**
     * 通信開始
     * TCAP通信を開始します。
     *
     * @return エラーコード
     */
    public long Start() {
        long lRetVal;

        // サーバへ接続する
        lRetVal = _tcapCommunicationAgent.Connect();
        if(lRetVal != FeliCaClient.FC_ERR_SUCCESS) {
            // サーバへ接続失敗した場合、TCAP通信は行わずに終了する。
            _tcapContext.SetErrorMessage(_tcapCommunicationAgent.GetLastErrorStr().getBytes());
            return lRetVal;
        }

        // TCAP通信のメインループ
        lRetVal = FeliCaClient.FC_ERR_SUCCESS;
        do {
            // ステータスが変更された場合、オブジェクトを次のステータスへ変更
            if(_nextStatus != _currentStatus) {
                if(_iState != null) {
                    _iState = null;
                }
                _iState = CreateStatus(_nextStatus);

                /***********************************************************************************/
                /*** TCAP試験用コード ***/
                if(iCASClientTest.IsStarted() && _iState != null) {
                    _iState.SetEventListener(_tcapCommunicationAgent.GetListener());
                }
                /***********************************************************************************/

                _currentStatus = _nextStatus;
            }

            // 設定されているステータスの処理を実行する
            if(_iState != null) {
                lRetVal = _iState.DoExec(this);
                // エラーが発生した場合はステータスを初期化して処理終了。
                if(lRetVal != FeliCaClient.FC_ERR_SUCCESS) {
                    _iState = null;
                }
            }
        } while(_iState != null);

        return lRetVal;
    }

/*
    public abstract boolean ValidatePacketOrder();
*/

    /**
     * TCAP通信コンテキストの取得
     * TCAP通信コンテキストを取得します。
     *
     * @return TCAP通信コンテキスト
     */
    public TCAPContext GetContext() {
        return _tcapContext;
    }

    /**
     * TCAP通信エージェントの取得
     * TCAP通信エージェントを取得します。
     *
     * @return TCAP通信エージェント
     */
    public TCAPCommunicationAgent GetCommunicationAgent() {
        return _tcapCommunicationAgent;
    }

    /**
     * ステータスオブジェクトの構築
     * 指定のステータスを構築します。
     *
     * @param createState 構築するステータス
     *
     * @return ステータスオブジェクト
     */
    public abstract IState CreateStatus(status createState);
}
