package jp.mcapps.android.multi_payment_terminal.data.sam;

import androidx.annotation.NonNull;

import com.pos.device.apdu.CommandApdu;

import jp.mcapps.android.multi_payment_terminal.data.Bytes;
import jp.mcapps.android.multi_payment_terminal.util.Crypto;
import timber.log.Timber;

/**
 * SAMのコマンドを生成します
 * Lcの計算は自動で行われます
 */
public class SamCommand {
    private final byte[] header;
    private final Bytes data;
    private final int Lc;

    private int Le = 0x00;
    public SamCommand setLe(int Le) {
        this.Le = Le;
        return this;
    }

    private final Bytes cmd;

    /**
     * コンストラクタ
     * コマンドは暗号化されません
     *
     * @param header ヘッダー
     * @param data データ
     */
    public SamCommand(byte[] header, Bytes data) {
        this.data = data;
        this.header = header;
        Lc = data.size();

        Bytes cmd = new Bytes();
        cmd.add(this.header);
        cmd.add(this.Lc);
        cmd.add(this.data);
        cmd.add(this.Le);

        this.cmd = cmd;
    }

    /**
     * コンストラクタ
     * コマンドは暗号化されます
     *
     * @param header ヘッダー
     * @param data データ
     * @param KYtr トランザクション鍵
     * @param KYmac Mac鍵
     * @param IV 初期ベクター
     */
    public SamCommand(byte[] header, Bytes data, byte[] KYtr, byte[] KYmac, byte[] IV) {
        Timber.d("SAM平文コマンド: %s", data);

        final Bytes encData = this.encrypt(data, KYtr, KYmac, IV);
        this.data = encData;

        this.header = header;
        Lc = this.data.size();

        final Bytes cmd = new Bytes();
        cmd.add(this.header);
        cmd.add(this.Lc);
        cmd.add(this.data);
        cmd.add(this.Le);

        this.cmd = cmd;
    }

    /**
     * APDUコマンドに変換します
     *
     * @return APDUコマンド
     */
    public CommandApdu toCommandApdu() {
        return new CommandApdu(this.cmd.toArray());
    }

    @NonNull
    public String toString() {
        return cmd.toString();
    }

    private Bytes encrypt(Bytes plainData, byte[] KYtr, byte[] KYmac, byte[] IV) {
        final Bytes headData = plainData.subList(0, 4);
        final Bytes footData = plainData.subList(4, plainData.size());

        final Bytes encData = new Bytes();

        final int padLen = 8 - footData.size() % 8;

        if (padLen < 8) {
            for (int i = 0; i < padLen; i++) {
                footData.add(0xFF);
            }
        }

        /*
            最初の 4 バイト(Dispatcher/Reserved/CMD)は、4 バイトの 00h が付加され
            KYmacで暗号化
        */
        headData.add(0x00, 0x00, 0x00, 0x00);
        byte[] mac = Crypto.TripleDES.encrypt(headData.toArray(), KYmac, IV);

        for (int cnt = 0; cnt < footData.size()/8; cnt++) {
            final byte[] nextBlock = footData.copyOfRange(cnt*8, cnt*8+8);
            final byte[] xor = new byte[] {0x00, 0x00, 0x00 ,0x00, 0x00, 0x00, 0x00, 0x00};

            for (int i = 0; i < 8; i++) {
                xor[i] = (byte)(mac[i] ^ nextBlock[i]);
            }

            mac = Crypto.TripleDES.encrypt(xor, KYmac, IV);
        }

        footData.add(mac);

        byte[] block = Crypto.TripleDES.encrypt(footData.copyOfRange(0, 8), KYtr, IV);
        encData.add(block);

        for (int cnt = 1; cnt < footData.size()/8; cnt++) {
            final byte[] nextBlock = footData.copyOfRange(cnt*8, cnt*8+8);
            final byte[] xor = new byte[] {0x00, 0x00, 0x00 ,0x00, 0x00, 0x00, 0x00, 0x00};

            for (int i = 0; i < 8; i++) {
                xor[i] = (byte)(block[i] ^ nextBlock[i]);
            }

            block = Crypto.TripleDES.encrypt(xor, KYtr, IV);
            encData.add(block);
        }

        return new Bytes(plainData.subList(0, 4), encData);
    }
}
