package jp.mcapps.android.multi_payment_terminal.devices;

import java.util.Arrays;

import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.data.okica.AccessKeyMaster;
import jp.mcapps.android.multi_payment_terminal.data.sam.Constants;
import jp.mcapps.android.multi_payment_terminal.data.sam.PackageData.*;
import jp.mcapps.android.multi_payment_terminal.data.sam.SamResponse;
import jp.mcapps.android.multi_payment_terminal.data.sam.SamResponseTypes.*;
import jp.mcapps.android.multi_payment_terminal.ui.emoney.okica.OkicaDateUtils;
import timber.log.Timber;

public class SamManagementUtils {
    public static boolean setup(AccessKeyMaster accessKeyMaster) {
        final SamRW.OpenResult mcKeyResult = SamRW.open(Constants.MC_ADMIN_KEY, SamRW.States.Admin);

        final int okicaSystemCode = ( ( 0xFF & Constants.SYSTEM_CODE[0] ) << 8 ) + ( 0xFF & Constants.SYSTEM_CODE[1] );

        AccessKeyMaster.AccessKeyDetail detail = null;
        for (final AccessKeyMaster.AccessKeyData data : accessKeyMaster.getData().getAccessKeyDataTable()) {
            if (data.getSystemCode() == okicaSystemCode) {
                detail = data.getDetail();
                break;
            }
        }

        if (detail == null) {
            Timber.e("アクセスキー世代判定異常");
            return false;
        }

        if (mcKeyResult == SamRW.OpenResult.SAM_NOT_FOUND) {
            Timber.e("SAMカード未挿入");
            return false;
        }

        else if (mcKeyResult == SamRW.OpenResult.AUTHENTICATION_FAILURE) {
            Timber.e("MCキーAdmin相互認証失敗");
            final SamRW.OpenResult initialKeyResult = SamRW.open(Constants.INITIAL_ADMIN_KEY, SamRW.States.Admin);

            if (initialKeyResult == SamRW.OpenResult.SAM_NOT_FOUND) {
                Timber.e("SAMカード未挿入");
                return false;
            }
            else if (initialKeyResult == SamRW.OpenResult.AUTHENTICATION_FAILURE) {
                Timber.e("初期キーAdmin相互認証失敗");
                return false;
            }

            if (!changeAdminKey(Constants.INITIAL_ADMIN_KEY, Constants.MC_ADMIN_KEY, Constants.MC_KEY_VERSION)) {
                Timber.e("Adminキー更新失敗");
                return false;
            }
        }

        SamResponse<ChangeCommunicationMode> r = SamRW.changeCommunicationModeCommand(0x02);
        if (r.hasError()) {
            Timber.e("SAM伝送速度変更失敗 38400bps");
        } else {
            Timber.i("SAM伝送速度変更成功 38400bps");
        }

        // openしなおさないと伝送速度が反映されないためもう一度行う
        SamRW.open(Constants.MC_ADMIN_KEY, SamRW.States.Admin);

        SamResponse<GetRWSAMKeyVersion> nKyeVerResp = SamRW.getRWSAMKeyVersionCommand(0x02);
        if (nKyeVerResp.hasError()) {
            Timber.e("Normalキーバージョン取得失敗");
            SamRW.getLastError();
            return false;
        }

        Timber.i("Normalキーバージョン: %02X %02X", nKyeVerResp.getData().getKeyVersion()[0], nKyeVerResp.getData().getKeyVersion()[1]);

        if (!Arrays.equals(nKyeVerResp.getData().getKeyVersion(), Constants.MC_KEY_VERSION)) {
            if (!changeNormalKey(Constants.INITIAL_NORMAL_KEY, Constants.MC_NORMAL_KEY, Constants.MC_KEY_VERSION)) {
                Timber.e("Normalキー更新失敗");
                return false;
            }
        }

        SamResponse<GetRWSAMKeyVersion> uKyeVerResp = SamRW.getRWSAMKeyVersionCommand(0x14);
        if (uKyeVerResp.hasError()) {
            Timber.e("Userキーバージョン取得失敗");
            SamRW.getLastError();
            return false;
        }

        Timber.i("Userキーバージョン: %02X %02X", uKyeVerResp.getData().getKeyVersion()[0], uKyeVerResp.getData().getKeyVersion()[1]);
        if (!Arrays.equals(uKyeVerResp.getData().getKeyVersion(), Constants.MC_KEY_VERSION)) {
            if (!changeUserPackageKey(Constants.INITIAL_USER_PACKAGE_KEY, Constants.MC_USER_PACKAGE_KEY, Constants.MC_KEY_VERSION)) {
                Timber.e("Userキー更新失敗");
                return false;
            }
        }

        if (!registerGSKUSK(detail)) {
            return false;
        }

        final MainApplication app = MainApplication.getInstance();
        app.isInitFeliCaSAM(SamRW.open(Constants.MC_NORMAL_KEY, SamRW.States.Normal) == SamRW.OpenResult.SUCCESS);

        return true;
    }

