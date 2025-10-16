package jp.mcapps.android.multi_payment_terminal.devices;

import android.util.Log;

import com.pos.device.SDKException;
import com.pos.device.apdu.ResponseApdu;
import com.pos.device.icc.ContactCard;
import com.pos.device.icc.IccReader;
import com.pos.device.icc.OperatorMode;
import com.pos.device.icc.SlotType;
import com.pos.device.icc.SpeedMode;
import com.pos.device.icc.VCC;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import jp.mcapps.android.multi_payment_terminal.data.Bytes;
import jp.mcapps.android.multi_payment_terminal.data.sam.Constants;
import jp.mcapps.android.multi_payment_terminal.data.sam.PackageData;
import jp.mcapps.android.multi_payment_terminal.data.sam.SamCommand;
import jp.mcapps.android.multi_payment_terminal.data.sam.SamExceptions.SamSDKException;
import jp.mcapps.android.multi_payment_terminal.data.sam.SamExceptions.SequenceMisMatchException;
import jp.mcapps.android.multi_payment_terminal.data.sam.SamResponse;
import jp.mcapps.android.multi_payment_terminal.data.sam.SamResponseTypes.*;
import jp.mcapps.android.multi_payment_terminal.util.Crypto;
import jp.mcapps.android.multi_payment_terminal.util.McUtils;
import timber.log.Timber;

/**
 * SAMカードと通信を行うクラスです
 *
 * M909_RC-S500_Command_InterfaceManual_2.1jを参考に実装
 */
public class SamRW {

    public enum States {
        Neutral,
        Normal,
        Admin,
    }

    public enum OpenResult {
        SAM_NOT_FOUND,
        AUTHENTICATION_FAILURE,
        SUCCESS,
    }


    public static String READ_BLOCK = "READ_BLOCK";

    public static String READ_REQ_BYTES = "READ_REQ_BYTES";

    public static String READ_RES_BYTES = "READ_RES_BYTES";

    public static String WRITE_BLOCK = "WRITE_BLOCK";

    public static String WRITE_RES_BYTES = "WRITE_RES_BYTES";

    private static final Map<String, Long> _Bytes = new HashMap<>();

    public static void SetBytes(String key, long bytes) {
        _Bytes.put(key, bytes);
    }

    public static long GetBytes(String key) {
        if (_Bytes.containsKey(key)) {
            return _Bytes.get(key);
        } else {
            return -1;
        }
    }

    public static void clearAll() {
        _Bytes.clear();
    }


    public static void PrintTime() {
        Log.i("OkicaRW", "◆◆◆ READ_BLOCK : [" + (GetBytes(READ_BLOCK)) + "] ");
        Log.i("OkicaRW", "◆◆◆ READ_REQ_BYTES : [" + (GetBytes(READ_REQ_BYTES)) + "] byte");
        Log.i("OkicaRW", "◆◆◆ READ_RES_BYTES : [" + (GetBytes(READ_RES_BYTES)) + "] byte");
        Log.i("OkicaRW", "◆◆◆ READ_TOTAL_BYTES : [" + (GetBytes(READ_REQ_BYTES) + GetBytes(READ_RES_BYTES)) + "] byte");
        Log.i("OkicaRW", "◆◆◆ WRITE_BLOCK : [" + (GetBytes(WRITE_BLOCK)) + "] ");
        Log.i("OkicaRW", "◆◆◆ WRITE_RES_BYTES : [" + (GetBytes(WRITE_RES_BYTES)) + "] byte");
    }


    // IVは常に8バイトのオール0
    private static final byte[] IV = new Bytes(0x00, 0x00, 0x00 ,0x00 ,0x00 ,0x00 ,0x00, 0x00).toArray();

    private static final byte[] HEADER = new Bytes()
            .add(0xA0)  // CLA
            .add(0x00)  // INS
            .add(0x00)  // P1
            .add(0x00)  // P2
            .toArray();

    private static final IccReader _iccReader = IccReader.getInstance(SlotType.PSAM1);
    private static ContactCard _contactCard = null;
    private static byte[] _serial = null;
    private static byte[] _rar = null;
    private static byte[] _rbr = null;
    private static byte[] _rcr = null;
    private static byte[] _KYtr = null;
    private static byte[] _KYmac = null;

    private static boolean isInitSuccess = false;

    // 最大値は0xFFFF 超えるとエラーになる その場合認証からやり直し
    // （OkicaChecker で、シーケンス番号の残りが1000h(4096)未満なら再認証する）
    private static int _snr = 1;

