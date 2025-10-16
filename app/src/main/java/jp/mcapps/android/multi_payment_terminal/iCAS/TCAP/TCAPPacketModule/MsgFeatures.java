package jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class MsgFeatures extends TCAPMessage {
    private final int MAX_OPTION_NUM = 255;     // 最大オプション数

    private final char _tcapProtocolVersion;          // TCAPプロトコルバージョン
    private byte _reserved1;                          // 予約
    private byte _reserved2;                          // 予約
    private final List<FeatureOption> _optionList;    // FEATUREリスト

    public MsgFeatures(TCAPMessage tcapMessage) {
        super(tcapMessage);

        if(tcapMessage instanceof MsgFeatures) {
            _tcapProtocolVersion = ((MsgFeatures) tcapMessage)._tcapProtocolVersion;
            _optionList = ((MsgFeatures) tcapMessage)._optionList;
        } else {
            _tcapProtocolVersion = 0;
            _optionList = new ArrayList<>(MAX_OPTION_NUM);
        }
    }

    public MsgFeatures(char tcapVersion) {
        super((char) TCAPPacket.TCAP_MSG_EXT_STANDARD,
              (char) TCAPPacket.TCAP_MSG_DEVICEID_INVALID,
              (char) TCAPPacket.TCAP_MSG_MT_FEATURES);

        _tcapProtocolVersion = tcapVersion;
        _optionList = new ArrayList<>(MAX_OPTION_NUM);
    }

    /**
     * FEATURESメッセージのバイナリ化
     * オプション名の長さを取得します。
     *
     * @param outBuffer 文字列情報格納先
     *
     * @return 格納結果
     */
    public long Dump(ByteBuffer outBuffer) {
//        long lRetVal = FeliCaClient.FC_ERR_SUCCESS;

        byte[] data = new byte[9];
        data[0] = (byte)GetExtension();
        data[1] = (byte)(GetDeviceID() >> 8);
        data[2] = (byte)GetDeviceID();
        data[3] = (byte)GetMessageType();
        data[4] = (byte)(GetLength() >> 8);
        data[5] = (byte)GetLength();
        data[6] = (byte)(_tcapProtocolVersion >> 8);
        data[7] = (byte)_tcapProtocolVersion;
        data[8] = (byte)_optionList.size();

        outBuffer.put(data);

        return data.length;
    }

    /**
     * オプション名追加
     * オプション名を追加します。
     *
     * @param featureOption オプション
     *
     * @return 格納結果
     */
    public boolean AddOption(FeatureOption featureOption) {
        boolean bRetVal;

        // オプション追加
        bRetVal = _optionList.add(featureOption);

        return bRetVal;
    }

    /**
     * TCAPプロトコルバージョン取得
     * TCAPプロトコルバージョンを取得します。
     *
     * @return TCAPプロトコルバージョン
     */
    public char GetVersion() {
        return _tcapProtocolVersion;
    }

    /**
     * オプション数取得
     * オプション数を取得します。
     *
     * @return オプション数
     */
    public char GetOptionNum() {
        return (char)_optionList.size();
    }

    @Override
    public int GetLength() {
        // 「+3」は「TCAPバージョン」と「OptNum」3byte分
        return _optionList.size() + 3;
    }

    @Override
    public long GetSize() {
        // 「+3」は「TCAPバージョン」と「OptNum」3byte分, 「+6」は「Featuresヘッダ」6byte分
        return _optionList.size() + 3 + 6;
    }

    /**
     * オプション数取得
     * オプション数を取得します。
     *
     * @return オプション数
     */
    public List<FeatureOption> OptionList() {
        return _optionList;
    }
}

