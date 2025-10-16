package jp.mcapps.android.multi_payment_terminal.iCAS;

import android.os.Handler;
import android.os.Message;

import com.google.gson.Gson;
import com.google.gson.internal.Primitives;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

import jp.mcapps.android.multi_payment_terminal.error.JremRasErrorCodes;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.FeliCaChip.FeliCaChip;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.FeliCaClient.Device;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.FeliCaClient.FeliCaClient;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.FeliCaClient.IDevice;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.TCAPMessage;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.TCAPPacket;
import jp.mcapps.android.multi_payment_terminal.iCAS.data.DeviceClient;
import timber.log.Timber;

import static jp.mcapps.android.multi_payment_terminal.iCAS.iCASClient.moneyType.MONEY_EDY;
import static jp.mcapps.android.multi_payment_terminal.iCAS.iCASClient.moneyType.MONEY_NANACO;
import static jp.mcapps.android.multi_payment_terminal.iCAS.iCASClient.moneyType.MONEY_WAON;

public class iCASDevice {

    private static final HashMap<Integer, String> _deviceMap = new HashMap<>();

    // デバイス定義
    ////////////////////////////////////////////////////////////////////////
    // デバイスID
    public static final int DEVICE_ID_FELICA    = 0x0001;
    public static final int DEVICE_ID_CLIENT    = 0x0002;
    public static final int DEVICE_ID_RW_UI     = 0x0003;
    public static final int DEVICE_ID_PINPAD_UI = 0x0004;

    public static boolean _isNoEnd = false;
    public static boolean _autoRetryFlg = false;    // 現状、WAONおよびEdyのときのみ使用

    private final IDevice.DeviceOperate _deviceOperate;
    private final Gson _gson = new Gson();
    private final iCASClient.moneyType _money;
    private final int _businessId;

    public static int test = 0;

    public iCASDevice(IDevice.DeviceOperate deviceOperate, iCASClient.moneyType money, int businessId) {
        _deviceOperate = deviceOperate;
        _money = money;
        _businessId = businessId;
    }

    static {
        _deviceMap.put(DEVICE_ID_FELICA, "R/W");
        _deviceMap.put(DEVICE_ID_CLIENT, "CLIENT");
        _deviceMap.put(DEVICE_ID_RW_UI, "R/W_UI");
        _deviceMap.put(DEVICE_ID_PINPAD_UI, "PINPAD_UI");
    }

    public static void Initialize() {
        _isNoEnd = false;
        _autoRetryFlg = false;
    }

    public static long AddDevice(FeliCaClient feliCaClient) {
        long lRetVal = FeliCaClient.FC_ERR_SUCCESS;

        for(Integer key : _deviceMap.keySet()) {
            int val = key;

            if(key == DEVICE_ID_FELICA) {
                FeliCaChip feliCaChip = new FeliCaChip((char) val);
                // FeliCaデバイス追加
                feliCaChip.SetName(_deviceMap.get(key).getBytes());
                feliCaChip.SetType("FeliCa".getBytes());

                lRetVal = feliCaClient.AddFeliCaChip(feliCaChip);
            } else {
                // その他デバイス追加
                Device device = new Device((char)val);
                device.SetName(_deviceMap.get(key).getBytes());
                device.SetType("Generic".getBytes());

                lRetVal = feliCaClient.AddDevice(device);
            }

            if(lRetVal != FeliCaClient.FC_ERR_SUCCESS) {
                break;
            }
        }

        return lRetVal;
    }

