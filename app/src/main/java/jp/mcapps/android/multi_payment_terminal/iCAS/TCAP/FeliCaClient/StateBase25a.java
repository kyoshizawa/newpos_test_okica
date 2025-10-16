package jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.FeliCaClient;

import java.util.List;

import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.MsgPacketFormatError;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.MsgReturnCode;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.MsgUnexpectedError;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.TCAPMessage;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.TCAPPacket;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAPTest.iCASClientTest;
import timber.log.Timber;

public class StateBase25a {
    private TCAPMessage _errorMessage = null;
    protected IFeliCaClientEventListener _listener = null;

    public StateBase25a() {}

    private boolean ValidatePacketListOrder(List<TCAPPacket> recvPacketList) {
        boolean bRetVal = true;

        // 受信パケットリスト内のパケット数取得
        int packetNum = recvPacketList.size();

        if(packetNum < 1) {
            // 予期せぬエラー
            bRetVal = false;
        } else {
            int countHP = 0;
            int countFP = 0;

            // 全パケットをチェック
            for(int i = 0; i < packetNum; i++) {
                TCAPPacket tcapPacket = recvPacketList.get(i);

                if(tcapPacket == null) {
                    // 予期せぬエラー
                    return false;
                }

                switch(tcapPacket.GetSubProtocolType()) {
                    case TCAPPacket.TCAP_SPT_HAND_SHAKE:
                        countHP++;
                        break;
                    case TCAPPacket.TCAP_SPT_FAREWELL:
                        countFP++;
                        break;
                    case TCAPPacket.TCAP_SPT_APPLICATION_DATA_TRANSFER:
                    case TCAPPacket.TCAP_SPT_UPDATE_ENTITY:
                    case TCAPPacket.TCAP_SPT_OPERATE_ENTITY:
                        break;
                    case TCAPPacket.TCAP_SPT_ERROR:
                        if((i != 0) || (1 < packetNum)) {
                            // パケットエラー
                            return false;
                        }
                        break;
                    default:
                        return false;
                }
            }

            if((countHP > 1) || (countFP > 1)) {
                // パケットエラー
                bRetVal = false;
            }
        }

        return bRetVal;
    }

    /**
     * Farewellパケット受信ハンドラ(全ステータス共通)
     * Farewellパケットを受信した時の動作を行います。
     *
     * @param packet       受信したFarewellパケット
     * @param stateMachine 現在の状態オブジェクト
     *
     * @return エラーコード
     */
    protected long OnFarewell(RcvPacketBase packet, ClientStateMachine25a stateMachine) {
        TCAPContext tcapContext = stateMachine.GetContext();
        List<TCAPPacket> sendPacketList = tcapContext.GetSendPacketList();

        _errorMessage = null;

        // Farewellがきた場合は何も送信しないため送信パケットはクリアする
        sendPacketList.clear();

        // パケット内のメッセージ数取得
        int messageNum = packet.GetMessageNum();
        TCAPMessage tcapMessage;
        long lRetVal = FeliCaClient.FC_ERR_CLIENT_UNKNOWN;

        // 全てのメッセージをチェックする
        for(int i = 0; i < messageNum; i++) {
            // 中止要求チェック
            if(tcapContext.IsStop()) {
                return FeliCaClient.FC_ERR_CLIENT_CANCEL;
            }

            tcapMessage = packet.GetMessage(i);

            /***********************************************************************************/
            /*** TCAP試験用コード ***/
            if(iCASClientTest.IsStarted() && iCASClientTest.TCAP_TEST_MODE == iCASClientTest.TCAP_TEST_FP_E_053) {
                tcapMessage = null;
            }
            /***********************************************************************************/

            if(tcapMessage == null) {
                MsgUnexpectedError msgUnexpectedError = new MsgUnexpectedError(MsgUnexpectedError.errorMessage.UNEXPECTED_ERROR.getInt(), null, 0);
                SetErrorMessage(msgUnexpectedError);
                return FeliCaClient.FC_ERR_CLIENT_MSG_UNEXPECTED;
            }

            // メッセージタイプチェック
            int mid = (tcapMessage.GetExtension() << 8) | (tcapMessage.GetMessageType() & 0x00FF);
            switch(mid) {
                case RcvPacketBase.MID_RETURN_CODE:
                    Timber.tag("iCAS").d("StateBase25a OnFarewell returnCode=%04x", ((MsgReturnCode)tcapMessage).GetReturnCode());
                    tcapContext.SetReturnCode(((MsgReturnCode)tcapMessage).GetReturnCode());
                    lRetVal = FeliCaClient.FC_ERR_SUCCESS;
                    break;
                default:
                    break;
            }
        }

        // ステータスを「終了」（ステータスなし）に設定
        stateMachine.ChangeState(ClientStateMachine.status.STATUS_NONE);

        return lRetVal;
    }

