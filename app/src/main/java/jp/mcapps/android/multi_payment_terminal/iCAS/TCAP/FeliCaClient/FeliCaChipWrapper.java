package jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.FeliCaClient;

public class FeliCaChipWrapper extends DeviceElement {
    public static final int RETRY_COUNT_DEFAULT    = 3;           // リトライカウント初期値
    public static final int RETRY_COUNT_MIN        = 0;           // リトライカウント最小値
    public static final int RETRY_COUNT_MAX        = 10;          // リトライカウント最大値
    public static final int FELICA_TIMEOUT_DEFAULT = 1000;        // タイムアウト初期値
    public static final int FELICA_TIMEOUT_MIN     = 0;           // タイムアウト最小値
    public static final int FELICA_TIMEOUT_MAX     = 60000;       // タイムアウト最大値


    private final IFeliCaChip _iFeliCaChip;           // FeliCaチップオブジェクトへの参照
    private long _retryCount;                   // チップアクセスリトライカウント

    public FeliCaChipWrapper(char ID, IFeliCaChip iFeliCaChip) {
        super(ID);

        _iFeliCaChip = iFeliCaChip;
        _retryCount = RETRY_COUNT_DEFAULT;
        _iFeliCaChip.SetTimeout(FELICA_TIMEOUT_DEFAULT);
    }

    /**
     * デバイス種別取得
     * デバイスの種別を取得します。
     *
     * @return デバイス種別文字列
     */
    public final byte[] GetType() {
        return _iFeliCaChip.GetType();
    }

    /**
     * デバイス名称取得
     * デバイスの名称を取得します。
     *
     * @return デバイス名称文字列
     */
    public final byte[] GetName() {
        return _iFeliCaChip.GetName();
    }

    /**
     * データ比較
     * デバイスデータを比較します。
     *
     * @param type デバイス種別
     * @param name デバイス名称
     *
     * @return true :一致
     *         false:不一致
     */
    public boolean Compare(final byte[] type, final byte[] name) {
        boolean bRetVal = false;
        final String deviceType = new String(GetType());
        final String deviceName = new String(GetName());

        if(type == null || name == null) {
            // パラメータエラー
        } else if(deviceType == null || deviceName == null) {
            // 内部エラー
        } else {
            final String typeStr = new String(type);
            final String nameStr = new String(name);

            if(!typeStr.equals(deviceType)) {
                // タイプが異なる
            } else if(!nameStr.equals(deviceName)) {
                // 名前が異なる
            } else {
                bRetVal = true;
            }
        }

        return bRetVal;
    }

    /**
     * リトライカウント値設定
     * リトライカウント値を設定します。
     *
     * @param retryCount リトライカウント値
     */
    public void SetRetryCount(long retryCount) {
        if(retryCount < RETRY_COUNT_MIN) {
            _retryCount = RETRY_COUNT_MIN;
        } else if(RETRY_COUNT_MAX < retryCount) {
            _retryCount = RETRY_COUNT_MAX;
        } else {
//            int cnt = (int)((_iFeliCaChip.GetTimeout() * retryCount) / 1000);
            _retryCount = retryCount;
//            _retryCount = cnt;
        }
    }

    /**
     * リトライカウント値取得
     * リトライカウント値を取得します。
     *
     * @return リトライカウント値
     */
    public long GetRetryCount() {
        return _retryCount;
    }

    /**
     * タイムアウト値設定
     * タイムアウト値を設定します。
     *
     * @param timeout タイムアウト値(ミリ秒)
     */
    public void SetTimeout(long timeout) {
        if(timeout < FELICA_TIMEOUT_MIN) {
            timeout = FELICA_TIMEOUT_MIN;
        } else if(FELICA_TIMEOUT_MAX < timeout) {
            timeout = FELICA_TIMEOUT_MAX;
        }
        _iFeliCaChip.SetTimeout(timeout);
    }

