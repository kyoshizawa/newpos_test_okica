package jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.FeliCaChip;

import java.util.Arrays;

import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.FeliCaClient.DeviceElement;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.FeliCaClient.IFeliCaChip;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.FeliCaClient.IFeliCaClientEventListener;
import timber.log.Timber;

public class FeliCaChip extends DeviceElement implements IFeliCaChip {
    private static final int DEVICE_INFO_LENGTH = 255;          // デバイス情報の最大サイズ
    private static final int MAX_COMMAND_LENGTH = 255;          // 送受信可能な最大サイズ
//    private static final int COMMAND_HEADER_LENGTH = 5;         // ヘッダサイズ
    private static final int RESPONSE_TAILER_LENGTH = 2;        // テーラサイズ
    private static final int MIN_COMMAND_TIMEOUT = 10;          // コマンドに設定できるタイムアウトの最小値(単位：ミリ秒)
    private static final int MAX_COMMAND_TIMEOUT = 0xFE * 10;   // コマンドに設定できるタイムアウトの最大値(単位：ミリ秒)

    //    private Handler _handler;           // メインスレッドへのイベントハンドラ
    private IFeliCaClientEventListener _eventListener;  // イベントリスナ
    private byte[] _type;                   // デバイスタイプ
    private byte[] _name;                   // デバイス名称
    private byte[] _commandPacketData;      // デバイス操作コマンド
    private final byte[] _responsePacketData;     // デバイス操作実行結果
    private long _timeout;                  // デバイス操作タイムアウト(単位：ミリ秒)

    public FeliCaChip(char deviceID) {
        super(deviceID);
        _timeout = 1000;
        _type = null;
        _name = null;

//        _commandPacketData = new byte[MAX_COMMAND_LENGTH + COMMAND_HEADER_LENGTH];
        _commandPacketData = null;
        _responsePacketData = new byte[MAX_COMMAND_LENGTH + RESPONSE_TAILER_LENGTH];

/*
        // フェリカデバイスのデバイス種別は「FeliCa」固定
        SetType("FeliCa".getBytes());
*/

        // レスポンス格納バッファを初期化
        Arrays.fill(_responsePacketData, (byte)0);

/*
        // デバイス操作コマンドのヘッダを設定
        _commandPacketData[0] = (byte)0xFF;   // CLA = 0xFF(Data Exchange)
        _commandPacketData[1] = (byte)0xFE;   // INS = 0xFE(Data Exchange)
        _commandPacketData[2] = (byte)0x01;   // P1 = Mode = 0x01(Direct通信)
        _commandPacketData[3] = (byte)(_timeout / 10);   // P2 = Timeout(単位：10ミリ秒)
*/
    }

    //    public void SetHandler(Handler handler) { _handler = handler; }
    public void SetListener(IFeliCaClientEventListener listener) { _eventListener = listener; }

    @Override
    public byte[] GetType() {
        return _type;
    }

    @Override
    public void SetType(byte[] deviceType) {
        if(deviceType != null) {
            if(deviceType.length <= DEVICE_INFO_LENGTH) {
                _type = new byte[deviceType.length];
                System.arraycopy(deviceType, 0, _type, 0, _type.length);
            }
        }
    }

    @Override
    public byte[] GetName() {
        return _name;
    }

    @Override
    public void SetName(byte[] deviceName) {
        if(deviceName != null) {
            if(deviceName.length <= DEVICE_INFO_LENGTH) {
                _name = new byte[deviceName.length];
                System.arraycopy(deviceName, 0, _name, 0, _name.length);
            }
        }
    }

    @Override
    public long Open() {
        long lRetVal;

        // オープン処理は不要のため常に成功で返す
        lRetVal = errorCode.ERR_NONE.getInt();

        return lRetVal;
    }

    @Override
    public void Cancel() {
        // SDK for FeliCa にキャンセル処理はない
    }

    @Override
    public long Close() {
        long lRetVal;

        // クローズ処理は不要ため常に成功で返す
        lRetVal = errorCode.ERR_NONE.getInt();

        // 計測用にiCASClientに通知
        _eventListener.OnRWClose();

        return lRetVal;
    }

    @Override
    public long Execute(byte[] command, long commandLength) {
        int responseLength;
        long timeout = _timeout;
        long commandTimeout;
        long lRetVal;

        if(command == null || commandLength < 1 || MAX_COMMAND_LENGTH < commandLength) {
            // パラメータ不正
            lRetVal = errorCode.ERR_BAD_PARAM.getInt();
        } else {
            // レスポンス格納バッファを初期化
            Arrays.fill(_responsePacketData, (byte)0);

            // 送信タイムアウト値の丸め込み
            commandTimeout = CaluculateCommandTimeout(timeout);

/*
            // 送信コマンド設定
            long length = CreateCommandPacket(command, commandTimeout);
*/

            // FeliCaコマンド実行
//            lRetVal = _eventListener.OnTransmitRW(_commandPacketData, length, _responsePacketData);
            _commandPacketData = new byte[command.length];
            System.arraycopy(command, 0, _commandPacketData, 0, command.length);
            lRetVal = _eventListener.OnTransmitRW(_commandPacketData, commandTimeout, _responsePacketData);
            responseLength = (int)lRetVal;

            // 現在のタイムアウト値からコマンド送信時に設定したタイムアウト値を減算する
            timeout = UpdateTimeout(timeout, commandTimeout);

            if(lRetVal < IFeliCaClientEventListener.errorType.TYPE_SUCCESS.getInt()) {
                // 実行失敗
                lRetVal = errorCode.ERR_EXCUTE.getInt();
            } else if(responseLength < 2) {
                Timber.tag("iCAS").i("responseLength error %d!!!", responseLength);
                // 実行失敗? レングス０の場合エラーとする
                lRetVal = errorCode.ERR_EXCUTE.getInt();
            } else if(((_responsePacketData[responseLength-2] & 0x000000FF) == 0x63) && (_responsePacketData[responseLength-1] == 0x00)) {
                // (SW1, SW2) = (0x63, 0x00)の場合はタイムアウトエラー
                // ただし、本来のタイムアウト値が残っている間は受信をリトライする
                lRetVal = RetryReceiveResponsePacket(timeout);
            } else if(((_responsePacketData[responseLength-2] & 0x000000FF) == 0x90) && (_responsePacketData[responseLength-1] == 0x00)) {
                // (SW1, SW2) != (0x90, 0x00)の場合はエラーレスポンス
                //FeliCaコマンド実行失敗
                lRetVal = errorCode.ERR_EXCUTE.getInt();
            } else {
                // コマンド実行成功
                lRetVal = errorCode.ERR_NONE.getInt();
            }
        }

        return lRetVal;
    }