    /**
     * SAMの相互認証を行います
     *
     * @param state Normal/Admin
     * @return 相互認証結果(カード未挿入/成功/失敗)
     */
    public static OpenResult open(byte[] key, States state) {
        synchronized (SamRW.class) {
            try {
                // 備考 伝送速度を設定した状態でもOperatorModeがISO_38400の場合プロトコルエラーが発生する
                _contactCard = _iccReader.connectCard(VCC.VOLT_5, OperatorMode.ISO_MODE, SpeedMode.HIGH);
            } catch (SDKException e) {
                Timber.e(e);
                return OpenResult.SAM_NOT_FOUND;
            }

            isInitSuccess = setRWSAMMode(state) && attention() && authentication1(key) && authentication2(key);

            if (isInitSuccess) {
                Timber.i("SAM 相互認証成功");
                _snr = 1;
                return OpenResult.SUCCESS;
            } else {
                Timber.i("SAM 相互認証失敗");
                return OpenResult.AUTHENTICATION_FAILURE;
            }
        }
    }

    public static void close() {
        _iccReader.disconnectCard();
    }

    /**
     * 6.3.1 Set RWSAM Mode コマンド
     *
     * <概要>
     * RW-SAM のステートを設定するコマンドです。
     * Neutral ステートの時に、RW-SAM との相互認証なしで実行することができます。
     *
     * <実行可能ステート>
     * ・Neutral
     *
     * @param state Normal/Admin
     * @return 成功/失敗
     */
    public static boolean setRWSAMMode(States state) {
        Timber.d("setRWSAMMode() 開始");

        final Bytes data = new Bytes()
                .add(0x00)        // Dispatcher
                .add(0x00, 0x00)  // Reserved
                .add(0xE6)        // コマンドコード
                .add(0x02)        // サブコマンドコード
                .add(state == States.Admin ? 0x03 : 0x02);  // RW-SAM ステート(02h: Normal, 03h: Admin)

        final SamCommand cmd = new SamCommand(HEADER, data);

        final SamResponse<SetRWSAMMode> resp = transmit(cmd, new SetRWSAMMode());

        Timber.d("setRWSAMMode() 終了");
        return !resp.hasError();
    }

    /**
     * 6.3.2 Get RWSAM Mode コマンド
     *
     * ＜概要＞
     * 現在の RW-SAM ステートを取得するコマンドです。
     * RW-SAM との相互認証なしで実行することができます
     *
     * <実行可能ステート>
     * ・Normal
     * ・Admin
     */
    public static States getRWSAMMode() {
        Timber.d("getRWSAMMode() 開始");

        final Bytes data = new Bytes()
                .add(0x00)        // Dispatcher
                .add(0x00, 0x00)  // Reserved1
                .add(0xE6)        // コマンドコード
                .add(0x04);       // サブコマンドコード

        final SamCommand cmd = new SamCommand(HEADER, data);

        final SamResponse<GetRWSAMMode> resp = transmit(cmd, new GetRWSAMMode());

        Timber.d("getRWSAMMode() 終了");

        if (!resp.hasError()) {
            switch (resp.getData().getRwSamState()[0]) {
                case 0x02:
                    Timber.d("RW-SAM: Normalステート");
                    return States.Normal;
                case 0x03:
                    Timber.d("RW-SAM: Adminステート");
                    return States.Admin;
            }
        }

        return States.Neutral;
    }

    /**
     * 6.3.3 Attention コマンド
     *
     * ＜概要＞
     * RW-SAM の動作を確認するコマンドです。実行中のコマンドを停止する目的でも使用します。
     * FeliCa ドライバが Attention コマンドを実行した場合、RW-SAM はどのモードにおいても、処理中のコマンド以外の
     * 内部状態をすべて保持したまま、コマンド待ち状態となります。FeliCa ドライバと RW-SAM 間での
     * 相互認証後の各種パラメータや、カードに対する処理対象のカードテーブルの内容もそのまま保持されるため
     * その後の処理もその状態から続行することができます
     *
     * ＜実行可能なステート＞
     * ・Normal
     * ・Admin
     *
     * @return 成功/失敗
     */
    public static boolean attention() {
        Timber.d("attention() 開始");

        final Bytes data = new Bytes()
                .add(0x00)        // Dispatcher
                .add(0x00, 0x00)  // Reserved
                .add(0x00)        // コマンドコード
                .add(0x00, 0x00); // Reserved

        final SamCommand cmd = new SamCommand(HEADER, data);

        final SamResponse<Attention> resp = transmit(cmd, new Attention());

        if (resp.hasError()) {
            return false;
        }

        Attention respData = resp.getData();

        _serial = respData.getSerialNo();

        Timber.d("attention() 終了");
        return true;
    }

