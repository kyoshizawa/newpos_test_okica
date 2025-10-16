package jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule;

public class MsgAccept extends TCAPMessage {
    private static final int MIN_LEN_ACCEPT = 3;        // ACCEPTメッセージデータの最小値
    private static final int MIN_NUM_OPTION = 0;        // OPTION数の最小値
    private static final int MIN_LEN_OPTION = 1;        // OPTION名長の最小値

    private static final int POS_START_OPTION_LIST = 3;     // オプションリストの開始位置

    public MsgAccept(TCAPMessage tcapMessage) {
        super(tcapMessage);
    }

    /**
     * TCAPプロトコルバージョン取得
     * TCAPプロトコルバージョンを取得します。
     *
     * @return TCAPプロトコルバージョン
     */
    public char GetTCAPProtocolVersion() {
        char retVal = 0;

        final byte[] message = GetMessageData();

        if(message == null) {
            // データなし
        } else if(GetLength() < 2) {
            // データ長不正
        } else {
            retVal = (char)((message[0] << 8) | (message[1]));
        }

        return retVal;
    }

    /**
     * オプションの数取得
     * オプションの数を取得します。
     *
     * @return オプションの数
     */
    public char GetOptionNum() {
        char retVal = 0;

        final byte[] message = GetMessageData();

        if(message == null) {
            // データなし
        } else if(GetLength() < 3) {
            // データ長不正
        } else {
            retVal = (char)message[2];
        }

        return retVal;
    }

    /**
     * オプション名の長さ取得
     * オプション名の長さを取得します。
     *
     * @param index オプションのインデックス
     *
     * @return インデックスで指定したオプション名の長さ
     */
    public char GetOptionLength(int index) {
        char retVal = 0;
        int mesageLength = GetLength();

        final byte[] message = GetMessageData();

        if(message == null) {
            // データなし
        } else if(mesageLength < 4) {
            // データ長不正
        } else {
            int num = POS_START_OPTION_LIST;    // オプションリストの開始位置
            int optionNum = GetOptionNum();

            for(int i = 0; i < optionNum; i++) {
                if(mesageLength <= num) {
                    // データ不正
                    break;
                } else {
                    if(i == index) {
                        retVal = (char)message[num];
                        break;
                    } else {
                        // 次のオプション名の長さへ移動
                        num += (message[num] + 1);
                    }
                }
            }
        }

        return retVal;
    }

    /**
     * オプション名取得
     * オプション名を取得します。
     *
     * @param index インデックス
     *
     * @return インデックスで指定したオプション名が存在する場合オプション名、それ以外はnull
     */
    public byte[] GetOptionName(int index) {
        byte[] retVal = null;
        final byte[] message = GetMessageData();

        if(message == null) {
            // データなし
        } else if(GetLength() < 5) {
            // データ長不正
        } else {
            int num = POS_START_OPTION_LIST;    // オプションリストの開始位置
            int optionNum = GetOptionNum();

            for(int i = 0; i < optionNum; i++) {
                int optionLength = GetOptionLength(i);

                if(optionLength < 1) {
                    // データが存在しない
                    break;
                } else if(i == index) {
                    retVal = new byte[optionLength];
                    System.arraycopy(message, num+1, retVal, 0, optionLength);
                    break;
                } else {
                    // 次のオプション名へ移動
                    num += (optionLength + 1);
                }
            }
        }

        return retVal;
    }

    @Override
    public boolean ValidateFormat() {
        boolean bRetVal = true;

        if(GetLength() < MIN_LEN_ACCEPT) {
            bRetVal = false;
        }

        return bRetVal;
    }

    @Override
    public boolean ValidateData() {
        boolean bRetVal = true;
        int optionNum;
        int optionLen;
        int pos = 2;
        int messageLength = GetLength();

        if(GetMessageData() == null || messageLength < 1) {

        } else {
            byte[] data = new byte[messageLength];
            System.arraycopy(GetMessageData(), 0, data, 0, messageLength);

            optionNum = data[pos++];
            // オプション数チェック, 1以上の場合
            if(MIN_NUM_OPTION < optionNum) {
                for(int i = 0; i < optionNum; i++) {
                    // メッセージ長を超えていないか
                    if(messageLength < pos + 1) {
                        bRetVal = false;
                        break;
                    }

                    // オプション名長チェック
                    optionLen = data[pos++];
                    if(optionLen < MIN_LEN_OPTION) {
                        // オプションの長さが最小値より低い場合
                        bRetVal = false;
                        break;
                    }

                    // メッセージ長を超えていないか
                    if(messageLength < optionLen + pos) {
                        bRetVal = false;
                        break;
                    }

                    // オプション名チェック
                    for(; 0 < optionLen; optionLen--) {
                        // オプション名は0x20～0x7Eのコードしか許容しない
                        if((data[pos] < 0x20) || (0x7E < data[pos])) {
                            // 文字コード不正
                            return false;
                        }
                        pos++;
                    }
                }
            }

            if(pos != messageLength) {
                bRetVal = false;
            }
        }

        return bRetVal;
    }
}
