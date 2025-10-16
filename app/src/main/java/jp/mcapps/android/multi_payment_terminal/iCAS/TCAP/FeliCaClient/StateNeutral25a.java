package jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.FeliCaClient;

import java.util.List;
import java.util.Objects;

import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.MsgCloseRWStatus;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.MsgFeliCaExCommand;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.MsgFeliCaExCommandThruRW;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.MsgFeliCaPreCommand;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.MsgFeliCaPreCommandThruRW;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.MsgFeliCaResponse;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.MsgFeliCaResponseThruRW;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.MsgIllegalStateError;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.MsgOpenRWStatus;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.MsgOperateDevice;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.MsgRequestID;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.MsgSetFeliCaTimeout;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.MsgSetNetworkTimeout;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.MsgSetRetryCount;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.MsgUnexpectedError;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.TCAPMessage;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.TCAPPacket;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAPTest.iCASClientTest;
import jp.mcapps.android.multi_payment_terminal.iCAS.iCASClient;
import timber.log.Timber;

public class StateNeutral25a extends StateBase25a implements IState {
    private boolean _bFarewellDone;         // Farewellパケットを受信したかどうか


    @Override
    public long DoExec(ClientStateMachine stateMachine) {
        long lRetVal = FeliCaClient.FC_ERR_CLIENT_UNKNOWN;

        _bFarewellDone = false;

        if(stateMachine == null) {
            return FeliCaClient.FC_ERR_CLIENT_BADPARAM;
        }

        // ダウンキャスト
        ClientStateMachine25a stateMachine25a = (ClientStateMachine25a)stateMachine;

        TCAPCommunicationAgent tcapCommunicationAgent = stateMachine25a.GetCommunicationAgent();
        TCAPContext tcapContext = stateMachine25a.GetContext();
        List<TCAPPacket> sendPacketList = tcapContext.GetSendPacketList();
        List<TCAPPacket> recvPacketList = tcapContext.GetRecvPacketList();
        TCAPMessage errorMessage;

/*      一番下でSendPacketする際に受信処理もしているためここは不要
        // パケットを受信していない場合
        if(recvPacketList.size() < 1) {
            // サーバからデータ受信
            lRetVal = tcapCommunicationAgent.ReceiveTCAPPacket(recvPacketList, errorMessage);
            if(lRetVal == FeliCaClient.FC_ERR_SUCCESS) {
                // パケット受信成功
            } else if(lRetVal == FeliCaClient.FC_ERR_CLIENT_HTTP) {
                // HTTPエラー
                tcapContext.SetErrorMessage(tcapCommunicationAgent.GetLastErrorStr().getBytes());
                return lRetVal;
            } else {
                // その他エラーの場合、メッセージをサーバに送信して処理を終了する
                return DoErrorTransaction(errorMessage, stateMachine25a);
            }

            // 受信したパケットリストが正常か
            lRetVal = ValidatePacketList(recvPacketList);
            if(lRetVal != FeliCaClient.FC_ERR_SUCCESS) {
                // エラーメッセージを取得
                errorMessage = GetErrorMessage();
                // メッセージをサーバに送信して処理を終了する
                return DoErrorTransaction(errorMessage, stateMachine25a);
            }
        }
*/

        // 受信パケットループ
        for(TCAPPacket recvPacket : recvPacketList) {
            char spt = recvPacket.GetSubProtocolType();

            switch(spt) {
                case TCAPPacket.TCAP_SPT_FAREWELL:
                    RcvPacketFarewell25a farewellPacket = new RcvPacketFarewell25a(recvPacket);
                    // 受信したパケットがサブプロトコルタイプとして有効か
                    lRetVal = farewellPacket.ValidatePacket(tcapContext.GetDeviceList());
                    errorMessage = farewellPacket.GetErrorTCAPMessage();
                    if(lRetVal == FeliCaClient.FC_ERR_SUCCESS) {
                        // サブプロトコル処理を実行
                        Timber.tag("iCAS").d("OnFarewell start");
                        lRetVal = OnFarewell(farewellPacket, stateMachine25a);
                        errorMessage = GetErrorMessage();
                        Timber.tag("iCAS").d("OnFarewell end %d %s", iCASClient.getErrorCode(lRetVal), errorMessage);
                    }
                    break;
                case TCAPPacket.TCAP_SPT_ERROR:
                    RcvPacketError25a errorPacket = new RcvPacketError25a(recvPacket);
                    // 受信したパケットがサブプロトコルタイプとして有効か
                    lRetVal = errorPacket.ValidatePacket(tcapContext.GetDeviceList());
                    errorMessage = errorPacket.GetErrorTCAPMessage();
                    if(lRetVal == FeliCaClient.FC_ERR_SUCCESS) {
                        // サブプロトコル処理を実行
                        Timber.tag("iCAS").d("StateNeutral OnError start");
                        lRetVal = OnError(errorPacket, stateMachine25a);
                        errorMessage = GetErrorMessage();
                        Timber.tag("iCAS").d("StateNeutral OnError end %d %s", iCASClient.getErrorCode(lRetVal), errorMessage);
                    }
                    break;
                case TCAPPacket.TCAP_SPT_OPERATE_ENTITY:
                    RcvPacketOperateEntity25a operateEntityPacket = new RcvPacketOperateEntity25a(recvPacket);
                    // 受信したパケットがサブプロトコルタイプとして有効か
                    lRetVal = operateEntityPacket.ValidatePacket(tcapContext.GetDeviceList());
                    errorMessage = operateEntityPacket.GetErrorTCAPMessage();
                    if(lRetVal == FeliCaClient.FC_ERR_SUCCESS) {
                        // サブプロトコル処理を実行
                        Timber.tag("iCAS").d("OnOperateEntity start");
                        lRetVal = OnOperateEntity(operateEntityPacket, stateMachine25a);
                        errorMessage = GetErrorMessage();
                        Timber.tag("iCAS").d("OnOperateEntity end %d %s", iCASClient.getErrorCode(lRetVal), errorMessage);
                    }
                    break;
                case TCAPPacket.TCAP_SPT_APPLICATION_DATA_TRANSFER:
                    RcvPacketApplicationDataTransfer25a applicationDataTransferPacket = new RcvPacketApplicationDataTransfer25a(recvPacket);
                    // 受信したパケットがサブプロトコルタイプとして有効か
                    lRetVal = applicationDataTransferPacket.ValidatePacket(tcapContext.GetDeviceList());
                    errorMessage = applicationDataTransferPacket.GetErrorTCAPMessage();
                    if(lRetVal == FeliCaClient.FC_ERR_SUCCESS) {
                        // サブプロトコル処理を実行
                        Timber.tag("iCAS").d("OnApplicationDataTransfer start");
                        lRetVal = OnApplicationDataTransfer(applicationDataTransferPacket, stateMachine25a);
                        errorMessage = GetErrorMessage();
                        Timber.tag("iCAS").d("OnApplicationDataTransfer end %d %s", iCASClient.getErrorCode(lRetVal), errorMessage);
                    }
                    break;
                case TCAPPacket.TCAP_SPT_UPDATE_ENTITY:
                    RcvPacketUpdateEntity25a updateEntityPacket = new RcvPacketUpdateEntity25a(recvPacket);
                    // 受信したパケットがサブプロトコルタイプとして有効か
                    lRetVal = updateEntityPacket.ValidatePacket(tcapContext.GetDeviceList());
                    errorMessage = updateEntityPacket.GetErrorTCAPMessage();
                    if(lRetVal == FeliCaClient.FC_ERR_SUCCESS) {
                        // サブプロトコル処理を実行
                        Timber.tag("iCAS").d("OnUpdateEntity start");
                        lRetVal = OnUpdateEntity(updateEntityPacket, stateMachine25a);
                        errorMessage = GetErrorMessage();
                        Timber.tag("iCAS").d("OnUpdateEntity end %d %s", iCASClient.getErrorCode(lRetVal), errorMessage);
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
                break;
            }
        }

        // 中止要求チェック
        if(tcapContext.IsStop()) {
            // 中止の場合はエラーメッセージを送信して処理を終了
            return DoCancelTransaction(stateMachine25a);
        }

        recvPacketList.clear();

        // パケットリストを送信
        if(0 < sendPacketList.size()) {
//            lRetVal = tcapCommunicationAgent.SendTCAPPacket(sendPacketList);
            lRetVal = tcapCommunicationAgent.SendTCAPPacket(sendPacketList, recvPacketList);
            if(lRetVal == FeliCaClient.FC_ERR_SUCCESS) {
                // 送信成功したので、PRE_COMMANDの持つパラメータを削除する
                stateMachine25a.GetParamList().clear();
                // 受信したパケットリストが正常か
                lRetVal = ValidatePacketList(recvPacketList);
                if(lRetVal != FeliCaClient.FC_ERR_SUCCESS) {
                    // エラーメッセージを取得
                    errorMessage = GetErrorMessage();
                    // メッセージをサーバに送信して処理を終了する
                    return DoErrorTransaction(errorMessage, stateMachine25a);
                }
            } else if(lRetVal == TCAPPacket.FC_ERR_TCAP_PACKETS_SIZE) {
                // パケットサイズオーバの場合は、バッファオーバメッセージをサーバに送信し、処理終了。
                String message = "Exception: Buffer overflow.";
                MsgUnexpectedError msgUnexpectedError = new MsgUnexpectedError(MsgUnexpectedError.errorMessage.UNEXPECTED_ERROR.getInt(), message.getBytes(), message.getBytes().length);
                return DoErrorTransaction(msgUnexpectedError, stateMachine25a);
            } else {
                errorMessage = tcapCommunicationAgent.GetTCAPMesasge();
                if(errorMessage != null) {
                    // メッセージをサーバに送信して処理を終了する
                    return DoErrorTransaction(errorMessage, stateMachine25a);       // TCAP試験 FP-E-004対応
                } else {
                    tcapContext.SetErrorMessage(tcapCommunicationAgent.GetLastErrorStr().getBytes());
                }
            }
        }

        return lRetVal;
    }

    @Override
    public void Release() {
        // 処理なし
    }

    @Override
    protected long OnFarewell(RcvPacketBase packet, ClientStateMachine25a stateMachine25a) {
        // Farewell後にこのパケットが来た場合、状態遷移不正メッセージを設定し、ステータスを「エラー」に変更
        if(_bFarewellDone) {
            // 事前にエラーチェックされているためこのパスは通らないが念のため
            MsgIllegalStateError msgIllegalStateError = new MsgIllegalStateError(MsgIllegalStateError.ILLEGAL_STATE);
            SetErrorMessage(msgIllegalStateError);
            return FeliCaClient.FC_ERR_CLIENT_MSG_ILLEGAL;
        }

        _bFarewellDone = true;

        return super.OnFarewell(packet, stateMachine25a);
    }

    @Override
    protected long OnError(RcvPacketBase packet, ClientStateMachine25a stateMachine25a) {
        return super.OnError(packet, stateMachine25a);
    }

    /**
     * OperateEntityパケット受信ハンドラ
     * OperateEntityパケットを受信した時の動作を行います。
     *
     * @param packet          受信したOperateEntityパケット
     * @param stateMachine25a 現在の状態オブジェクト
     *
     * @return エラーコード
     */
    private long OnOperateEntity(RcvPacketBase packet, ClientStateMachine25a stateMachine25a) {
        TCAPContext tcapContext = stateMachine25a.GetContext();
        List<TCAPPacket> sendPacketList = tcapContext.GetSendPacketList();
        List<DeviceElement> deviceList = tcapContext.GetDeviceList();

        SetErrorMessage(null);

        // Farewell後にこのパケットが来た場合、状態遷移不正メッセージを設定し、ステータスを「エラー」に変更
        if(_bFarewellDone) {
            MsgIllegalStateError msgIllegalStateError = new MsgIllegalStateError(MsgIllegalStateError.ILLEGAL_STATE);
            SetErrorMessage(msgIllegalStateError);
            return FeliCaClient.FC_ERR_CLIENT_MSG_ILLEGAL;
        }

        int messageNum = packet.GetMessageNum();
        TCAPMessage message;
        FeliCaChipWrapper feliCaChip = null;
        DeviceWrapper device = null;
        TCAPPacket replyPacket = new TCAPPacket((char)TCAPPacket.TCAP_VERSION_25, (char)TCAPPacket.TCAP_SPT_OPERATE_ENTITY);
        long lRetVal = FeliCaClient.FC_ERR_CLIENT_UNKNOWN;

        for(int i = 0; i < messageNum; i++) {
            // 中止要求チェック
            if(tcapContext.IsStop()) {
                return FeliCaClient.FC_ERR_CLIENT_CANCEL;
            }

            // メッセージ取得
            message = packet.GetMessage(i);

            /***********************************************************************************/
            /*** TCAP試験用コード ***/
            if(iCASClientTest.IsStarted() && iCASClientTest.TCAP_TEST_MODE == iCASClientTest.TCAP_TEST_OEP_E_048) {
                message = null;
            }
            /***********************************************************************************/

            if(message == null) {
                MsgUnexpectedError msgUnexpectedError = new MsgUnexpectedError(MsgUnexpectedError.errorMessage.UNEXPECTED_ERROR.getInt(), null, 0);
                SetErrorMessage(msgUnexpectedError);
                return FeliCaClient.FC_ERR_CLIENT_MSG_UNEXPECTED;
            }

            // メッセージタイプチェック
            int mid = (message.GetExtension() << 8) | (message.GetMessageType());
            switch(mid) {
                case RcvPacketBase.MID_WARNING:
                    Timber.tag("iCAS").d("WARNING");
                    break;
                case RcvPacketBase.MID_OPERATE_DEVIDE:
                    for(DeviceElement deviceElement : deviceList) {
                        if(message.GetDeviceID() == deviceElement.GetDeviceID()) {
                            device = (DeviceWrapper)deviceElement;
                            break;
                        }
                    }
                    if(device == null) {
                        MsgUnexpectedError msgUnexpectedError = new MsgUnexpectedError(MsgUnexpectedError.errorMessage.UNEXPECTED_ERROR.getInt(), null, 0);
                        SetErrorMessage(msgUnexpectedError);
                        return FeliCaClient.FC_ERR_CLIENT_MSG_UNEXPECTED;
                    }

                    // デバイスに対して、パラメータとデータを通知
                    Timber.tag("iCAS").d("OPERATE_DEVIDE");
                    lRetVal = device.Operate(replyPacket, ((MsgOperateDevice)message).GetParameterName(), ((MsgOperateDevice)message).GetData(), ((MsgOperateDevice)message).GetDataLength());
                    if(lRetVal == FeliCaClient.FC_ERR_CLIENT_MSG_FORMAT) {
                        MsgUnexpectedError msgUnexpectedError = new MsgUnexpectedError(MsgUnexpectedError.errorMessage.OPERATE_DEVICE_FAILED.getInt(), device.GetResponse(), device.GetResponseLength());
                        SetErrorMessage(msgUnexpectedError);
                        return FeliCaClient.FC_ERR_CLIENT_MSG_UNEXPECTED;
                    } else if(lRetVal != FeliCaClient.FC_ERR_SUCCESS) {
                        MsgUnexpectedError msgUnexpectedError = new MsgUnexpectedError(MsgUnexpectedError.errorMessage.UNEXPECTED_ERROR.getInt(), device.GetResponse(), device.GetResponseLength());
                        SetErrorMessage(msgUnexpectedError);
                        return FeliCaClient.FC_ERR_CLIENT_MSG_UNEXPECTED;
                    }
                    break;
                case RcvPacketBase.MID_OPEN_RW_REQUEST:
                    for(DeviceElement deviceElement : deviceList) {
                        if(message.GetDeviceID() == deviceElement.GetDeviceID()) {
                            feliCaChip = (FeliCaChipWrapper)deviceElement;
                            break;
                        }
                    }
                    if(feliCaChip == null) {
                        MsgUnexpectedError msgUnexpectedError = new MsgUnexpectedError(MsgUnexpectedError.errorMessage.UNEXPECTED_ERROR.getInt(), null, 0);
                        SetErrorMessage(msgUnexpectedError);
                        return FeliCaClient.FC_ERR_CLIENT_MSG_UNEXPECTED;
                    }

                    // チップオープン
                    Timber.tag("iCAS").d("OPEN_RW_REQUEST");
                    if(feliCaChip.Open() != FeliCaClient.FC_ERR_SUCCESS) {
                        // パケットにOPEN_RW_STATUS メッセージ追加
                        replyPacket.AddMessage(new MsgOpenRWStatus(message.GetDeviceID(), (char)MsgOpenRWStatus.STATUS_FAILED));
                        sendPacketList.add(new TCAPPacket(replyPacket));
                        return FeliCaClient.FC_ERR_CLIENT_FAILURE;
                    } else {
                        replyPacket.AddMessage(new MsgOpenRWStatus(message.GetDeviceID(), (char)MsgOpenRWStatus.STATUS_SUCCESS));
                    }
                    lRetVal = FeliCaClient.FC_ERR_SUCCESS;
                    break;
                case RcvPacketBase.MID_CLOSE_RW_REQUEST:
                    for(DeviceElement deviceElement : deviceList) {
                        if(message.GetDeviceID() == deviceElement.GetDeviceID()) {
                            feliCaChip = (FeliCaChipWrapper)deviceElement;
                            break;
                        }
                    }
                    if(feliCaChip == null) {
                        MsgUnexpectedError msgUnexpectedError = new MsgUnexpectedError(MsgUnexpectedError.errorMessage.UNEXPECTED_ERROR.getInt(), null, 0);
                        SetErrorMessage(msgUnexpectedError);
                        return FeliCaClient.FC_ERR_CLIENT_MSG_UNEXPECTED;
                    }

                    // チップクローズ
                   Timber.tag("iCAS").d("CLOSE_RW_REQUEST");
                   if(feliCaChip.Close() != FeliCaClient.FC_ERR_SUCCESS) {
                        // パケットに CLOSE_RW_STATUS メッセージ追加
                        replyPacket.AddMessage(new MsgCloseRWStatus(message.GetDeviceID(), (char)MsgCloseRWStatus.STATUS_FAILED));
                        sendPacketList.add(new TCAPPacket(replyPacket));
                        return FeliCaClient.FC_ERR_CLIENT_FAILURE;
                    } else {
                        replyPacket.AddMessage(new MsgCloseRWStatus(message.GetDeviceID(), (char)MsgCloseRWStatus.STATUS_SUCCESS));
                    }
                    lRetVal = FeliCaClient.FC_ERR_SUCCESS;
                    break;
                default:
                    break;
            }
        }

        if(0 < replyPacket.GetMessageNum()) {
            sendPacketList.add(new TCAPPacket(replyPacket));
        }

        return lRetVal;
    }

    /**
     * ApplicationDataTransferパケット受信ハンドラ
     * ApplicationDataTransferパケットを受信した時の動作を行います。
     *
     * @param packet          受信したApplicationDataTransferパケット
     * @param stateMachine25a 現在の状態オブジェクト
     *
     * @return エラーコード
     */
    private long OnApplicationDataTransfer(RcvPacketBase packet, ClientStateMachine25a stateMachine25a) {
        TCAPContext tcapContext = stateMachine25a.GetContext();
        List<TCAPPacket> sendPacketList = tcapContext.GetSendPacketList();
        List<DeviceElement> deviceList = tcapContext.GetDeviceList();
        List<FeliCaParam> paramList = stateMachine25a.GetParamList();
        String str = "";

        SetErrorMessage(null);

        // Farewell後にこのパケットが来た場合、状態遷移不正メッセージを設定してステータスをエラーに変更
        if(_bFarewellDone) {
            MsgIllegalStateError msgIllegalStateError = new MsgIllegalStateError(MsgIllegalStateError.ILLEGAL_STATE);
            SetErrorMessage(msgIllegalStateError);
            return FeliCaClient.FC_ERR_CLIENT_MSG_ILLEGAL;
        }

        int messageNum = packet.GetMessageNum();
        TCAPMessage message;
        FeliCaChipWrapper feliCaChip = null;
        long lRetVal = FeliCaClient.FC_ERR_CLIENT_UNKNOWN;
        TCAPPacket replyPacket = new TCAPPacket((char)TCAPPacket.TCAP_VERSION_25, (char)TCAPPacket.TCAP_SPT_APPLICATION_DATA_TRANSFER);

        for(int i = 0; i < messageNum; i++) {
            // 中止要求チェック
            if(tcapContext.IsStop()) {
                return FeliCaClient.FC_ERR_CLIENT_CANCEL;
            }

            // メッセージ取得
            message = packet.GetMessage(i);

            /***********************************************************************************/
            /*** TCAP試験用コード ***/
            if(iCASClientTest.IsStarted() && iCASClientTest.TCAP_TEST_MODE == iCASClientTest.TCAP_TEST_ADTP_E_073) {
                message = null;
            }
            /***********************************************************************************/

            if(message == null) {
                MsgUnexpectedError msgUnexpectedError = new MsgUnexpectedError(MsgUnexpectedError.errorMessage.UNEXPECTED_ERROR.getInt(), null, 0);
                SetErrorMessage(msgUnexpectedError);
                return FeliCaClient.FC_ERR_CLIENT_MSG_UNEXPECTED;
            }

            // メッセージタイプチェック
            int mid = (message.GetExtension() << 8) | (message.GetMessageType());
            switch(mid) {
                case RcvPacketBase.MID_WARNING:
                    break;
                case RcvPacketBase.MID_FELICA_COMMAND:
                    for(DeviceElement deviceElement : deviceList) {
                        if(message.GetDeviceID() == deviceElement.GetDeviceID()) {
                            feliCaChip = (FeliCaChipWrapper)deviceElement;
                            break;
                        }
                    }
                    if(feliCaChip == null) {
                        MsgUnexpectedError msgUnexpectedError = new MsgUnexpectedError(MsgUnexpectedError.errorMessage.UNEXPECTED_ERROR.getInt(), null, 0);
                        SetErrorMessage(msgUnexpectedError);
                        return FeliCaClient.FC_ERR_CLIENT_MSG_UNEXPECTED;
                    }

                    str = "";
                    for(int idx=0; idx<20; idx++) {
                        if(message.GetLength() < idx+1) {
                            break;
                        }
                        str += String.format("%02X", message.GetMessageData()[idx]);
                    }
                    Timber.tag("iCAS").d("FeliCa Command %s", str);
                    lRetVal = feliCaChip.Execute(message.GetMessageData(), message.GetLength());
                    if(lRetVal != FeliCaClient.FC_ERR_SUCCESS) {
                        // パケットにFELICA_ERROR メッセージ追加
                        replyPacket.AddMessage(new TCAPMessage(message.GetExtension(), message.GetDeviceID(), (char)TCAPPacket.TCAP_MSG_MT_FELICA_ERROR));
                        sendPacketList.add(new TCAPPacket(replyPacket));
                        return FeliCaClient.FC_ERR_CLIENT_FAILURE;
                    } else {
                        // パケットにFELICA_RESPONSE メッセージ追加
                        replyPacket.AddMessage(new MsgFeliCaResponse(message.GetDeviceID(), feliCaChip.GetResponse(), feliCaChip.GetResponseLength()));
                    }
                    lRetVal = FeliCaClient.FC_ERR_SUCCESS;
                    break;
                case RcvPacketBase.MID_FELICA_COMMAND_THRURW:
                    for(DeviceElement deviceElement : deviceList) {
                        if(message.GetDeviceID() == deviceElement.GetDeviceID()) {
                            feliCaChip = (FeliCaChipWrapper)deviceElement;
                            break;
                        }
                    }
                    if(feliCaChip == null) {
                        MsgUnexpectedError msgUnexpectedError = new MsgUnexpectedError(MsgUnexpectedError.errorMessage.UNEXPECTED_ERROR.getInt(), null, 0);
                        SetErrorMessage(msgUnexpectedError);
                        return FeliCaClient.FC_ERR_CLIENT_MSG_UNEXPECTED;
                    }

                    str = "";
                    for(int idx=0; idx<20; idx++) {
                        if(message.GetLength() < idx+1) {
                            break;
                        }
                        str += String.format("%02X", message.GetMessageData()[idx]);
                    }
                    Timber.tag("iCAS").d("FeliCa CommandThru %s", str);
                    lRetVal = feliCaChip.ExecuteThru(message.GetMessageData(), message.GetLength());
                    if(lRetVal != FeliCaClient.FC_ERR_SUCCESS) {
                        // パケットにFELICA_ERROR_THRURW メッセージ追加
                        replyPacket.AddMessage(new TCAPMessage(message.GetExtension(), message.GetDeviceID(), (char)TCAPPacket.TCAP_MSG_MT_FELICA_ERROR_THRURW));
                        sendPacketList.add(new TCAPPacket(replyPacket));
                        return FeliCaClient.FC_ERR_CLIENT_FAILURE;
                    } else {
                        // パケットにFELICA_RESPONSE メッセージ追加
                        replyPacket.AddMessage(new MsgFeliCaResponseThruRW(message.GetDeviceID(), feliCaChip.GetResponse(), feliCaChip.GetResponseLength()));
                    }
                    lRetVal = FeliCaClient.FC_ERR_SUCCESS;
                    break;
                case RcvPacketBase.MID_FELICA_PRECOMMAND:
                    for(DeviceElement deviceElement : deviceList) {
                        if(message.GetDeviceID() == deviceElement.GetDeviceID()) {
                            feliCaChip = (FeliCaChipWrapper)deviceElement;
                            break;
                        }
                    }
                    if(feliCaChip == null) {
                        MsgUnexpectedError msgUnexpectedError = new MsgUnexpectedError(MsgUnexpectedError.errorMessage.UNEXPECTED_ERROR.getInt(), null, 0);
                        SetErrorMessage(msgUnexpectedError);
                        return FeliCaClient.FC_ERR_CLIENT_MSG_UNEXPECTED;
                    }

                    MsgFeliCaPreCommand messagePre = (MsgFeliCaPreCommand)message;
                    str = "";
                    for(int idx=0; idx<20; idx++) {
                        if(messagePre.GetCommandLength() < idx+1) {
                            break;
                        }
                        str += String.format("%02X", messagePre.GetCommand()[idx]);
                    }
                    Timber.tag("iCAS").d("FeliCa PreCommand %s", str);
                    lRetVal = feliCaChip.Execute(messagePre.GetCommand(), messagePre.GetCommandLength());

                    if(lRetVal != FeliCaClient.FC_ERR_SUCCESS) {
                        // パケットにFELICA_ERROR メッセージ追加
                        replyPacket.AddMessage(new TCAPMessage(message.GetExtension(), message.GetDeviceID(), (char)TCAPPacket.TCAP_MSG_MT_FELICA_ERROR));
                        sendPacketList.add(new TCAPPacket(replyPacket));
                        return FeliCaClient.FC_ERR_CLIENT_FAILURE;
                    } else {
                        lRetVal = AddParam(paramList, messagePre.GetParameterID(), feliCaChip.GetResponse(), feliCaChip.GetResponseLength(), messagePre.GetStartPosition(), messagePre.GetParameterLength());
                        if(lRetVal != FeliCaClient.FC_ERR_SUCCESS) {
                            MsgUnexpectedError msgUnexpectedError = new MsgUnexpectedError(MsgUnexpectedError.errorMessage.PRE_EXCOMMAND_ERROR.getInt(), null, 0);
                            SetErrorMessage(msgUnexpectedError);
                            return FeliCaClient.FC_ERR_CLIENT_MSG_UNEXPECTED;
                        }

                        // パケットにFELICA_RESPONSE メッセージ追加
                        replyPacket.AddMessage(new MsgFeliCaResponse(message.GetDeviceID(), feliCaChip.GetResponse(), feliCaChip.GetResponseLength()));
                    }
                    lRetVal = FeliCaClient.FC_ERR_SUCCESS;
                    break;
                case RcvPacketBase.MID_FELICA_PRECOMMAND_THRURW:
                    for(DeviceElement deviceElement : deviceList) {
                        if(message.GetDeviceID() == deviceElement.GetDeviceID()) {
                            feliCaChip = (FeliCaChipWrapper)deviceElement;
                            break;
                        }
                    }
                    if(feliCaChip == null) {
                        MsgUnexpectedError msgUnexpectedError = new MsgUnexpectedError(MsgUnexpectedError.errorMessage.UNEXPECTED_ERROR.getInt(), null, 0);
                        SetErrorMessage(msgUnexpectedError);
                        return FeliCaClient.FC_ERR_CLIENT_MSG_UNEXPECTED;
                    }

                    MsgFeliCaPreCommandThruRW messagePreThru = (MsgFeliCaPreCommandThruRW)message;

                    str = "";
                    for(int idx=0; idx<20; idx++) {
                        if(messagePreThru.GetCommandLength() < idx+1) {
                            break;
                        }
                        str += String.format("%02X", messagePreThru.GetCommand()[idx]);
                    }
                    Timber.tag("iCAS").d("FeliCa PreCommandThru %s", str);
                    lRetVal = feliCaChip.ExecuteThru(messagePreThru.GetCommand(), messagePreThru.GetCommandLength());
                    if(lRetVal != FeliCaClient.FC_ERR_SUCCESS) {
                        // パケットに FELICA_ERROR メッセージ追加
                        replyPacket.AddMessage(new TCAPMessage(message.GetExtension(), message.GetDeviceID(), (char)TCAPPacket.TCAP_MSG_MT_FELICA_ERROR_THRURW));
                        sendPacketList.add(new TCAPPacket(replyPacket));
                        return FeliCaClient.FC_ERR_CLIENT_FAILURE;
                    } else {
                        lRetVal = AddParam(paramList, messagePreThru.GetParameterID(), feliCaChip.GetResponse(), feliCaChip.GetResponseLength(), messagePreThru.GetStartPosition(), messagePreThru.GetParameterLength());
                        if(lRetVal != FeliCaClient.FC_ERR_SUCCESS) {
                            MsgUnexpectedError msgUnexpectedError = new MsgUnexpectedError(MsgUnexpectedError.errorMessage.PRE_EXCOMMAND_ERROR.getInt(), null, 0);
                            SetErrorMessage(msgUnexpectedError);
                            return FeliCaClient.FC_ERR_CLIENT_MSG_UNEXPECTED;
                        }

                        // パケットにFELICA_RESPONSE_THRU メッセージ追加
                        replyPacket.AddMessage(new MsgFeliCaResponseThruRW(message.GetDeviceID(), feliCaChip.GetResponse(), feliCaChip.GetResponseLength()));
                    }
                    lRetVal = FeliCaClient.FC_ERR_SUCCESS;
                    break;
                case RcvPacketBase.MID_FELICA_EXCOMMAND:
                    for(DeviceElement deviceElement : deviceList) {
                        if(message.GetDeviceID() == deviceElement.GetDeviceID()) {
                            feliCaChip = (FeliCaChipWrapper)deviceElement;
                            break;
                        }
                    }
                    if(feliCaChip == null) {
                        MsgUnexpectedError msgUnexpectedError = new MsgUnexpectedError(MsgUnexpectedError.errorMessage.UNEXPECTED_ERROR.getInt(), null, 0);
                        SetErrorMessage(msgUnexpectedError);
                        return FeliCaClient.FC_ERR_CLIENT_MSG_UNEXPECTED;
                    }

                    MsgFeliCaExCommand messageEx = (MsgFeliCaExCommand)message;
                    FeliCaParam param = null;
                    for(FeliCaParam feliCaParam : paramList) {
                        if(messageEx.GetParameterID() == feliCaParam.GetKeyID()) {
                            param = feliCaParam;
                            break;
                        }
                    }
                    if(param != null) {
                        str = "";
                        for(int idx=0; idx<20; idx++) {
                            if(param.GetLength() < idx+1) {
                                break;
                            }
                            str += String.format("%02X", param.GetParam()[idx]);
                        }
                        Timber.tag("iCAS").d("GetParam %s", str);
                        lRetVal = messageEx.SetPreCommand(param.GetParam(), param.GetLength());
                        if(lRetVal != FeliCaClient.FC_ERR_SUCCESS) {
                            MsgUnexpectedError msgUnexpectedError = new MsgUnexpectedError(MsgUnexpectedError.errorMessage.PRE_EXCOMMAND_ERROR.getInt(), null, 0);
                            SetErrorMessage(msgUnexpectedError);
                            return FeliCaClient.FC_ERR_CLIENT_MSG_UNEXPECTED;
                        }
                    } else {
                        MsgUnexpectedError msgUnexpectedError = new MsgUnexpectedError(MsgUnexpectedError.errorMessage.PRE_EXCOMMAND_ERROR.getInt(), null, 0);
                        SetErrorMessage(msgUnexpectedError);
                        return FeliCaClient.FC_ERR_CLIENT_MSG_UNEXPECTED;
                    }

                    str = "";
                    for(int idx=0; idx<20; idx++) {
                        if(messageEx.GetCommandLength() < idx+1) {
                            break;
                        }
                        str += String.format("%02X", messageEx.GetCommand()[idx]);
                    }
                    Timber.tag("iCAS").d("FeliCa ExCommand %s", str);
                    lRetVal = feliCaChip.Execute(messageEx.GetCommand(), messageEx.GetCommandLength());

                    if(lRetVal != FeliCaClient.FC_ERR_SUCCESS) {
                        // パケットに FELICA_ERROR メッセージ追加
                        replyPacket.AddMessage(new TCAPMessage(message.GetExtension(), message.GetDeviceID(), (char)TCAPPacket.TCAP_MSG_MT_FELICA_ERROR));
                        sendPacketList.add(new TCAPPacket(replyPacket));
                        return FeliCaClient.FC_ERR_CLIENT_FAILURE;
                    } else {
                        // パケットに FELICA_RESPONSE メッセージ追加
                        replyPacket.AddMessage(new MsgFeliCaResponse(message.GetDeviceID(), feliCaChip.GetResponse(), feliCaChip.GetResponseLength()));
                    }
                    lRetVal = FeliCaClient.FC_ERR_SUCCESS;
                    break;
                case RcvPacketBase.MID_FELICA_EXCOMMAND_THRURW:
                    for(DeviceElement deviceElement : deviceList) {
                        if(message.GetDeviceID() == deviceElement.GetDeviceID()) {
                            feliCaChip = (FeliCaChipWrapper)deviceElement;
                            break;
                        }
                    }
                    if(feliCaChip == null) {
                        MsgUnexpectedError msgUnexpectedError = new MsgUnexpectedError(MsgUnexpectedError.errorMessage.UNEXPECTED_ERROR.getInt(), null, 0);
                        SetErrorMessage(msgUnexpectedError);
                        return FeliCaClient.FC_ERR_CLIENT_MSG_UNEXPECTED;
                    }

                    MsgFeliCaExCommandThruRW messageExThru = (MsgFeliCaExCommandThruRW)message;
                    FeliCaParam paramEx = null;

                    for(FeliCaParam feliCaParam : paramList) {
                        if(messageExThru.GetParameterID() == feliCaParam.GetKeyID()) {
                            paramEx = feliCaParam;
                            break;
                        }
                    }

                    if(paramEx != null) {
                        lRetVal = messageExThru.SetPreCommand(paramEx.GetParam(), paramEx.GetLength());
                        if(lRetVal != FeliCaClient.FC_ERR_SUCCESS) {
                            MsgUnexpectedError msgUnexpectedError = new MsgUnexpectedError(MsgUnexpectedError.errorMessage.PRE_EXCOMMAND_ERROR.getInt(), null, 0);
                            SetErrorMessage(msgUnexpectedError);
                            return FeliCaClient.FC_ERR_CLIENT_MSG_UNEXPECTED;
                        }
                    } else {
                        MsgUnexpectedError msgUnexpectedError = new MsgUnexpectedError(MsgUnexpectedError.errorMessage.PRE_EXCOMMAND_ERROR.getInt(), null, 0);
                        SetErrorMessage(msgUnexpectedError);
                        return FeliCaClient.FC_ERR_CLIENT_MSG_UNEXPECTED;
                    }

                    str = "";
                    for(int idx=0; idx<20; idx++) {
                        if(messageExThru.GetCommandLength() < idx+1) {
                            break;
                        }
                        str += String.format("%02X", messageExThru.GetCommand()[idx]);
                    }
                    Timber.tag("iCAS").d("FeliCa ExCommandThru %s", str);
                    lRetVal = feliCaChip.ExecuteThru(messageExThru.GetCommand(), messageExThru.GetCommandLength());

                    if(lRetVal != FeliCaClient.FC_ERR_SUCCESS) {
                        // パケットに FELICA_ERROR メッセージ追加
                        replyPacket.AddMessage(new TCAPMessage(message.GetExtension(), message.GetDeviceID(), (char)TCAPPacket.TCAP_MSG_MT_FELICA_ERROR_THRURW));
                        sendPacketList.add(new TCAPPacket(replyPacket));
                        return FeliCaClient.FC_ERR_CLIENT_FAILURE;
                    } else {
                        // パケットに FELICA_RESPONSE メッセージ追加
                        replyPacket.AddMessage(new MsgFeliCaResponseThruRW(message.GetDeviceID(), feliCaChip.GetResponse(), feliCaChip.GetResponseLength()));
                    }
                    lRetVal = FeliCaClient.FC_ERR_SUCCESS;
                    break;
                default:
                    break;
            }
        }

        sendPacketList.add(new TCAPPacket(replyPacket));

        return lRetVal;
    }

    private long AddParam(List<FeliCaParam> paramList, char keyId, byte[] param, long paramLength, char offset, char length) {
        long lRetVal = FeliCaClient.FC_ERR_CLIENT_UNKNOWN;
        byte[] addParam;

        if(param == null && 0 < paramLength) {
            // パラメータ不正（データが無いのにデータ長あり）
        } else if(param == null && (0<offset || 0<length)) {
            // パラメータ不正（データが無いのにオフセットと抜き出しデータ長あり）
        } else if(paramLength < (offset + length)) {
            // パラメータ不正（抜き出しデータ長がデータ実体を超えている）
        } else {
            FeliCaParam tempParam = null;

            for(int i = 0; i < paramList.size(); i++) {
                if(keyId == paramList.get(i).GetKeyID()) {
                    tempParam = paramList.get(i);
                    // すでに同じパラメータIDが記録されていた場合、パラメータを上書きする
                    addParam = new byte[length];
                    System.arraycopy(Objects.requireNonNull(param), offset, addParam, 0, length);
                    lRetVal = tempParam.SetParam(addParam, length);
                }
            }

            if(tempParam == null) {
                // パラメータオブジェクトを引数のキーで構築
                tempParam = new FeliCaParam(keyId);
                // パラメータオブジェクトに引数のデータを格納
                addParam = new byte[length];
                System.arraycopy(Objects.requireNonNull(param), offset, addParam, 0, length);
                String str = "";
                for(int idx=0; idx<20; idx++) {
                    if(length < idx+1) {
                        break;
                    }
                    str += String.format("%02X", addParam[idx]);
                }
                Timber.tag("iCAS").d("AddParam %s", str);
                lRetVal = tempParam.SetParam(addParam, length);
                if(lRetVal == FeliCaClient.FC_ERR_SUCCESS) {
                    if(!paramList.add(tempParam)) {
                        lRetVal = FeliCaClient.FC_ERR_CLIENT_UNKNOWN;
                    }
                }
            }
        }

        return lRetVal;
    }

    /**
     * UpdateEntityパケット受信ハンドラ
     * UpdateEntityパケットを受信した時の動作を行います。
     *
     * @param packet          受信したUpdateEntityパケット
     * @param stateMachine25a 現在の状態オブジェクト
     *
     * @return エラーコード
     */
    private long OnUpdateEntity(RcvPacketBase packet, ClientStateMachine25a stateMachine25a) {
        TCAPCommunicationAgent tcapCommunicationAgent = stateMachine25a.GetCommunicationAgent();
        TCAPContext tcapContext = stateMachine25a.GetContext();
        List<TCAPPacket> sendPacketList = tcapContext.GetSendPacketList();
        List<DeviceElement> deviceList = tcapContext.GetDeviceList();

        SetErrorMessage(null);

        // Farewell後にこのパケットが来た場合、状態遷移不正メッセージを設定し、ステータスを「エラー」に変更
        if(_bFarewellDone) {
            MsgIllegalStateError msgIllegalStateError = new MsgIllegalStateError(MsgIllegalStateError.ILLEGAL_STATE);
            SetErrorMessage(msgIllegalStateError);
            return FeliCaClient.FC_ERR_CLIENT_MSG_ILLEGAL;
        }

        int messageNum = packet.GetMessageNum();
        TCAPMessage message;
        FeliCaChipWrapper feliCaChip = null;

        long lRetVal = FeliCaClient.FC_ERR_SUCCESS;
        TCAPPacket replyPacket = new TCAPPacket((char)TCAPPacket.TCAP_VERSION_25, (char)TCAPPacket.TCAP_SPT_UPDATE_ENTITY);

        for(int i = 0; i < messageNum; i++) {
            // 中止要求チェック
            if(tcapContext.IsStop()) {
                return FeliCaClient.FC_ERR_CLIENT_CANCEL;
            }

            // メッセージ取得
            message = packet.GetMessage(i);

            /***********************************************************************************/
            /*** TCAP試験用コード ***/
            if(iCASClientTest.IsStarted() && iCASClientTest.TCAP_TEST_MODE == iCASClientTest.TCAP_TEST_UEP_E_046) {
                message = null;
            }
            /***********************************************************************************/

            if(message == null) {
                MsgUnexpectedError msgUnexpectedError = new MsgUnexpectedError(MsgUnexpectedError.errorMessage.UNEXPECTED_ERROR.getInt(), null, 0);
                SetErrorMessage(msgUnexpectedError);
                return FeliCaClient.FC_ERR_CLIENT_MSG_UNEXPECTED;
            }

            // メッセージタイプチェック
            int mid = (message.GetExtension() << 8) | (message.GetMessageType());
            switch (mid) {
                case RcvPacketBase.MID_WARNING:
                    Timber.tag("iCAS").d("WARNING");
                    break;
                case RcvPacketBase.MID_SET_NETWORK_TIMEOUT:
                    Timber.tag("iCAS").d("SET_NETWORK_TIMEOUT %d", (int)((MsgSetNetworkTimeout)message).GetTime());
                    tcapCommunicationAgent.SetNetworkTimeout((int)((MsgSetNetworkTimeout)message).GetTime());
                    break;
                case RcvPacketBase.MID_REQUEST_ID:
                    Timber.tag("iCAS").d("REQUEST_ID %d", (int)((MsgRequestID)message).GetID());
                    replyPacket.AddMessage(new MsgRequestID(((MsgRequestID)message).GetID()));
                    break;
                case RcvPacketBase.MID_SET_FELICA_TIMEOUT:
                    for(DeviceElement deviceElement : deviceList) {
                        if(deviceElement.GetDeviceID() == message.GetDeviceID()) {
                            feliCaChip = (FeliCaChipWrapper)deviceElement;
                            break;
                        }
                    }
                    if(feliCaChip != null) {
                        Timber.tag("iCAS").d("SET_FELICA_TIMEOUT %d", ((MsgSetFeliCaTimeout)message).GetTime());
                        feliCaChip.SetTimeout(((MsgSetFeliCaTimeout)message).GetTime());
                    }
                    break;
                case RcvPacketBase.MID_SET_RETRY_COUNT:
                    for(DeviceElement deviceElement : deviceList) {
                        if(deviceElement.GetDeviceID() == message.GetDeviceID()) {
                            feliCaChip = (FeliCaChipWrapper)deviceElement;
                            break;
                        }
                    }
                    if(feliCaChip != null) {
                        Timber.tag("iCAS").d("SET_RETRY_COUNT %d", ((MsgSetRetryCount)message).GetRetryCount());
                        feliCaChip.SetRetryCount(((MsgSetRetryCount)message).GetRetryCount());
                    }
                    break;
                default:
                    MsgUnexpectedError msgUnexpectedError = new MsgUnexpectedError(MsgUnexpectedError.errorMessage.UNEXPECTED_ERROR.getInt(), null, 0);
                    SetErrorMessage(msgUnexpectedError);
                    return FeliCaClient.FC_ERR_CLIENT_MSG_UNEXPECTED;
            }
        }

        sendPacketList.add(new TCAPPacket(replyPacket));

        return lRetVal;
    }

    @Override
    public void SetEventListener(IFeliCaClientEventListener listener) {
        _listener = listener;
    }
}