    /**
     * 6.3.4 Authentication1 コマンド
     *
     * パケットデータタイプ 1、暗号化種別：3-key Triple DES, 暗号なし（相互認証タイプ 2）
     *
     * ＜概要＞
     * FeliCa ドライバが RW-SAM を認証するコマンドです。「4 セキュリティ」で説明した認証手順で使用されます。
     * Authentication1 コマンドが完了すると、RW-SAM のモードはモード 1 に遷移します。
     * コマンドパケット内に不正な RW-SAM 製造番号を指定すると、RW-SAM はシンタックスエラーを返答します。
     *
     * ＜実行可能なステート＞
     * ・Normal
     * ・Admin
     *
     * @param key キー
     *
     * @return 成功/失敗
     */
    public static boolean authentication1(byte[] key) {
        Timber.d("authentication1 開始");

        _rar = McUtils.generateRandomBytes(8);

        final Bytes data = new Bytes()
                .add(0x00)        // Dispatcher
                .add(0x00, 0x00)  // Reserved
                .add(0xE0)        // コマンドコード
                .add(0x00, 0x00)  // Reserved
                .add(_serial)     // RW-SAM 製造番号
                .add(_rar);       // Rar

        final SamCommand cmd = new SamCommand(HEADER, data);

        final SamResponse<Authentication1> resp = transmit(cmd, new Authentication1(key, IV));

        if (resp.hasError()) {
            return false;
        }

        Authentication1 respData = resp.getData();

        final byte[] rar = respData.getRar();
        if (rar.length != _rar.length) {
            Timber.e("rarサイズ不一致");
            return false;
        }

        boolean isMatch = true;

        for (int i = 0; i < rar.length; i++) {
            if (rar[i] != _rar[i]) {
                isMatch = false;
                break;
            }
        }

        Timber.d("authentication1 終了");
        if (isMatch) {
            _rbr = respData.getRbr();
            _KYtr = respData.getKYtr();
            _KYmac = respData.getKYmac();
            return true;
        } else {
            return false;
        }
    }

    /**
     * 6.3.5 Authentication2 コマンド
     * パケットデータタイプ 1、暗号化種別：3-key Triple DES, 暗号なし（相互認証タイプ 2）
     *
     * ＜概要＞
     * RW-SAM が FeliCa ドライバを認証するコマンドです。「4 セキュリティ」で説明した認証手順で使用されます。
     * このコマンドは、モード 1 でのみ実行可能です。
     * Authentication2 コマンドが完了すると、RW-SAM のモードはモード 2 に遷移します。
     * Authentication2 コマンドが失敗すると、RW-SAM のモードはモード 0 に遷移します。
     * コマンドパケット内に不正な RW-SAM 製造番号を指定すると、RW-SAM はシンタックスエラーを返答します
     *
     * ＜実行可能なステート＞
     * ・Normal
     * ・Admin
     *
     * @param key キー
     *
     * @return 成功/失敗
     */
    public static boolean authentication2(byte[] key) {
        Timber.d("authentication2 開始");

        _rcr = McUtils.generateRandomBytes(8);
        final byte[] M2r = Crypto.TripleDES.encrypt(new Bytes(_rar, _rbr, _rcr).toArray(), key, IV);

        if (M2r == null) {
            Timber.e("M2r生成失敗");
            return false;
        }

        final Bytes data = new Bytes()
                .add(0x00)        // Dispatcher
                .add(0x00, 0x00)  // Reserved
                .add(0xE0)        // コマンドコード
                .add(0x02)        // サブコマンドコード
                .add(_serial)     // RW-SAM 製造番号
                .add(M2r);        // M2r

        final SamCommand cmd = new SamCommand(HEADER, data);

        final SamResponse<Authentication2> resp = transmit(cmd, new Authentication2());

        Timber.d("authentication2 終了");
        return !resp.hasError();
    }

    /**
     * 6.5.1 Polling コマンド
     *
     * パケットデータタイプ 1
     *
     * ＜概要＞
     * Polling コマンドは、カードを検出する場合に使うコマンドです。RW-SAM はカードに対する Polling
     * コマンドパケットを生成します。また、カードからのレスポンスパケットを RW-SAM に送信することで
     * RW-SAM は IDm, PMm の管理を行います。
     * RW-SAM は、1 枚以上のカードからのレスポンスを受信するまで、Polling コマンドパケットの生成を無限に
     * 繰り返します。RW-SAM は最大 4 枚のカードレスポンスを処理することができます。RW-SAM は、本コマンドで
     * 取得したカードの PMm を記憶し、以降のカードコマンドのタイムアウト時間を計算します
     *
     * @return コマンドレスポンス
     */
    @NotNull
    public static SamResponse<SubResponse> pollingCommand() {
        Timber.d("pollingCommand() 開始");

        final Bytes data = new Bytes()
                .add(0x00)         // Dispatcher
                .add(0x00, 0x00)   // Reserved
                .add(0x80)         // コマンドコード
                .add(getIDtr())    // IDtr
                .add(Constants.SYSTEM_CODE)  // システムコード
                .add(0x00);        // タイムスロット

        final SamCommand cmd = new SamCommand(HEADER, data, _KYtr, _KYmac, IV);

        final SamResponse<SubResponse> resp = transmit(cmd, new SubResponse());

        Timber.d("pollingCommand() 終了");
        return resp;
    }

