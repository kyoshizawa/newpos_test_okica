package jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.FeliCaClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TLAMResponse {
    private enum state
    {
        TLAM_KEYWORD_WAIT(1),       // TLAMメタデータ解析ステータス(キーワード待ち)
        TLAM_EQUAL_WAIT(2),         // TLAMメタデータ解析ステータス('='待ち)
        TLAM_VALUE_WAIT(3),         // TLAMメタデータ解析ステータス(値待ち)
        TLAM_SKIP_TO_EOL(4),        // TLAMメタデータ解析ステータス(行末まで読み飛ばし)
        ;

        private final int _val;

        state(final int val) {
            _val = val;
        }

        public int getInt() {
            return _val;
        }
    }

    private static final int MAX_LIST_NUM = 65535;          // 最大リストサイズ

    private final List<TLAMData> _dataList;                 // TLAMレスポンスパラメータリスト

    public TLAMResponse() {
        _dataList = new ArrayList<>(MAX_LIST_NUM);
    }

    /**
     * TLAMデータの解析
     * 指定のTLAMデータを解析し、保持します。
     *
     * @param tlamData  TLAMデータ
     * @param dataSize  TLAMデータサイズ
     *
     * @return エラーコード
     */
    public long Parse(final byte[] tlamData, int dataSize) {
        state eState = state.TLAM_KEYWORD_WAIT;
        int offset = 0;
        byte[] tempData = new byte[dataSize+1];
        byte[] keyword = null;
        byte[] value;

        while(true) {
            switch (eState) {
                // キーワード待ち
                case TLAM_KEYWORD_WAIT:
                    // 空白、タブは読み飛ばす
                    while (offset < dataSize) {
                        if((tlamData[offset] != ' ') && (tlamData[offset] != '\t')) {
                            break;
                        }
                        offset++;
                    }

                    if(offset >= dataSize) {
                        return FeliCaClient.FC_ERR_SUCCESS;
                    }

                    // 行末を見つけたので解析しなおす
                    if((tlamData[offset] == '\n') || (tlamData[offset] == '\r')) {
                        offset++;
                        eState = state.TLAM_KEYWORD_WAIT;
                    } else {
                        int copySize = 0;

                        while(offset < dataSize) {
                            if((('a' <= tlamData[offset]) && (tlamData[offset] <= 'z'))
                            || (('A' <= tlamData[offset]) && (tlamData[offset] <= 'Z'))
                            || (('1' <= tlamData[offset]) && (tlamData[offset] <= '9'))) {
                                tempData[copySize++] = tlamData[offset++];
                            } else if((tlamData[offset] == ' ')
                                   || (tlamData[offset] == '\t')
                                   || (tlamData[offset] == '=')) {
                                if(0 < copySize) {
                                    keyword = new byte[copySize];
                                    System.arraycopy(tempData, 0, keyword, 0, copySize);
                                    Arrays.fill(tempData, (byte)0x0);
                                    eState = state.TLAM_EQUAL_WAIT;
                                } else {
                                    eState = state.TLAM_SKIP_TO_EOL;
                                }
                                break;
                            } else if((tlamData[offset] == '\n')
                                   || (tlamData[offset] == '\r')) {
                                offset++;
                                eState = state.TLAM_KEYWORD_WAIT;
                                break;
                            } else {
                                eState = state.TLAM_SKIP_TO_EOL;
                                break;
                            }
                        }
                        if(offset >= dataSize) {
                            return FeliCaClient.FC_ERR_SUCCESS;
                        }
                    }
                    break;

                // '='待ち
                case TLAM_EQUAL_WAIT:
                    // 空白、タブは読み飛ばす
                    while(offset < dataSize) {
                        if((tlamData[offset] != ' ') && (tlamData[offset] != '\t')) {
                            break;
                        }
                        offset++;
                    }

                    if(offset >= dataSize) {
                        return FeliCaClient.FC_ERR_SUCCESS;
                    }

                    // 行末を見つけたので解析しなおす
                    if((tlamData[offset] == '\n') || (tlamData[offset] == '\r')) {
                        offset++;
                        eState = state.TLAM_KEYWORD_WAIT;
                    } else {
                        if(tlamData[offset] == '=') {
                            offset++;
                            eState = state.TLAM_VALUE_WAIT;
                        } else {
                            eState = state.TLAM_SKIP_TO_EOL;
                        }
                    }
                    break;

                // 値取得
                case TLAM_VALUE_WAIT:
                    // 空白、タブは読み飛ばす
                    while(offset < dataSize) {
                        if((tlamData[offset] != ' ') && (tlamData[offset] != '\t')) {
                            break;
                        }
                        offset++;
                    }

                    if(offset >= dataSize) {
                        return FeliCaClient.FC_ERR_SUCCESS;
                    }

                    // 行末を見つけたので解析しなおす
                    if((tlamData[offset] == '\n') || (tlamData[offset] == '\r')) {
                        offset++;
                        eState = state.TLAM_KEYWORD_WAIT;
                    } else {
                        int copySize = 0;

                        // 行末もしくは空白、タブまで読み進める
                        while(offset < dataSize) {
                            if((tlamData[offset] == '\n') || (tlamData[offset] == '\r')
                            || (tlamData[offset] == ' ') || (tlamData[offset] == '\t')) {
                                break;
                            }
                            tempData[copySize++] = tlamData[offset++];
                        }

                        if(offset < dataSize) {
                            // 行末に達していない場合
                            if((tlamData[offset] != '\n') && (tlamData[offset] != '\r')) {
                                while(offset < dataSize) {
                                    if((tlamData[offset] == '\n') || (tlamData[offset] == '\r')) {
                                        break;
                                    } else {
                                        // 空白、タブ以外の文字を見つけたらエラーとする
                                        if((tlamData[offset] != ' ') && (tlamData[offset] != '\t')) {
                                            eState = state.TLAM_KEYWORD_WAIT;
                                            // 内容を破棄
                                            copySize = 0;
                                            break;
                                        }
                                    }
                                    offset++;
                                }
                            }
                        }

                        if(0 < copySize) {
                            value = new byte[copySize];
                            System.arraycopy(tempData, 0, value, 0, copySize);

                            TLAMData tempTLAMData = new TLAMData(keyword, value);

                            if(!_dataList.contains(tempTLAMData)) {
                                _dataList.add(tempTLAMData);
                            }
                        }
                        eState = state.TLAM_KEYWORD_WAIT;
                    }
                    break;

                // 行末まで読み飛ばし
                case TLAM_SKIP_TO_EOL:
                default:
                    while(offset < dataSize) {
                        if((tlamData[offset] == '\n') || (tlamData[offset] == '\r')) {
                            offset++;
                            break;
                        }
                        offset++;
                    }

                    if(offset >= dataSize) {
                        return FeliCaClient.FC_ERR_SUCCESS;
                    } else {
                        eState = state.TLAM_KEYWORD_WAIT;
                    }
                    break;
            }
        }
        // ここまで到達しないのでリターンなし
    }

    /**
     * TLAMデータ内パラメータの取得
     * TLAMデータ内にあるパラメータを取得します。
     *
     * @param keyWord  キーワード文字列
     *
     * @return パラメータ文字列(キーワードがない場合はNULL)
     */
    public byte[] GetValue(final byte[] keyWord) {

        for(int i = 0; i < _dataList.size(); i++) {
            if(Arrays.equals(_dataList.get(i).GetKeyword(), keyWord)) {
                return _dataList.get(i).GetValue();
            }
        }

        return null;
    }



    public static class TLAMData {
        private byte[] _keyWord;         // キーワード文字列
        private byte[] _value;           // 設定されている文字列

        public TLAMData(final byte[] keyWord, final byte[] value) {
            _keyWord = null;
            _value = null;

            if(keyWord != null && value != null) {
                _keyWord = new byte[keyWord.length];
                _value = new byte[value.length];

                if(_keyWord != null) {
                    System.arraycopy(keyWord, 0, _keyWord, 0, _keyWord.length);
                }
                if(_value != null) {
                    System.arraycopy(value, 0, _value, 0, _value.length);
                }
            }
        }

        /**
         * キーワードの取得
         *
         * @return キーワード文字列
         */
        public final byte[] GetKeyword() {
            return _keyWord;
        }

        /**
         * 設定されている文字列の取得
         *
         * @return 設定されている文字列
         */
        public final byte[] GetValue() {
            return _value;
        }
    }

}