    public void NoneChipAccessResponse(byte[] data, int dataLength, Handler handlerToMain) throws IOException {
        Message message = new Message();
        int operationStatus = 1;    //  不正立をデフォルトとする
        String cancelFlg = null;

        switch(_businessId) {
            case iCASClient.BUSINESS_ID_EDY_FIRST_COMM:
            case iCASClient.BUSINESS_ID_EDY_CHARGE_AUTH:
                DeviceClient.EdyFirstComm edyFirstComm;
                edyFirstComm = getJsonObject(data, DeviceClient.EdyFirstComm.class);
                Timber.tag("iCAS").i("EDY_FIRST_COMM code %s result %s", edyFirstComm.code, edyFirstComm.result);

                if(edyFirstComm.result.equals("false")){
                    // Edy初回通信失敗
                    message.what = iCASClient.FC_MSG_TO_MAIN_ON_ERROR;
                    message.arg1 = iCASClient.ERROR_CLIENT_FAILURE;
                    handlerToMain.sendMessage(message);
                    return;
                }
                break;
            case iCASClient.BUSINESS_ID_EDY_REMOVAL:
                DeviceClient.JournalEdy journalEdy;
                journalEdy = getJsonObject(data, DeviceClient.JournalEdy.class);
                Timber.tag("iCAS").i("BUSINESS_ID_EDY_REMOVAL code %s result %s", journalEdy.code, journalEdy.result);
                break;
            case iCASClient.BUSINESS_ID_EDY_JOURNAL:
                DeviceClient.JournalEdy journalEdy2;
                journalEdy2 = getJsonObject(data, DeviceClient.JournalEdy.class);
                Timber.tag("iCAS").i("BUSINESS_ID_EDY_JOURNAL code %s result %s", journalEdy2.code, journalEdy2.result);

                // resultがfalseの場合はUIに渡すtermToをnullにする
//                journalEdy2.result = "false";
                if(journalEdy2.result != null && journalEdy2.result.equals("false")) {
                    journalEdy2.termTo = null;
                }
                message.what = iCASClient.FC_MSG_TO_MAIN_ON_JOURNAL;
                message.obj = journalEdy2.termTo;
                handlerToMain.sendMessage(message);
                break;
            case iCASClient.BUSINESS_ID_SUICA_STATUS_REPLY:
                DeviceClient.BusinessStatusResponse businessStatusResponse;
                businessStatusResponse = getJsonObject(data, DeviceClient.BusinessStatusResponse.class);
                Timber.tag("iCAS").i("SUICA_STATUS_REPLY code %s result %s", businessStatusResponse.code, businessStatusResponse.result);
                if(businessStatusResponse.resultData != null) {
                    Timber.tag("iCAS").i("resultData result %s operationStatus %s code %s operationResultCode %s", businessStatusResponse.resultData.result, businessStatusResponse.resultData.operationStatus, businessStatusResponse.resultData.code, businessStatusResponse.resultData.operationResultCode);
                    operationStatus = Integer.parseInt(businessStatusResponse.resultData.operationStatus);
                }

                // operationStatusの値によって処理分岐
                switch (operationStatus) {
                    case 0: // 成立
                    case 2: // 未了
                        if(businessStatusResponse.resultData != null) {
                            // 結果を呼び出し元に通知
                            DeviceClient.Result result = new DeviceClient.Result();
                            result.result = businessStatusResponse.resultData.result;
                            result.IDi = businessStatusResponse.resultData.IDi;
                            result.IDm = businessStatusResponse.resultData.IDm;
                            result.rem = businessStatusResponse.resultData.rem;
                            result.value = businessStatusResponse.resultData.value;
                            result.sid = businessStatusResponse.resultData.sid;
                            result.sprwid = businessStatusResponse.resultData.sprwid;
                            result.time = businessStatusResponse.resultData.time;
                            result.code = businessStatusResponse.resultData.code;
                            result.bRem = businessStatusResponse.resultData.bRem;
                            result.statementID = businessStatusResponse.resultData.statementID;
                            result.ICsequence = businessStatusResponse.resultData.ICsequence;
                            result.SFLogID = businessStatusResponse.resultData.SFLogID;
                            result.oldstatementID = businessStatusResponse.resultData.oldstatementID;
                            result.oldSFLogID = businessStatusResponse.resultData.oldSFLogID;

                            message.what = iCASClient.FC_MSG_TO_MAIN_ON_RECOVERY;
                            if(operationStatus == 2) {
                                result.code = String.valueOf(JremRasErrorCodes.E353);  // 処理未了のコードに書き換える
                            }
                            message.obj = result;
                            if (result.code != null) {
                                message.arg1 = Integer.parseInt(result.code);
                            }
                            handlerToMain.sendMessage(message);
                        } else {
                            // 業務処理状態応答でできる期間を過ぎている場合（自動で行うためここには来ない）
                        }
                        break;
                    case 1: // 不成立
                    default:
                        // エラー終了
                        message.what = iCASClient.FC_MSG_TO_MAIN_ON_ERROR;
                        message.arg1 = iCASClient.ERROR_CLIENT_FAILURE;
                        handlerToMain.sendMessage(message);
                        return;
                }
                break;
            case iCASClient.BUSINESS_ID_ID_STATUS_REPLY:
                DeviceClient.BusinessStatusResponseID businessStatusResponseID;
                businessStatusResponseID = getJsonObject(data, DeviceClient.BusinessStatusResponseID.class);
                Timber.tag("iCAS").i("iD_STATUS_REPLY code %s result %s", businessStatusResponseID.code, businessStatusResponseID.result);
                if(businessStatusResponseID.resultData != null) {
                    Timber.tag("iCAS").i("resultData result %s operationStatus %s code %s operationResultCode %s", businessStatusResponseID.resultData.result, businessStatusResponseID.resultData.operationStatus, businessStatusResponseID.resultData.code, businessStatusResponseID.resultData.operationResultCode);
                    operationStatus = Integer.parseInt(businessStatusResponseID.resultData.operationStatus);
                }

                // operationStatusの値によって処理分岐
                switch (operationStatus) {
                    case 0: // 成立
                        if(businessStatusResponseID.resultData != null) {
                            // 結果を呼び出し元に通知
                            message.what = iCASClient.FC_MSG_TO_MAIN_ON_RECOVERY;
                            message.obj = businessStatusResponseID.resultData;
                            if (businessStatusResponseID.resultData.code != null) {
                                message.arg1 = Integer.parseInt(businessStatusResponseID.resultData.code);
                            }
                            handlerToMain.sendMessage(message);
                        } else {
                            // 業務処理状態応答でできる期間を過ぎている場合（自動で行うためここには来ない）
                        }
                        break;
                    case 1: // 不成立
                    default:
                        // エラー終了
                        message.what = iCASClient.FC_MSG_TO_MAIN_ON_ERROR;
                        message.arg1 = iCASClient.ERROR_CLIENT_FAILURE;
                        handlerToMain.sendMessage(message);
                        return;
                }
                break;
            case iCASClient.BUSINESS_ID_WAON_STATUS_REPLY:
                DeviceClient.BusinessStatusResponseWAON businessStatusResponseWAON;
                businessStatusResponseWAON = getJsonObject(data, DeviceClient.BusinessStatusResponseWAON.class);
                Timber.tag("iCAS").i("WAON_STATUS_REPLY code %s result %s", businessStatusResponseWAON.code, businessStatusResponseWAON.result);
                if(businessStatusResponseWAON.resultData != null) {
                    Timber.tag("iCAS").i("resultData result %s operationStatus %s code %s operationResultCode %s", businessStatusResponseWAON.resultData.result, businessStatusResponseWAON.resultData.operationStatus, businessStatusResponseWAON.resultData.code, businessStatusResponseWAON.resultData.operationResultCode);
                    operationStatus = Integer.parseInt(businessStatusResponseWAON.resultData.operationStatus);
                }
                // operationStatusの値によって処理分岐
                switch (operationStatus) {
                    case 0: // 成立
                    case 2: // 未了
                        if(businessStatusResponseWAON.resultData != null) {
                            // 結果を呼び出し元に通知
                            message.what = iCASClient.FC_MSG_TO_MAIN_ON_RECOVERY;
                            if(operationStatus == 2) {
                                businessStatusResponseWAON.resultData.code = String.valueOf(JremRasErrorCodes.E353);  // 処理未了のコードに書き換える
                            }
                            message.obj = businessStatusResponseWAON.resultData;
                            if (businessStatusResponseWAON.resultData.code != null) {
                                message.arg1 = Integer.parseInt(businessStatusResponseWAON.resultData.code);
                            }
                            handlerToMain.sendMessage(message);
                        } else {
                            // 業務処理状態応答でできる期間を過ぎている場合（自動で行うためここには来ない）
                        }
                        break;
                    case 1: // 不成立
                    default:
                        // エラー終了
                        message.what = iCASClient.FC_MSG_TO_MAIN_ON_ERROR;
                        message.arg1 = iCASClient.ERROR_CLIENT_FAILURE;
                        handlerToMain.sendMessage(message);
                        return;
                }
                break;
            case iCASClient.BUSINESS_ID_QUICPAY_STATUS_REPLY:
                DeviceClient.BusinessStatusResponseQUICPay businessStatusResponseQUICPay;
                businessStatusResponseQUICPay = getJsonObject(data, DeviceClient.BusinessStatusResponseQUICPay.class);
                Timber.tag("iCAS").i("QUICPAY_STATUS_REPLY code %s result %s", businessStatusResponseQUICPay.code, businessStatusResponseQUICPay.result);
                if(businessStatusResponseQUICPay.resultData != null) {
                    Timber.tag("iCAS").i("resultData result %s operationStatus %s code %s operationResultCode %s", businessStatusResponseQUICPay.resultData.result, businessStatusResponseQUICPay.resultData.operationStatus, businessStatusResponseQUICPay.resultData.code, businessStatusResponseQUICPay.resultData.operationResultCode);
                    operationStatus = Integer.parseInt(businessStatusResponseQUICPay.resultData.operationStatus);
                }

                // operationStatusの値によって処理分岐
                switch (operationStatus) {
                    case 0: // 成立
                        if(businessStatusResponseQUICPay.resultData != null) {
                            // 結果を呼び出し元に通知
                            message.what = iCASClient.FC_MSG_TO_MAIN_ON_RECOVERY;
                            message.obj = businessStatusResponseQUICPay.resultData;
                            if (businessStatusResponseQUICPay.resultData.code != null) {
                                message.arg1 = Integer.parseInt(businessStatusResponseQUICPay.resultData.code);
                            }
                            handlerToMain.sendMessage(message);
                        } else {
                            // 業務処理状態応答でできる期間を過ぎている場合（自動で行うためここには来ない）
                        }
                        break;
                    case 1: // 不成立
                    default:
                        // エラー終了
                        message.what = iCASClient.FC_MSG_TO_MAIN_ON_ERROR;
                        message.arg1 = iCASClient.ERROR_CLIENT_FAILURE;
                        handlerToMain.sendMessage(message);
                        return;
                }
                break;
            case iCASClient.BUSINESS_ID_EDY_STATUS_REPLY:
                DeviceClient.BusinessStatusResponseEdy businessStatusResponseEdy;
                businessStatusResponseEdy = getJsonObject(data, DeviceClient.BusinessStatusResponseEdy.class);
                Timber.tag("iCAS").i("EDY_STATUS_REPLY code %s result %s", businessStatusResponseEdy.code, businessStatusResponseEdy.result);
                if(businessStatusResponseEdy.resultData != null) {
                    Timber.tag("iCAS").i("resultData result %s code %s cardResultCode %s", businessStatusResponseEdy.resultData.result, businessStatusResponseEdy.resultData.code, businessStatusResponseEdy.resultData.saleHistories[0].cardResultCode);
                    operationStatus = Integer.parseInt(businessStatusResponseEdy.resultData.saleHistories[0].cardResultCode);
                }

                // operationStatusの値によって処理分岐
                switch (operationStatus) {
                    case 0: // 成立
                    case 2: // 未了
                        if(businessStatusResponseEdy.resultData != null) {
                            // 結果を呼び出し元に通知
                            message.what = iCASClient.FC_MSG_TO_MAIN_ON_RECOVERY;
                            if(operationStatus == 2) {
                                businessStatusResponseEdy.resultData.code = String.valueOf(JremRasErrorCodes.E353);  // 処理未了のコードに書き換える
                            }
                            message.obj = businessStatusResponseEdy.resultData;
                            if (businessStatusResponseEdy.resultData.code != null) {
                                message.arg1 = Integer.parseInt(businessStatusResponseEdy.resultData.code);
                            }
                            handlerToMain.sendMessage(message);
                        } else {
                            // 業務処理状態応答でできる期間を過ぎている場合（自動で行うためここには来ない）
                        }
                        break;
                    case 1: // 不成立
                    default:
                        // エラー終了
                        message.what = iCASClient.FC_MSG_TO_MAIN_ON_ERROR;
                        message.arg1 = iCASClient.ERROR_CLIENT_FAILURE;
                        handlerToMain.sendMessage(message);
                        return;
                }
                break;
            case iCASClient.BUSINESS_ID_NANACO_PREV_TRAN:
                DeviceClient.BusinessStatusResponsenanaco businessStatusResponsenanaco;
                businessStatusResponsenanaco = getJsonObject(data, DeviceClient.BusinessStatusResponsenanaco.class);
                Timber.tag("iCAS").i("NANACO_STATUS_REPLY code %s result %s", businessStatusResponsenanaco.code, businessStatusResponsenanaco.result);
                if(businessStatusResponsenanaco.resultData != null) {
                    Timber.tag("iCAS").i("resultData result %s code %s cardResultCode %s", businessStatusResponsenanaco.resultData.result, businessStatusResponsenanaco.resultData.code, businessStatusResponsenanaco.resultData.saleHistories[0].cardResultCode);
                    operationStatus = Integer.parseInt(businessStatusResponsenanaco.resultData.saleHistories[0].cardResultCode);
                }

                // operationStatusの値によって処理分岐
                switch (operationStatus) {
                    case 0: // 成立
                    case 2: // 未了
                        if(businessStatusResponsenanaco.resultData != null) {
                            // 結果を呼び出し元に通知
                            message.what = iCASClient.FC_MSG_TO_MAIN_ON_RECOVERY;
                            if(operationStatus == 2) {
                                businessStatusResponsenanaco.resultData.code = String.valueOf(JremRasErrorCodes.E353);  // 処理未了のコードに書き換える
                            }
                            message.obj = businessStatusResponsenanaco.resultData;
                            if (businessStatusResponsenanaco.resultData.code != null) {
                                message.arg1 = Integer.parseInt(businessStatusResponsenanaco.resultData.code);
                            }
                            handlerToMain.sendMessage(message);
                        } else {
                            // 業務処理状態応答でできる期間を過ぎている場合（自動で行うためここには来ない）
                        }
                        break;
                    case 1: // 不成立
                    default:
                        // エラー終了
                        message.what = iCASClient.FC_MSG_TO_MAIN_ON_ERROR;
                        message.arg1 = iCASClient.ERROR_CLIENT_FAILURE;
                        handlerToMain.sendMessage(message);
                        return;
                }
                break;
            case iCASClient.BUSINESS_ID_QUICPAY_JOURNAL:
                DeviceClient.JournalQUICPay journalQUICPay;
                journalQUICPay = getJsonObject(data, DeviceClient.JournalQUICPay.class);
                Timber.tag("iCAS").i("QUICPAY_JOURNAL code %s termTo %s", journalQUICPay.code, journalQUICPay.termTo);
                // resultがfalseの場合はUIに渡すtermToをnullにする
//                journalQUICPay.result = "false";
                if(journalQUICPay.result != null && journalQUICPay.result.equals("false")) {
                    journalQUICPay.termTo = null;
                }

                message.what = iCASClient.FC_MSG_TO_MAIN_ON_JOURNAL;
                message.obj = journalQUICPay.termTo;
                handlerToMain.sendMessage(message);
                break;
            case iCASClient.BUSINESS_ID_NANACO_JOURNAL:
                DeviceClient.Journalnanaco journalnanaco;
                journalnanaco = getJsonObject(data, DeviceClient.Journalnanaco.class);
                Timber.tag("iCAS").i("NANACO_JOURNAL code %s termTo %s", journalnanaco.code, journalnanaco.termTo);
                // resultがfalseの場合はUIに渡すtermToをnullにする
//                journalnanaco.result = "false";
                if(journalnanaco.result != null && journalnanaco.result.equals("false")) {
                    journalnanaco.termTo = null;
                }

                message.what = iCASClient.FC_MSG_TO_MAIN_ON_JOURNAL;
                message.obj = journalnanaco.termTo;
                handlerToMain.sendMessage(message);
                break;
            case iCASClient.BUSINESS_ID_WAON_PRE_STATUS_REPLY:
                DeviceClient.BusinessStatusResponseWAON businessStatusResponseWAON2;
                businessStatusResponseWAON2 = getJsonObject(data, DeviceClient.BusinessStatusResponseWAON.class);
                Timber.tag("iCAS").i("WAON_PRE_STATUS_REPLY code %s result %s", businessStatusResponseWAON2.code, businessStatusResponseWAON2.result);
                if(businessStatusResponseWAON2.resultData != null) {
                    Timber.tag("iCAS").i("resultData result %s operationStatus %s code %s cancelFlg %s", businessStatusResponseWAON2.resultData.result, businessStatusResponseWAON2.resultData.operationStatus, businessStatusResponseWAON2.resultData.code, businessStatusResponseWAON2.resultData.cancelFlg);

                    cancelFlg = businessStatusResponseWAON2.resultData.cancelFlg;
                }

                if(cancelFlg != null && cancelFlg.equals("cancelPossible")) {
                    // 取消可能な場合
                    message.what = iCASClient.FC_MSG_TO_MAIN_ON_WAON_PRE_STATUS;
                    message.obj = businessStatusResponseWAON2.resultData;
                    handlerToMain.sendMessage(message);
                } else {
                    // 取消不可能な場合
                    message.what = iCASClient.FC_MSG_TO_MAIN_ON_ERROR;
                    if(businessStatusResponseWAON2.code != null) {
                        message.arg1 = Integer.parseInt(businessStatusResponseWAON2.code);
                    } else if(cancelFlg != null && businessStatusResponseWAON2.resultData.code != null) {
                        message.arg1 = Integer.parseInt(businessStatusResponseWAON2.resultData.code);
                    } else {
                        message.arg1 = iCASClient.ERROR_CLIENT_FAILURE;
                    }
                    handlerToMain.sendMessage(message);
                }
                // OnFinishさせないためreturn
                return;
        }

        // チップアクセスなしの業務は順番制御のためOnNoneChipAccessResponse()からOnFinishedを通知
        Message message2 = new Message();
        if(operationStatus == 2) {
            message2.what = iCASClient.FC_MSG_TO_MAIN_ON_ERROR;
            message2.arg1 = iCASClient.ERROR_STATUS_REPLY_NO_END;
        } else {
            message2.what = iCASClient.FC_MSG_TO_MAIN_ON_FINISH;
        }

        handlerToMain.sendMessage(message2);
    }