    @NotNull
    public static SamResponse<Polling> pollingSubCommand(byte[] IDm, byte[] PMm) {
        Timber.d("pollingSubCommand() 開始");

        final Bytes data = new Bytes()
                .add(0x01)        // Dispatcher
                .add(0x00, 0x00)  // Reserved
                .add(0x01)        // Reserved
                .add(0x01)        // 受信数
                .add(IDm)         // IDm
                .add(PMm);        // PMm

        final SamCommand cmd = new SamCommand(HEADER, data);

        final SamResponse<Polling> resp = transmit(cmd, new Polling(_KYtr, _KYmac, IV));

        if (!resp.hasError() && !checkAndCountUpSnr(resp.getData().getSnr())) {
            resp.setError(new SequenceMisMatchException());
        }

        Timber.d("pollingSubCommand() 終了");
        return resp;
    }

    /**
     * 6.5.5 Mutual Authentication コマンド
     *
     * パケットデータタイプ 1
     *
     * ＜概要＞
     * RW-SAM は、カードに対する Authentication1 コマンドパケットおよび Authentication2 コマンドパケット
     * を生成します。また、IDm で指定されたカードの PMm からタイムアウトを計算します。
     * 暗号通信に必要な情報はカード管理番号（IDt）で管理されます。以降の暗号化されたカードコマンドパケット
     * の生成には、IDt で指定された情報を使用します。
     *
     * <実行可能なステート>
     * Normal
     *
     * @param IDm IDm
     *
     * @return コマンドレスポンス(対カード用暗号化パケット)
     */
    public static SamResponse<SubResponse> mutualAuthenticationRWSAMCommand(byte[] IDm) {
        Timber.d("mutualAuthenticationRWSAMCommand() 開始");

        final Bytes data = new Bytes()
                .add(0x00)                    // Dispatcher
                .add(0x00, 0x00)              // Reserved
                .add(0xE2)                    // コマンドコード
                .add(getIDtr())               // IDtr
                .add(IDm)                     // IDm
                .add(0x00)                    // Reserved
                .add(0x02)                    // 指定鍵タイプ (01h : エリア／サービス鍵 02h : GSK/USK)
                .add(Constants.SYSTEM_CODE)   // パラメータ
                .add(Constants.GSK_CODE)      // パラメータ
                .add(Constants.GSK_VERSION)   // パラメータ
                .add(Constants.USK_CODE)      // パラメータ
                .add(Constants.USK_VERSION);  // パラメータ

        final SamCommand cmd = new SamCommand(HEADER, data, _KYtr, _KYmac, IV);
        final SamResponse<SubResponse> resp = transmit(cmd, new SubResponse());

        Timber.d("mutualAuthenticationRWSAMCommand() 終了");
        return resp;
    }

    public static SamResponse<SubResponse> mutualAuthenticationRWSAMSubCommand1(byte[] cardResp) {
        Timber.d("mutualAuthenticationRWSAMSubCommand1() 開始");

        final Bytes data = new Bytes()
                .add(0x01)        // Dispatcher
                .add(0x00, 0x00)  // Reserved
                .add(cardResp);   // カードレスポンスパケット

        final SamCommand cmd = new SamCommand(HEADER, data);

        final SamResponse<SubResponse> resp = transmit(cmd, new SubResponse());

        Timber.d("mutualAuthenticationRWSAMSubCommand1() 終了");
        return resp;
    }

    public static SamResponse<MutualAuthenticationRWSAM> mutualAuthenticationRWSAMSubCommand2(byte[] cardResp) {
        Timber.d("mutualAuthenticationRWSAMSubCommand2() 開始");

        final Bytes data = new Bytes()
                .add(0x01)        // Dispatcher
                .add(0x00, 0x00)  // Reserved
                .add(cardResp);   // カードレスポンスパケット

        final SamCommand cmd = new SamCommand(HEADER, data);

        final SamResponse<MutualAuthenticationRWSAM> resp = transmit(cmd, new MutualAuthenticationRWSAM(_KYtr, _KYmac, IV));

        if (!resp.hasError() && !checkAndCountUpSnr(resp.getData().getSnr())) {
            resp.setError(new SequenceMisMatchException());
        }

        Timber.d("mutualAuthenticationRWSAMSubCommand2() 終了");
        return resp;
    }

