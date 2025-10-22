package jp.mcapps.android.multi_payment_terminal.data.okica;

import static jp.mcapps.android.multi_payment_terminal.AppPreference.setOkicaNegaDatetime;

import android.content.Context;

import com.google.protobuf.ByteString;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

import jp.mcapps.android.multi_payment_terminal.MainApplication;
//import jp.mcapps.android.multi_payment_terminal.thread.emv.utils.ISOUtil;
import jp.mcapps.android.multi_payment_terminal.database.history.uriOkica.ISOUtil;
import jp.mcapps.android.multi_payment_terminal.util.Crypto;
import timber.log.Timber;

/**
 * OKICAネガリストをオブジェクトのように扱うためのクラスです
 */

public class OkicaNegaFile {
    private static final String NegaFileName = "OkicaNegaList.dat";
    private static final String NewNegaFileName = "OkicaNegaListNew.dat";
    private static final int NegaFileHeaderSize = 16;
    private static final int IDiSize = 8;
    private static final MainApplication _app;
    private static byte[] _negas = null;    // 復号化したネガリスト

    static {
        _app = MainApplication.getInstance();
    }

    /**
     * @param negaDate 比較する年月日(YYYYMMDD)
     * @return 比較する年月日の方が新しければtrue, 取得済みのネガ年月日と一致するか古ければfalse
     */
    public static boolean isNegaVersionNew(final String negaDate) {
        boolean ret = true; // ファイルアクセス中に例外が発生したら、比較できないので新しい日付とみなす
        Context context = MainApplication.getInstance().getApplicationContext();
        try {
            BufferedInputStream bf = new BufferedInputStream(new FileInputStream(new File(context.getFilesDir(), NegaFileName)));
            byte[] header = new byte[9];
            int readLen = bf.read(header);
            bf.close();
            if (readLen != 9) {
                // 9バイト読み込めなかったら比較できないので新しい日付とみなす
                return true;
            }
            if (header[8] != 1) {
                // 有効性フラグが無効なら既存ネガリストは使えないので新しい日付とみなす
                return true;
            }
            String curNegaDate = ISOUtil.bcd2str(header, 0, 8, false);
            if (negaDate.compareTo(curNegaDate) <= 0) {
                ret = false;
            }
        }
        catch (IOException e) {

        }
        return ret;
    }

    /**
     * ネガリストをファイルに保存します
     *
     * @param negaDate ネガ年月日
     * @param negaList ネガリスト
     */
    public static void saveOkicaNegaList(final String negaDate, final List<ByteString> negaList) {
        // long requestStart = System.nanoTime();
        Context context = MainApplication.getInstance().getApplicationContext();
        try {
            RandomAccessFile newNegaFile = new RandomAccessFile(new File(context.getFilesDir(), NewNegaFileName), "rw");
            byte[] negaBytes = null;
            try {
                // 年月日をBCD形式で書き込む
                byte[] negaDateBcd = ISOUtil.str2bcd(negaDate, false);
                newNegaFile.write(negaDateBcd);
                // ネガ券数を書き込む
                int negaSize = negaList.size();
                newNegaFile.writeInt(negaSize);
                // 有効性フラグを0にして、予備とともに書き込む
                byte[] reserved = new byte[8];  // 0で初期化される
                reserved[1] = 1;    // AES128で暗号
                reserved[2] = IDiSize;
                newNegaFile.write(reserved);
                // ネガリスト全体を暗号化して書き込む
                negaBytes = new byte[IDiSize * negaSize];
                int pos = 0;
                for (ByteString nega : negaList) {
                    System.arraycopy(nega.toByteArray(), 0, negaBytes, pos, IDiSize);
                    pos += IDiSize;
                }
                newNegaFile.write(Crypto.AES128.encrypt(negaBytes, _app.getRoomAesKye()));
                // 有効性フラグを1に変更する
                newNegaFile.seek(8);
                newNegaFile.writeByte(1);
            }
            catch (IOException e) {
                // ネガファイルを置き換えないようにする
                throw e;
            }
            finally {
                // 書き込み終了
                newNegaFile.close();
            }

            // 新ネガファイルを置き換える
            // androidは置き換え先のファイルが存在しても置き換え可能
            File negaFile = new File(context.getFilesDir(), NewNegaFileName);
            File negaFileToMove = new File(context.getFilesDir(), NegaFileName);
            negaFile.renameTo(negaFileToMove);
            // ネガチェック用にネガ配列を保持しておく
            _negas = negaBytes;
            // ネガ取得日時セット
            setOkicaNegaDatetime(negaDate);
        } catch (IOException ignored) {
            // ここに到達するなら旧ネガファイルが残る
        }
        // Timber.d("ネガリスト保存終了, %,d ns", System.nanoTime() - requestStart);
    }

