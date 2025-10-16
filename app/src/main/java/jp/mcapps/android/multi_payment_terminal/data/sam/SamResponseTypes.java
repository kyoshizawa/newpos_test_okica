package jp.mcapps.android.multi_payment_terminal.data.sam;

import java.util.Arrays;

import jp.mcapps.android.multi_payment_terminal.data.Bytes;
import jp.mcapps.android.multi_payment_terminal.util.Crypto;
import timber.log.Timber;

@SuppressWarnings("all")
public class SamResponseTypes {
    public interface ISamResponseType {
        void parse(byte[] bytes) throws IllegalArgumentException, SamExceptions.RWStatusException;
    }

    // Set RWSAM Mode コマンド
    public static class SetRWSAMMode implements ISamResponseType {
        private byte[] dispatcher;  // 対 FeliCa ドライバ交換パケット
        public byte[] getDispatcher() { return dispatcher; }

        private byte[] reserved;
        public byte[] getReserved() { return reserved; }

        private byte[] responseCode;
        public byte[] getResponseCode() { return responseCode; }

        private byte[] subResponseCode;
        public byte[] getSubResponseCode() { return subResponseCode; }

        private byte[] result;  // 00h : 成功
        public byte[] getResult() { return result; }

        public void parse(byte[] rawData) throws IllegalArgumentException {
            int offset = 0;
            int size = 1;

            dispatcher = Arrays.copyOfRange(rawData, offset, offset + size);
            offset += size;

            size = 2;
            reserved = Arrays.copyOfRange(rawData, offset, offset + size);
            offset += size;

            size = 1;
            responseCode = Arrays.copyOfRange(rawData, offset, offset + size);
            offset += size;

            size = 1;
            subResponseCode = Arrays.copyOfRange(rawData, offset, offset + size);
            offset += size;

            size = 1;
            result = Arrays.copyOfRange(rawData, offset, offset + size);
        }
    }

    // Get RWSAM Mode コマンド
    public static class GetRWSAMMode implements ISamResponseType {
        private byte[] dispatcher;  // 対 FeliCa ドライバ交換パケット
        public byte[] getDispatcher() { return dispatcher; }

        private byte[] reserved;
        public byte[] getReserved() { return reserved; }

        private byte[] responseCode;
        public byte[] getResponseCode() { return responseCode; }

        private byte[] subResponseCode;
        public byte[] getSubResponseCode() { return subResponseCode; }

        private byte[] rwSamState;  // 00h : 成功
        public byte[] getRwSamState() { return rwSamState; }

        public void parse(byte[] rawData) throws IllegalArgumentException {
            int offset = 0;
            int size = 1;

            dispatcher = Arrays.copyOfRange(rawData, offset, offset + size);
            offset += size;

            size = 2;
            reserved = Arrays.copyOfRange(rawData, offset, offset + size);
            offset += size;

            size = 1;
            responseCode = Arrays.copyOfRange(rawData, offset, offset + size);
            offset += size;

            size = 1;
            subResponseCode = Arrays.copyOfRange(rawData, offset, offset + size);
            offset += size;

            size = 1;
            rwSamState = Arrays.copyOfRange(rawData, offset, offset + size);
        }
    }

    // Attention コマンド
    public static class Attention implements ISamResponseType {
        private byte[] dispatcher;  // 対 FeliCa ドライバ交換パケット
        public byte[] getDispatcher() { return dispatcher; }

        private byte[] reserved1;
        public byte[] getReserved1() { return reserved1; }

        private byte[] responseCode;
        public byte[] getResponseCode() { return responseCode; }

        private byte[] serialNo;
        public byte[] getSerialNo() { return serialNo; }

        private byte[] reserved2;
        public byte[] getReserved2() { return reserved2; }

        private byte[] mutualAuthenticationKeyVersion;
        private byte[] getMutualAuthenticationKeyVersion() { return mutualAuthenticationKeyVersion; }