    /**
     * ＜概要＞
     * RW-SAM は、カードに対する Read コマンドパケット、または Read v2 コマンドパケットを生成します。
     * カードパケットの暗号化／復号は、指定した IDt のパラメータを使用して RW-SAM が行います。また、IDtで
     * 指定されたカードの PMm からタイムアウトを計算します。
     *
     * ＜実行可能なステート＞
     * ・Normal ステート
     *
     * @param IDt IDt
     * @param blockList ブロックリスト
     *
     * @return 対カード用コマンドパケット
     */
    public static SamResponse<SubResponse> readBlockCommand(byte[] IDt, byte[] blockList) {
        Timber.d("ReadBlockCommand() 開始");

        SetBytes(READ_BLOCK, calcBlockSize(blockList));

        final Bytes data = new Bytes()
                .add(0x00)                     // Dispatcher
                .add(0x00, 0x00)               // Reserved
                .add(0x88)                     // コマンドコード
                .add(getIDtr())                // IDtr
                .add(IDt)                      // IDt
                .add(calcBlockSize(blockList))  // ブロック数
                .add(blockList);               // ブロックリスト

        SetBytes(READ_REQ_BYTES, data.toArray().length);

        OkicaRW.SetStartTime(OkicaRW.KEY_SAM_READ1, System.currentTimeMillis());

        final SamCommand cmd = new SamCommand(HEADER, data, _KYtr, _KYmac, IV);

        OkicaRW.SetEndTime(OkicaRW.KEY_SAM_READ1, System.currentTimeMillis());

        OkicaRW.SetStartTime(OkicaRW.KEY_SAM_READ2, System.currentTimeMillis());

        final SamResponse<SubResponse> resp = transmit(cmd, new SubResponse());

        OkicaRW.SetEndTime(OkicaRW.KEY_SAM_READ2, System.currentTimeMillis());

        Timber.d("ReadBlockCommand() 終了");
        return resp;
    }

    public static SamResponse<ReadBlock> readBlockSubCommand(byte[] cardResp) {
        Timber.i("ReadBlockSubCommand() 開始");

        final Bytes data = new Bytes()
                .add(0x01)        // Dispatcher
                .add(0x00, 0x00)  // Reserved
                .add(cardResp);   // カードレスポンスパケット

        OkicaRW.SetStartTime(OkicaRW.KEY_SAM_READRES1, System.currentTimeMillis());

        final SamCommand cmd = new SamCommand(HEADER, data);

        OkicaRW.SetEndTime(OkicaRW.KEY_SAM_READRES1, System.currentTimeMillis());

        OkicaRW.SetStartTime(OkicaRW.KEY_SAM_READRES2, System.currentTimeMillis());

        final SamResponse<ReadBlock> resp = transmit(cmd, new ReadBlock(_KYtr, _KYmac, IV));

        OkicaRW.SetEndTime(OkicaRW.KEY_SAM_READRES2, System.currentTimeMillis());

        if (!resp.hasError() && !checkAndCountUpSnr(resp.getData().getSnr())) {
            resp.setError(new SequenceMisMatchException());
        }

        SetBytes(READ_RES_BYTES, resp.getRawData().length);

        Timber.i("ReadBlockSubCommand() 終了");
        return resp;
    }

    /**
     * ＜概要＞
     * RW-SAM は、カードに対する Write コマンドパケット、または Write v2 コマンドパケットを生成します。
     * カードパケットの暗号化／復号は、指定した IDt のパラメータを使用して RW-SAM が行います。また、IDt
     * で指定されたカードの PMm からタイムアウトを計算します。
     *
     * ＜実行可能なステート＞
     * ・Normal ステート
     *
     * @param IDt IDt
     * @param blockList ブロックリスト
     * @param blockData ブロックリスト
     *
     * @return 対カード用コマンドパケット
     */
    public static SamResponse<SubResponse> writeBlockCommand(byte[] IDt, byte[] blockList, byte[] blockData) {
        Timber.d("WriteBlockCommand() 開始");

        SetBytes(WRITE_BLOCK, calcBlockSize(blockList));

        final Bytes data = new Bytes()
                .add(0x00)                     // Dispatcher
                .add(0x00, 0x00)               // Reserved
                .add(0x8A)                     // コマンドコード
                .add(getIDtr())                // IDtr
                .add(IDt)                      // IDt
                .add(calcBlockSize(blockList))  // ブロック数
                .add(blockList)                // ブロックリスト
                .add(blockData);               // ブロックデータ

        final SamCommand cmd = new SamCommand(HEADER, data, _KYtr, _KYmac, IV);
        final SamResponse<SubResponse> resp = transmit(cmd, new SubResponse());

        SetBytes(WRITE_RES_BYTES, resp.getRawData().length);

        Timber.d("WriteBlockCommand() 終了");
        return resp;
    }

    public static SamResponse<WriteBlock> writeBlockSubCommand(byte[] cardResp) {
        Timber.i("WriteBlockSubCommand() 開始");

        final Bytes data = new Bytes()
                .add(0x01)        // Dispatcher
                .add(0x00, 0x00)  // Reserved
                .add(cardResp);   // カードレスポンスパケット

        final SamCommand cmd = new SamCommand(HEADER, data);
        final SamResponse<WriteBlock> resp = transmit(cmd, new WriteBlock(_KYtr, _KYmac, IV));

        if (!resp.hasError() && !checkAndCountUpSnr(resp.getData().getSnr())) {
            resp.setError(new SequenceMisMatchException());
        }

        Timber.i("WriteBlockSubCommand() 終了");
        return resp;
    }