    public static boolean reset() {
        final SamRW.OpenResult mcKeyResult = SamRW.open(Constants.MC_ADMIN_KEY, SamRW.States.Admin);

        clearGSKUSK();

        SamResponse<GetRWSAMKeyVersion> uKyeVerResp = SamRW.getRWSAMKeyVersionCommand(0x14);
        if (uKyeVerResp.hasError()) {
            Timber.e("Userキーバージョン取得失敗");
            SamRW.getLastError();
            return false;
        }

        Timber.i("Userキーバージョン: %02X %02X", uKyeVerResp.getData().getKeyVersion()[0], uKyeVerResp.getData().getKeyVersion()[1]);
        if (!Arrays.equals(uKyeVerResp.getData().getKeyVersion(), Constants.INITIAL_KEY_VERSION)) {
            if (!changeUserPackageKey(Constants.MC_USER_PACKAGE_KEY, Constants.INITIAL_USER_PACKAGE_KEY, Constants.INITIAL_KEY_VERSION)) {
                Timber.e("Userキー更新失敗");
                return false;
            }
        }

        SamResponse<GetRWSAMKeyVersion> nKyeVerResp = SamRW.getRWSAMKeyVersionCommand(0x02);
        if (nKyeVerResp.hasError()) {
            Timber.e("Normalキーバージョン取得失敗");
            SamRW.getLastError();
            return false;
        }

        Timber.i("Normalキーバージョン: %02X %02X", nKyeVerResp.getData().getKeyVersion()[0], nKyeVerResp.getData().getKeyVersion()[1]);

        if (!Arrays.equals(nKyeVerResp.getData().getKeyVersion(), Constants.INITIAL_KEY_VERSION)) {
            if (!changeNormalKey(Constants.MC_NORMAL_KEY, Constants.INITIAL_NORMAL_KEY, Constants.INITIAL_KEY_VERSION)) {
                Timber.e("Normalキー更新失敗");
                return false;
            }
        }

        SamResponse<GetRWSAMKeyVersion> aKyeVerResp = SamRW.getRWSAMKeyVersionCommand(0x03);
        if (aKyeVerResp.hasError()) {
            Timber.e("Normalキーバージョン取得失敗");
            SamRW.getLastError();
            return false;
        }

        Timber.i("Adminキーバージョン: %02X %02X", aKyeVerResp.getData().getKeyVersion()[0], aKyeVerResp.getData().getKeyVersion()[1]);

        if (!Arrays.equals(aKyeVerResp.getData().getKeyVersion(), Constants.INITIAL_KEY_VERSION)) {
            if (!changeAdminKey(Constants.MC_ADMIN_KEY, Constants.INITIAL_ADMIN_KEY, new byte[] {0x01, 0x00})) {
                Timber.e("Adminキー更新失敗");
                return false;
            }
        }

        SamResponse<ChangeCommunicationMode> r = SamRW.changeCommunicationModeCommand(0x00);
        if (r.hasError()) {
            Timber.e("SAM伝送速度変更失敗 9600bps");
            return false;
        } else {
            Timber.i("SAM伝送速度変更成功 9600bps");
        }

        return true;
    }