        public void parse(byte[] rawData) throws IllegalArgumentException {
            int offset = 0;

            int size = 1;
            dispatcher = Arrays.copyOfRange(rawData, offset, offset + size);
            offset += size;

            size = 2;
            reserved1 = Arrays.copyOfRange(rawData, offset, offset + size);
            offset += size;

            size = 1;
            responseCode = Arrays.copyOfRange(rawData, offset, offset + size);
            offset += size;

            size = 8;
            serialNo = Arrays.copyOfRange(rawData, offset, offset + size);
            offset += size;

            size = 8;
            reserved2 = Arrays.copyOfRange(rawData, offset, offset + size);
            offset += size;

            size = 2;
            mutualAuthenticationKeyVersion = Arrays.copyOfRange(rawData, offset, offset + size);
        }
    }

    // Authentication1 コマンド（パケットデータタイプ 1、暗号化種別：3-key Triple DES, 暗号なし（相互認証タイプ 2））
    public static class Authentication1 implements ISamResponseType {
        private final byte[] key;
        private final byte[] iv;

        public Authentication1(byte[] key, byte[] iv) {
            this.key = key;
            this.iv = iv;
        }

        private byte[] dispatcher;  // 対 FeliCa ドライバ交換パケット
        public byte[] getDispatcher() { return dispatcher; }

        private byte[] reserved;
        public byte[] getReserved1() { return reserved; }

        private byte[] responseCode;
        public byte[] getResponseCode() { return responseCode; }

        private byte[] subResponseCode;
        public byte[] getSubResponseCode() { return responseCode; }

        private byte[] serialNo;
        public byte[] getSerialNo() { return serialNo; }

        private byte[] m1r;
        public byte[] getM1r() { return m1r; }

        private byte[] rar;
        public byte[] getRar() { return rar; }

        private byte[] rbr;
        public byte[] getRbr() { return rbr; }

        private byte[] KYtr;
        public byte[] getKYtr() { return KYtr; }

        private byte[] KYmac;
        public byte[] getKYmac() { return KYmac; }

        public void parse(byte[] rawData) throws IllegalArgumentException {
            int offset = 0;

            int size = 1;
            dispatcher = Arrays.copyOfRange(rawData, offset, offset + size);
            offset += size;

            size = 2;
            reserved = Arrays.copyOfRange(rawData, offset, offset + size);
            offset += size;

            size = 1;
            responseCode = Arrays.copyOfRange(rawData, offset, offset + size);
            offset += size;

            size = 1;
            subResponseCode = Arrays.copyOfRange(rawData, offset, offset + size);
            offset += size;

            size = 56;
            m1r = Arrays.copyOfRange(rawData, offset, offset + size);

            final byte[] decrypted = Crypto.TripleDES.decrypt(m1r, key, iv);

            rbr = Arrays.copyOfRange(decrypted, 0, 48);
            rar = Arrays.copyOfRange(decrypted, rbr.length, decrypted.length);

            KYmac = Arrays.copyOfRange(rbr, 0, 24);
            KYtr = Arrays.copyOfRange(rbr, KYmac.length, rbr.length);
        }
    }

    public static class Authentication2 implements ISamResponseType {
        private byte[] dispatcher;  // 対 FeliCa ドライバ交換パケット
        public byte[] getDispatcher() { return dispatcher; }

        private byte[] reserved;
        public byte[] getReserved() { return reserved; }

        private byte[] responseCode;
        public byte[] getResponseCode() { return responseCode; }

        private byte[] subResponseCode;
        public byte[] getSubResponseCode() { return subResponseCode; }

        private byte[] result;
        public byte[] getResult() { return result; }

        public void parse(byte[] rawData) throws IllegalArgumentException {
            int offset = 0;

            int size = 1;
            dispatcher = Arrays.copyOfRange(rawData, offset, offset + size);
            offset += size;

            size = 2;
            reserved = Arrays.copyOfRange(rawData, offset, offset + size);
            offset += size;

            size = 1;
            responseCode = Arrays.copyOfRange(rawData, offset, offset + size);
            offset += size;

            size = 1;
            subResponseCode = Arrays.copyOfRange(rawData, offset, offset + size);
            offset += size;

            size = 1;
            result = Arrays.copyOfRange(rawData, offset, offset + size);
        }
    }