    /**
     * ＜概要＞
     * RW-SAM は、カードに対する Request Service コマンドパケットを生成します。また、IDm で指定された
     * カードの PMm からタイムアウトを計算します。
     *
     * ＜実行可能なステート＞
     * •Normal ステート
     *
     * @param IDm IDm
     * @param areaServiceCodeList エリア/サービスコードリスト
     *
     * @return 対カード用コマンドパケット
     */
    public static SamResponse<SubResponse> requestServiceCommand(byte[] IDm, byte[] areaServiceCodeList) {
        Timber.d("requestServiceCommand() 開始");

        final Bytes data = new Bytes()
                .add(0x00)                // Dispatcher
                .add(0x00, 0x00)          // Reserved
                .add(0x82)                // コマンドコード
                .add(getIDtr())           // IDtr
                .add(IDm)                 // IDm
                .add(areaServiceCodeList.length/2)
                .add(areaServiceCodeList);

        final SamCommand cmd = new SamCommand(HEADER, data, _KYtr, _KYmac, IV);
        final SamResponse<SubResponse> resp = transmit(cmd, new SubResponse());

        Timber.d("requestServiceCommand() 終了");
        return resp;
    }

    public static SamResponse<RequestService> requestServiceSubCommand(byte[] cardResp) {
        Timber.i("requestServiceSubCommand() 開始");

        final Bytes data = new Bytes()
                .add(0x01)        // Dispatcher
                .add(0x00, 0x00)  // Reserved
                .add(cardResp);   // カードレスポンスパケット

        final SamCommand cmd = new SamCommand(HEADER, data);
        final SamResponse<RequestService> resp = transmit(cmd, new RequestService(_KYtr, _KYmac, IV));

        if (!resp.hasError() && !checkAndCountUpSnr(resp.getData().getSnr())) {
            resp.setError(new SequenceMisMatchException());
        }

        Timber.i("requestServiceSubCommand() 終了");
        return resp;
    }

    /**
     * ＜概要＞
     * 認証不要のブサービスからブロックデータを読みだします
     *
     * ＜実行可能なステート＞
     * •Normal ステート
     *
     * @param IDm IDm
     * @param areaServiceCodeList エリア/サービスコードリスト
     * @param blockList ブロックリスト
     *
     * @return 対カード用コマンドパケット
     */
    public static SamResponse<SubResponse> readBlockWithoutEncryptionCommand(byte[] IDm, byte[] areaServiceCodeList, byte[] blockList) {
        Timber.d("readBlockWithoutEncryptionCommand() 開始");

        final Bytes data = new Bytes()
                .add(0x00)                          // Dispatcher
                .add(0x00, 0x00)                    // Reserved
                .add(0x98)                          // コマンドコード
                .add(getIDtr())                     // IDtr
                .add(IDm)                           // IDt
                .add(areaServiceCodeList.length/2)  // エリア/サービス数
                .add(areaServiceCodeList)           // エリア/サービスコードリスト
                .add(blockList.length/2)            // ブロック数
                .add(blockList);                    // ブロックリスト

        final SamCommand cmd = new SamCommand(HEADER, data, _KYtr, _KYmac, IV);
        final SamResponse<SubResponse> resp = transmit(cmd, new SubResponse());

        Timber.d("readBlockWithoutEncryptionCommand() 終了");
        return resp;
    }

    public static SamResponse<GetRWSAMKeyVersion> getRWSAMKeyVersionCommand(int keyType) {
        Timber.d("getRWSAMKeyVersionCommand() 開始");

        final Bytes data = new Bytes()
                .add(0x00)        // Dispatcher
                .add(0x00, 0x00)  // Reserved
                .add(0xE4)        // コマンドコード
                .add(getIDtr())   // IDtr
                .add(0xA2)        // サブコマンドコード
                .add(keyType);    // パッケージ鍵

        final SamCommand cmd = new SamCommand(HEADER, data, _KYtr, _KYmac, IV);
        final SamResponse<GetRWSAMKeyVersion> resp = transmit(cmd, new GetRWSAMKeyVersion(_KYtr, _KYmac, IV));

        if (!resp.hasError() && !checkAndCountUpSnr(resp.getData().getSnr())) {
            resp.setError(new SequenceMisMatchException());
        }

        Timber.d("getRWSAMKeyVersionCommand() 終了");
        return resp;
    }

    public static SamResponse<GenerateRWSAMPackage> generateRWSAMPackageCommand(byte[] pkgKey, PackageData.IPackageData pkgData) {
        Timber.d("generateRWSAMPackageCommand() 開始");

        final Bytes data = new Bytes()
                .add(0x00)                      // Dispatcher
                .add(0x00, 0x00)                // Reserved
                .add(0xE4)                      // コマンドコード
                .add(getIDtr())                 // IDtr
                .add(0xAA)                      // サブコマンドコード
                .add(pkgKey)                    // パッケージ鍵
                .add(pkgData.getPackageType())  // パッケージタイプ
                .add(pkgData.getData().length)  // データ長
                .add(pkgData.getData());        // ブロック数

        final SamCommand cmd = new SamCommand(HEADER, data, _KYtr, _KYmac, IV);
        final SamResponse<GenerateRWSAMPackage> resp = transmit(cmd, new GenerateRWSAMPackage(_KYtr, _KYmac, IV));

        if (!resp.hasError() && !checkAndCountUpSnr(resp.getData().getSnr())) {
            resp.setError(new SequenceMisMatchException());
        }

        Timber.d("generateRWSAMPackageCommand() 終了");
        return resp;
    }

