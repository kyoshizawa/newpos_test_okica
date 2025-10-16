package jp.mcapps.android.multi_payment_terminal.model;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;

import io.grpc.Status;
import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.data.okica.AccessKeyInfo;
import jp.mcapps.android.multi_payment_terminal.data.okica.AccessKeyMaster;
import jp.mcapps.android.multi_payment_terminal.data.okica.ICMaster;
import jp.mcapps.android.multi_payment_terminal.data.okica.ICMasterInfo;
import jp.mcapps.android.multi_payment_terminal.data.okica.OkicaNegaFile;
import jp.mcapps.android.multi_payment_terminal.data.sam.Constants;
import jp.mcapps.android.multi_payment_terminal.database.DBManager;
import jp.mcapps.android.multi_payment_terminal.devices.SamManagementUtils;
import jp.mcapps.android.multi_payment_terminal.util.McUtils;
import jp.mcapps.android.multi_payment_terminal.webapi.grpc.McOkicaCenterApi;
import jp.mcapps.android.multi_payment_terminal.webapi.grpc.McOkicaCenterApiImpl;
import jp.mcapps.android.multi_payment_terminal.webapi.grpc.data.GetAccessToken;
import jp.mcapps.android.multi_payment_terminal.webapi.grpc.data.GetMasterFile;
import jp.mcapps.android.multi_payment_terminal.webapi.grpc.data.GetMasterFileVerion;
import jp.mcapps.android.multi_payment_terminal.webapi.grpc.data.GetNegaList;
import jp.mcapps.android.multi_payment_terminal.webapi.grpc.data.GetNegaVersion;
import jp.mcapps.android.multi_payment_terminal.webapi.grpc.data.TerminalInfo;
import timber.log.Timber;

public class OkicaMasterControl {
    private static final String IC_MASTER_FILE_ID = "11";
    private static final String ACCESS_KEY_FILE_ID_WITH_CHARGE = "46";
    private static final String ACCESS_KEY_FILE_ID_WITHOUT_CHARGE = "47";

    public static final int FORCE_DEACT_NONE = 0;
    public static final int FORCE_DEACT_EXIST_UNSENT = 1;
    public static final int FORCE_DEACT_EXIST_AGGREGATE = 2;
    public static final int FORCE_DEACT_END = 3;

    private final MainApplication _app = MainApplication.getInstance();
    private final McOkicaCenterApi _api = new McOkicaCenterApiImpl();

    public static int force_deactivation_stat = FORCE_DEACT_NONE;
    public static boolean force_okica_off = false;

    public static String getIcMasterFileID() { return IC_MASTER_FILE_ID; }

    public static String getAccessKeyFileID() {
        return (AppPreference.getOkicaTerminalInfo().isCharge) ? ACCESS_KEY_FILE_ID_WITH_CHARGE : ACCESS_KEY_FILE_ID_WITHOUT_CHARGE;
    }

