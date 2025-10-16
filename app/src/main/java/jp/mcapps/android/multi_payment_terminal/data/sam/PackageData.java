package jp.mcapps.android.multi_payment_terminal.data.sam;

public class PackageData {
    public interface IPackageData {
        byte[] getData();
        byte getPackageType();
    }

    public static class ChangeAdmin implements IPackageData {
        public static final int DATA_LENGTH = 64;
        public static final byte PACKAGE_TYPE = 0x03;

        private final byte[] data = new byte[DATA_LENGTH];
        public byte[] getData() {
            return this.data;
        }

        public byte getPackageType() {
            return PACKAGE_TYPE;
        }

        public ChangeAdmin(byte[] oldKey, byte[] newKey, byte[] newKeyVersion) {
            data[0] = PACKAGE_TYPE;
            data[1] = 0x03;  // RW-SAM鍵タイプ
            System.arraycopy(oldKey, 0, data, 2, 24);
            System.arraycopy(newKey, 0, data, 26, 24);
            System.arraycopy(newKeyVersion, 0, data, 50, 2);

            for (int i = 52; i < DATA_LENGTH; i++) {
                data[i] = 0x00;
            }
        }
    }

    public static class ChangeNormal implements IPackageData {
        public static final int DATA_LENGTH = 64;
        public static final byte PACKAGE_TYPE = 0x03;

        private final byte[] data = new byte[DATA_LENGTH];
        public byte[] getData() {
            return this.data;
        }

        @Override
        public byte getPackageType() {
            return PACKAGE_TYPE;
        }

        public ChangeNormal(byte[] oldKey, byte[] newKey, byte[] newKeyVersion) {
            data[0] = PACKAGE_TYPE;
            data[1] = 0x02;  // RW-SAM鍵タイプ
            System.arraycopy(oldKey, 0, data, 2, 24);
            System.arraycopy(newKey, 0, data, 26, 24);
            System.arraycopy(newKeyVersion, 0, data, 50, 2);

            for (int i = 52; i < DATA_LENGTH; i++) {
                data[i] = 0x00;
            }
        }
    }

    public static class ChangeUserPackage implements IPackageData {
        public static final int DATA_LENGTH = 32;
        public static final byte PACKAGE_TYPE = 0x04;

        private final byte[] data = new byte[DATA_LENGTH];
        public byte[] getData() {
            return this.data;
        }

        @Override
        public byte getPackageType() {
            return PACKAGE_TYPE;
        }

        public ChangeUserPackage(byte[] newKey, byte[] newKeyVersion) {
            data[0] = PACKAGE_TYPE;
            data[1] = 0x14;  // RW-SAM鍵タイプ
            System.arraycopy(newKey, 0, data, 2, 16);
            System.arraycopy(newKeyVersion, 0, data, 18, 2);

            for (int i = 20; i < DATA_LENGTH; i++) {
                data[i] = 0x00;
            }
        }
    }

    public static class RegisterGSK implements IPackageData {
        public static final int DATA_LENGTH = 64;
        public static final byte PACKAGE_TYPE = 0x13;

        private final byte[] data = new byte[DATA_LENGTH];
        public byte[] getData() {
            return this.data;
        }

        @Override
        public byte getPackageType() {
            return PACKAGE_TYPE;
        }

        public RegisterGSK(byte[] groupAccessKey, int areaNum, byte[] areaCodeList) {
            data[0] = PACKAGE_TYPE;
            data[1] = 0x02;  // RW-SAM鍵タイプ
            System.arraycopy(Constants.SYSTEM_CODE, 0, data, 2, 2);
            System.arraycopy(Constants.GSK_CODE, 0, data, 4, 2);
            System.arraycopy(Constants.GSK_VERSION, 0, data, 6, 2);
            System.arraycopy(groupAccessKey, 0, data, 8, 8);
            data[16] = (byte) areaNum;
            System.arraycopy(areaCodeList, 0, data, 17, 32);

            for (int i = 49; i < DATA_LENGTH; i++) {
                data[i] = 0x00;
            }
        }
    }

    public static class RegisterUSK implements IPackageData {
        public static final int DATA_LENGTH = 64;
        public static final byte PACKAGE_TYPE = 0x13;

        private final byte[] data = new byte[DATA_LENGTH];
        public byte[] getData() {
            return this.data;
        }

        @Override
        public byte getPackageType() {
            return PACKAGE_TYPE;
        }

        public RegisterUSK(byte[] serviceAccessKey, int serviceNum, byte[] serviceCodeList) {
            data[0] = PACKAGE_TYPE;
            data[1] = 0x02;  // RW-SAM鍵タイプ
            System.arraycopy(Constants.SYSTEM_CODE, 0, data, 2, 2);
            System.arraycopy(Constants.USK_CODE, 0, data, 4, 2);
            System.arraycopy(Constants.USK_VERSION, 0, data, 6, 2);
            System.arraycopy(serviceAccessKey, 0, data, 8, 8);
            data[16] = (byte) serviceNum;
            System.arraycopy(serviceCodeList, 0, data, 17, 32);

            for (int i = 49; i < DATA_LENGTH; i++) {
                data[i] = 0x00;
            }
        }
    }
}