    public long Operate(boolean bIsAborted, Handler handlerToMain, TCAPPacket replyPacket, boolean isPolling) throws IOException, InterruptedException {
        long lRetVal = FeliCaClient.FC_ERR_SUCCESS;
        String paramName = new String(_deviceOperate.GetParam());
        Message message = new Message();

        switch (_deviceOperate.GetDeviceID()) {
            case DEVICE_ID_CLIENT:
                if(paramName.equals("CANCEL")) {
                    int datalen = ((_deviceOperate.GetData()[0] << 8) & 0x0000FF00) | (_deviceOperate.GetData()[1] & 0x000000FF);
                    if(datalen > 0) {
                        byte[] opeData = new byte[datalen];
                        System.arraycopy(_deviceOperate.GetData(), 2, opeData, 0, datalen);
                        // キャンセル最終確認チェック
                        DeviceClient.Cancel cancel;
                        cancel = getJsonObject(opeData, DeviceClient.Cancel.class);
                        Timber.tag("iCAS").i("CLIENT CANCEL cancel %s", cancel.cancel);
                        if (cancel.cancel.equals("99") && !bIsAborted) { // 最終確認 かつ まだキャンセルされていない
                            // これ以降キャンセル受付不可とする
                            message.what = iCASClient.FC_MSG_TO_MAIN_ON_OPERATE_CANCEL_FINAL;
                            handlerToMain.sendMessage(message);
                        } else if(!cancel.cancel.equals("99") && !bIsAborted && (!_isNoEnd || _autoRetryFlg)) { // まだキャンセルされていない
                            // キャンセル受付不可後に再びキャンセル有効にするために通知する
                            message.what = iCASClient.FC_MSG_TO_MAIN_ON_OPERATE_CANCEL;
                            if(_autoRetryFlg) {
                                message.arg1 = 1;   // _autoRetryFlgが有効なときは中止を有効にするため引数に１をセット
                            } else {
                                message.arg1 = 0;
                            }
                            handlerToMain.sendMessage(message);
                        }

                        // キャンセル確認結果通知を作成
                        lRetVal = makeDeviceResponseCommon(replyPacket, bIsAborted);
                    } else {
                        // 不明なパラメータ名
                        lRetVal = FeliCaClient.FC_ERR_CLIENT_MSG_FORMAT;
                    }
                } else if(paramName.equals("STATUS")) {
                    // ステータス
                    DeviceClient.Status status;
                    int datalen = ((_deviceOperate.GetData()[0] << 8) & 0x0000FF00) | (_deviceOperate.GetData()[1] & 0x000000FF);
                    if(datalen > 0) {
                        byte[] opeData = new byte[datalen];
                        System.arraycopy(_deviceOperate.GetData(), 2, opeData, 0, datalen);
                        if (_money != iCASClient.moneyType.MONEY_SUICA || _businessId == iCASClient.BUSINESS_ID_SUICA_REMAIN) {
                            DeviceClient.StatusSimple statusSimple;
                            statusSimple = getJsonObject(opeData, DeviceClient.StatusSimple.class);
                            status = new DeviceClient.Status();
                            status.status = statusSimple.status;
                        } else {
                            status = getJsonObject(opeData, DeviceClient.Status.class);
                        }
/*
                        // 決済不成立 ケース１確認用 (取消を確認する場合はHistoryTransactionDetailFragmentの_viewModel.setCancelable(false)をコメントアウトすること）WAONの場合はMcPosCenterApilmplのpostPaymentの対象マネー処理も
                        if(status.status.equals("1")) {
                            if(test == 0) {
                                Timber.tag("iCAS").d("★★★★★ ジャマー開始 ★★★★★");
                                Thread.sleep(10000);
                                test = 1;
                            }
                        }
*/
/*
                        // 決済不成立 ケース３、６、７確認用
                        if(status.status.equals("2")) {
                            if(test == 0) {
                                Timber.tag("iCAS").d("★★★★★ ジャマー開始 ★★★★★");
                                Thread.sleep(10000);
                                test = 1;
                            }
                        }
*/
/*
                        // 決済不成立 ケース４確認用
                        if(status.status.equals("1")) {
                            if(test == 0) {
                                status.status = "2";
                                Timber.tag("iCAS").d("★★★★★ ジャマー開始 ★★★★★");
                                Thread.sleep(10000);
                                test = 1;
                            }
                        }
*/
/*
                        // 決済不成立 ケース５確認用
                        if(status.status.equals("3")) {
                            if(test == 0) {
                                status.status = "2";
                                Timber.tag("iCAS").d("★★★★★ ジャマー開始 ★★★★★");
                                Thread.sleep(10000);
                                test = 1;
                            }
                        }
*/
                        Timber.tag("iCAS").i("CLIENT STATUS status %s", status.status);
                        message.what = iCASClient.FC_MSG_TO_MAIN_ON_OPERATE_STATUS;
                        message.obj = status;
                        handlerToMain.sendMessage(message);
                        // ステータス設定結果通知を作成
//                        lRetVal = makeDeviceResponseCommon(replyPacket, bIsAborted);
                        lRetVal = makeDeviceResponseCommon(replyPacket, false);     // 異常通知はしない
                    } else {
                        // 不明なパラメータ名
                        lRetVal = FeliCaClient.FC_ERR_CLIENT_MSG_FORMAT;
                    }
                } else if(paramName.equals("DISPLAY")) {
                    // 端末画面操作
                    DeviceClient.Display display;
                    final int datalen = ((_deviceOperate.GetData()[0] << 8) & 0x0000FF00) | (_deviceOperate.GetData()[1] & 0x000000FF);
                    if(datalen > 0) {
                        byte[] opeData = new byte[datalen];
                        System.arraycopy(_deviceOperate.GetData(), 2, opeData, 0, datalen);
                        final String str = new String(opeData, "SJIS");
                        display = getJsonObject(str.getBytes(), DeviceClient.Display.class);
                        String dsp1 = "";
                        String dsp2 = "";
                        String dsp3 = "";
                        if(display.display.length > 0) {
                            dsp1 = display.display[0];
                        }
                        if(display.display.length > 1) {
                            dsp2 = display.display[1];
                        }
                        if(display.display.length > 2) {
                            dsp3 = display.display[2];
                        }
                        Timber.tag("iCAS").i("iCASDevice Operate display1 %s display2 %s display3 %s", dsp1, dsp2, dsp3);
                        message.what = iCASClient.FC_MSG_TO_MAIN_ON_OPERATE_DISPLAY;
                        message.obj = display;
                        handlerToMain.sendMessage(message);
                        // 端末画面操作結果通知を作成
//                        lRetVal = makeDeviceResponseAdvance(replyPacket, bIsAborted);
                        lRetVal = makeDeviceResponseAdvance(replyPacket, false);      // 異常通知はしない
                    } else {
                        // 不明なパラメータデータ
                        lRetVal = FeliCaClient.FC_ERR_CLIENT_MSG_FORMAT;
                    }
                } else if(paramName.equals("RETRY")) {
                    // リトライ
                    final int datalen = ((_deviceOperate.GetData()[0] << 8) & 0x0000FF00) | (_deviceOperate.GetData()[1] & 0x000000FF);
                    if(datalen > 0) {
                        byte[] opeData = new byte[datalen];
                        System.arraycopy(_deviceOperate.GetData(), 2, opeData, 0, datalen);
                        String str = new String(opeData);
                        Timber.tag("iCAS").i("CLIENT RETRY %s", str);

                        switch(_money) {
                            case MONEY_ID:
                                DeviceClient.RetryID retryID;
                                retryID = getJsonObject(str.getBytes(), DeviceClient.RetryID.class);

                                if (retryID.unFinInfo != null && retryID.unFinRetryFlg != null) {
                                    Timber.tag("iCAS").i("CLIENT RETRY unFinRetryFlg %s code %s", retryID.unFinRetryFlg, retryID.unFinInfo.code);
                                }
                                if(retryID.unFinRetryFlg.equals("true")) {
                                    // 処理未了によるリトライ時はメインスレッドにメッセージ送信
                                    message.what = iCASClient.FC_MSG_TO_MAIN_ON_OPERATE_RETRY_NOEND;
                                    if(retryID.unFinInfo.code.equals("353")) {
                                        retryID.unFinInfo.code = String.valueOf(Integer.parseInt(retryID.unFinInfo.code) + 10000);
                                    }
                                    message.obj = retryID.unFinInfo;
                                    handlerToMain.sendMessage(message);
                                }
//                                lRetVal = makeDeviceResponseAdvance(replyPacket, bIsAborted);
                                lRetVal = makeDeviceResponseAdvance(replyPacket, false);    // 異常通知はしない
                                break;
                            case MONEY_WAON:
                                DeviceClient.RetryWAON retryWAON;
                                retryWAON = getJsonObject(str.getBytes(), DeviceClient.RetryWAON.class);

                                if(retryWAON.unFinRetryFlg.equals("true")) {
                                    if (retryWAON.unFinInfo != null) {
                                        Timber.tag("iCAS").i("CLIENT RETRY unFinRetryFlg %s code %s", retryWAON.unFinRetryFlg, retryWAON.unFinInfo.code);
                                    }
                                    // 処理未了によるリトライ時はメインスレッドにメッセージ送信
                                    message.what = iCASClient.FC_MSG_TO_MAIN_ON_OPERATE_RETRY_NOEND;
                                    message.obj = retryWAON.unFinInfo;
                                    handlerToMain.sendMessage(message);
                                }
//                                lRetVal = makeDeviceResponseAdvance(replyPacket, bIsAborted);
                                lRetVal = makeDeviceResponseAdvance(replyPacket, false);    // 異常通知はしない
                                break;
                            case MONEY_QUICPAY:
                                break;
                            case MONEY_EDY:
                                // 空のJSONが送られてくる
                                break;
                            case MONEY_NANACO:
                                str = new String(opeData, "SJIS");
                                Timber.tag("iCAS").i("CLIENT RETRY %s", str);
                                DeviceClient.Retrynanaco retrynanaco;
                                retrynanaco = getJsonObject(str.getBytes(), DeviceClient.Retrynanaco.class);
                                Timber.tag("iCAS").i("CLIENT RETRY unFinRetryFlg %s", retrynanaco.unFinRetryFlg);
                                if(retrynanaco.unFinInfo != null) {
                                    Timber.tag("iCAS").i("CLIENT RETRY cancelButtonReDispFlg %s dispMsg %s notifyCode %s", retrynanaco.unFinInfo.cancelButtonReDispFlg, retrynanaco.unFinInfo.dispMsg, retrynanaco.unFinInfo.notifyCode);
                                }

                                // nanacoの場合、処理未了データはタイムアウト時に来るためここではメッセージ通知しない
/*
                                    if(retrynanaco.unFinRetryFlg.equals("true")) {
                                        // 処理未了によるリトライ時はメインスレッドにメッセージ送信
                                        message.what = iCASClient.FC_MSG_TO_MAIN_ON_OPERATE_RETRY_NOEND;
                                        message.obj = retrynanaco.unFinInfo;
                                        handlerToMain.sendMessage(message);
                                    }
*/
                                // キャンセルボタン表示フラグを反転して取消ボタン非表示フラグに設定する
                                if(retrynanaco.unFinRetryFlg.equals("true") && retrynanaco.unFinInfo != null) {
                                    message.what = iCASClient.FC_MSG_TO_MAIN_ON_OPERATE_RETRY_NOEND;
                                    if (retrynanaco.unFinInfo.cancelButtonReDispFlg.equals("true")) {
                                        message.obj = false;
                                    } else {
                                        message.obj = true;
                                    }
                                    handlerToMain.sendMessage(message);
                                }

//                                lRetVal = makeDeviceResponseAdvance(replyPacket, bIsAborted);
                                lRetVal = makeDeviceResponseAdvance(replyPacket, false);    // 異常通知はしない
                                break;
                            case MONEY_SUICA:
                                // SuicaにはRETRYはないためここには来ない
                            default:
                                break;
                        }
                    }
                } else if(paramName.equals("RESULT")) {
/*
                    // 決済不成立 ケース５確認用
                    if(test == 1) {
                        // 1回目のRESULTは飛ばす
                        test = 2;
                        return makeDeviceResponseCommon(replyPacket, false);     // 異常通知はしない
                    }
*/
                    final int datalen = ((_deviceOperate.GetData()[0] << 8) & 0x0000FF00) | (_deviceOperate.GetData()[1] & 0x000000FF);
                    if(datalen > 0) {
                        byte[] opeData = new byte[datalen];
                        System.arraycopy(_deviceOperate.GetData(), 2, opeData, 0, datalen);
                        String str = new String(opeData,"SJIS");
                        switch (_money) {
                            case MONEY_SUICA:
                                //                            if(_businessId == iCASClient.BUSINESS_ID_SUICA_REMAIN) {
                                DeviceClient.Result result;
                                result = getJsonObject(str.getBytes(), DeviceClient.Result.class);
                                Timber.tag("iCAS").i("CLIENT RESULT result %s rem %s value %s brem %s code %s", result.result, result.rem, result.value, result.bRem, result.code);

                                message.what = iCASClient.FC_MSG_TO_MAIN_ON_OPERATE_RESULT;
                                message.obj = result;
                                if (result.code != null) {
/*
                                    if(result.code.equals("95") && isPolling) {
                                        // かざし中のタイムアウトは読み出しエラーのためエラーコード差し替え
                                        result.code = "10095";
                                    }
*/
                                    message.arg1 = Integer.parseInt(result.code);
                                }
                                handlerToMain.sendMessage(message);
                                // 端末画面操作結果通知を作成
//                                lRetVal = makeDeviceResponseCommon(replyPacket, bIsAborted);
                                lRetVal = makeDeviceResponseCommon(replyPacket, false);     // 異常通知はしない
                                break;
                            case MONEY_ID:
                                DeviceClient.ResultID resultID;
                                resultID = getJsonObject(str.getBytes(), DeviceClient.ResultID.class);
                                Timber.tag("iCAS").i("CLIENT RESULT result %s value %s rem %s code %s operationResultCode %s slipNo %s termIdentId %s", resultID.result, resultID.value, resultID.rem, resultID.code, resultID.operationResultCode, resultID.slipNo, resultID.termIdentId);
                                Timber.tag("iCAS").i("CLIENT RESULT authErrCode %s authErrMsg %s", resultID.authErrCode, resultID.authErrMsg);

                                message.what = iCASClient.FC_MSG_TO_MAIN_ON_OPERATE_RESULT;
                                message.obj = resultID;
                                if (resultID.code != null) {
                                    if(resultID.code.equals("95") && isPolling) {
                                        // かざし中のタイムアウトは読み出しエラーのためエラーコード差し替え
                                        resultID.code = "10095";
                                    }
                                    message.arg1 = Integer.parseInt(resultID.code);
                                    // iDでは別文言を表示するためエラーコードを書き換える
                                    if(resultID.code.equals("87") || resultID.code.equals("88") || resultID.code.equals("353")) {
                                        message.arg1 = Integer.parseInt(resultID.code) + 10000;
                                        resultID.code = String.valueOf(message.arg1);
                                    }
                                }
                                handlerToMain.sendMessage(message);
                                // 端末画面操作結果通知を作成
//                                lRetVal = makeDeviceResponseCommon(replyPacket, bIsAborted);
                                lRetVal = makeDeviceResponseCommon(replyPacket, false);     // 異常通知はしない
                                break;
                            case MONEY_WAON:
                                DeviceClient.ResultWAON resultWaon;
                                resultWaon = getJsonObject(str.getBytes(), DeviceClient.ResultWAON.class);
                                Timber.tag("iCAS").i("CLIENT RESULT result %s value %s balance %s code %s slipNo %s termIdentId %s unFinFlg %s", resultWaon.result, resultWaon.value, resultWaon.balance, resultWaon.code, resultWaon.slipNo, resultWaon.termIdentId, resultWaon.unFinFlg);
                                Timber.tag("iCAS").i("CLIENT RESULT authErrCode %s authErrMsg %s", resultWaon.authErrCode, resultWaon.authErrMsg);

                                message.what = iCASClient.FC_MSG_TO_MAIN_ON_OPERATE_RESULT;
                                message.obj = resultWaon;
                                if (resultWaon.code != null) {
/*
                                    if(resultWaon.code.equals("95") && isPolling && resultWaon.unFinFlg != null && resultWaon.unFinFlg.equals("false")) {
                                        // かざし中のタイムアウトは読み出しエラーのためエラーコード差し替え
                                        resultWaon.code = "10095";
                                    }
*/
                                    message.arg1 = Integer.parseInt(resultWaon.code);
                                    // WAONの未ネガ・ネガ化エラーは別文言を表示するためエラーコードを書き換える
                                    if(resultWaon.code.equals("92") || resultWaon.code.equals("83")) {
                                        message.arg1 = Integer.parseInt(resultWaon.code) + 10000;
                                        resultWaon.code = String.valueOf(message.arg1);
                                    }
                                }
                                if(resultWaon.unFinFlg != null && resultWaon.unFinFlg.equals("true")) {
                                    _autoRetryFlg = true;
                                } else {
                                    _autoRetryFlg = false;
                                }
                                handlerToMain.sendMessage(message);
                                // 端末画面操作結果通知を作成
//                                lRetVal = makeDeviceResponseCommon(replyPacket, bIsAborted);
                                lRetVal = makeDeviceResponseCommon(replyPacket, false);     // 異常通知はしない
                                break;
                            case MONEY_QUICPAY:
                                DeviceClient.ResultQUICPay resultQUICPay;
                                resultQUICPay = getJsonObject(str.getBytes(), DeviceClient.ResultQUICPay.class);
                                Timber.tag("iCAS").i("CLIENT RESULT result %s code %s operationResultCode %s sid %s slipNo %s termIdentId %s", resultQUICPay.result, resultQUICPay.code, resultQUICPay.operationResultCode, resultQUICPay.sid, resultQUICPay.slipNo, resultQUICPay.termIdentId);
                                Timber.tag("iCAS").i("CLIENT RESULT authErrCode %s authErrMsg %s", resultQUICPay.authErrCode, resultQUICPay.authErrMsg);
                                int cnt = 1;
                                if (resultQUICPay.addInfo.historyData != null) {
                                    for (DeviceClient.HistoryDataQUICPay history : resultQUICPay.addInfo.historyData) {
                                        Timber.tag("iCAS").i("addInfo %d date %s trade %s termIdentId %s dealingsThroughNum %s value %s", cnt++, history.date, history.trade, history.termIdentId, history.dealingsThroughNum, history.value);
                                    }
                                }

                                message.what = iCASClient.FC_MSG_TO_MAIN_ON_OPERATE_RESULT;
                                message.obj = resultQUICPay;
                                if (resultQUICPay.code != null) {
/*
                                    if(resultQUICPay.code.equals("95") && isPolling) {
                                        // かざし中のタイムアウトは読み出しエラーのためエラーコード差し替え
                                        resultQUICPay.code = "10095";
                                    }
*/
                                    message.arg1 = Integer.parseInt(resultQUICPay.code);
                                }
                                handlerToMain.sendMessage(message);
                                // 端末画面操作結果通知を作成
//                                lRetVal = makeDeviceResponseCommon(replyPacket, bIsAborted);
                                lRetVal = makeDeviceResponseCommon(replyPacket, false);     // 異常通知はしない
                                break;
                            case MONEY_EDY:
                                DeviceClient.ResultEdy resultEdy;
                                resultEdy = getJsonObject(str.getBytes(), DeviceClient.ResultEdy.class);
                                if(resultEdy.saleHistories != null) {
                                    Timber.tag("iCAS").i("CLIENT RESULT result %s code %s cardResultCode %s sid %s autoRetryFlg %s forceBalanceFlg %s termIdentId %s edyTransactionNo %s cardTransactionNo %s", resultEdy.result, resultEdy.code, resultEdy.saleHistories[0].cardResultCode, resultEdy.sid, resultEdy.autoRetryFlg, resultEdy.forcedBalanceFlg, resultEdy.termIdentId, resultEdy.saleHistories[0].edyTransactionNo, resultEdy.saleHistories[0].cardTransactionNo);
                                } else {
                                    Timber.tag("iCAS").i("CLIENT RESULT result %s code %s sid %s autoRetryFlg %s forceBalanceFlg %s", resultEdy.result, resultEdy.code, resultEdy.sid, resultEdy.autoRetryFlg, resultEdy.forcedBalanceFlg);
                                }

                                message.what = iCASClient.FC_MSG_TO_MAIN_ON_OPERATE_RESULT;
                                message.obj = resultEdy;
                                if (resultEdy.code != null) {
/*
                                    if(resultEdy.code.equals("95") && isPolling) {
                                        // かざし中のタイムアウトは読み出しエラーのためエラーコード差し替え
                                        resultEdy.code = "10095";
                                    }
*/
/*
                                    // Edy閉塞試験用
                                    resultEdy.code = "1027";
*/
                                    message.arg1 = Integer.parseInt(resultEdy.code);
                                }
                                if(resultEdy.autoRetryFlg != null && resultEdy.autoRetryFlg.equals("true")) {
                                    _autoRetryFlg = true;
                                } else {
                                    _autoRetryFlg = false;
                                }
                                handlerToMain.sendMessage(message);
                                // 端末画面操作結果通知を作成
//                                lRetVal = makeDeviceResponseCommon(replyPacket, bIsAborted);
                                lRetVal = makeDeviceResponseCommon(replyPacket, false);     // 異常通知はしない
                                break;
                            case MONEY_NANACO:
                                DeviceClient.Resultnanaco resultnanaco;
                                resultnanaco = getJsonObject(str.getBytes(), DeviceClient.Resultnanaco.class);
                                if(resultnanaco.saleHistories != null) {
                                    Timber.tag("iCAS").i("CLIENT RESULT result %s code %s cardResultCode %s sid %s", resultnanaco.result, resultnanaco.code, resultnanaco.saleHistories[0].cardResultCode, resultnanaco.sid);
                                } else {
                                    Timber.tag("iCAS").i("CLIENT RESULT result %s code %s sid %s", resultnanaco.result, resultnanaco.code, resultnanaco.sid);
                                }

                                message.what = iCASClient.FC_MSG_TO_MAIN_ON_OPERATE_RESULT;
                                message.obj = resultnanaco;
                                if (resultnanaco.code != null) {
/*
                                    if(resultnanaco.code.equals("95") && isPolling) {
                                        // かざし中のタイムアウトは読み出しエラーのためエラーコード差し替え
                                        resultnanaco.code = "10095";
                                    }
*/
                                    message.arg1 = Integer.parseInt(resultnanaco.code);
                                }
                                handlerToMain.sendMessage(message);
                                // 端末画面操作結果通知を作成
//                                lRetVal = makeDeviceResponseCommon(replyPacket, bIsAborted);
                                lRetVal = makeDeviceResponseCommon(replyPacket, false);     // 異常通知はしない
                                break;
                            default:
                                // 不明なパラメータデータ
                                return FeliCaClient.FC_ERR_CLIENT_MSG_FORMAT;
                        }
/*
                        // 決済不成立 ケース２確認用
                        Timber.tag("iCAS").d("★★★★★ ジャマー開始 ★★★★★");
                        try {
                            Thread.sleep(30000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
*/
                    }
                } else {
                    // 不明なパラメータ名
                    lRetVal = FeliCaClient.FC_ERR_CLIENT_MSG_FORMAT;
                }
                break;
            case DEVICE_ID_RW_UI:
                if(paramName.equals("R/W_PARAM")) {
                    // R/W操作
                    DeviceClient.RWParam rwParam;
                    final int datalen = ((_deviceOperate.GetData()[0] << 8) & 0x0000FF00) | (_deviceOperate.GetData()[1] & 0x000000FF);
                    if(datalen > 0) {
                        byte[] opeData = new byte[datalen];
                        System.arraycopy(_deviceOperate.GetData(), 2, opeData, 0, datalen);
                        final String str = new String(opeData, "Shift_JIS");
                        rwParam = getJsonObject(str.getBytes(), DeviceClient.RWParam.class);
/*
                        // Edy閉塞試験用
                        if(_money == MONEY_EDY) {
                            rwParam.lcd1[0] = "E00-1-000";
                            rwParam.lcd2[0] = "E00-2-000";
                            rwParam.lcd3[0] = "E00-3-001";
                        }
*/
                        if(rwParam.lcd1 != null) {
                            Timber.tag("iCAS").i("RW_UI R/W_PARAM lcd1 0:%s 1:%s 2:%s", rwParam.lcd1[0], rwParam.lcd1[1], rwParam.lcd1[2]);
                        }
                        if(rwParam.lcd2 != null) {
                            Timber.tag("iCAS").i("RW_UI R/W_PARAM lcd2 0:%s 1:%s 2:%s", rwParam.lcd2[0], rwParam.lcd2[1], rwParam.lcd2[2]);
                        }
                        if(rwParam.lcd3 != null) {
                            Timber.tag("iCAS").i("RW_UI R/W_PARAM lcd3 0:%s 1:%s 2:%s", rwParam.lcd3[0], rwParam.lcd3[1], rwParam.lcd3[2]);
                        }
//                        Timber.tag("iCAS").i("RW_UI R/W_PARAM lcd1 %s lcd2 %s lcd3 %s time %s", lcd1, lcd2, lcd3, time);
                        if (rwParam.bar != null) {
                            Timber.tag("iCAS").i("RW_UI R/W_PARAM bar0 %d bar1 %d bar2 %d", rwParam.bar[0], rwParam.bar[1], rwParam.bar[2]);
                        }
                        if (rwParam.ring != null) {
                            Timber.tag("iCAS").i("RW_UI R/W_PARAM ring0 %d ring1 %d ring2 %d", rwParam.ring[0], rwParam.ring[1], rwParam.ring[2]);
                        }
                        if (rwParam.sound != null) {
                            Timber.tag("iCAS").i("RW_UI R/W_PARAM sound0 %d sound1 %d", rwParam.sound[0], rwParam.sound[1]);

                            if(!_isNoEnd && rwParam.sound[1] == 6) {
                                // 処理未了発生時はキャンセル不可とするためフラグをセット
                                _isNoEnd = true;
                                Timber.tag("iCAS").i("NoEnd Cancel Disable");
                                message.what = iCASClient.FC_MSG_TO_MAIN_ON_OPERATE_CANCEL_FINAL;
                                message.arg1 = 1;
                                handlerToMain.sendMessage(message);
                                message = new Message();
                            }
                        }

                        message.what = iCASClient.FC_MSG_TO_MAIN_ON_OPERATE_RW_PARAM;
                        message.obj = rwParam;
                        handlerToMain.sendMessage(message);
                        // R/Wデバイス操作結果通知を作成
//                        lRetVal = makeDeviceResponseCommon(replyPacket, bIsAborted);
                        lRetVal = makeDeviceResponseCommon(replyPacket, false);     // 異常通知はしない
                    } else {
                        // 不明なパラメータデータ
                        lRetVal = FeliCaClient.FC_ERR_CLIENT_MSG_FORMAT;
                    }
                } else {
                    // 不明なパラメータ名
                    lRetVal = FeliCaClient.FC_ERR_CLIENT_MSG_FORMAT;
                }
                break;
            case DEVICE_ID_PINPAD_UI:
                if(paramName.equals("OPERATION")) {
                    // 暗証番号データ初期化
                    iCASClient.SetPinData(null, 0);
                    iCASClient.SetPinCancel(false);
                    // PINパッド操作
                    DeviceClient.Operation operation;
                    final int datalen = ((_deviceOperate.GetData()[0] << 8) & 0x0000FF00) | (_deviceOperate.GetData()[1] & 0x000000FF);
                    if(datalen > 0) {
                        if(!bIsAborted) {
                            byte[] opeData = new byte[datalen];
                            System.arraycopy(_deviceOperate.GetData(), 2, opeData, 0, datalen);
                            final String str = new String(opeData, "Shift_JIS");
                            operation = getJsonObject(str.getBytes(), DeviceClient.Operation.class);
                            Timber.tag("iCAS").i("iCASDevice Operate input %s value %s timeout %s keyId %s", operation.inputRequest, operation.value, operation.timeout, operation.keyId);
                            message.what = iCASClient.FC_MSG_TO_MAIN_ON_OPERATE_OPERATION;
                            message.obj = operation;
                            handlerToMain.sendMessage(message);
                        }
                        // PINパッド操作結果通知を作成
//                        lRetVal = makeDeviceResponseAdvance(replyPacket, bIsAborted);
                        lRetVal = makeDeviceResponseAdvance(replyPacket, false);    // 異常通知はしない
                    } else {
                        // 不明なパラメータデータ
                        lRetVal = FeliCaClient.FC_ERR_CLIENT_MSG_FORMAT;
                    }
                } else if(paramName.equals("CONFIRM")) {
                    // PINパッド入力
                    DeviceClient.Confirm confirm;
                    final int datalen = ((_deviceOperate.GetData()[0] << 8) & 0x0000FF00) | (_deviceOperate.GetData()[1] & 0x000000FF);
                    if(datalen > 0) {
                        byte[] opeData = new byte[datalen];
                        System.arraycopy(_deviceOperate.GetData(), 2, opeData, 0, datalen);
                        final String str = new String(opeData, "Shift_JIS");
                        confirm = getJsonObject(str.getBytes(), DeviceClient.Confirm.class);
                        Timber.tag("iCAS").i("iCASDevice Operate confirm %s waittime %s", confirm.confirm, confirm.waittime);

                        if(!bIsAborted) {
                            // 暗証番号が入力されているか待ち時間内で確認
                            int cnt;
                            int waittime = Integer.parseInt(confirm.waittime);

                            if(waittime > 500) {
                                cnt = waittime / 500;
                            } else {
                                cnt = 1;
                            }
                            // 500msec余裕を持って応答を返却する
                            for(int i = 0; i < cnt; i++) {
                                if(iCASClient.GetPinData() != null) {
                                    // pin入力された
                                    Timber.tag("iCAS").i("iCASDevice Operate confirm pin input!");
                                    message.what = iCASClient.FC_MSG_TO_MAIN_ON_OPERATE_CONFIRM;
                                    handlerToMain.sendMessage(message);
                                    break;
                                } else if(iCASClient.GetPinCancel()) {
                                    break;
                                }
                                // 入力されていないのでsleepして次の確認へ
                                Timber.tag("iCAS").i("iCASDevice Operate confirm sleep 500msec");
                                Thread.sleep(500);
                            }
                        }
                        // PINパッド入力結果通知を作成
                        lRetVal = makeDeviceResponseConfirm(replyPacket, iCASClient.GetPinCancel() | bIsAborted);
                    } else {
                        // 不明なパラメータデータ
                        lRetVal = FeliCaClient.FC_ERR_CLIENT_MSG_FORMAT;
                    }
                } else {
                    // 不明なパラメータ名
                    lRetVal = FeliCaClient.FC_ERR_CLIENT_MSG_FORMAT;
                }
                break;
            default:
                // 不明なID
                lRetVal = FeliCaClient.FC_ERR_CLIENT_MSG_FORMAT;
                break;
        }

        return lRetVal;
    }
    public long makeDeviceResponseCommon(TCAPPacket replyPacket, boolean bIsAborted) {
        long lRetVal;
        // 共通の結果通知を作成（CANCEL, STATUS, R/W_PARAM）
        byte[] data = new byte[2 + 2 + 1];  // 予約2byte + 応答データの長さ2byte + data
        TCAPMessage tcapMessage = new TCAPMessage((char) TCAPPacket.TCAP_MSG_EXT_STANDARD, _deviceOperate.GetDeviceID(), (char) TCAPPacket.TCAP_MSG_MT_DEVICE_RESPONSE);
        data[2] = 0x00;
        data[3] = 0x01;     // データサイズ
        if(bIsAborted) {
            data[4] = 0x01;     // 異常終了（処理中止）
        } else {
            data[4] = 0x00;     // 正常終了（処理継続）
        }
        lRetVal = tcapMessage.SetMessageData(data, data.length);
        if(lRetVal == FeliCaClient.FC_ERR_SUCCESS) {
            lRetVal = replyPacket.AddMessage(tcapMessage);
        }

        return lRetVal;
    }