    public static class GetLastError implements ISamResponseType {
        private byte[] dispatcher;  // 対 FeliCa ドライバ交換パケット
        public byte[] getDispatcher() { return dispatcher; }

        private byte[] reserved;
        public byte[] getReserved() { return reserved; }

        private byte[] responseCode;
        public byte[] getResponseCode() { return responseCode; }

        private byte[] errorCode;
        public byte[] getErrorCode() { return errorCode; }
        public int getErrorCodeAsInt() {
            // リトルエンディアン
            return ( 0xFF & errorCode[0] ) + ( (0xFF & errorCode[1] ) << 8);
        }
        public String getErrorCodeAsHexString() {
            return String.format("%02X%02X", errorCode[1], errorCode[0]);
        }

        public void parse(byte[] rawData) throws IllegalArgumentException {
            int offset = 0;

            int size = 1;
            dispatcher = Arrays.copyOfRange(rawData, offset, offset + size);
            offset += size;

            size = 2;
            reserved = Arrays.copyOfRange(rawData, offset, offset + size);
            offset += size;

            size = 1;
            responseCode = Arrays.copyOfRange(rawData, offset, offset + size);
            offset += size;

            size = 2;
            errorCode = Arrays.copyOfRange(rawData, offset, offset + size);
        }
    }

    public static class SubResponse implements ISamResponseType {
        private byte[] dispatcher;  // 対 FeliCa ドライバ交換パケット
        public byte[] getDispatcher() { return dispatcher; }

        private byte[] timeout;
        public byte[] getTimeout() { return timeout; }

        // ビッグエンディアン x0.1[ms] 少数は切り捨てる
        public int getTimeoutAsInt() { return (int) Math.floor( ( ( 0xFF & timeout[0] ) << 8 ) + (0xFF & timeout[1]) * 0.1 ); }

        private byte[] cardCommandPacket;
        public byte[] getCardCommandPacket() { return cardCommandPacket; }

        public void parse(byte[] rawData) throws IllegalArgumentException {
            int offset = 0;

            int size = 1;
            dispatcher = Arrays.copyOfRange(rawData, offset, offset + size);
            offset += size;

            size = 2;
            timeout = Arrays.copyOfRange(rawData, offset, offset + size);
            offset += size;

            size = rawData.length - offset;
            cardCommandPacket = Arrays.copyOfRange(rawData, offset, offset + size);
        }
    }

    public static class Polling implements ISamResponseType {
        private final byte[] KYtr;
        private final byte[] KYmac;
        private final byte[] iv;

        public Polling(byte[] KYtr, byte[] KYmac, byte[] iv) {
            this.KYtr = KYtr;
            this.KYmac = KYmac;
            this.iv = iv;
        }

        private byte[] dispatcher;  // 対 FeliCa ドライバ交換パケット
        public byte[] getDispatcher() { return dispatcher; }

        private byte[] reserved;
        public byte[] getReserved() { return reserved; }

        private byte[] responseCode;
        public byte[] getResponseCode() { return responseCode; }

        private byte[] IDtr;
        public byte[] getIDtr() { return IDtr; }

        private byte[] receiveNum;
        public byte[] getReceiveNum() { return receiveNum; }

        private IDmPMm[] IDmPMm;
        public IDmPMm[] getIDmPMm() { return IDmPMm; }

        public static class IDmPMm {
            private byte[] IDm;
            public byte[] getIDm() { return IDm; }

            private byte[] PMm;
            public byte[] getPMm() { return PMm; }
        }

        public int getSnr() {
            return (0xFF00 & IDtr[1] << 8) + (0x00FF & IDtr[0]);
        }

        public void parse(byte[] rawData) throws IllegalArgumentException {
            final byte[] decData = decryptData(rawData, KYtr, KYmac, iv);

            int offset = 0;

            int size = 1;
            dispatcher = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            size = 2;
            reserved = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            size = 1;
            responseCode = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            size = 8;
            IDtr = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            size = 1;
            receiveNum = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            int n = (int) receiveNum[0];
            IDmPMm = new IDmPMm[n];

            for (int i = 0; i < n; i++) {
                IDmPMm[i] = new IDmPMm();
            }

            for (int i = 0; i < n; i++) {
                size = 8;
                IDmPMm[i].IDm = Arrays.copyOfRange(decData, offset, offset + size);
                offset += size;

                size = 8;
                IDmPMm[i].PMm = Arrays.copyOfRange(decData, offset, offset + size);
                offset += size;
            }
        }
    }

