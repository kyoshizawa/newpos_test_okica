package jp.mcapps.android.multi_payment_terminal.devices;

import android.util.Log;

import com.pos.device.SDKException;
import com.pos.device.picc.FeliCa;
import com.pos.device.picc.PiccReader;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import jp.mcapps.android.multi_payment_terminal.data.Bytes;
import jp.mcapps.android.multi_payment_terminal.data.okica.OkicaCardResponse;
import jp.mcapps.android.multi_payment_terminal.data.okica.OkicaExceptions.OkicaSDKException;
import jp.mcapps.android.multi_payment_terminal.data.okica.OkicaExceptions.RWTimeoutException;
import jp.mcapps.android.multi_payment_terminal.data.okica.OkicaExceptions.SamCommandException;
import jp.mcapps.android.multi_payment_terminal.data.okica.OkicaExceptions.TerminationException;
import jp.mcapps.android.multi_payment_terminal.data.sam.SamResponse;
import jp.mcapps.android.multi_payment_terminal.data.sam.SamResponseTypes.MutualAuthenticationRWSAM;
import jp.mcapps.android.multi_payment_terminal.data.sam.SamResponseTypes.Polling;
import jp.mcapps.android.multi_payment_terminal.data.sam.SamResponseTypes.ReadBlock;
import jp.mcapps.android.multi_payment_terminal.data.sam.SamResponseTypes.RequestService;
import jp.mcapps.android.multi_payment_terminal.data.sam.SamResponseTypes.SubResponse;
import jp.mcapps.android.multi_payment_terminal.data.sam.SamResponseTypes.WriteBlock;
import jp.mcapps.android.multi_payment_terminal.error.SDKErrors;
import jp.mcapps.android.multi_payment_terminal.util.McUtils;
import timber.log.Timber;

/**
 * OKICAカードに対して読み書きを行うクラスです
 */
public class OkicaRW {
    private static final FeliCa _felica = FeliCa.getInstance();
    private static final PiccReader _piccReader = PiccReader.getInstance();
    private static boolean _doTerminate = false;
    private static boolean _isRunning = false;
    public static boolean isRunning() {
        return _isRunning;
    }



    public static String KEY_TOTAL = "TOTAL";
    public static String KEY_SAM_READ = "SAM_READ";
    public static String KEY_SAM_READ1 = "SAM_READ1";
    public static String KEY_SAM_READ2 = "SAM_READ2";
    public static String KEY_READ = "READ";
    public static String KEY_SAM_READRES = "SAM_READRES";
    public static String KEY_SAM_READRES1 = "SAM_READRES1";
    public static String KEY_SAM_READRES2 = "SAM_READRES2";

    public static String KEY_READ_TOTAL = "READ_TOTAL";
    public static String KEY_SAM_WRITE = "SAM_WRITE";
    public static String KEY_WRITE = "WRITE";
    public static String KEY_SAM_WRITERES = "SAM_WRITERES";

    public static String KEY_WRITE_TOTAL = "WRITE_TOTAL";

    private static final Map<String, Long> _startTimes = new HashMap<>();

    private static final Map<String, Long> _endTimes = new HashMap<>();

    public static boolean GetWriteDisable() {
        return _test_write_disable;
    }

    private static boolean _test_write_disable = true;

    public static void SetStartTime(String key, long time) {
        _startTimes.put(key, time);
    }

    public static void SetEndTime(String key, long time) {
        _endTimes.put(key, time);
    }

    public static long getElapsed(String key) {
        if (_startTimes.containsKey(key) && _endTimes.containsKey(key)) {
            Long start = _startTimes.get(key);
            Long end = _endTimes.get(key);
            if (start != null && end != null) {
                return end - start;
            }
        }
        return -1; // 不完全なデータ
    }

    public static void clearAll() {
        _startTimes.clear();
        _endTimes.clear();
    }


