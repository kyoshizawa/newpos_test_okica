package jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.FeliCaClient;

import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.TCAPPacket;

public interface IFeliCaClientEventListener {
    enum errorType
    {
        TYPE_SUCCESS(0),    // 成功
        TYPE_CANCEL(1),     // クライアント主導によりFeliCaサーバとのオンライン処理が終了及び中断した
        TYPE_UNKNOWN(-1),   // 予期せぬエラー
        TYPE_HTTP(-2),      // HTTP通信エラー
        TYPE_PROTOCOL(-3),  // アプリケーションプロトコル関連のエラー
        TYPE_MEMORY(-4),    // メモリ確保失敗
        ;

        private final int _val;

        errorType(final int val) {
            _val = val;
        }

        public int getInt() {
            return _val;
        }
    }

    /**
     * 通信終了通知
     * FeliCaサーバとのオンライン処理が正常に終了した場合に呼び出されます。
     *
     * @param statusCode 終了ステータス
     */
    void OnFinished(int statusCode);

    /**
     * エラー通知
     * FeliCaサーバとのオンライン処理でエラーが発生した場合に呼び出されます。
     * このメソッドに渡されるパラメータは以下の通りです。
     *
     * ・FeliCaサーバからエラーメッセージを受信した、もしくは受信したメッセージが正しくない場合
     * 　エラータイプ： IFeliCaClientEventListener::TYPE_PROTOCOL
     * 　エラー文字列：
     *      ・FeliCaサーバからエラーメッセージを受信した場合
     *      　エラーメッセージの内容
     *      ・FeliCaサーバから受信したメッセージのフォーマットが正しくない場合
     *      　"Packet format error."
     *      ・FeliCaサーバから受信したメッセージが状態と適合しない場合
     *      　"Illegal state error."
     *      ・FeliCaサーバから受信した通信パラメータが正しくない場合
     *      　"Communication initiation error."
     * ・HTTPレスポンスヘッダ不正の場合
     * 　エラータイプ： IFeliCaClientEventListener::TYPE_HTTP
     * 　エラー文字列：
     *      ・HTTPレスポンスコードが200以外の場合
     *      　"Invalid response code: レスポンスコード "
     *      ・HTTPレスポンスのコンテントタイプが正しくない場合
     *      　"Invalid content-type: コンテントタイプ "
     *      ・HTTPレスポンスのコンテントタイプが設定されていない場合
     *      　"Invalid content-type: null"
     *      ・上記以外のHTTP通信エラーが発生した場合
     *      　"HTTP communication error."
     * ・中止の場合
     * 　エラータイプ： IFeliCaClientEventListener::TYPE_CANCEL
     * 　エラー文字列：
     *      ・IFeliCaClient::Stop() に false を指定して呼び出したことによりFeliCaサーバとのオンライン処理が終了した場合
     *      　"Canceled."
     *      ・IFeliCaClient::Stop() に true を指定して呼び出したことによりFeliCaサーバとのオンライン処理が終了した場合
     *      　"Canceled."、もしくは"HTTP communication error."
     * ・メモリ確保に失敗した場合
     * 　エラータイプ： IFeliCaClientEventListener::TYPE_MEMORY
     * 　エラー文字列："No memory."
     * ・予期せぬエラーが発生した場合
     * 　エラータイプ： IFeliCaClientEventListener::TYPE_UNKNOWN
     * 　エラー文字列：なし
     * @param errorCode    エラーコード
     * @param errorMessage エラーメッセージ文字列
     */
    void OnErrorOccurred(int errorCode, final String errorMessage);

    /**
     * RWトランスミット実行
     * RWトランスミットを実行します。
     * 本ハンドラはサブスレッド内で動作します。
     *
     * @param command  コマンドデータ
     * @param timeout  タイムアウト時間（ミリ秒）
     * @param response RWレスポンスデータ
     *
     * @return エラーコード（enum errorType 参照）
     *         成功時はレスポンスデータレングス
     */
    long OnTransmitRW(byte[] command, long timeout, byte[] response);

    /**
     * デバイス操作要求
     * デバイス操作要求を実行します。
     *
     * @param deviceOperate デバイス操作要求
     * @param replayPacket  応答パケット
     *
     * @return エラーコード
     */
    long OnDeviceOperate(IDevice.DeviceOperate deviceOperate, TCAPPacket replayPacket);

    long OnNoneChipAccessResponse(byte[] data, int dataLength);

    // テスト時のみ利用
    void OnCancelAvailable();
    // テスト時のみ利用
    void OnCommunicationOff();
    void OnRWClose();   // 処理速度計測用
}