    @Override
    public long ExecuteThru(byte[] command, long commandLength) {
        return Execute(command, commandLength);
    }

    @Override
    public void SetTimeout(long timeoutMilliseconds) {
        _timeout = timeoutMilliseconds;
    }

    @Override
    public long GetTimeout() {
        return _timeout;
    }

/*
    public long CreateCommandPacket(byte[] felicaCommand, long timeout) {
        // タイムアウト値設定（単位：ミリ秒）
        _commandPacketData[3] = (byte)(timeout/10);     // P2 = Timeout(単位：10ミリ秒)
        _commandPacketData[4] = felicaCommand[0];       // Lc = コマンド長
        System.arraycopy(felicaCommand, 0, _commandPacketData, 5, felicaCommand[0]);
        return felicaCommand[0] + COMMAND_HEADER_LENGTH;
    }
*/

    /**
     * 受信のみ行うコマンドをタイムアウトになるまで送信します
     *
     * @param remainingRetryTime コマンドタイムアウト値
     *
     * @return エラーコード
     */
    public long RetryReceiveResponsePacket(long remainingRetryTime) {
//        long commandLength;
        int responseLength;
        long lRetVal;
        long timeout = remainingRetryTime;
        long commandTimeout;

        while(timeout > 0) {
            // レスポンス格納バッファを初期化
            Arrays.fill(_responsePacketData, (byte)0);

            // タイムアウト値の丸め込み
            commandTimeout = CaluculateCommandTimeout(timeout);
//            commandLength = CreateReceiveCommandPacket(commandTimeout);

            // 受信実行
//            lRetVal = _eventListener.OnTransmitRW(_commandPacketData, commandLength, _responsePacketData);
            lRetVal = _eventListener.OnTransmitRW(_commandPacketData, commandTimeout, _responsePacketData);
            responseLength = (int)lRetVal;

            if(lRetVal != IFeliCaClientEventListener.errorType.TYPE_SUCCESS.getInt()) {
                // 実行失敗
                return errorCode.ERR_EXCUTE.getInt();
            } else if(((_responsePacketData[responseLength-2] & 0x000000FF) == 0x63) && (_responsePacketData[responseLength-1] == 0x00)) {
                // (SW1, SW2) = (0x63, 0x00)の場合はタイムアウトエラー
                // タイムアウト値から今回の受信で待機した時間を減算しておく
                timeout = UpdateTimeout(timeout, commandTimeout);
            } else if(((_responsePacketData[responseLength-2] & 0x000000FF) == 0x90) && (_responsePacketData[responseLength-1] == 0x00)) {
                // (SW1, SW2) != (0x90, 0x00)の場合はエラーレスポンス
                //FeliCaコマンド実行失敗
                return errorCode.ERR_EXCUTE.getInt();
            } else {
                // コマンド実行成功
                return errorCode.ERR_NONE.getInt();
            }
        }

        return errorCode.ERR_TIMEOUT.getInt();
    }


/*
    public long CreateReceiveCommandPacket( long commandTimeout ) {
        _commandPacketData[3] = (byte)(commandTimeout / 10);            // P2 = Timeout(単位：10ミリ秒)
        _commandPacketData[4] = 0;                                      // Lc = コマンド長
        return COMMAND_HEADER_LENGTH;
    }
*/

    /**
     * 直近のコマンドのタイムアウト値から次のタイムアウト値を更新する
     *
     * @return タイムアウト値
     */
    public long UpdateTimeout(long timeout, long lastCommandTimeout) {
        if(timeout <= lastCommandTimeout) {
            return 0;
        } else {
            return timeout - lastCommandTimeout;
        }
    }

    /**
     * タイムアウト値の丸め込みを行います
     *
     * @return タイムアウト値
     */
    public long CaluculateCommandTimeout(long timeout) {
        if(timeout <= MIN_COMMAND_TIMEOUT) {
            return MIN_COMMAND_TIMEOUT;
        } else if(timeout >= MAX_COMMAND_TIMEOUT) {
            return MAX_COMMAND_TIMEOUT;
        } else {
            return timeout;
        }
    }

    @Override
    public byte[] GetResponse() {
        return _responsePacketData;
    }

    @Override
    public int GetResponseLength() {
        return (_responsePacketData[0] & 0x000000FF);
    }
}