    public static void PrintTime() {
//        Log.i("OkicaRW", "◆◆◆ READ SAM処理時間 *enc : [" + (getElapsed(KEY_SAM_READ)) + "] ms");
//        Log.i("OkicaRW", "◆◆◆ READ処理時間 : [" + (getElapsed(KEY_READ)) + "] ms");
//        Log.i("OkicaRW", "◆◆◆ READ SAM復号処理時間 *dec : [" + (getElapsed(KEY_SAM_READRES)) + "] ms");
//        Log.i("OkicaRW", "◆◆◆ READ TOTAL : [" + (getElapsed(KEY_READ_TOTAL)) + "] ms");
//        Log.i("OkicaRW", "◆◆◆ WRITE SAM処理時間 *enc : [" + (getElapsed(KEY_SAM_WRITE)) + "] ms");
//        Log.i("OkicaRW", "◆◆◆ WRITE処理時間 : [" + (getElapsed(KEY_WRITE)) + "] ms");
//        Log.i("OkicaRW", "◆◆◆ WRITE SAM復号処理時間 *dec : [" + (getElapsed(KEY_SAM_WRITERES)) + "] ms");
//        Log.i("OkicaRW", "◆◆◆ WRITE TOTAL : [" + (getElapsed(KEY_WRITE_TOTAL)) + "] ms");
//        Log.i("OkicaRW", "◆◆◆ TOTAL処理時間 : [" + (getElapsed(KEY_TOTAL)) + "] ms");

        Log.i("OkicaRW", "◆◆◆ READ処理時間 SAM : [" + (getElapsed(KEY_SAM_READ) + getElapsed(KEY_SAM_READRES)) + "] ms");
        Log.i("OkicaRW", "◆◆◆     内 暗号化(要求) : [" + (getElapsed(KEY_SAM_READ1)) + "] ms");
        Log.i("OkicaRW", "◆◆◆     内 SDK  (要求) : [" + (getElapsed(KEY_SAM_READ2)) + "] ms");
        Log.i("OkicaRW", "◆◆◆     内 暗号化(応答) : [" + (getElapsed(KEY_SAM_READRES1)) + "] ms");
        Log.i("OkicaRW", "◆◆◆     内 SDK  (応答) : [" + (getElapsed(KEY_SAM_READRES2)) + "] ms");
        Log.i("OkicaRW", "◆◆◆ READ処理時間 Felica : [" + (getElapsed(KEY_READ)) + "] ms");
        Log.i("OkicaRW", "◆◆◆ READ TOTAL : [" + (getElapsed(KEY_READ_TOTAL)) + "] ms");

        if (_test_write_disable == false) {
            Log.i("OkicaRW", "◆◆◆ WRITE処理時間 SAM : [" + (getElapsed(KEY_SAM_WRITE) + getElapsed(KEY_SAM_WRITERES)) + "] ms");
            Log.i("OkicaRW", "◆◆◆ WRITE処理時間 Felica : [" + (getElapsed(KEY_WRITE)) + "] ms");
            Log.i("OkicaRW", "◆◆◆ WRITE TOTAL : [" + (getElapsed(KEY_WRITE_TOTAL)) + "] ms");
        }

        Log.i("OkicaRW", "◆◆◆ TOTAL処理時間 : [" + (getElapsed(KEY_TOTAL)) + "] ms");
    }

    /**
     * transmitの実行結果を返すクラスです
     */
    private static class CardResult {
        private byte[] data;
        public byte[] getData() {
            return data;
        }
        public void setData(byte[] data) {
            this.data = data;
        }

        private Throwable error;
        public Throwable getError() {
            return error;
        }
        public boolean hasError() {
            return error != null;
        }
        public void setError(Throwable error) {
            this.error = error;
        }
    }