    public static class RequestService implements ISamResponseType {
        private final byte[] KYtr;
        private final byte[] KYmac;
        private final byte[] iv;

        public RequestService(byte[] KYtr, byte[] KYmac, byte[] iv) {
            this.KYtr = KYtr;
            this.iv = iv;
            this.KYmac = KYmac;
        }

        private byte[] dispatcher;  // 対 FeliCa ドライバ交換パケット
        public byte[] getDispatcher() { return dispatcher; }

        private byte[] reserved;
        public byte[] getReserved() { return reserved; }

        private byte[] responseCode;
        public byte[] getResponseCode() { return responseCode; }

        private byte[] IDtr;
        public byte[] getIDtr() { return IDtr; }

        private byte[] areaServiceNum;
        public byte[] getAreaServiceNum() { return areaServiceNum; }

        private byte[] areaServiceKeyVersionList;
        public byte[] getAreaServiceKeyVersionList() { return areaServiceKeyVersionList; }

        public int getSnr() {
            return (0xFF00 & IDtr[1] << 8) + (0x00FF & IDtr[0]);
        }

        public void parse(byte[] rawData) throws IllegalArgumentException {
            final byte[] decData = decryptData(rawData, KYtr, KYmac, iv);

            int offset = 0;

            int size = 1;
            dispatcher = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            size = 2;
            reserved = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            size = 1;
            responseCode = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            size = 8;
            IDtr = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            size = 1;
            areaServiceNum = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            size = 2;
            areaServiceKeyVersionList = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;
        }
    }

    public static class MutualAuthenticationRWSAM implements ISamResponseType {
        private final byte[] KYtr;
        private final byte[] KYmac;
        private final byte[] iv;

        public MutualAuthenticationRWSAM(byte[] KYtr, byte[] KYmac, byte[] iv) {
            this.KYtr = KYtr;
            this.iv = iv;
            this.KYmac = KYmac;
        }

        private byte[] dispatcher;  // 対 FeliCa ドライバ交換パケット
        public byte[] getDispatcher() { return dispatcher; }

        private byte[] reserved;
        public byte[] getReserved() { return reserved; }

        private byte[] responseCode;
        public byte[] getResponseCode() { return responseCode; }

        private byte[] IDtr;
        public byte[] getIDtr() { return IDtr; }

        private byte[] result;
        public byte[] getResult() { return result; }

        private byte[] IDi;
        public byte[] getIDi() { return IDi; }

        private byte[] PMi;
        public byte[] getPMi() { return PMi; }

        private byte[] IDt;
        public byte[] getIDt() { return IDt; }

        public int getSnr() {
            return (0xFF00 & IDtr[1] << 8) + (0x00FF & IDtr[0]);
        }

        public void parse(byte[] rawData) throws IllegalArgumentException {
            final byte[] decData = decryptData(rawData, KYtr, KYmac, iv);

            int offset = 0;

            int size = 1;
            dispatcher = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            size = 2;
            reserved = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            size = 1;
            responseCode = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            size = 8;
            IDtr = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            size = 1;
            result = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            size = 8;
            IDi = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            size = 8;
            PMi = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            size = 2;
            IDt = Arrays.copyOfRange(decData, offset, offset + size);
        }
    }

    public static class ReadBlock implements ISamResponseType {
        private final byte[] KYtr;
        private final byte[] KYmac;
        private final byte[] iv;

        public ReadBlock(byte[] KYtr, byte[] KYmac, byte[] iv) {
            this.KYtr = KYtr;
            this.iv = iv;
            this.KYmac = KYmac;
        }

        private byte[] dispatcher;  // 対 FeliCa ドライバ交換パケット
        public byte[] getDispatcher() { return dispatcher; }

        private byte[] reserved;
        public byte[] getReserved() { return reserved; }