    public static boolean changeAdminKey(byte[] oldKey, byte[] newKey, byte[] version) {
        final ChangeAdmin keyData = new ChangeAdmin(oldKey, newKey, version);
        SamResponse<GenerateRWSAMPackage> resp1 = SamRW.generateRWSAMPackageCommand(Constants.INITIAL_USER_PACKAGE_KEY, keyData);

        if (resp1.hasError()) {
            resp1 = SamRW.generateRWSAMPackageCommand(Constants.MC_USER_PACKAGE_KEY, keyData);
            if (resp1.hasError()) {
                Timber.e("パッケージ生成エラー");
                return false;
            }
        }

        final SamResponse<ChangeRWSAMKey> resp2 = SamRW.changeRWSAMKeyCommand(0x03, resp1.getData().getPackage());

        if (resp2.hasError()) {
            Timber.e("Adminキー変更エラー");
            return false;
        }

        return true;
    }

    public static boolean changeNormalKey(byte[] oldKey, byte[] newKey, byte[] version) {
        final ChangeNormal keyData = new ChangeNormal(oldKey, newKey, version);
        SamResponse<GenerateRWSAMPackage> resp1 = SamRW.generateRWSAMPackageCommand(Constants.INITIAL_USER_PACKAGE_KEY, keyData);
        if (resp1.hasError()) {
            resp1 = SamRW.generateRWSAMPackageCommand(Constants.MC_USER_PACKAGE_KEY, keyData);
            if (resp1.hasError()) {
                Timber.e("パッケージ生成エラー");
                return false;
            }
        }

        final SamResponse<ChangeRWSAMKey> resp2 = SamRW.changeRWSAMKeyCommand(0x02, resp1.getData().getPackage());

        if (resp2.hasError()) {
            Timber.e("Normalキー変更エラー");
            return false;
        }

        return true;
    }

    public static boolean changeUserPackageKey(byte[] oldKey, byte[] newKey, byte[] version) {
        final ChangeUserPackage keyData = new ChangeUserPackage(newKey, version);
        SamResponse<GenerateRWSAMPackage> resp1 = SamRW.generateRWSAMPackageCommand(oldKey, keyData);
        if (resp1.hasError()) {
            Timber.e("パッケージ生成エラー");
            return false;
        }

        final SamResponse<ChangeRWSAMKey> resp2 = SamRW.changeRWSAMKeyCommand(0x14, resp1.getData().getPackage());
        if (resp2.hasError()) {
            Timber.e("鍵変更エラー");
            return false;
        }

        return true;
    }

    private static boolean registerGSKUSK(AccessKeyMaster.AccessKeyDetail detail) {
        if (!clearGSKUSK()) {
            return false;
        }

        final RegisterGSK gskPkgData = new RegisterGSK(detail.getGroupAccessKey(), detail.getAreaNum(), detail.getAreaCodeList());

        final SamResponse<GenerateRWSAMPackage> gskGenResp = SamRW.generateRWSAMPackageCommand(Constants.MC_USER_PACKAGE_KEY, gskPkgData);
        if (gskGenResp.hasError()) {
            Timber.e("パッケージ生成エラー");
            return false;
        }

        final SamResponse<RegisterFeliCaKey> gskRegResp = SamRW.registerFeliCaKeyCommand(gskGenResp.getData().getPackage());
        if (gskRegResp.hasError()) {
            Timber.e(gskRegResp.getError());
            return false;
        }

        final RegisterUSK uskPkgData = new RegisterUSK(detail.getServiceAccessKey(), detail.getServiceNum(), detail.getServiceCodeList());

        SamResponse<GenerateRWSAMPackage> uskGenResp = SamRW.generateRWSAMPackageCommand(Constants.MC_USER_PACKAGE_KEY, uskPkgData);
        if (gskGenResp.hasError()) {
            Timber.e("パッケージ生成エラー");
            return false;
        }

        SamResponse<RegisterFeliCaKey> uskRegResp = SamRW.registerFeliCaKeyCommand(uskGenResp.getData().getPackage());
        if (uskRegResp.hasError()) {
            Timber.e("USK鍵登録エラー");
            return false;
        }

        return true;
    }

    public static boolean clearGSKUSK() {
        SamResponse<ClearRWSAMParameter> resp = SamRW.clearRWSAMParameterCommand();

        if (resp.hasError()) {
            Timber.e("鍵削除エラー");
            return false;
        }

        return true;
    }
}