    /**
     * カードの捕捉を行います
     *
     * @return 実行結果(IDm)
     */
    public static OkicaCardResponse<Polling> polling() {
        return run(allowTerminate -> {
            final OkicaCardResponse<Polling> ret = new OkicaCardResponse<>();

            // カードコマンド生成
            final SamResponse<SubResponse> subResp = SamRW.pollingCommand();
            if (subResp.hasError()) {
                return ret.setError(new SamCommandException(subResp.getError()));
            }

            // カード通信
            final CardResult cardResp = transmit(subResp.getData().getCardCommandPacket(), 10, allowTerminate);
            if (cardResp.hasError()) {
                return ret.setError(cardResp.getError());
            }

            // カードレスポンスのデータサイズはトリムされているのでIndexは-1
            final byte[] IDm = Arrays.copyOfRange(cardResp.getData(), 1, 9);
            final byte[] PMm = Arrays.copyOfRange(cardResp.getData(), 9, 17);

            // カードレスポンス復号
            final SamResponse<Polling> resp = SamRW.pollingSubCommand(IDm, PMm);
            if (resp.hasError()) {
                return ret.setError(new SamCommandException(resp.getError()));
            }
            return ret.setData(resp.getData());
        }, true);
    }

    /**
     * カードの読み取りを行います
     *
     * @return ブロックデータ
     */
    public static OkicaCardResponse<RequestService> requestService(byte[] IDm, byte[] areaServiceList) {
        return run(allowTerminate -> {
            final OkicaCardResponse<RequestService> ret = new OkicaCardResponse<>();

            // カードコマンド生成
            final SamResponse<SubResponse> subResp = SamRW.requestServiceCommand(IDm, areaServiceList);
            if (subResp.hasError()) {
                return ret.setError(new SamCommandException(subResp.getError()));
            }

            // カード通信
            final CardResult cardResp = transmit(subResp.getData().getCardCommandPacket(), subResp.getData().getTimeoutAsInt(), allowTerminate);
            if (cardResp.hasError()) {
                return ret.setError(cardResp.getError());
            }

            // カードレスポンス復号
            final SamResponse<RequestService> resp = SamRW.requestServiceSubCommand(cardResp.getData());
            if (resp.hasError()) {
                return ret.setError(new SamCommandException(resp.getError()));
            }

            return ret.setData(resp.getData());
        }, true);
    }

    /**
     * OKICA カード認証を行います
     *
     * @param IDm IDm
     * @return 実行結果(IDt)
     */
    public static OkicaCardResponse<MutualAuthenticationRWSAM> authentication(byte[] IDm) {
        return run(allowTerminate -> {
            final OkicaCardResponse<MutualAuthenticationRWSAM> ret = new OkicaCardResponse<>();

            // カードコマンド生成 (Authentication1)
            final SamResponse<SubResponse> subResp1 = SamRW.mutualAuthenticationRWSAMCommand(IDm);
            if (subResp1.hasError()) {
                return ret.setError(new SamCommandException(subResp1.getError()));
            }

            // カード通信 (Authentication1)
            final CardResult cardResp1 = transmit(subResp1.getData().getCardCommandPacket(), subResp1.getData().getTimeoutAsInt(), allowTerminate);
            if (cardResp1.hasError()) {
                return ret.setError(cardResp1.getError());
            }

            // カードコマンド生成 (Authentication2)
            final SamResponse<SubResponse> subResp2 = SamRW.mutualAuthenticationRWSAMSubCommand1(cardResp1.getData());
            if (subResp2.hasError()) {
                return ret.setError(new SamCommandException(subResp2.getError()));
            }

            // カード通信 (Authentication2)
            final CardResult cardResp2 = transmit(subResp2.getData().getCardCommandPacket(), subResp2.getData().getTimeoutAsInt(), allowTerminate);
            if (cardResp2.hasError()) {
                return ret.setError(cardResp2.getError());
            }

            // カードレスポンス復号
            final SamResponse<MutualAuthenticationRWSAM> resp = SamRW.mutualAuthenticationRWSAMSubCommand2(cardResp2.getData());
            if (resp.hasError()) {
                return ret.setError(new SamCommandException(resp.getError()));
            }

            return ret.setData(resp.getData());
        }, true);
    }