    /**
     *
     * @return OKICA利用可否
     */
    public boolean okicaOpening(ArrayList<String> errors) {
        final McOkicaCenterApi api = new McOkicaCenterApiImpl();

        // マスタデータの保持期限チェック
        okicaCheckMasterTimeLimit();

        // 端末情報取得
        TerminalInfo.Response termInfo = api.getTerminalInfo();
        if (termInfo.result) {
            if (!termInfo.isInstalled) {
                Timber.i("OKICA強制撤去受信");
                if (DBManager.getUriOkicaDao().getUnsentCnt() > 0) {
                    // 未送信売上データが存在する場合は強制撤去できない
                    Timber.e("【OKICA強制撤去】強制撤去の通知がありました。未送信の売上が存在するため、業務終了後に再起動してください。");
                    force_deactivation_stat = FORCE_DEACT_EXIST_UNSENT;
                    errors.add(_app.getString(R.string.error_type_okica_force_deactivation_uri_exist_error));
                } else if (DBManager.getSlipDao().getAggregate().size() > 0) {
                    // 未印刷集計データが存在する場合は強制撤去できない
                    Timber.e("【OKICA強制撤去】強制撤去の通知がありました。未印刷の集計が存在するため、業務終了後に再起動してください。");
                    force_deactivation_stat = FORCE_DEACT_EXIST_AGGREGATE;
                    errors.add(_app.getString(R.string.error_type_okica_force_deactivation_aggregate_exist_error));
                } else {
                    Timber.i("【OKICA強制撤去】OKICA強制撤去が行われました。");
                    force_deactivation_stat = FORCE_DEACT_END;
                    okicaUninstall();
                    // スタックエラーは表示しない
                }
                return false;
            }

            AppPreference.setOkicaTerminalInfo(termInfo);

            // IC運用マスタ、アクセスキー取得
            okicaGetMaster();

            // ネガリスト取得
            okicaGetNega();

            // 売上送信
            String mcTerminalErrCode = new McTerminal().postOkicaPayment();
            if (mcTerminalErrCode != null) {
                Timber.e("okicaOpening OKICA売上情報送信失敗：%s", mcTerminalErrCode);
            }
        }
        else if (termInfo.errorCode.equals(Status.UNAUTHENTICATED.getCode().toString())) {
            // サーバー再起動等の理由が原因でアクセストークンが無効になってしまった場合
            Timber.e("OKICAアクセストークン無効 再取得実行");
            final GetAccessToken.Response tokenResp = api.getAccessToken(
                    jp.mcapps.android.multi_payment_terminal.data.okica.Constants.TERMINAL_INSTALL_ID,
                    AppPreference.getOkicaAuthCode());

            if (tokenResp.result) {
                // トークンの再取得ができたらもう一度実行する
                Timber.i("OKICAアクセストークン再取得成功");
                AppPreference.setOkicaAccessToken(tokenResp.accessToken);
                return okicaOpening(errors);
            } else {
                Timber.e("OKICAアクセストークン再取得失敗 reason: %s", tokenResp.errorCode);

                // 再開局してもどうしようもないエラーが返って来た時は消す
                final boolean ng = tokenResp.errorCode.equals(Status.NOT_FOUND.getCode().toString())
                        || tokenResp.errorCode.equals(Status.PERMISSION_DENIED.getCode().toString());

                if (ng) {
                    okicaUninstall();
                    return false;
                }
            }
        } else {
            Timber.e("OKCIA端末情報取得失敗 reason: %s", termInfo.errorCode);
        }

        return true;
    }

    /**
     * OKICA IC運用マスタ、アクセスキー取得
     * （センターのマスタファイルバージョンと不一致の場合のみ）
     */
    public boolean okicaGetMaster() {
        boolean result = false;
        final Calendar cl = Calendar.getInstance();
        final TimeZone tz = TimeZone.getTimeZone("Asia/Tokyo");

        cl.setTimeZone(tz);

        // マスタファイルバージョン取得
        String[] fileIds = {"0", "0"};
        fileIds[0] = getIcMasterFileID();
        fileIds[1] = getAccessKeyFileID();
        GetMasterFileVerion.Response fileVersion = _api.getMasterFileVersion(fileIds);
        if (fileVersion.result) {
            // IC運用マスタ取得（IC運用マスタ未取得、または、バージョン不一致の場合のみ）
            ICMasterInfo icMasterInfo = AppPreference.getOkicaICMasterInfo();
            if (icMasterInfo == null || icMasterInfo.version != fileVersion.fileVersions[0].getVersion()) {
                if (icMasterInfo == null) {
                    Timber.i("IC運用マスタ取得情報 (新)Ver:%d (現)Ver:Null", fileVersion.fileVersions[0].getVersion());
                } else {
                    Timber.i("IC運用マスタ取得情報 (新)Ver:%d (現)Ver:%d", fileVersion.fileVersions[0].getVersion(), icMasterInfo.version);
                }
                getICMaster();
            } else {
                // バージョン一致の場合は取得日のみ更新
                ICMaster.updateCheckDate();
            }

            icMasterInfo = AppPreference.getOkicaICMasterInfo();
            if (icMasterInfo != null) {
                cl.setTimeInMillis(icMasterInfo.checkDate);
                Timber.i("IC運用マスタ使用情報 Ver:%d Date:%04d/%02d/%02d %02d:%02d:%02d", icMasterInfo.version,
                        cl.get(Calendar.YEAR), cl.get(Calendar.MONTH)+1, cl.get(Calendar.DATE),
                        cl.get(Calendar.HOUR_OF_DAY), cl.get(Calendar.MINUTE), cl.get(Calendar.SECOND));

                // IC運用マスタをファイルから読み込む
                ICMaster master = ICMaster.load();
                if (master != null) {
                    _app.getInstance().setOkicaICMaster(master);
                }
            }

            // アクセスキー取得（アクセスキー未取得、または、バージョン不一致の場合のみ）
            AccessKeyInfo accessKeyInfo = AppPreference.getOkicaAccessKeyInfo();
            if (accessKeyInfo == null || accessKeyInfo.version != fileVersion.fileVersions[1].getVersion()) {
                if (accessKeyInfo == null) {
                    Timber.i("アクセスキー取得情報 (新)Ver:%d (現)Ver:Null", fileVersion.fileVersions[1].getVersion());
                } else {
                    Timber.i("アクセスキー取得情報 (新)Ver:%d (現)Ver:%d", fileVersion.fileVersions[1].getVersion(), accessKeyInfo.version);
                }
                getAccessKey();
            } else {
                // バージョン一致の場合は取得日のみ更新
                AccessKeyMaster.updateCheckDate();
            }

            accessKeyInfo = AppPreference.getOkicaAccessKeyInfo();
            if (accessKeyInfo != null) {
                cl.setTimeInMillis(accessKeyInfo.checkDate);
                Timber.i("アクセスキー使用情報 Ver:%d Date:%04d/%02d/%02d %02d:%02d:%02d", accessKeyInfo.version,
                        cl.get(Calendar.YEAR), cl.get(Calendar.MONTH)+1, cl.get(Calendar.DATE),
                        cl.get(Calendar.HOUR_OF_DAY), cl.get(Calendar.MINUTE), cl.get(Calendar.SECOND));
            }
        }

        return result;
    }

