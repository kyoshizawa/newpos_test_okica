package jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.FeliCaClient;

public class FeliCaParam {
    private final char _keyID;      // パラメータキー
    private byte[] _param;          // パラメータ
    private char _length;           // パラメータ長さ

    public FeliCaParam(char keyID) {
        _keyID = keyID;
        _param = null;
        _length = 0;
    }

    /**
     * パラメータの設定
     * 引数のパラメータをコピーして保持します。
     *
     * @param param  パラメータ実体
     * @param length パラメータ長さ
     *
     * @return エラーコード
     */
    public long SetParam(final byte[] param, char length) {
        long lRetVal = FeliCaClient.FC_ERR_CLIENT_UNKNOWN;

        _param = null;

        if(param == null) {
            // 追加するパラメータがない
        } else if(length < 1) {
            // 空のデータを用意する
            lRetVal = FeliCaClient.FC_ERR_SUCCESS;
        } else {
            // パラメータリスト追加
            _param = new byte[length];
            System.arraycopy(param, 0, _param, 0, length);
            _length = length;
            lRetVal = FeliCaClient.FC_ERR_SUCCESS;
        }

        return lRetVal;
    }

    /**
     * パラメータキーの取得
     *
     * @return パラメータキー
     */
    public char GetKeyID() {
        return _keyID;
    }

    /**
     * パラメータの取得
     *
     * @return パラメータ
     */
    public byte[] GetParam() {
        return _param;
    }

    /**
     * パラメータの長さ取得
     *
     * @return パラメータの長さ
     */
    public char GetLength() {
        return _length;
    }
}