        private byte[] responseCode;
        public byte[] getResponseCode() { return responseCode; }

        private byte[] IDtr;
        public byte[] getIDtr() { return IDtr; }

        private byte[] statusFlg1;
        public byte[] getStatusFlg1() { return statusFlg1; }

        private byte[] statusFlg2;
        public byte[] getStatusFlg2() { return statusFlg2; }

        private byte[] blockSize;
        public byte[] getBlockSize() { return blockSize; }

        private byte[][] blockData;
        public byte[][] getBlockData() { return blockData; }

        public int getSnr() {
            return (0xFF00 & IDtr[1] << 8) + (0x00FF & IDtr[0]);
        }

        public void parse(byte[] rawData) throws IllegalArgumentException, SamExceptions.RWStatusException {
            final byte[] decData = decryptData(rawData, KYtr, KYmac, iv);

            int offset = 0;

            int size = 1;
            dispatcher = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            size = 2;
            reserved = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            size = 1;
            responseCode = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            size = 8;
            IDtr = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            size = 1;
            statusFlg1 = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            size = 1;
            statusFlg2 = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            if (statusFlg1[0] != 0x00) {
                throw new SamExceptions.RWStatusException(statusFlg1[0], statusFlg2[2]);
            }

            size = 1;
            blockSize = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            byte[][] bd = new byte[blockSize[0]][16];

            for (int i = 0; i < blockSize[0]; i++) {
                for (int j = 0; j < 16; j++) {
                    bd[i][j] = decData[offset];
                    offset += 1;
                }
            }

            blockData = bd;
        }
    }

    public static class WriteBlock implements ISamResponseType {
        private final byte[] KYtr;
        private final byte[] KYmac;
        private final byte[] iv;

        public WriteBlock(byte[] KYtr, byte[] KYmac, byte[] iv) {
            this.KYtr = KYtr;
            this.iv = iv;
            this.KYmac = KYmac;
        }

        private byte[] dispatcher;  // 対 FeliCa ドライバ交換パケット
        public byte[] getDispatcher() { return dispatcher; }

        private byte[] reserved;
        public byte[] getReserved() { return reserved; }

        private byte[] responseCode;
        public byte[] getResponseCode() { return responseCode; }

        private byte[] IDtr;
        public byte[] getIDtr() { return IDtr; }

        private byte[] statusFlg1;
        public byte[] getStatusFlg1() { return statusFlg1; }

        private byte[] statusFlg2;
        public byte[] getStatusFlg2() { return statusFlg2; }

        public int getSnr() {
            return (0xFF00 & IDtr[1] << 8) + (0x00FF & IDtr[0]);
        }

        public void parse(byte[] rawData) throws IllegalArgumentException, SamExceptions.RWStatusException {
            final byte[] decData = decryptData(rawData, KYtr, KYmac, iv);

            int offset = 0;

            int size = 1;
            dispatcher = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            size = 2;
            reserved = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            size = 1;
            responseCode = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            size = 8;
            IDtr = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            size = 1;
            statusFlg1 = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            size = 1;
            statusFlg2 = Arrays.copyOfRange(decData, offset, offset + size);

            if (statusFlg1[0] != 0x00) {
                throw new SamExceptions.RWStatusException(statusFlg1[0], statusFlg2[2]);
            }
        }
    }

    public static class GetRWSAMKeyVersion implements ISamResponseType {
        private final byte[] KYtr;
        private final byte[] KYmac;
        private final byte[] iv;

        public GetRWSAMKeyVersion(byte[] KYtr, byte[] KYmac, byte[] iv) {
            this.KYtr = KYtr;
            this.iv = iv;
            this.KYmac = KYmac;
        }

        private byte[] dispatcher;  // 対 FeliCa ドライバ交換パケット
        public byte[] getDispatcher() { return dispatcher; }

        private byte[] reserved;
        public byte[] getReserved() { return reserved; }

        private byte[] responseCode;
        public byte[] getResponseCode() { return responseCode; }

        private byte[] IDtr;
        public byte[] getIDtr() { return IDtr; }

        private byte[] subResponseCode;
        public byte[] getSubResponseCode() { return subResponseCode; }

