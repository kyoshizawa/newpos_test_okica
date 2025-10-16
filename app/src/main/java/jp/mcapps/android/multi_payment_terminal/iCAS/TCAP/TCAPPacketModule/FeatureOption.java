package jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule;

import java.nio.ByteBuffer;

import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.FeliCaClient.FeliCaClient;

public class FeatureOption {
    private byte[] _optionValue;         // オプション名

    public FeatureOption(final byte[] data) {
        if(data != null) {
            _optionValue = data;
        }
    }

    public FeatureOption(final FeatureOption featureOption) {
        if(this != featureOption && featureOption != null) {
            if(featureOption._optionValue != null) {
                _optionValue = featureOption._optionValue;
            }
        }
    }

    /**
     * オプションデータをバイナリ化
     * オプションデータをバイナリ化します。
     *
     * @param outBuffer 文字列情報格納先
     *
     * @return 格納結果
     */
    public long Dump(ByteBuffer outBuffer) {
        long lRetVal;

        if(_optionValue != null) {
            // オプション名長
            byte optionLength = (byte)_optionValue.length;
            outBuffer.put(optionLength);

            // オプション名
            outBuffer.put(_optionValue);

            lRetVal = FeliCaClient.FC_ERR_SUCCESS;
        } else {
            lRetVal = TCAPPacket.FC_ERR_TCAP_DUMP;
        }

        return lRetVal;
    }

    /**
     * オプション名の長さ取得
     * オプション名の長さを取得します。
     *
     * @return オプション名の長さ
     */
    public char GetOptionLength() {
        char retVal = 0;

        if(_optionValue != null) {
            retVal = (char)_optionValue.length;
        }

        return retVal;
    }

    /**
     * オプション名取得
     * オプション名を取得します。
     *
     * @return オプション名
     */
    public byte[] GetOption() {
        return _optionValue;
    }
}