    /**
     * OKICA IC運用マスタ取得
     */
    public boolean getICMaster() {
        boolean result = false;
        final GetMasterFile.Response response = _api.getMasterFile(IC_MASTER_FILE_ID);
        if (response.result) {
            if (ICMaster.save(response.file, response.fileName)) {
                result = true;
            } else {
                Timber.e("IC運用マスタファイル保存エラー");
            }
        } else {
            Timber.e("IC運用マスタファイル取得エラー");
        }
        return result;
    }

    /**
     * OKICA アクセスキー取得
     */
    public boolean getAccessKey() {
        boolean result = false;
        final String fileId = getAccessKeyFileID();
        final GetMasterFile.Response response = _api.getMasterFile(fileId);
        if (response.result) {
            // アクセスキーデータ正常性チェック
            if (AccessKeyMaster.validate(response.file)) {
                final AccessKeyMaster master = new AccessKeyMaster(response.file);

                if (SamManagementUtils.setup(master)) {
                    final int okicaSystemCode = ( ( 0xFF & Constants.SYSTEM_CODE[0] ) << 8 ) + ( 0xFF & Constants.SYSTEM_CODE[1] );

                    AccessKeyMaster.AccessKeyDetail detail = null;
                    for (final AccessKeyMaster.AccessKeyData data : master.getData().getAccessKeyDataTable()) {
                        if (data.getSystemCode() == okicaSystemCode) {
                            detail = data.getDetail();
                            break;
                        }
                    }

                    final AccessKeyInfo info = new AccessKeyInfo();
                    info.version = response.version;
                    info.generation = detail.getGeneration();
                    info.areaNum = detail.getAreaNum();
                    info.areaKeyVersions = McUtils.bytesToHexString(detail.getAreaKeyVersions());
                    info.areaCodeList = McUtils.bytesToHexString(detail.getAreaCodeList());
                    info.serviceNum = detail.getServiceNum();
                    info.serviceKeyVersions = McUtils.bytesToHexString(detail.getServiceKeyVersions());
                    info.serviceCodeList = McUtils.bytesToHexString(detail.getServiceCodeList());
                    info.checkDate = System.currentTimeMillis();

                    int e = detail.getEndDate();

                    // 0xFFFFは無期限Longの最大値を入れる
                    if (detail.getEndDate() == 0xFFFF) {
                        info.endDate = Long.MAX_VALUE;
                    } else {
                        int ey = ( 0b1111_1110_0000_0000 & e ) + 2000;
                        int em = ( 0b0000_0001_1110_0000 & e );
                        int ed = ( 0b0000_0000_0001_1111 & e );

                        final Calendar c = Calendar.getInstance();
                        final TimeZone tz = TimeZone.getTimeZone("Asia/Tokyo");

                        c.setTimeZone(tz);

                        c.set(Calendar.YEAR, ey);
                        c.set(Calendar.MONTH, em);
                        c.set(Calendar.DATE, ed);

                        // 運用日付として扱うため3時間59分59秒追加する
                        c.add(Calendar.HOUR_OF_DAY, +3);
                        c.add(Calendar.MINUTE, +59);
                        c.add(Calendar.SECOND, +59);
                        c.add(Calendar.DATE, +1);

                        info.endDate = c.getTimeInMillis();
                    }

                    final Calendar cl = Calendar.getInstance();
                    final TimeZone tz = TimeZone.getTimeZone("Asia/Tokyo");

                    cl.setTimeZone(tz);

                    AccessKeyInfo accessKeyInfo = AppPreference.getOkicaAccessKeyInfo();
                    if (accessKeyInfo == null || accessKeyInfo.version < info.version) {
                        if (accessKeyInfo == null) {
                            Timber.i("アクセスキー取得情報 (新)Ver:%d (現)Ver:Null", info.version);
                        } else {
                            Timber.i("アクセスキー取得情報 (新)Ver:%d (現)Ver:%d", info.version, accessKeyInfo.version);
                        }
                    }

                    AppPreference.setOkicaAccessKeyInfo(info);
                    accessKeyInfo = AppPreference.getOkicaAccessKeyInfo();
                    if (accessKeyInfo != null) {
                        cl.setTimeInMillis(accessKeyInfo.checkDate);
                        Timber.i("アクセスキー使用情報 Ver:%d Date:%04d/%02d/%02d %02d:%02d:%02d", accessKeyInfo.version,
                                cl.get(Calendar.YEAR), cl.get(Calendar.MONTH)+1, cl.get(Calendar.DATE),
                                cl.get(Calendar.HOUR_OF_DAY), cl.get(Calendar.MINUTE), cl.get(Calendar.SECOND));
                    }

                    result = true;
                } else {
                    Timber.e("SAMカードセットアップ失敗");
                }
            } else {
                Timber.e("アクセスキー保存エラー");
            }
        } else {
            Timber.e("アクセスキーファイル取得エラー");
        }
        return result;
    }

