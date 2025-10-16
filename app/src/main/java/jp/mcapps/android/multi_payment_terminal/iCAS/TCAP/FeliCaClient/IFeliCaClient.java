package jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.FeliCaClient;

import okhttp3.CookieJar;

public interface IFeliCaClient {
    enum errorCode
    {
        ERR_NONE(0),                // エラーなし
        ERR_UNKNOWN(-1),            // 予期せぬエラー
        ERR_BAD_PARAM(-2),          // パラメータエラー
        ERR_ADD_DEVICE(-3),         // デバイスリスト追加失敗
        ERR_ALREADY_STARTED(-4),    // 既に動作中
        ERR_NO_MEMORY(-5),          // メモリが足らない
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
     * サーバURL設定
     * 通信するサーバのURLを設定します。
     *
     * @param url サーバURL
     */
    void SetUrl(final String url);

    /**
     * Cookie設定
     * Cookieを設定します。
     *
     * @param cookiejar Cookie
     */
    void SetCookie(final CookieJar cookiejar);

    /**
     * サーバURLの取得
     * 設定されているサーバURLを取得します。
     *
     * @return サーバURL文字列
     */
    String GetUrl();

    /**
     * サーバとのオンライン処理開始
     * サーバとのオンライン処理を開始します
     *
     * @param bUseParam  通信開始設定
     *        true :FeliCaサーバから通信パラメータを取得して通信を開始する(TLAM通信を行う)
     *        false:FeliCaサーバから通信パラメータを取得せずに通信を開始する(TLAM通信を行わない)
     * @param data       メッセージボディに設定するデータ(TLAM通信を行わない場合は使用されない)
     * @param dataLength データ長
     * @return エラーコード　IFeliCaClient::errorCode 参照
     */
    long Start(boolean bUseParam, final byte[] data, int dataLength);

    /**
     * サーバとのオンライン処理を終了、または中断する
     * FeliCaサーバとのオンライン処理を終了、または中断します。オンライン処理中でない場合は何も行いません。
     *
     * @param bForced 中止、終了指定
     *        true :オンライン処理を中断（通信中であっても強制中断します）
     *        false:オンライン処理を終了（TCAPで定められた中断シーケンスを行い処理を終了します）
     * @return エラーコード IFeliCaClient::ERROR_CODE 参照
     */
    long Stop(boolean bForced);

    /**
     * オンライン処理中か否か
     * FeliCaサーバとのオンライン処理が開始しているかを調べます。
     *
     * @return true :通信中
     *         false:通信を行っていない
     */
    boolean IsStarted();

    /**
     * 通信終了コールバックオブジェクト設定
     * 通信終了コールバックオブジェクトを設定します。
     *
     * @param iFeliCaClientEventListener コールバックオブジェクト
     */
    void SetEventListener(IFeliCaClientEventListener iFeliCaClientEventListener);

    /**
     * 通信終了コールバックオブジェクト取得
     * 設定されている通信終了コールバックオブジェクトを取得します。
     *
     * @return コールバックオブジェクト
     */
    IFeliCaClientEventListener GetEventListener();

    /**
     * デバイスリストの追加
     * 通常デバイス(FeliCaChip以外)をデバイスリストに追加します。
     *
     * @param iDevice デバイスオブジェクト
     *
     * @return エラーコード　IFeliCaClient::errorCode 参照
     */
    long AddDevice(IDevice iDevice);

    /**
     * デバイスリストの追加
     * FeliCaChipデバイスをデバイスリストに追加します。
     *
     * @param iFeliCaChip デバイスオブジェクト
     *
     * @return エラーコード　IFeliCaClient::errorCode 参照
     */
    long AddFeliCaChip(IFeliCaChip iFeliCaChip);

    /**
     * デバイスリストからデバイス取得
     * デバイスリストから指定の通常デバイス(FeliCaChip以外)を取得します。
     *　(取得したデバイスはデバイスリストから削除されません)
     * パラメータのデバイスがFeliCaChipデバイスの場合、戻り値はNULLになります。
     *
     * @param deviceType デバイス種別の文字列
     * @param deviceName デバイス名称文字列
     *
     * @return デバイスオブジェクト
     */
    IDevice GetDevice(final String deviceType, final String deviceName);

    /**
     * デバイスリストからFeliCaChipデバイス取得
     * デバイスリストから指定のFeliCaChipデバイスを取得します。
     *　(取得したデバイスはデバイスリストから削除されません)
     * パラメータのデバイスがFeliCaChipデバイスではない場合、戻り値はNULLになります。
     *
     * @param deviceType デバイス種別の文字列
     * @param deviceName デバイス名称文字列
     *
     * @return デバイスオブジェクト
     */
    IFeliCaChip GetFeliCaDevice(final String deviceType, final String deviceName);

    /**
     * デバイスリストから削除
     * デバイスリストから指定のデバイスを削除します。
     *
     * @param deviceType デバイス種別の文字列
     * @param deviceName デバイス名称文字列
     *
     * @return エラーコード　IFeliCaClient::errorCode 参照
     */
    long RemoveDevice(final String deviceType, final String deviceName);

    /**
     * 全てのデバイス削除
     * FeliCaClientに設定されている全てのデバイスを削除します。
     *
     * @return エラーコード　IFeliCaClient::errorCode 参照
     */
    long RemoveAllDevices();
}