    public static SamResponse<ChangeRWSAMKey> changeRWSAMKeyCommand(int keyType, byte[] pkg) {
        Timber.d("changeRWSAMKeyCommand() 開始");

        final Bytes data = new Bytes()
                .add(0x00)                      // Dispatcher
                .add(0x00, 0x00)                // Reserved
                .add(0xE4)                      // コマンドコード
                .add(getIDtr())                 // IDtr
                .add(0xA0)                      // サブコマンドコード
                .add(keyType)                   // RW-SAM鍵タイプ
                .add(pkg);                      // パッケージ

        final SamCommand cmd = new SamCommand(HEADER, data, _KYtr, _KYmac, IV);
        final SamResponse<ChangeRWSAMKey> resp = transmit(cmd, new ChangeRWSAMKey(_KYtr, _KYmac, IV));

        if (!resp.hasError() && !checkAndCountUpSnr(resp.getData().getSnr())) {
            resp.setError(new SequenceMisMatchException());
        }

        Timber.d("changeRWSAMKeyCommand() 終了");
        return resp;
    }

    public static SamResponse<RegisterFeliCaKey> registerFeliCaKeyCommand(byte[] pkg) {
        Timber.d("registerFeliCaKeyCommand() 開始");

        final Bytes data = new Bytes()
                .add(0x00)        // Dispatcher
                .add(0x00, 0x00)  // Reserved
                .add(0xE4)        // コマンドコード
                .add(getIDtr())   // IDtr
                .add(0xA4)        // サブコマンドコード
                .add(pkg.length)  // パッケージ長
                .add(pkg);        // パッケージ

        final SamCommand cmd = new SamCommand(HEADER, data, _KYtr, _KYmac, IV);
        final SamResponse<RegisterFeliCaKey> resp = transmit(cmd, new RegisterFeliCaKey(_KYtr, _KYmac, IV));

        if (!resp.hasError() && !checkAndCountUpSnr(resp.getData().getSnr())) {
            resp.setError(new SequenceMisMatchException());
        }

        Timber.d("registerFeliCaKeyCommand() 終了");
        return resp;
    }

    public static SamResponse<ClearRWSAMParameter> clearRWSAMParameterCommand() {
        Timber.d("clearRWSAMParameterCommand() 開始");

        final Bytes data = new Bytes()
                .add(0x00)        // Dispatcher
                .add(0x00, 0x00)  // Reserved
                .add(0xE4)        // コマンドコード
                .add(getIDtr())   // IDtr
                .add(0xB4);       // サブコマンドコード

        final SamCommand cmd = new SamCommand(HEADER, data, _KYtr, _KYmac, IV);
        final SamResponse<ClearRWSAMParameter> resp = transmit(cmd, new ClearRWSAMParameter(_KYtr, _KYmac, IV));

        if (!resp.hasError() && !checkAndCountUpSnr(resp.getData().getSnr())) {
            resp.setError(new SequenceMisMatchException());
        }

        Timber.d("clearRWSAMParameterCommand() 終了");
        return resp;
    }

    public static SamResponse<SetISO7816Mode> setISO7816Mode(int mode) {
        Timber.d("setISO7816Mode() 開始");

        final Bytes data = new Bytes()
                .add(0x00)        // Dispatcher
                .add(0x00, 0x00)  // Reserved
                .add(0xE4)        // コマンドコード
                .add(getIDtr())   // IDtr
                .add(0xBC)        // サブコマンドコード
                .add(mode);       // ISO7816動作モード

        final SamCommand cmd = new SamCommand(HEADER, data, _KYtr, _KYmac, IV);
        final SamResponse<SetISO7816Mode> resp = transmit(cmd, new SetISO7816Mode(_KYtr, _KYmac, IV));

        if (!resp.hasError() && !checkAndCountUpSnr(resp.getData().getSnr())) {
            resp.setError(new SequenceMisMatchException());
        }

        Timber.d("setISO7816Mode() 終了");
        return resp;
    }

