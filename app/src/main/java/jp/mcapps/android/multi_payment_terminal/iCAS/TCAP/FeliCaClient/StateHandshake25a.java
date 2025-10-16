package jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.FeliCaClient;

import java.util.List;
import java.util.Objects;

import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.FeatureOption;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.MsgAccept;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.MsgDevices;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.MsgFeatures;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.MsgIllegalStateError;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.MsgUnexpectedError;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.TCAPMessage;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.TCAPPacket;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAPTest.iCASClientTest;
import jp.mcapps.android.multi_payment_terminal.iCAS.iCASClient;
import timber.log.Timber;

public class StateHandshake25a extends StateBase25a implements IState {
    public StateHandshake25a() {
        super();
    }

    @Override
    public long DoExec(ClientStateMachine stateMachine) {
        long lRetVal;

        if(stateMachine == null) {
            return FeliCaClient.FC_ERR_CLIENT_BADPARAM;
        }

        ClientStateMachine25a stateMachine25a = (ClientStateMachine25a)stateMachine;
        TCAPContext tcapContext = stateMachine25a.GetContext();
        TCAPCommunicationAgent tcapCommunicationAgent = stateMachine25a.GetCommunicationAgent();
        List<TCAPPacket> sendPacketList = tcapContext.GetSendPacketList();
        List<TCAPPacket> recvPacketList = tcapContext.GetRecvPacketList();
        TCAPMessage errorMessage;

        // 中止要求チェック
        if(tcapContext.IsStop()) {
            // 中止の場合、エラーメッセージを送信して処理を終了
            return DoCancelTransaction(stateMachine25a);
        }

        // 送受信バッファをクリア
        sendPacketList.clear();
        recvPacketList.clear();

        // ハンドシェイクメッセージを送信パケットリストに追加
        lRetVal = SetHandshakePacket(sendPacketList, tcapContext.GetDeviceList(), stateMachine25a.GetFeatureList());
        if(lRetVal != FeliCaClient.FC_ERR_SUCCESS) {
            // エラーの場合、中止扱い
            return DoCancelTransaction(stateMachine25a);
        }
        /***********************************************************************************/
        /*** TCAP試験用コード ***/
        if(iCASClientTest.IsStarted()) {
            if(iCASClientTest.TCAP_TEST_MODE == iCASClientTest.TCAP_TEST_EP_A_007) {
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

        // サーバにデータ送信
        lRetVal = tcapCommunicationAgent.SendTCAPPacket(sendPacketList, recvPacketList);
        if(lRetVal == FeliCaClient.FC_ERR_SUCCESS) {
            // 送信成功
        } else if(lRetVal == TCAPPacket.FC_ERR_TCAP_PACKETS_SIZE) {
            // パケットサイズオーバーの場合はバッファオーバーメッセージをサーバに送信して処理終了。
            String strMessage = "Exception: Buffer overflow.";
            MsgUnexpectedError msgUnexpectedError = new MsgUnexpectedError(MsgUnexpectedError.errorMessage.UNEXPECTED_ERROR.getInt(), strMessage.getBytes(), strMessage.getBytes().length);
            return DoErrorTransaction(msgUnexpectedError, stateMachine25a);
        } else {
            errorMessage = tcapCommunicationAgent.GetTCAPMesasge();
            if(errorMessage != null) {
                // メッセージをサーバに送信して処理を終了する
                return DoErrorTransaction(errorMessage, stateMachine25a);
            } else {
                // Httpエラー
                tcapContext.SetErrorMessage(tcapCommunicationAgent.GetLastErrorStr().getBytes());
                return lRetVal;
            }
        }

        // 受信したパケットリストが正常か
        lRetVal = ValidatePacketList(recvPacketList);
        if(lRetVal != FeliCaClient.FC_ERR_SUCCESS) {
            // エラーメッセージを取得
            errorMessage = GetErrorMessage();
            // メッセージをサーバに送信して処理を終了する
            return DoErrorTransaction(errorMessage, stateMachine25a);
        }

        RcvPacketBase rcvPacket = (RcvPacketBase)recvPacketList.get(0);
        if(rcvPacket == null) {
            // 内部エラー発生。メッセージをサーバに送信して処理を終了する
            MsgUnexpectedError msgUnexpectedError = new MsgUnexpectedError(MsgUnexpectedError.errorMessage.UNEXPECTED_ERROR.getInt(), null, 0);
            return DoErrorTransaction(msgUnexpectedError, stateMachine25a);
        }

        char spt = rcvPacket.GetSubProtocolType();

        // サブプロトコルタイプに一致する関数をコールする
        switch(spt) {
            case TCAPPacket.TCAP_SPT_HAND_SHAKE:
                // 受信したパケットがサブプロトコルタイプとして有効か
                lRetVal = rcvPacket.ValidatePacket(tcapContext.GetDeviceList());
                errorMessage = rcvPacket.GetErrorTCAPMessage();
                if(lRetVal == FeliCaClient.FC_ERR_SUCCESS) {
                    // サブプロトコル処理を実行
                    Timber.tag("iCAS").d("OnHandShake start");
                    lRetVal = OnHandShake(rcvPacket, stateMachine25a);
                    errorMessage = GetErrorMessage();
                    Timber.tag("iCAS").d("OnHandShake end %d %s", iCASClient.getErrorCode(lRetVal), errorMessage);
                }
                break;
            case TCAPPacket.TCAP_SPT_ERROR:
                // 受信したパケットがサブプロトコルタイプとして有効か
                lRetVal = rcvPacket.ValidatePacket(tcapContext.GetDeviceList());
                errorMessage = rcvPacket.GetErrorTCAPMessage();
                if(lRetVal == FeliCaClient.FC_ERR_SUCCESS) {
                    // サブプロトコル処理を実行
                    Timber.tag("iCAS").d("StateHandshake OnError start");
                    lRetVal = OnError(rcvPacket, stateMachine25a);
                    errorMessage = GetErrorMessage();
                    Timber.tag("iCAS").d("StateHandshake OnError end %d %s", iCASClient.getErrorCode(lRetVal), errorMessage);
                }
                break;
            default:
                // 予期せぬSPTの場合はエラーステータスへ遷移
                MsgIllegalStateError msgIllegalStateError = new MsgIllegalStateError(MsgIllegalStateError.ILLEGAL_STATE);
                return DoErrorTransaction(msgIllegalStateError, stateMachine25a);
        }

        // 中止要求の場合はErrorを除き中止処理実行
        if(lRetVal == FeliCaClient.FC_ERR_CLIENT_CANCEL) {
            if(spt != TCAPPacket.TCAP_SPT_ERROR) {
                lRetVal = DoCancelTransaction(stateMachine25a);
            }
            return lRetVal;
        // 致命的エラーが発生した場合はメッセージをサーバへ送信して処理を終了する
        } else if(lRetVal == FeliCaClient.FC_ERR_CLIENT_MSG_UNEXPECTED
               || lRetVal == FeliCaClient.FC_ERR_CLIENT_MSG_FORMAT
               || lRetVal == FeliCaClient.FC_ERR_CLIENT_MSG_ILLEGAL) {
            return DoErrorTransaction(errorMessage, stateMachine25a);
        }

        // エラーが発生した場合
        if(lRetVal != FeliCaClient.FC_ERR_SUCCESS) {
            return lRetVal;
        }

        // 中止要求チェック
        if(tcapContext.IsStop()) {
            // 中止の場合はエラーメッセージを送信して処理を終了
            return DoCancelTransaction(stateMachine25a);
        }

        // パケット数が複数の場合は先頭(Handshake)パケットを削除してNEUTRALステータスへ遷移
        if(1 < recvPacketList.size()) {
            recvPacketList.remove(0);
        // 受信パケットがHandshakeのみの場合は空応答をしてNEUTRALステータスへ遷移
        } else {
            /***********************************************************************************/
            /*** TCAP試験用コード ***/
            if(iCASClientTest.IsStarted()) {
                if(iCASClientTest.TCAP_TEST_MODE == iCASClientTest.TCAP_TEST_HP_A_009) {
                    _listener.OnCancelAvailable();
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else if(iCASClientTest.TCAP_TEST_MODE == iCASClientTest.TCAP_TEST_EP_A_008) {
                    // NeutralステータスでERRORパケットを受信するため、不正なパケットを送信する
                    SetHandshakePacket(sendPacketList, tcapContext.GetDeviceList(), stateMachine25a.GetFeatureList());
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
            recvPacketList.clear();
//            lRetVal = tcapCommunicationAgent.SendTCAPPacket(sendPacketList);
            lRetVal = tcapCommunicationAgent.SendTCAPPacket(sendPacketList, recvPacketList);
            if(lRetVal != FeliCaClient.FC_ERR_SUCCESS) {
                tcapContext.SetErrorMessage(tcapCommunicationAgent.GetLastErrorStr().getBytes());
            }
        }

        return lRetVal;
    }

    @Override
    public void Release() {
        // 処理なし
    }

    /**
     * Handshakeパケットの設定
     * Handshakeパケットを送信パケットリストに設定します。
     *
     * @param sendPacketList 送信パケットリスト
     * @param deviceList     デバイスリスト
     * @param featuresList   Featureリスト
     *
     * @return エラーコード
     */
    public long SetHandshakePacket(List<TCAPPacket> sendPacketList, List<DeviceElement> deviceList, final List<MsgFeatures> featuresList) {
        long lRetVal;

        // 送信パケットクリア
        sendPacketList.clear();

        // パケット構築
        TCAPPacket tcapPacket = new TCAPPacket((char)TCAPPacket.TCAP_VERSION_25, (char)TCAPPacket.TCAP_SPT_HAND_SHAKE);
        // パケットにCLIENT_HELLOメッセージ追加
        tcapPacket.AddMessage(new TCAPMessage((char)TCAPPacket.TCAP_MSG_EXT_STANDARD, (char)TCAPPacket.TCAP_MSG_DEVICEID_INVALID, (char)TCAPPacket.TCAP_MSG_MT_CLIENT_HELLO));

        // パケットにDEVICESメッセージ追加
        MsgDevices msgDevices = new MsgDevices();
        for(DeviceElement device : deviceList) {
            msgDevices.AddData(device.GetDeviceID(), device.GetType(), device.GetName());
        }
        tcapPacket.AddMessage(msgDevices);

        // パケットにFEATURESメッセージ追加
        for(MsgFeatures features : featuresList) {
            tcapPacket.AddMessage(new MsgFeatures(features));
        }

        // パケットにCLIENT_HELLO_DONEメッセージ追加
        tcapPacket.AddMessage(new TCAPMessage((char)TCAPPacket.TCAP_MSG_EXT_STANDARD, (char)TCAPPacket.TCAP_MSG_DEVICEID_INVALID, (char)TCAPPacket.TCAP_MSG_MT_CLIENT_HELLO_DONE));

        // 送信パケットリストにパケット追加
        sendPacketList.add(tcapPacket);

        lRetVal = FeliCaClient.FC_ERR_SUCCESS;

        return lRetVal;
    }

    @Override
    protected long OnError(RcvPacketBase packet, ClientStateMachine25a stateMachine25a) {
        return super.OnError(packet, stateMachine25a);
    }

    /**
     * Handshakeパケット受信ハンドラ
     * Handshakeパケットを受信した時の動作を行います。
     *
     * @param packet          受信したErrorパケット
     * @param stateMachine25a 現在の状態オブジェクト
     *
     * @return エラーコード
     */
    private long OnHandShake(RcvPacketBase packet, ClientStateMachine25a stateMachine25a) {
        long lRetVal = FeliCaClient.FC_ERR_SUCCESS;
        TCAPContext tcapContext = stateMachine25a.GetContext();
        int messageNum = packet.GetMessageNum();
        TCAPMessage message;

        SetErrorMessage(null);

        for(int i = 0; i < messageNum; i++) {
            // 中止要求チェック
            if(tcapContext.IsStop()) {
                return FeliCaClient.FC_ERR_CLIENT_CANCEL;
            }

            // メッセージ取得
            message = packet.GetMessage(i);
            if(message == null) {
                MsgUnexpectedError msgUnexpectedError = new MsgUnexpectedError(MsgUnexpectedError.errorMessage.UNEXPECTED_ERROR.getInt(), null, 0);
                SetErrorMessage(msgUnexpectedError);
                return FeliCaClient.FC_ERR_CLIENT_MSG_UNEXPECTED;
            }

            // メッセージタイプチェック
            int mid = (message.GetExtension() << 8) | (message.GetMessageType());
            switch(mid) {
                case RcvPacketBase.MID_WARNING:
                case RcvPacketBase.MID_SERVER_HELLO:
                case RcvPacketBase.MID_SERVER_HELLO_DONE:
                    break;
                case RcvPacketBase.MID_ACCEPT:
                    for(MsgFeatures msgFeatures : stateMachine25a.GetFeatureList()) {
                        if(!ValidateFeaturesList((MsgAccept)message, msgFeatures)) {
                            // UNEXPECTED_ERRORメッセージを送信パケットに設定し、ステータスを「エラー」に変更
                            MsgUnexpectedError msgUnexpectedError = new MsgUnexpectedError(MsgUnexpectedError.errorMessage.HANDSHAKE_ERROR.getInt(), null, 0);
                            SetErrorMessage(msgUnexpectedError);
                            return FeliCaClient.FC_ERR_CLIENT_MSG_UNEXPECTED;
                        } else {
                            Timber.tag("iCAS").d("OnHandShake accept version %04X", (int)((MsgAccept)message).GetTCAPProtocolVersion());
                            tcapContext.SetVersion(((MsgAccept)message).GetTCAPProtocolVersion());
                        }
                    }
                    break;
                default:
                    // 事前にチェックしているため上記以外が来ることはないので、内部エラーとしてサーバに通知
                    MsgUnexpectedError msgUnexpectedError = new MsgUnexpectedError(MsgUnexpectedError.errorMessage.UNEXPECTED_ERROR.getInt(), null, 0);
                    SetErrorMessage(msgUnexpectedError);
                    return FeliCaClient.FC_ERR_CLIENT_MSG_UNEXPECTED;
            }
        }

        // Neutralステータスへ遷移
        stateMachine25a.ChangeState(ClientStateMachine.status.STATUS_NEUTRAL);

        return lRetVal;
    }

    /**
     * 受信したフィーチャーリストが正しいかどうか
     *
     * @param messageAccept   サーバから受信したAcceptメッセージ
     * @param messageFeatures サーバに送付したFeaturesメッセージ
     *
     * @return true :正しい
     *         false:誤り
     */
    public boolean ValidateFeaturesList(final MsgAccept messageAccept, final MsgFeatures messageFeatures) {
        boolean bRetVal = false;

        if(messageFeatures == null) {
            // 内部動作エラー
        } else if(messageFeatures.GetVersion() != messageAccept.GetTCAPProtocolVersion()) {
            // バージョン違い
        } else if(messageFeatures.GetOptionNum() != messageAccept.GetOptionNum()) {
            // オプション数違い
        } else {
            bRetVal = true;
            int optionNum = messageAccept.GetOptionNum();
            FeatureOption featureOption = null;

            for(int i = 0; i < optionNum; i++) {
                String featureOptionName = new String(Objects.requireNonNull(featureOption).GetOption());
                String acceptOptionName = new String(messageAccept.GetOptionName(i));
                featureOption = messageFeatures.OptionList().get(i);
                if(featureOption == null) {
                    bRetVal = false;
                    break;
                } else if(featureOption.GetOptionLength() != messageAccept.GetOptionLength(i)) {
                    bRetVal = false;
                    break;
                } else if(!featureOptionName.equals(acceptOptionName)) {
                    bRetVal = false;
                    break;
                }
            }

        }

        return bRetVal;
    }

    @Override
    public void SetEventListener(IFeliCaClientEventListener listener) {
        _listener = listener;
    }
}