    /**
     * Errorパケット受信ハンドラ(全ステータス共通)
     * Errorパケットを受信した時の動作を行います。
     *
     * @param packet       受信したErrorパケット
     * @param stateMachine 現在の状態オブジェクト
     *
     * @return エラーコード
     */
    protected long OnError(RcvPacketBase packet, ClientStateMachine25a stateMachine) {
        TCAPContext tcapContext = stateMachine.GetContext();

        _errorMessage = null;

        // パケット内のメッセージ数取得
        int messageNum = packet.GetMessageNum();
        TCAPMessage tcapMessage;

        // 全てのメッセージをチェックする
        for(int i = 0; i < messageNum; i++) {
            // 中止要求チェック
            if(tcapContext.IsStop()) {
                return FeliCaClient.FC_ERR_CLIENT_CANCEL;
            }

            // メッセージ取得
            tcapMessage = packet.GetMessage(i);
            if(tcapMessage == null) {
                MsgUnexpectedError msgUnexpectedError = new MsgUnexpectedError(MsgUnexpectedError.errorMessage.UNEXPECTED_ERROR.getInt(), null, 0);
                SetErrorMessage(msgUnexpectedError);
                return FeliCaClient.FC_ERR_CLIENT_MSG_UNEXPECTED;
            }

            // メッセージタイプチェック
            int mid = (tcapMessage.GetExtension() << 8) | (tcapMessage.GetMessageType() & 0x00FF);
            switch(mid) {
                case RcvPacketBase.MID_PACKET_FORMAT_ERROR:
                case RcvPacketBase.MID_ILLEGAL_STATE_ERROR:
                case RcvPacketBase.MID_UNEXPECTED_ERROR:
                    Timber.tag("iCAS").d("StateBase25a OnError ext=0x%02x mt=0x%02x", (int)tcapMessage.GetExtension(), (int)tcapMessage.GetMessageType());
                    // エラーメッセージを記録
                    if(tcapMessage.GetMessageData() == null) {
                        if(mid == RcvPacketBase.MID_PACKET_FORMAT_ERROR) {
                            tcapContext.SetErrorMessage("PACKET_FORMAT_ERROR".getBytes());
                        } else if(mid == RcvPacketBase.MID_ILLEGAL_STATE_ERROR) {
                            tcapContext.SetErrorMessage("ILLEGAL_STATE_ERROR".getBytes());
                        } else {
                            tcapContext.SetErrorMessage("UNEXPECTED_ERROR".getBytes());
                        }
                    } else {
                        tcapContext.SetErrorMessage(tcapMessage.GetMessageData());
                    }
                    break;
                default:
                    Timber.tag("iCAS").d("StateBase25a OnError default ext=0x%02x mt=0x%02x", (int)tcapMessage.GetExtension(), (int)tcapMessage.GetMessageType());
                    break;
            }
        }

        // サーバからエラーメッセージが来たことをコール元に通知する
        return FeliCaClient.FC_ERR_CLIENT_MSG_SERVER;
    }

    public void SetErrorMessage(TCAPMessage message) { _errorMessage = message; }

    public TCAPMessage GetErrorMessage() {
        return _errorMessage;
    }