    /**
     * OKICA ネガリスト取得
     */
    public boolean okicaGetNega() {
        boolean result = false;
        final McOkicaCenterApi api = new McOkicaCenterApiImpl();
        // ネガ取得
        GetNegaVersion.Response negaVersion = api.getNegaVersion();
        Timber.d("GetNegaVersion");
        Timber.d("result: " + negaVersion.result);
        if (negaVersion.result) {
            // ネガ日付比較
            if (true == OkicaNegaFile.isNegaVersionNew(negaVersion.negaDate)) {
                Timber.d("nega_date: " + negaVersion.negaDate);
                GetNegaList.Response negaList = api.getNegaList();
                Timber.d("GetNegaList");
                Timber.d("result: " + negaList.result);
                if (negaList.result) {
                    Timber.d("nega_date: " + negaList.negaDate);
                    Timber.i("ネガリスト取得情報 (新):%s", negaList.negaDate.substring(0,4) + "/" + negaList.negaDate.substring(4,6) + "/" + negaList.negaDate.substring(6,8));
                    OkicaNegaFile.saveOkicaNegaList(negaList.negaDate, negaList.negas);
                    result = true;
                } else {
                    Timber.e("ネガリスト取得エラー error:" + negaList.errorCode);
                }
            } else {
                Timber.d("Keep NegaList");
                result = true;
            }
            Timber.i("ネガリスト使用情報　Date:%s", AppPreference.getOkicaNegaDatetime());
        } else {
            Timber.e("ネガバージョン取得エラー error:" + negaVersion.errorCode);
        }

        return result;
    }