        private byte[] result;  // 00h : 成功
        public byte[] getResult() { return result; }

        private byte[] keyVersion;
        public byte[] getKeyVersion() { return keyVersion; }

        public int getSnr() {
            return (0xFF00 & IDtr[1] << 8) + (0x00FF & IDtr[0]);
        }

        public void parse(byte[] rawData) throws IllegalArgumentException, SamExceptions.RWStatusException {
            final byte[] decData = decryptData(rawData, KYtr, KYmac, iv);

            int offset = 0;

            int size = 1;
            dispatcher = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            size = 2;
            reserved = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            size = 1;
            responseCode = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            size = 8;
            IDtr = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            size = 1;
            subResponseCode = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            size = 1;
            result = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            size = 2;
            keyVersion = Arrays.copyOfRange(decData, offset, offset + size);
        }
    }

    public static class GenerateRWSAMPackage implements ISamResponseType {
        private final byte[] KYtr;
        private final byte[] KYmac;
        private final byte[] iv;

        public GenerateRWSAMPackage(byte[] KYtr, byte[] KYmac, byte[] iv) {
            this.KYtr = KYtr;
            this.iv = iv;
            this.KYmac = KYmac;
        }

        private byte[] dispatcher;  // 対 FeliCa ドライバ交換パケット
        public byte[] getDispatcher() { return dispatcher; }

        private byte[] reserved;
        public byte[] getReserved() { return reserved; }

        private byte[] responseCode;
        public byte[] getResponseCode() { return responseCode; }

        private byte[] IDtr;
        public byte[] getIDtr() { return IDtr; }

        private byte[] subResponseCode;
        public byte[] getSubResponseCode() { return subResponseCode; }

        private byte[] result;  // 00h : 成功
        public byte[] getResult() { return result; }

        private byte[] packageLength;
        public byte[] getPackageLength() { return packageLength; }

        private byte[] _package;
        public byte[] getPackage() { return _package; }

        public int getSnr() {
            return (0xFF00 & IDtr[1] << 8) + (0x00FF & IDtr[0]);
        }

        public void parse(byte[] rawData) throws IllegalArgumentException, SamExceptions.RWStatusException {
            final byte[] decData = decryptData(rawData, KYtr, KYmac, iv);

            int offset = 0;

            int size = 1;
            dispatcher = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            size = 2;
            reserved = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            size = 1;
            responseCode = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            size = 8;
            IDtr = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            size = 1;
            subResponseCode = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            size = 1;
            result = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            size = 1;
            packageLength = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            size = packageLength[0];
            _package = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;
        }
    }

    public static class ChangeRWSAMKey implements ISamResponseType {
        private final byte[] KYtr;
        private final byte[] KYmac;
        private final byte[] iv;

        public ChangeRWSAMKey(byte[] KYtr, byte[] KYmac, byte[] iv) {
            this.KYtr = KYtr;
            this.iv = iv;
            this.KYmac = KYmac;
        }

        private byte[] dispatcher;  // 対 FeliCa ドライバ交換パケット
        public byte[] getDispatcher() { return dispatcher; }

        private byte[] reserved;
        public byte[] getReserved() { return reserved; }

        private byte[] responseCode;
        public byte[] getResponseCode() { return responseCode; }

        private byte[] IDtr;
        public byte[] getIDtr() { return IDtr; }

        private byte[] subResponseCode;
        public byte[] getSubResponseCode() { return subResponseCode; }

        private byte[] result;  // 00h : 成功
        public byte[] getResult() { return result; }

        public int getSnr() {
            return (0xFF00 & IDtr[1] << 8) + (0x00FF & IDtr[0]);
        }

        public void parse(byte[] rawData) throws IllegalArgumentException, SamExceptions.RWStatusException {
            final byte[] decData = decryptData(rawData, KYtr, KYmac, iv);

            int offset = 0;

            int size = 1;
            dispatcher = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            size = 2;
            reserved = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            size = 1;
            responseCode = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            size = 8;
            IDtr = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            size = 1;
            subResponseCode = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            size = 1;
            result = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;
        }
    }