    /**
     * 受信パケットリストの検証
     * 受信パケットリストの内容を検証します。
     *
     * @param recvPacketList 受信パケットリスト
     *
     * @return エラーコード
     */
    public long ValidatePacketList(List<TCAPPacket> recvPacketList) {
        // 受信パケットリスト内のパケット数を取得する
        int packetNum = recvPacketList.size();

        TCAPPacket tcapPacket;

        // 全てのパケットをチェック
        for(int i = 0; i < packetNum; i++) {
            tcapPacket = recvPacketList.get(i);
            if(tcapPacket == null) {
                // 予期せぬエラー（内部エラー）
                _errorMessage = new MsgUnexpectedError(MsgUnexpectedError.errorMessage.UNEXPECTED_ERROR.getInt(), null, 0);
                return FeliCaClient.FC_ERR_CLIENT_MSG_UNEXPECTED;
            }

            // バージョンチェック
            if(tcapPacket.GetVersion() != TCAPPacket.TCAP_VERSION_25) {
                // サポート外バージョン
                byte[] msg = new byte[2];
                msg[0] = (byte)tcapPacket.GetMajorVersion();
                msg[1] = (byte)tcapPacket.GetMinorVersion();
                _errorMessage = new MsgPacketFormatError(MsgPacketFormatError.errorMessage.UNSUPORTED_VERSION.getInt(), msg, msg.length);
                return FeliCaClient.FC_ERR_CLIENT_MSG_FORMAT;
            }

            // サブプロトコルタイプチェック
            char spt = tcapPacket.GetSubProtocolType();
            switch (spt) {
                case RcvPacketBase.TCAP_SPT_HAND_SHAKE:
                    recvPacketList.set(i, new RcvPacketHandshake25a(tcapPacket));
                    break;
                case RcvPacketBase.TCAP_SPT_FAREWELL:
                    recvPacketList.set(i, new RcvPacketFarewell25a(tcapPacket));
                    break;
                case RcvPacketBase.TCAP_SPT_ERROR:
                    recvPacketList.set(i, new RcvPacketError25a(tcapPacket));
                    break;
                case RcvPacketBase.TCAP_SPT_APPLICATION_DATA_TRANSFER:
                    recvPacketList.set(i, new RcvPacketApplicationDataTransfer25a(tcapPacket));
                    break;
                case RcvPacketBase.TCAP_SPT_UPDATE_ENTITY:
                    recvPacketList.set(i, new RcvPacketUpdateEntity25a(tcapPacket));
                    break;
                case RcvPacketBase.TCAP_SPT_OPERATE_ENTITY:
                    recvPacketList.set(i, new RcvPacketOperateEntity25a(tcapPacket));
                    break;
                default:
                    byte[] data = new byte[1];
                    data[0] = (byte)spt;
                    // サポート外
                    _errorMessage = new MsgPacketFormatError(MsgPacketFormatError.errorMessage.UNSUPORTED_SUBPROTOCOL.getInt(), data, data.length);
                    return FeliCaClient.FC_ERR_CLIENT_MSG_FORMAT;
            }
        }

        // パケットオーダーチェック
        if(!ValidatePacketListOrder(recvPacketList)) {
            // パケットオーダーエラー
            _errorMessage = new MsgPacketFormatError(MsgPacketFormatError.errorMessage.PACKET_FORMAT_ERROR.getInt(), null, 0);
            return FeliCaClient.FC_ERR_CLIENT_MSG_FORMAT;
        }

        // ここまでくると正常パケット
        return FeliCaClient.FC_ERR_SUCCESS;
    }