    /**
     * カードの読み取りを行います
     *
     * @return ブロックデータ
     */
    public static OkicaCardResponse<ReadBlock> read(byte[] IDt, byte[] blockList) {
        return run(allowTerminate -> {
            final OkicaCardResponse<ReadBlock> ret = new OkicaCardResponse<>();

            SetStartTime(KEY_SAM_READ, System.currentTimeMillis());
            SetStartTime(KEY_READ_TOTAL, System.currentTimeMillis());

            // カードコマンド生成
            final SamResponse<SubResponse> subResp = SamRW.readBlockCommand(IDt, blockList);
            if (subResp.hasError()) {
                return ret.setError(new SamCommandException(subResp.getError()));
            }

            SetEndTime(KEY_SAM_READ, System.currentTimeMillis());

            SetStartTime(KEY_READ, System.currentTimeMillis());

            // カード通信
            final CardResult cardResp = transmit(subResp.getData().getCardCommandPacket(), subResp.getData().getTimeoutAsInt(), allowTerminate);
            if (cardResp.hasError()) {
                return ret.setError(cardResp.getError());
            }

            SetEndTime(KEY_READ, System.currentTimeMillis());

            SetStartTime(KEY_SAM_READRES, System.currentTimeMillis());

            // カードレスポンス復号
            final SamResponse<ReadBlock> resp = SamRW.readBlockSubCommand(cardResp.getData());
            if (resp.hasError()) {
                return ret.setError(new SamCommandException(resp.getError()));
            }

            SetEndTime(KEY_SAM_READRES, System.currentTimeMillis());
            SetEndTime(KEY_READ_TOTAL, System.currentTimeMillis());

            return ret.setData(resp.getData());
        }, true);
    }

    /**
     * カードの書き込みを行います
     *
     * @return 実行結果
     */
    public static OkicaCardResponse<WriteBlock> write(byte[] IDt, byte[] blockList, byte[] blockData) {
        return run(allowTerminate -> {
            final OkicaCardResponse<WriteBlock> ret = new OkicaCardResponse<>();

            // 書き込まない
            if (_test_write_disable) {
                return ret;
            }

            SetStartTime(KEY_SAM_WRITE, System.currentTimeMillis());
            SetStartTime(KEY_WRITE_TOTAL, System.currentTimeMillis());

            // カードコマンド生成 (Write)
            final SamResponse<SubResponse> subResp = SamRW.writeBlockCommand(IDt, blockList, blockData);
            if (subResp.hasError()) {
                return ret.setError(subResp.getError());
            }

            SetEndTime(KEY_SAM_WRITE, System.currentTimeMillis());

            SetStartTime(KEY_WRITE, System.currentTimeMillis());

            // カード通信 (Write)
            final CardResult wCardResp = transmit(subResp.getData().getCardCommandPacket(), subResp.getData().getTimeoutAsInt(), allowTerminate);
            if (wCardResp.hasError()) {
                return ret.setError(wCardResp.getError());
            }

            SetEndTime(KEY_WRITE, System.currentTimeMillis());

            SetStartTime(KEY_SAM_WRITERES, System.currentTimeMillis());

            // カードレスポンス復号 (Write)
            final SamResponse<WriteBlock> resp = SamRW.writeBlockSubCommand(wCardResp.getData());
            if (resp.hasError()) {
                return ret.setError(new SamCommandException(resp.getError()));
            }

            SetEndTime(KEY_SAM_WRITERES, System.currentTimeMillis());
            SetEndTime(KEY_WRITE_TOTAL, System.currentTimeMillis());

            return ret.setData(resp.getData());
        }, false);
    }

    /**
     * 暗号化されていない部分のカードの読み取りを行います
     *
     * @return ブロックデータ
     */
    public static OkicaCardResponse<ReadBlock> readWithoutEncryption(byte[] IDm, byte[] serviceCodeList, byte[] blockList) {
        return run(allowTerminate -> {
            final OkicaCardResponse<ReadBlock> ret = new OkicaCardResponse<>();

            // カードコマンド生成
            final SamResponse<SubResponse> subResp = SamRW.readBlockWithoutEncryptionCommand(IDm ,serviceCodeList , blockList);
            if (subResp.hasError()) {
                return ret.setError(new SamCommandException(subResp.getError()));
            }

            // カード通信
            final CardResult cardResp = transmit(subResp.getData().getCardCommandPacket(), subResp.getData().getTimeoutAsInt(), allowTerminate);
            if (cardResp.hasError()) {
                return ret.setError(cardResp.getError());
            }

            // カードレスポンス復号
            final SamResponse<ReadBlock> resp = SamRW.readBlockSubCommand(cardResp.getData());
            if (resp.hasError()) {
                return ret.setError(new SamCommandException(resp.getError()));
            }

            return ret.setData(resp.getData());
        }, true);
    }