    public static class RegisterFeliCaKey implements ISamResponseType {
        private final byte[] KYtr;
        private final byte[] KYmac;
        private final byte[] iv;

        public RegisterFeliCaKey(byte[] KYtr, byte[] KYmac, byte[] iv) {
            this.KYtr = KYtr;
            this.iv = iv;
            this.KYmac = KYmac;
        }

        private byte[] dispatcher;  // 対 FeliCa ドライバ交換パケット
        public byte[] getDispatcher() { return dispatcher; }

        private byte[] reserved;
        public byte[] getReserved() { return reserved; }

        private byte[] responseCode;
        public byte[] getResponseCode() { return responseCode; }

        private byte[] IDtr;
        public byte[] getIDtr() { return IDtr; }

        private byte[] subResponseCode;
        public byte[] getSubResponseCode() { return subResponseCode; }

        private byte[] result;  // 00h : 成功
        public byte[] getResult() { return result; }

        public int getSnr() {
            return (0xFF00 & IDtr[1] << 8) + (0x00FF & IDtr[0]);
        }

        public void parse(byte[] rawData) throws IllegalArgumentException, SamExceptions.RWStatusException {
            final byte[] decData = decryptData(rawData, KYtr, KYmac, iv);

            int offset = 0;

            int size = 1;
            dispatcher = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            size = 2;
            reserved = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            size = 1;
            responseCode = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            size = 8;
            IDtr = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            size = 1;
            subResponseCode = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            size = 1;
            result = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;
        }
    }

    public static class ClearRWSAMParameter implements ISamResponseType {
        private final byte[] KYtr;
        private final byte[] KYmac;
        private final byte[] iv;

        public ClearRWSAMParameter(byte[] KYtr, byte[] KYmac, byte[] iv) {
            this.KYtr = KYtr;
            this.iv = iv;
            this.KYmac = KYmac;
        }

        private byte[] dispatcher;  // 対 FeliCa ドライバ交換パケット
        public byte[] getDispatcher() { return dispatcher; }

        private byte[] reserved;
        public byte[] getReserved() { return reserved; }

        private byte[] responseCode;
        public byte[] getResponseCode() { return responseCode; }

        private byte[] IDtr;
        public byte[] getIDtr() { return IDtr; }

        private byte[] subResponseCode;
        public byte[] getSubResponseCode() { return subResponseCode; }

        private byte[] result;  // 00h : 成功
        public byte[] getResult() { return result; }

        public int getSnr() {
            return (0xFF00 & IDtr[1] << 8) + (0x00FF & IDtr[0]);
        }

        public void parse(byte[] rawData) throws IllegalArgumentException, SamExceptions.RWStatusException {
            final byte[] decData = decryptData(rawData, KYtr, KYmac, iv);

            int offset = 0;

            int size = 1;
            dispatcher = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            size = 2;
            reserved = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            size = 1;
            responseCode = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            size = 8;
            IDtr = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            size = 1;
            subResponseCode = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            size = 1;
            result = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;
        }
    }

    public static class SetISO7816Mode implements ISamResponseType {
        private final byte[] KYtr;
        private final byte[] KYmac;
        private final byte[] iv;

        public SetISO7816Mode(byte[] KYtr, byte[] KYmac, byte[] iv) {
            this.KYtr = KYtr;
            this.iv = iv;
            this.KYmac = KYmac;
        }

        private byte[] dispatcher;  // 対 FeliCa ドライバ交換パケット
        public byte[] getDispatcher() { return dispatcher; }

        private byte[] reserved;
        public byte[] getReserved() { return reserved; }

        private byte[] responseCode;
        public byte[] getResponseCode() { return responseCode; }

        private byte[] IDtr;
        public byte[] getIDtr() { return IDtr; }

        private byte[] subResponseCode;
        public byte[] getSubResponseCode() { return subResponseCode; }

        private byte[] result;  // 00h : 成功
        public byte[] getResult() { return result; }

        public int getSnr() {
            return (0xFF00 & IDtr[1] << 8) + (0x00FF & IDtr[0]);
        }