    /**
     * サーバとのエラーメッセージ通信(全ステータス共通)
     * サーバにエラーを送信し、サーバからの応答を受信します。
     *
     * @param errorMessage 送信するエラーメッセージ
     * @param stateMachine 現在の状態オブジェクト
     *
     * @return エラーコード
     */
    public long DoErrorTransaction(TCAPMessage errorMessage, ClientStateMachine25a stateMachine) {
        long lRetVal = FeliCaClient.FC_ERR_CLIENT_UNKNOWN;
        TCAPCommunicationAgent tcapCommunicationAgent = stateMachine.GetCommunicationAgent();
        TCAPContext tcapContext = stateMachine.GetContext();
        List<TCAPPacket> sendPacketList = tcapContext.GetSendPacketList();
        List<TCAPPacket> recvPacketList = tcapContext.GetRecvPacketList();

        // 送受信パケットをクリア
        sendPacketList.clear();
        recvPacketList.clear();

        // パケット構築
        TCAPPacket tcapPacket = new TCAPPacket((char)TCAPPacket.TCAP_VERSION_25, (char)TCAPPacket.TCAP_SPT_ERROR);

        /***********************************************************************************/
        /*** TCAP試験用コード ***/
        if(iCASClientTest.IsStarted() && iCASClientTest.TCAP_TEST_MODE == iCASClientTest.TCAP_TEST_EP_E_055) {
            tcapPacket = null;
        }
        /***********************************************************************************/

        if(tcapPacket == null) {
            // メモリ確保失敗
            return FeliCaClient.FC_ERR_CLIENT_NO_MEMORY;
        }

        // パケットに引数のERRORメッセージ追加
        tcapPacket.AddMessage(new TCAPMessage(errorMessage));

        // 送信パケットリストにパケット追加
        if(!sendPacketList.add(tcapPacket)) {
            return lRetVal;
        }

        /***********************************************************************************/
        /*** TCAP試験用コード ***/
        if(iCASClientTest.IsStarted()) {
            if(iCASClientTest.TCAP_TEST_MODE == iCASClientTest.TCAP_TEST_EP_A_009) {
                // ユーザー操作のキャンセル
                _listener.OnCancelAvailable();
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        /***********************************************************************************/

        lRetVal = tcapCommunicationAgent.SendTCAPPacket(sendPacketList, recvPacketList);
        if(lRetVal == FeliCaClient.FC_ERR_CLIENT_HTTP) {
            tcapContext.SetErrorMessage(tcapCommunicationAgent.GetLastErrorStr().getBytes());
            return lRetVal;
        } else if(lRetVal != FeliCaClient.FC_ERR_SUCCESS) {
            tcapContext.SetErrorMessage(tcapCommunicationAgent.GetLastErrorStr().getBytes());
            return lRetVal;
        }

        /***********************************************************************************/
        /*** TCAP試験用コード ***/
        if(iCASClientTest.IsStarted()) {
            if(iCASClientTest.TCAP_TEST_MODE == iCASClientTest.TCAP_TEST_EP_A_012) {
                // ユーザー操作のキャンセル
                _listener.OnCancelAvailable();
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        /***********************************************************************************/

        // エラー時なので何も処理せず受信パケットクリア
        recvPacketList.clear();

        // 送信したエラーメッセージに応じた戻り値を設定する。
        switch (errorMessage.GetMessageType()) {
            case TCAPPacket.TCAP_MSG_MT_UNEXPECTED_ERROR:
                // エラーメッセージをアプリケーションに通知
                if(errorMessage.GetMessageData() != null) {
                    String data = new String(errorMessage.GetMessageData());
                    if(data.length() > 3) {
                        // "02_aaa"の場合、aaaを返却する
                        String subStr = data.substring(3);
                        tcapContext.SetErrorMessage(subStr.getBytes());
                    }
                }
                lRetVal = FeliCaClient.FC_ERR_CLIENT_MSG_UNEXPECTED;
                break;
            case TCAPPacket.TCAP_MSG_MT_PACKET_FORMAT_ERROR:
                lRetVal = FeliCaClient.FC_ERR_CLIENT_MSG_FORMAT;
                break;
            case TCAPPacket.TCAP_MSG_MT_ILLEGAL_STATE_ERROR:
                lRetVal = FeliCaClient.FC_ERR_CLIENT_MSG_ILLEGAL;
                break;
            default:
                lRetVal = FeliCaClient.FC_ERR_CLIENT_UNKNOWN;
                break;
        }

        return lRetVal;
    }

    /**
     * サーバとのキャンセルメッセージ通信(全ステータス共通)
     * サーバにキャンセルメッセージを送信し、サーバからの応答を受信します。
     *
     * @param stateMachine 現在の状態オブジェクト
     *
     * @return エラーコード
     */
    public long DoCancelTransaction(ClientStateMachine25a stateMachine) {
        // 中止の場合、エラーメッセージを送信して処理を終了する。
        MsgUnexpectedError msgUnexpectedError = new MsgUnexpectedError(MsgUnexpectedError.errorMessage.INTERRUPTED_BY_USER.getInt(), null, 0);

        // エラーメッセージ通信実行
        DoErrorTransaction(msgUnexpectedError, stateMachine);

        // キャンセルの場合はHTTPエラーが発生してもCANCELとしてアプリに通知する。
        return FeliCaClient.FC_ERR_CLIENT_CANCEL;
    }
}
