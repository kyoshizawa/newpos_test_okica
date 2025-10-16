package jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.FeliCaClient;

public interface IFeliCaChip {
    enum errorCode
    {
        ERR_NONE(0),                // エラーなし
        ERR_UNKNOWN(-1),            // 予期せぬエラー
        ERR_BAD_PARAM(-2),          // パラメータエラー
        ERR_TIMEOUT(-3),            // タイムアウトにより処理中断
        ERR_ABORTED(-4),            // アボート
        ERR_OPEN(-5),               // オープン失敗
        ERR_EXCUTE(-6),             // コマンド実行エラー
        ERR_BUFFER_LEN(-7),         // バッファ長不正
        ERR_NOT_OPEN(-8),           // チップをオープンしていないのにアクセス使用とした
        ;

        private final int _val;

        errorCode(final int val) {
            _val = val;
        }

        public int getInt() {
            return _val;
        }
    }

    /**
     * デバイス種別取得
     * デバイスの種別を取得します。
     *
     * @return デバイス種別文字列
     */
    byte[] GetType();

    /**
     * デバイス種別設定
     * デバイスの種別を設定します。
     *
     * @param deviceType デバイス種別文字列
     */
    void SetType(byte[] deviceType );

    /**
     * デバイス名称取得
     * デバイスの名称を取得します。
     *
     * @return デバイス名称文字列
     */
    byte[] GetName();

    /**
     * デバイス名称設定
     * デバイスの名称を設定します。
     *
     * @param deviceName デバイス名称文字列
     */
    void SetName(byte[] deviceName);

    /**
     * FeliCaChipオープン
     * FeliCaChipデバイスをオープンします。
     *
     * @return エラーコード IFeliCaChip::ERROR_CODE 参照
     */
    long Open();

    /**
     * 処理の中断
     * FeliCaChipデバイスに対する処理を中断します。
     */
    void Cancel();

    /**
     * FeliCaChipクローズ
     * FeliCaChipデバイスをクローズします。
     *
     * @return エラーコード IFeliCaChip::ERROR_CODE 参照
     */
    long Close();

    /**
     * タイムアウト設定
     * FeliCaChipデバイス実行時のタイムアウト時間を設定します。
     *
     * @param timeoutMilliseconds タイムアウト時間（単位：ミリ秒）
     */
    void SetTimeout(long timeoutMilliseconds);

    /**
     * タイムアウト取得
     * FeliCaChipデバイス実行時のタイムアウト時間を取得します。
     *
     * @return タイムアウト時間（単位：ミリ秒）
     */
    long GetTimeout();

    /**
     * FeliCaChipコマンド実行
     * FeliCaChipデバイスに対するコマンドを実行します。
     *
     * @param command       コマンドデータ
     * @param commandLength コマンドデータ長
     * @return エラーコード IFeliCaChip::ERROR_CODE 参照
     */
    long Execute(byte[] command, long commandLength);

    /**
     * FeliCaChipスルーコマンド実行
     * FeliCaChipデバイスに対するスルーコマンドを実行します。
     *
     * @param command       コマンドデータ
     * @param commandLength コマンドデータ長
     * @return エラーコード IFeliCaChip::ERROR_CODE 参照
     */
    long ExecuteThru(byte[] command, long commandLength);

    /**
     * FeliCaChipデバイス操作実行結果取得
     * FeliCaChipデバイス実行結果を取得します。
     *
     * @return 実行結果
     */
    byte[] GetResponse();

    /**
     * FeliCaChipデバイス操作実行結果長取得
     * FeliCaChipデバイス実行結果長を取得します。
     *
     * @return 実行結果データ長
     */
    int GetResponseLength();
}