        public void parse(byte[] rawData) throws IllegalArgumentException, SamExceptions.RWStatusException {
            final byte[] decData = decryptData(rawData, KYtr, KYmac, iv);

            int offset = 0;

            int size = 1;
            dispatcher = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            size = 2;
            reserved = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            size = 1;
            responseCode = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            size = 8;
            IDtr = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            size = 1;
            subResponseCode = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            size = 1;
            result = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;
        }
    }

    public static class ChangeCommunicationMode implements ISamResponseType {
        private final byte[] KYtr;
        private final byte[] KYmac;
        private final byte[] iv;

        public ChangeCommunicationMode(byte[] KYtr, byte[] KYmac, byte[] iv) {
            this.KYtr = KYtr;
            this.iv = iv;
            this.KYmac = KYmac;
        }

        private byte[] dispatcher;  // 対 FeliCa ドライバ交換パケット
        public byte[] getDispatcher() { return dispatcher; }

        private byte[] reserved;
        public byte[] getReserved() { return reserved; }

        private byte[] responseCode;
        public byte[] getResponseCode() { return responseCode; }

        private byte[] IDtr;
        public byte[] getIDtr() { return IDtr; }

        private byte[] result;  // 00h : 成功
        public byte[] getResult() { return result; }

        public int getSnr() {
            return (0xFF00 & IDtr[1] << 8) + (0x00FF & IDtr[0]);
        }

        public void parse(byte[] rawData) throws IllegalArgumentException, SamExceptions.RWStatusException {
            final byte[] decData = decryptData(rawData, KYtr, KYmac, iv);

            int offset = 0;

            int size = 1;
            dispatcher = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            size = 2;
            reserved = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            size = 1;
            responseCode = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            size = 8;
            IDtr = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;

            size = 1;
            result = Arrays.copyOfRange(decData, offset, offset + size);
            offset += size;
        }
    }

    private static byte[] decryptData(byte[] encData, byte[] KYtr, byte[] KYmac, byte[] IV) {
        final byte[] headData = Arrays.copyOfRange(encData, 0, 4);
        final byte[] footData = Arrays.copyOfRange(encData, 4, encData.length);

        final Bytes decData = new Bytes();

        byte[] currentBlock = Arrays.copyOfRange(footData, 0,8);
        byte[] dec = Crypto.TripleDES.decrypt(currentBlock, KYtr, IV);

        decData.add(dec);
        for (int cnt = 1; cnt < footData.length/8; cnt++) {
            final byte[] nextBlock = Arrays.copyOfRange(footData, cnt*8, cnt*8+8);
            dec = Crypto.TripleDES.decrypt(nextBlock, KYtr, IV);

            final byte[] xor = new byte[] {0, 0, 0 ,0, 0, 0, 0, 0};

            for (int i = 0; i < 8; i++) {
                xor[i] = (byte)(currentBlock[i] ^ dec[i]);
            }

            currentBlock = nextBlock;
            decData.add(xor);
        }

        byte[] mac = Crypto.TripleDES.encrypt(new Bytes(headData).add(0x00,0x00,0x00,0x00).toArray(), KYmac, IV);

        for (int cnt = 0; cnt < (decData.size()-8)/8; cnt++) {
            final byte[] nextBlock = decData.copyOfRange(cnt*8, cnt*8+8);
            final byte[] xor = new byte[] {0, 0, 0 ,0, 0, 0, 0, 0};

            for (int i = 0; i < 8; i++) {
                xor[i] = (byte)(mac[i] ^ nextBlock[i]);
            }

            mac = Crypto.TripleDES.encrypt(xor, KYmac, IV);
        }

        byte[] origMac = decData.copyOfRange(decData.size()-8, decData.size());

        boolean verify = true;

        for (int i = 0; i < mac.length; i++) {
            if (mac[i] != origMac[i]) {
                verify = false;
                break;
            }
        }

        if (!verify) {
            throw new IllegalArgumentException("Mac不一致");
        }

        Timber.d("SAM平文レスポンス: %02X%02X%02X%02X%s", headData[0], headData[1], headData[2], headData[3], decData);
        return new Bytes(headData).add(decData.copyOfRange(0, decData.size()-8)).toArray();
    }
}