    /**
     * カード処理を中断します
     */
    public static void terminate() {
        if (_isRunning) {
            _doTerminate = true;
        }
    }

    /**
     * カード処理を行います
     *
     * @param cardCommandPacket カードコマンドパケット
     * @param timeout タイムアウト時間(ミリ秒
     * @return カード処理実行結果(成功 or タイムアウト or 強制終了)
     */
    @NotNull
    private static CardResult transmit(byte[] cardCommandPacket, int timeout, boolean allowTerminate) {
        // PiccReaderは一見未使用に見えるがこれをしておかないとtransmitでSDKエラーが発生する
        _piccReader.selectCarrierType(PiccReader.MIF_TYPE_C1);

        // カードと通信するには先頭バイトにデータサイズの情報が必要
        byte[] cmd = new Bytes(cardCommandPacket.length+1).add(cardCommandPacket).toArray();
        final CardResult result = new CardResult();

        //Timber.d("カードタイムアウト時間: %s [ms]", timeout);
        //Timber.d("OKICAカードコマンド: %s", McUtils.bytesToHexString(cmd));

        byte[] cardResp = null;

        long start = System.currentTimeMillis();

        while(cardResp == null) {
            if (_doTerminate) {
                _doTerminate = false;
                SamRW.terminate();
                result.setError(new TerminationException());
                break;
            }

            int elapsed = (int) (System.currentTimeMillis() - start);

            if (timeout > 0 && timeout < elapsed) {
                SamRW.terminate();
                result.setError(new RWTimeoutException());
                break;
            }

            try {
                /*
                 * 明確な仕様ではないが調査した範囲のtransmitのtimeoutの挙動
                 *
                 * タイムアウトの上限は1秒
                 * タイムアウトの単位は1/100000秒っぽい
                 * 謎のオフセット(90000なら820ミリ秒になる)がかかって期待値とタイムアウト時間が異なる
                 * 0で渡した場合も1秒でタイムアウトになる
                 */
                cardResp = _felica.transmit(cmd, timeout*106); // 1/106　ms らしい

                // SAMに渡すパケットにデータサイズの情報は不要なのでトリム
                result.setData(Arrays.copyOfRange(cardResp, 1, cardResp.length));
            } catch (SDKException e) {
                final OkicaSDKException okicaEx = new OkicaSDKException(e);
                if (okicaEx.getCode() == SDKErrors.ETIME || okicaEx.getCode() == SDKErrors.ETIME2) {
                    //Timber.v("transmit タイムアウト: %s", e.getMessage());
                } else {
                    Timber.v("SDKエラー: %s", e.getMessage());
                    SamRW.terminate();
                    result.setError(okicaEx);
                    break;
                }
            } catch (Exception e) {
                SamRW.terminate();
                result.setError(e);
                break;
            }
        }

        //Timber.d("OKICAカードレスポンス: %s", McUtils.bytesToHexString(cardResp));

        return result;
    }

    @FunctionalInterface
    private interface IOkicaCardTask<T> {
        OkicaCardResponse<T> run(boolean allowTerminate);
    }

    private static synchronized <T> OkicaCardResponse<T> run(IOkicaCardTask<T> task, boolean allowTerminate) {
        _isRunning = true;
        OkicaCardResponse<T> resp = task.run(allowTerminate);

        /*
         * カード処理後にterminate()が呼ばれるとすり抜けてしまうためここでも中断チェックを行う
         */
        if (allowTerminate && !resp.hasError() && _doTerminate) {
            resp.setError(new TerminationException());
        }

        _isRunning = _doTerminate = false;

        return resp;
    }
}