    /**
     * カードIDiがネガ対象か確認します
     *
     * @param IDi ネガチェックするIDi
     * @return ネガヒットならtrue
     */
    public static boolean checkNegaHit(final byte[] IDi) {
        // long requestStart = System.nanoTime();
        boolean ret = false;
        Context context = MainApplication.getInstance().getApplicationContext();
        if (_negas == null) {
            try {
                File negaFile = new File(context.getFilesDir(), NegaFileName);
                byte[] header = new byte[NegaFileHeaderSize];
                byte[] negasEncrypt = new byte[(int) negaFile.length() - NegaFileHeaderSize];
                try (FileInputStream negaFileStream = new FileInputStream(negaFile)) {
                    negaFileStream.read(header, 0, NegaFileHeaderSize);
                    negaFileStream.read(negasEncrypt, 0, negasEncrypt.length);
                }
                _negas = Crypto.AES128.decrypt(negasEncrypt, _app.getRoomAesKye());
            } catch (IOException e) {

            }
        }
        if (_negas == null) {
            return false;
        }
        int top = _negas.length / IDiSize - 1;
        int bottom = 0;
        byte[] negaRead = new byte[IDiSize];
        while (bottom <= top) {
            int middle = (bottom + top) / 2;
            System.arraycopy(_negas, middle * IDiSize, negaRead, 0, IDiSize);
            int comparison = byteArrayCompare(IDi, negaRead);
            if (0 == comparison) {
                ret = true;
                break;
            } else if (comparison > 0) {
                bottom = middle + 1;
            } else {
                top = middle - 1;
            }
        }
        // Timber.d("ネガ検索終了, %,d ns", System.nanoTime() - requestStart);
        return ret;
    }

    /**
     * 2つのbyte配列を比較します
     *
     * @param a1 比較するbyte配列
     * @param a2 比較するbyte配列
     * @return byte配列が一致していれば0, 不一致の場合はa1側の大小に応じて正負のint値
     */
    private static int byteArrayCompare(final byte[] a1, final byte[] a2) {
        int min = Math.min(a1.length, a2.length);
        for (int index = 0; index < min; index++) {
            int comparison = Byte.toUnsignedInt(a1[index]) - Byte.toUnsignedInt(a2[index]);
            if (comparison != 0) {
                return comparison;
            }
        }
        if (a1.length > min) {
            return 1;
        } else if (a2.length > min) {
            return -1;
        }
        return 0;
    }

    /**
     * ネガリストファイルが存在するか確認します
     *
     * @return trueならネガリストファイルが存在
     */
    public static Boolean isExistNegaList() {
        if (_negas != null) {
            // ネガリストをメモリに保持していれば、ファイルもあるとみなす
            return true;
        }
        Context context = MainApplication.getInstance().getApplicationContext();
        File negaFile = new File(context.getFilesDir(), NegaFileName);
        // 有効性フラグを確認する場合
        if (negaFile.exists()) {
            try {
                try (FileInputStream negaFileInputStream = new FileInputStream(negaFile)) {
                    byte[] header = new byte[NegaFileHeaderSize];
                    negaFileInputStream.read(header);
                    if (header[8] == 1) {   // 有効性フラグを確認
                        return true;
                    }
                }
            } catch (IOException e) {
                // ファイルの存在はチェック済み
                // ファイル読み込み中に例外が発生したら、アクセス不可と判断してネガリストファイルがないとみなす
                Timber.e("ネガリストファイルを読み込み中に例外が発生");
            }
        }
        Timber.e("ネガリストファイルが存在しない");
        return false;

        // 有効性フラグを確認しない場合は、確認する場合の処理を削除して、以下のコメントを外す
        // return negaFile.exists();
    }

    /**
     * ファイルの削除をおこないます
     */
    public static boolean delete() {
        try {
            Context context = MainApplication.getInstance().getApplicationContext();
            File negaFile = new File(context.getFilesDir(), NegaFileName);
            if (negaFile.exists()) {
                MainApplication.getInstance().deleteFile(NegaFileName);
                // ネガ取得日時クリア
                setOkicaNegaDatetime(null);
                Timber.i("OKICA ネガファイル削除");
                return true;
            }
        } catch (Exception e) {
            Timber.e(e);
        }
        return false;
    }
}