    /**
     * OKICAマスタデータの保持期限をチェック
     * 保持期限を経過した場合はマスタデータを削除
     */
    public boolean okicaCheckMasterTimeLimit() {
        boolean result = false;

        final Calendar calNow = Calendar.getInstance();
        final Calendar calLimit = Calendar.getInstance();
        final TimeZone tz = TimeZone.getTimeZone("Asia/Tokyo");

        calNow.setTimeZone(tz);
        calLimit.setTimeZone(tz);

        Timber.d("Now : %s", calNow.getTime().toString());

        if (AppPreference.getOkicaICMasterInfo() != null) {
            // IC運用マスタ取得日 + 3日を求める
            calLimit.setTimeInMillis(AppPreference.getOkicaICMasterInfo().checkDate);
            calLimit.add(Calendar.DATE, 3);
            calLimit.set(Calendar.HOUR_OF_DAY, 0);
            calLimit.set(Calendar.MINUTE, 0);
            calLimit.set(Calendar.SECOND, 0);
            Timber.d("3days from get icmaster : %s", calLimit.getTime().toString());

            if (calNow.getTimeInMillis() >= calLimit.getTimeInMillis()) {
                Timber.e("IC運用マスタ取得日から3日以上経過(現在%04d/%02d/%02d %02d:%02d:%02d >= 取得日(+3日)%04d/%02d/%02d %02d:%02d:%02d)",
                        calNow.get(Calendar.YEAR), calNow.get(Calendar.MONTH)+1, calNow.get(Calendar.DATE),
                        calNow.get(Calendar.HOUR_OF_DAY), calNow.get(Calendar.MINUTE), calNow.get(Calendar.SECOND),
                        calLimit.get(Calendar.YEAR), calLimit.get(Calendar.MONTH)+1, calLimit.get(Calendar.DATE),
                        calLimit.get(Calendar.HOUR_OF_DAY), calLimit.get(Calendar.MINUTE), calLimit.get(Calendar.SECOND));
                // IC運用マスタ取得日から3日以上経過
                okicaDeleteMaster();
                result = true;
            }
        }

        if (AppPreference.getOkicaAccessKeyInfo() != null && result == false) {
            // アクセスキー取得日 + 3日を求める
            calLimit.setTimeInMillis(AppPreference.getOkicaAccessKeyInfo().checkDate);
            calLimit.add(Calendar.DATE, 3);
            calLimit.set(Calendar.HOUR_OF_DAY, 0);
            calLimit.set(Calendar.MINUTE, 0);
            calLimit.set(Calendar.SECOND, 0);
            Timber.d("3days from get accesskey : %s", calLimit.getTime().toString());

            if (calNow.getTimeInMillis() >= calLimit.getTimeInMillis()) {
                Timber.e("アクセスキー取得日から3日以上経過(現在%04d/%02d/%02d %02d:%02d:%02d >= 取得日(+3日)%04d/%02d/%02d %02d:%02d:%02d)",
                        calNow.get(Calendar.YEAR), calNow.get(Calendar.MONTH)+1, calNow.get(Calendar.DATE),
                        calNow.get(Calendar.HOUR_OF_DAY), calNow.get(Calendar.MINUTE), calNow.get(Calendar.SECOND),
                        calLimit.get(Calendar.YEAR), calLimit.get(Calendar.MONTH)+1, calLimit.get(Calendar.DATE),
                        calLimit.get(Calendar.HOUR_OF_DAY), calLimit.get(Calendar.MINUTE), calLimit.get(Calendar.SECOND));
                // アクセスキー取得日から3日以上経過
                okicaDeleteMaster();
                result = true;
            }
        }

        return result;
    }

    /**
     * OKICAマスタデータの削除
     */
    public void okicaDeleteMaster() {
        Timber.i("【強制マスタデータ削除開始】");

        if (AppPreference.getOkicaAccessKeyInfo() != null && SamManagementUtils.reset() == false) {
            Timber.e("【強制マスタデータ削除失敗】SAMリセット失敗");
        } else if (AppPreference.getOkicaICMasterInfo() != null && ICMaster.delete() == false) {
            Timber.e("【強制マスタデータ削除失敗】IC運用マスタファイル削除失敗");
        } else {
            AppPreference.clearOkicaMasterInfo();
            Timber.i("【強制マスタデータ削除完了】削除成功");
        }
    }

    public void okicaUninstall() {
        Timber.i("【強制撤去開始】");

        if (SamManagementUtils.reset() == false) {
            Timber.e("【強制撤去失敗】SAMリセット失敗");
        } else if (ICMaster.delete() == false) {
            Timber.e("【強制撤去失敗】IC運用マスタファイル削除失敗");
        } else if (OkicaNegaFile.delete() == false) {
            Timber.e("【強制撤去失敗】ネガファイル削除失敗");
        } else {
            AppPreference.clearOkica();
            Timber.i("【強制撤去完了】撤去成功");
        }
    }
}