    public static SamResponse<ChangeCommunicationMode> changeCommunicationModeCommand(int mode) {
        Timber.d("changeCommunicationModeCommand() 開始");

        final Bytes data = new Bytes()
                .add(0x00)        // Dispatcher
                .add(0x00, 0x00)  // Reserved
                .add(0x46)        // コマンドコード
                .add(getIDtr())   // IDtr
                .add(mode)        // 伝送速度
                .add(0x00)
                .add(0x12);


        final SamCommand cmd = new SamCommand(HEADER, data, _KYtr, _KYmac, IV);
        final SamResponse<ChangeCommunicationMode> resp = transmit(cmd, new ChangeCommunicationMode(_KYtr, _KYmac, IV));

        if (!resp.hasError() && !checkAndCountUpSnr(resp.getData().getSnr())) {
            resp.setError(new SequenceMisMatchException());
        }

        Timber.d("changeCommunicationModeCommand() 終了");
        return resp;
    }

    /**
     * 3.3.3 カード無応答パケット
     *
     * カードコマンドを送信した後、タイムアウト時間が経過してもカードレスポンスが受信できない場合には
     * RW-SAM に下記のカード無応答パケットを送信してください。RW-SAM はカードコマンドのリトライのために
     * 再びカードコマンドパケットを生成・暗号化し、FeliCa ドライバに返送します。
     *
     * @return リトライパケット
     */
    public static SamResponse<SubResponse> cardNoResponse() {
        Timber.i("CardNoResponse() 開始");

        final Bytes data = new Bytes()
                .add(0x01)         // Dispatcher
                .add(0x00, 0x00);  // Reserved

        final SamCommand cmd = new SamCommand(HEADER, data);
        final SamResponse<SubResponse> resp = transmit(cmd, new SubResponse());

        Timber.i("CardNoResponse() 終了");
        return resp;
    }

    /**
     * 6.4.3 Get Last Error コマンド
     *
     * ＜概要＞
     * RW-SAM のエラー情報を提供します。RW-SAM は問題を検出するとシンタックスエラーを返します。
     * シンタックスエラーの理由はこのコマンドで確認できます。また、カードレスポンスが期待と異なる場合、
     * カードコマンドの再送を行います。このとき、本コマンドでエラーコードを取得することができます。
     * コマンドごとの詳細な情報については、各コマンド仕様の章を参照してください。
     *
     * ＜実行可能なステート＞
     * ・Neutral
     * ・Normal
     * ・Admin
     */
    public static SamResponse<GetLastError> getLastError() {
        Timber.i("getLastError() 開始");

        final Bytes data = new Bytes()
                .add(0x00)        // Dispatcher
                .add(0x00, 0x00)  // Reserved
                .add(0x28)        // コマンドコード
                .add(0x00, 0x00); // Reserved

        final SamCommand cmd = new SamCommand(HEADER, data);

        final SamResponse<GetLastError> resp = transmit(cmd, new GetLastError());

        Timber.i("getLastError() 終了");

        return resp;
    }

    public static void terminate() {
        attention();
        _snr += 1;
    }

    private static <T extends ISamResponseType> SamResponse<T> transmit(SamCommand cmd, T impl) {
        SamResponse<T> ret;

        try {
            long start = System.currentTimeMillis();

            final ResponseApdu resp = _iccReader.transmit(_contactCard, cmd.toCommandApdu());
            Timber.d("SAN処理時間: %s", System.currentTimeMillis() - start);
            ret = new SamResponse<T>(resp, impl);
        } catch (SDKException  e) {
            ret = new SamResponse<T>(new SamSDKException(e));
        } catch (Exception e) {
            ret = new SamResponse<T>(e);
        }

        final Throwable e = ret.getError();

        if (e != null)  {
            _snr += 1;
        }

        //Timber.d("SAMコマンド: %s", cmd);
        //Timber.d("SAMレスポンス: %s", ret);

        return ret;
    }

    /**
     * シーケンス番号を取得します
     * @return
     */
    public static int getSnr () {
        return _snr;
    }

    /**
     * トランザクションIDを返却します
     * シーケンス番号はリトルエンディアン形式となります
     *
     * @return IDtr
     */
    public static byte[] getIDtr() {
        return new Bytes()
                .add(0x00FF & _snr)
                .add((0xFF00 & _snr) >> 8)
                .add(Arrays.copyOfRange(_rcr, 2, _rcr.length))
                .toArray();
    }

    /**
     * シーケンス番号のチェックを行います
     * 成否にかかわらずシーケンス番号を2足します
     *
     * @param snr シーケンス番号
     *
     * @return 一致/不一致
     */
    private static boolean checkAndCountUpSnr(int snr) {
        // リトルエンディアン
        final boolean b = _snr+1 == snr;

        _snr += 2;
        return b;
    }

    /**
     * ブロック数を計算します
     *
     * @param blockList ブロックリスト
     *
     * @return ブロック数
     */
    private static int calcBlockSize(byte[] blockList) {
        int idx = 0;
        int size = 0;

        while (idx < blockList.length) {
            if ((blockList[idx] & 0b1000_0000) != 0) {
                // 2バイトデータ
                size += 1;
                idx += 2;
            } else {
                // 3バイトデータ
                size += 2;
                idx += 3;
            }
        }

        return size;
    }
}