    /**
     * タイムアウト値取得
     * タイムアウト値を取得します。
     *
     * @return タイムアウト値(ミリ秒)
     */
    public long GetTimeout() {
        return _iFeliCaChip.GetTimeout();
    }

    /**
     * FeliCaChipオープン
     * FeliCaChipデバイスをオープンします。
     *
     * @return エラーコード
     */
    public long Open() {
        long lRetVal = FeliCaClient.FC_ERR_CLIENT_UNKNOWN;

        if(_iFeliCaChip.Open() == IFeliCaChip.errorCode.ERR_NONE.getInt()) {
            lRetVal = FeliCaClient.FC_ERR_SUCCESS;
        }

        return lRetVal;
    }

    /**
     * FeliCaChipクローズ
     * FeliCaChipデバイスをクローズします。
     *
     * @return エラーコード
     */
    public long Close() {
        long lRetVal = FeliCaClient.FC_ERR_CLIENT_UNKNOWN;

        if(_iFeliCaChip.Close() == IFeliCaChip.errorCode.ERR_NONE.getInt()) {
            lRetVal = FeliCaClient.FC_ERR_SUCCESS;
        }

        return lRetVal;
    }

    /**
     * FeliCaChipコマンド実行
     * FeliCaChipデバイスに対するコマンドを実行します。
     * 失敗した場合はこのオブジェクトに設定されているリトライカウントの数だけ
     * リトライを内部で行います。
     *
     * @param command       コマンドデータ
     * @param commandLength コマンドデータ長
     *
     * @return エラーコード
     */
    public long Execute(final byte[] command, long commandLength) {
        long lRetVal = FeliCaClient.FC_ERR_CLIENT_UNKNOWN;

        for(int i = 0; i <= _retryCount; i++) {
            lRetVal = _iFeliCaChip.Execute(command, commandLength);
            if(lRetVal == IFeliCaChip.errorCode.ERR_TIMEOUT.getInt()) {
                continue;   // リトライ
            }

            if(lRetVal == IFeliCaChip.errorCode.ERR_NONE.getInt()) {
                lRetVal = FeliCaClient.FC_ERR_SUCCESS;
            }
            break;
        }

        return lRetVal;
    }

    /**
     * FeliCaChip Thruコマンド実行
     * FeliCaChipデバイスに対するThruコマンドを実行します。
     * 失敗した場合はこのオブジェクトに設定されているリトライカウントの数だけ
     * リトライを内部で行います。
     *
     * @param command       コマンドデータ
     * @param commandLength コマンドデータ長
     *
     * @return エラーコード
     */
    public long ExecuteThru(final byte[] command, long commandLength) {
        long lRetVal = FeliCaClient.FC_ERR_CLIENT_UNKNOWN;

        for(int i = 0; i <= _retryCount; i++) {
            lRetVal = _iFeliCaChip.ExecuteThru(command, commandLength);
            if(lRetVal == IFeliCaChip.errorCode.ERR_TIMEOUT.getInt()) {
                continue;   // リトライ
            }

            if(lRetVal == IFeliCaChip.errorCode.ERR_NONE.getInt()) {
                lRetVal = FeliCaClient.FC_ERR_SUCCESS;
            }
            break;
        }

        return lRetVal;
    }

    /**
     * FeliCaChipデバイス操作実行結果取得
     * FeliCaChipデバイス実行結果を取得します。
     *
     * @return 実行結果が入ったバッファの先頭アドレス
     */
    public final byte[] GetResponse() {
        return _iFeliCaChip.GetResponse();
    }

    /**
     * FeliCaChipデバイス操作実行結果長取得
     * FeliCaChipデバイス実行結果長を取得します。
     *
     * @return 実行結果データ長
     */
    public final int GetResponseLength() {
        return _iFeliCaChip.GetResponseLength();
    }

    /**
     * 処理の中断
     * デバイスに対する処理を中断します。
     */
    public void Cancel() {
        _iFeliCaChip.Cancel();
    }
}