    public long makeDeviceResponseAdvance(TCAPPacket replyPacket, boolean bIsAborted) {
        long lRetVal;
        // 結果通知を作成（DISPLAY, OPERATION, RETRY）
        byte[] data = new byte[2 + 2 + 2];  // 予約2byte + 応答データの長さ2byte + data
        TCAPMessage tcapMessage = new TCAPMessage((char) TCAPPacket.TCAP_MSG_EXT_STANDARD, _deviceOperate.GetDeviceID(), (char) TCAPPacket.TCAP_MSG_MT_DEVICE_RESPONSE);
        data[2] = 0x00;
        data[3] = 0x02;     // データサイズ
        // 応答データ
        if(bIsAborted) {
            data[4] = 0x01;     // 異常終了（処理中止）
        } else {
            data[4] = 0x00;     // 正常終了（処理継続）
        }
        // メッセージバージョン
        data[5] = 0x00;
        lRetVal = tcapMessage.SetMessageData(data, data.length);
        if(lRetVal == FeliCaClient.FC_ERR_SUCCESS) {
            lRetVal = replyPacket.AddMessage(tcapMessage);
        }

        return lRetVal;
    }

    public long makeDeviceResponseConfirm(TCAPPacket replyPacket, boolean bIsAborted) {
        long lRetVal;
        int pinLength;
        byte[] pinData;
        boolean bInput;

        bInput = iCASClient.GetPinData() != null;

        pinData = iCASClient.GetPinData();
        pinLength = iCASClient.GetPinLength();

        // ピンパッド入力確認結果
        byte[] data = new byte[2 + 2 + 5+pinLength];  // 予約2byte + 応答データの長さ2byte + data(5+暗号化した暗証番号の長さ)
        Arrays.fill(data, (byte)0);
        TCAPMessage tcapMessage = new TCAPMessage((char) TCAPPacket.TCAP_MSG_EXT_STANDARD, _deviceOperate.GetDeviceID(), (char) TCAPPacket.TCAP_MSG_MT_DEVICE_RESPONSE);
        data[2] = (byte)(((5+pinLength) & 0x0000FF00) >> 8);
        data[3] = (byte)((5+pinLength) & 0x000000FF);     // データサイズ
        // リターンコード
        if(bIsAborted || !bInput) {
            data[4] = 0x01;     // 異常終了（処理中止）
        } else {
            data[4] = 0x00;     // 正常終了（処理継続）
        }
        // メッセージバージョン
        data[5] = 0x00;
        // エラーコード
        if(data[4] == 0x00) {
            data[6] = 0x00;
        } else if(bIsAborted) {
            data[6] = 0x01;     // キャンセル or タイムアウト
        } else if(!bInput) {
            data[6] = 0x03;     // 暗証番号未入力
        }

        if(pinData != null) {
            // Data長
            data[7] = (byte)((pinLength & 0x0000FF00) >> 8);
            data[8] = (byte)(pinLength & 0x000000FF);     // データサイズ
            // Data
            System.arraycopy(Objects.requireNonNull(pinData), 0, data, 9, pinLength);
        }

        lRetVal = tcapMessage.SetMessageData(data, data.length);
        if(lRetVal == FeliCaClient.FC_ERR_SUCCESS) {
            lRetVal = replyPacket.AddMessage(tcapMessage);
        }

        return lRetVal;
    }

    public <T> T getJsonObject(byte[] data, Class<T> classOfT) {
        final String json = new String(data);
        final Object object = _gson.fromJson(json, (Type) classOfT);
        return Primitives.wrap(classOfT).cast(object);
    }
}
