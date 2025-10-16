package jp.mcapps.android.multi_payment_terminal.webapi.grpc;

import static jp.mcapps.android.multi_payment_terminal.model.OkicaMasterControl.FORCE_DEACT_NONE;

import android.os.Build;

import com.google.protobuf.ByteString;

import java.util.Arrays;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import io.grpc.Deadline;
import io.grpc.Grpc;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.TlsChannelCredentials;
import io.grpc.ictgate.ictgate_terminal.FileVersion;
import io.grpc.ictgate.ictgate_terminal.GetAccessTokenRequest;
import io.grpc.ictgate.ictgate_terminal.GetAccessTokenResult;
import io.grpc.ictgate.ictgate_terminal.GetMasterFileRequest;
import io.grpc.ictgate.ictgate_terminal.GetMasterFileResult;
import io.grpc.ictgate.ictgate_terminal.GetMasterFileVersionRequest;
import io.grpc.ictgate.ictgate_terminal.GetMasterFileVersionResult;
import io.grpc.ictgate.ictgate_terminal.GetNegaListRequest;
import io.grpc.ictgate.ictgate_terminal.GetNegaListResult;
import io.grpc.ictgate.ictgate_terminal.GetNegaVersionRequest;
import io.grpc.ictgate.ictgate_terminal.GetNegaVersionResult;
import io.grpc.ictgate.ictgate_terminal.GetTerminalInfoRequest;
import io.grpc.ictgate.ictgate_terminal.GetTerminalInfoResult;
import io.grpc.ictgate.ictgate_terminal.IDHeader;
import io.grpc.ictgate.ictgate_terminal.SendDtlRequest;
import io.grpc.ictgate.ictgate_terminal.SendDtlResult;
import io.grpc.ictgate.ictgate_terminal.TerminalInstallationRequest;
import io.grpc.ictgate.ictgate_terminal.TerminalInstallationResult;
import io.grpc.ictgate.ictgate_terminal.TerminalServiceGrpc;
import io.grpc.ictgate.ictgate_terminal.TerminalUninstallationRequest;
import io.grpc.stub.MetadataUtils;
import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.BuildConfig;
import jp.mcapps.android.multi_payment_terminal.model.OkicaMasterControl;
import jp.mcapps.android.multi_payment_terminal.webapi.grpc.data.GetAccessToken;
import jp.mcapps.android.multi_payment_terminal.webapi.grpc.data.GetMasterFile;
import jp.mcapps.android.multi_payment_terminal.webapi.grpc.data.GetMasterFileVerion;
import jp.mcapps.android.multi_payment_terminal.webapi.grpc.data.GetNegaList;
import jp.mcapps.android.multi_payment_terminal.webapi.grpc.data.GetNegaVersion;
import jp.mcapps.android.multi_payment_terminal.webapi.grpc.data.SendDtl;
import jp.mcapps.android.multi_payment_terminal.webapi.grpc.data.TerminalInfo;
import jp.mcapps.android.multi_payment_terminal.webapi.grpc.data.TerminalInstallation;
import jp.mcapps.android.multi_payment_terminal.webapi.grpc.data.TerminalUninstallation;
import timber.log.Timber;

public class McOkicaCenterApiImpl implements McOkicaCenterApi {
    /**
     * 端末設置認証データ取得
     * @param terminalInstallId 機器設置時識別番号
     * @return 端末設置データ取得結果、resultがtrueなら認証ページのURLと認証コード、認証コードの有効期限を返す
     */
    public TerminalInstallation.Response installTerminal(String terminalInstallId) {
        TerminalInstallation.Response responseTerminalInstallation = new TerminalInstallation.Response();
        TerminalInstallationRequest requestInstallTerminal = TerminalInstallationRequest.newBuilder().setTerminalInstallId(terminalInstallId).build();
        ManagedChannel secureChannel = Grpc.newChannelBuilder(BuildConfig.OKICA_ICT_GATE_ENDPOINT, TlsChannelCredentials.create()).build();
        TerminalServiceGrpc.TerminalServiceBlockingStub secureStub = TerminalServiceGrpc.newBlockingStub(secureChannel).withDeadline(Deadline.after(30, TimeUnit.SECONDS));
        try {
            // 端末設置要求
            TerminalInstallationResult replyInstallTerminal = secureStub.installTerminal(requestInstallTerminal);
            responseTerminalInstallation.url = replyInstallTerminal.getUrl();
            responseTerminalInstallation.code = replyInstallTerminal.getCode();
            responseTerminalInstallation.expiredTime = replyInstallTerminal.getExpiredTime();
            responseTerminalInstallation.result = true;
            responseTerminalInstallation.errorCode = "";

        } catch (StatusRuntimeException e) {
            e.printStackTrace();
            responseTerminalInstallation.result = false;
            responseTerminalInstallation.errorCode = e.getStatus().getCode().toString();
        } finally {
            secureChannel.shutdown();
        }
        TimberLog("端末設置認証データ取得", responseTerminalInstallation.result, responseTerminalInstallation.errorCode);
        return responseTerminalInstallation;
    }

    /**
     * アクセストークン取得
     * @param terminalInstallId 機器設置時識別番号
     * @return アクセストークン取得結果、取得したアクセストークンは本クラスで保持し要求元には返さない
     */
    public GetAccessToken.Response getAccessToken(String terminalInstallId, String code) {
        GetAccessToken.Response responseGetAccessToken = new GetAccessToken.Response();
        GetAccessTokenRequest requestAccessToken = GetAccessTokenRequest.newBuilder().setTerminalInstallId(terminalInstallId).setCode(code).build();
        ManagedChannel secureChannel = Grpc.newChannelBuilder(BuildConfig.OKICA_ICT_GATE_ENDPOINT, TlsChannelCredentials.create()).build();
        TerminalServiceGrpc.TerminalServiceBlockingStub secureStub = TerminalServiceGrpc.newBlockingStub(secureChannel).withDeadline(Deadline.after(30, TimeUnit.SECONDS));
        try {
            // アクセストークン取得
            GetAccessTokenResult replyAccessToken = secureStub.getAccessToken(requestAccessToken);
            responseGetAccessToken.result = true;
            responseGetAccessToken.errorCode = "";
            responseGetAccessToken.accessToken = replyAccessToken.getAccessToken();
            responseGetAccessToken.terminalId = replyAccessToken.getTerminalId();
        } catch (StatusRuntimeException e) {
            e.printStackTrace();
            responseGetAccessToken.result = false;
            responseGetAccessToken.errorCode = e.getStatus().getCode().toString();
        } finally {
            secureChannel.shutdown();
        }
        TimberLog("アクセストークン取得", responseGetAccessToken.result, responseGetAccessToken.errorCode);
        return responseGetAccessToken;
    }

    /**
     * アクセストークンを付加した認証用チャネル取得
     * @return ヘッダ情報に認証用のアクセストークンを付加したサーバー認証用のチャネル
     */
    private ManagedChannel getSecureChannelWithAccessToken()  {
        // "authorization": "Bearer " + _AccessTokenをheaderに設定する
        Metadata metadata = new Metadata();
        metadata.put(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER), "Bearer " + AppPreference.getOkicaAccessToken());
        ManagedChannel secureChannel = Grpc.newChannelBuilder(BuildConfig.OKICA_ICT_GATE_ENDPOINT, TlsChannelCredentials.create())
                .intercept(MetadataUtils.newAttachHeadersInterceptor(metadata)).build();
        return secureChannel;
    }

    /**
     * 端末情報取得
     * @return 端末情報取得結果、resultがtrueなら取得した端末情報を返す
     */
    public TerminalInfo.Response getTerminalInfo() {
        TerminalInfo.Response responseGetTerminalInfo = new TerminalInfo.Response();
        GetTerminalInfoRequest requestTerminalInfo = GetTerminalInfoRequest.newBuilder().build();
        ManagedChannel secureChannel = getSecureChannelWithAccessToken();
        TerminalServiceGrpc.TerminalServiceBlockingStub secureStub = TerminalServiceGrpc.newBlockingStub(secureChannel).withDeadline(Deadline.after(30, TimeUnit.SECONDS));
        try {
            // 端末情報取得
            GetTerminalInfoResult replyTerminalInfo = secureStub.getTerminalInfo(requestTerminalInfo);
            responseGetTerminalInfo.terminalId = replyTerminalInfo.getTerminalId();
            responseGetTerminalInfo.shopCd = replyTerminalInfo.getShopCd();
            responseGetTerminalInfo.machineModelCd = replyTerminalInfo.getMachineModelCd();
            responseGetTerminalInfo.machineId = replyTerminalInfo.getMachineId();
            responseGetTerminalInfo.isCharge = replyTerminalInfo.getIsCharge();
            responseGetTerminalInfo.isInstalled = replyTerminalInfo.getIsInstalled();
            responseGetTerminalInfo.result = true;
            responseGetTerminalInfo.errorCode = "";
            if (responseGetTerminalInfo.isInstalled) { OkicaMasterControl.force_deactivation_stat = FORCE_DEACT_NONE; }
        } catch (StatusRuntimeException e) {
            e.printStackTrace();
            responseGetTerminalInfo.result = false;
            responseGetTerminalInfo.errorCode = e.getStatus().getCode().toString();
        } finally {
            secureChannel.shutdown();
        }
        TimberLog("端末情報取得", responseGetTerminalInfo.result, responseGetTerminalInfo.errorCode);
        return responseGetTerminalInfo;
    }

    /**
     * 一件明細データ送信、送信に必要なID管理ヘッダは本メソッド内で作成
     * @param dtls 送信する一件明細データ(複数可)
     * @return 一件明細データ送信結果
     */
    public SendDtl.Response sendDtl(byte[] dtls) {
        final int DtlSize = 192;
        final Calendar now = Calendar.getInstance();
        Calendar operation = Calendar.getInstance();
        operation.add(Calendar.HOUR, -4);    //
        final byte[] reasonCd = {0x05, 0x00, 0x01};   // OK固定
        final byte[] testMode = {0};    // 通常データ固定
        IDHeader idHeader = IDHeader.newBuilder()
                .setCommunicationYear(now.get(Calendar.YEAR))
                .setCommunicationMonth(now.get(Calendar.MONTH)+1)
                .setCommunicationDay(now.get(Calendar.DAY_OF_MONTH))
                .setCommunicationHour(now.get(Calendar.HOUR_OF_DAY))
                .setCommunicationMinute(now.get(Calendar.MINUTE))
                .setCommunicationSecond(now.get(Calendar.SECOND))
                .setMachineSeq(1)   // 仮
                .setOperationYear(operation.get(Calendar.YEAR))
                .setOperationMonth(operation.get(Calendar.MONTH)+1)
                .setOperationDay(operation.get(Calendar.DAY_OF_MONTH))
                .setReasonCd(ByteString.copyFrom(reasonCd))
                .setTestMode(ByteString.copyFrom(testMode))
                .build();
        SendDtl.Response responseSendDtl = new SendDtl.Response();
        SendDtlRequest.Builder builderSendDtl = SendDtlRequest.newBuilder();
        builderSendDtl.setIdHeader(idHeader);
        for (int index = 0; index < dtls.length / DtlSize; index++) {
            builderSendDtl.addDtls(ByteString.copyFrom(dtls, index * DtlSize, DtlSize));
        }
        SendDtlRequest requestSendDtl = builderSendDtl.build();
        ManagedChannel secureChannel = getSecureChannelWithAccessToken();
        TerminalServiceGrpc.TerminalServiceBlockingStub secureStub = TerminalServiceGrpc.newBlockingStub(secureChannel).withDeadline(Deadline.after(30, TimeUnit.SECONDS));
        try {
            // 一件明細送信
            SendDtlResult replySendDtl = secureStub.sendDtl(requestSendDtl);
            responseSendDtl.result = true;
            responseSendDtl.errorCode = "";
        } catch (StatusRuntimeException e) {
            e.printStackTrace();
            responseSendDtl.result = false;
            responseSendDtl.errorCode = e.getStatus().getCode().toString();
        } finally {
            secureChannel.shutdown();
        }
        TimberLog("一件明細データ送信", responseSendDtl.result, responseSendDtl.errorCode);
        return responseSendDtl;
    }

    /**
     * マスターファイルバージョン取得
     * @param fileIds マスターファイルID(複数可)
     * @return マスターファイルバージョン取得結果、resultがtrueなら取得したマスターファイル毎のバージョンを返す
     */
    public GetMasterFileVerion.Response getMasterFileVersion(String[] fileIds) {
        GetMasterFileVerion.Response responseGetMasterFileVersion = new GetMasterFileVerion.Response();
        GetMasterFileVersionRequest.Builder builderGetMasterFileVersion = GetMasterFileVersionRequest.newBuilder();
        builderGetMasterFileVersion.addAllFileIds(Arrays.asList(fileIds));
        GetMasterFileVersionRequest requestGetMasterFileVersion = builderGetMasterFileVersion.build();
        ManagedChannel secureChannel = getSecureChannelWithAccessToken();
        TerminalServiceGrpc.TerminalServiceBlockingStub secureStub = TerminalServiceGrpc.newBlockingStub(secureChannel).withDeadline(Deadline.after(30, TimeUnit.SECONDS));
        try {
            GetMasterFileVersionResult replyGetMasterFileVerion = secureStub.getMasterFileVerion(requestGetMasterFileVersion);
            responseGetMasterFileVersion.fileVersions = new FileVersion[replyGetMasterFileVerion.getFileVersionsCount()];
            for (int index = 0; index < replyGetMasterFileVerion.getFileVersionsCount(); index++) {
                responseGetMasterFileVersion.fileVersions[index] = replyGetMasterFileVerion.getFileVersions(index);
            }
            responseGetMasterFileVersion.result = true;
            responseGetMasterFileVersion.errorCode = "";
        } catch (StatusRuntimeException e) {
            e.printStackTrace();
            responseGetMasterFileVersion.result = false;
            responseGetMasterFileVersion.errorCode = e.getStatus().getCode().toString();
        } finally {
            secureChannel.shutdown();
        }
        TimberLog("IC運用マスタとアクセスキーのファイルバージョン取得", responseGetMasterFileVersion.result, responseGetMasterFileVersion.errorCode);
        return responseGetMasterFileVersion;
    }

    /**
     * マスターファイル取得
     * @param fileId マスターファイルID(単数のみ)
     * @return マスターファイル取得結果、resultがtrueなら取得したマスターファイルとバージョン、ファイル名を返す
     */
    public GetMasterFile.Response getMasterFile(String fileId) {
        final String IC_MASTER_FILE_ID = "11";
        final String ACCESS_KEY_FILE_ID_WITH_CHARGE = "46";
        final String ACCESS_KEY_FILE_ID_WITHOUT_CHARGE = "47";

        GetMasterFile.Response responseGetMasterFile = new GetMasterFile.Response();
        GetMasterFileRequest requestGetMasterFile = GetMasterFileRequest.newBuilder().setFileId(fileId).build();
        ManagedChannel secureChannel = getSecureChannelWithAccessToken();
        TerminalServiceGrpc.TerminalServiceBlockingStub secureStub = TerminalServiceGrpc.newBlockingStub(secureChannel).withDeadline(Deadline.after(30, TimeUnit.SECONDS));
        try {
            // マスターファイル取得
            GetMasterFileResult replyGetMasterFile = secureStub.getMasterFile(requestGetMasterFile);
            responseGetMasterFile.version = replyGetMasterFile.getVersion();
            responseGetMasterFile.fileName = replyGetMasterFile.getFileName();
            responseGetMasterFile.file = replyGetMasterFile.getFile().toByteArray();
            responseGetMasterFile.result = true;
            responseGetMasterFile.errorCode = "";
        } catch (StatusRuntimeException e) {
            e.printStackTrace();
            responseGetMasterFile.result = false;
            responseGetMasterFile.errorCode = e.getStatus().getCode().toString();
        } finally {
            secureChannel.shutdown();
        }

        if (IC_MASTER_FILE_ID.equals(fileId)) {
            TimberLog("IC運用マスタファイル取得", responseGetMasterFile.result, responseGetMasterFile.errorCode);
        } else if (ACCESS_KEY_FILE_ID_WITH_CHARGE.equals(fileId)) {
            TimberLog("アクセスキーファイル取得(チャージ機能あり)", responseGetMasterFile.result, responseGetMasterFile.errorCode);
        } else if (ACCESS_KEY_FILE_ID_WITHOUT_CHARGE.equals(fileId)) {
            TimberLog("アクセスキーファイル取得(チャージ機能なし)", responseGetMasterFile.result, responseGetMasterFile.errorCode);
        } else {
            TimberLog("不明なファイルID指定(" + fileId + ") ファイル取得", responseGetMasterFile.result, responseGetMasterFile.errorCode);
        }
        return responseGetMasterFile;
    }

    /**
     * ネガバージョン取得
     * @return ネガバージョン取得結果、resultがtrueなら取得したネガ年月日を返す
     */
    public GetNegaVersion.Response getNegaVersion() {
        GetNegaVersion.Response responseGetNegaVersion = new GetNegaVersion.Response();
        GetNegaVersionRequest requestGetNegaVersion = GetNegaVersionRequest.newBuilder().build();
        ManagedChannel secureChannel = getSecureChannelWithAccessToken();
        TerminalServiceGrpc.TerminalServiceBlockingStub secureStub = TerminalServiceGrpc.newBlockingStub(secureChannel).withDeadline(Deadline.after(30, TimeUnit.SECONDS));
        try {
            // ネガバージョン取得
            GetNegaVersionResult replyGetNegaVersion = secureStub.getNegaVerion(requestGetNegaVersion);
            responseGetNegaVersion.negaDate = replyGetNegaVersion.getNegaDate();
            responseGetNegaVersion.result = true;
            responseGetNegaVersion.errorCode = "";
        } catch (StatusRuntimeException e) {
            responseGetNegaVersion.result = false;
            responseGetNegaVersion.errorCode = e.getStatus().getCode().toString();
        } finally {
            secureChannel.shutdown();
        }
        TimberLog("ネガバージョン取得", responseGetNegaVersion.result, responseGetNegaVersion.errorCode);
        return responseGetNegaVersion;
    }

    /**
     * ネガリスト取得
     * @return ネガリスト取得結果、resultがtrueなら取得したネガリストとネガ年月日を返す
     */
    public GetNegaList.Response getNegaList() {
        final int NegaSize = 8;
        GetNegaList.Response responseGetNegaList = new GetNegaList.Response();
        GetNegaListRequest requestGetNegaList = GetNegaListRequest.newBuilder().build();
        ManagedChannel secureChannel = getSecureChannelWithAccessToken();
        TerminalServiceGrpc.TerminalServiceBlockingStub secureStub = TerminalServiceGrpc.newBlockingStub(secureChannel).withDeadline(Deadline.after(30, TimeUnit.SECONDS));
        try {
            // ネガリスト取得
            GetNegaListResult replyNegaList = secureStub.getNegaList(requestGetNegaList);
            responseGetNegaList.negaDate = replyNegaList.getNegaDate();
            responseGetNegaList.negas = replyNegaList.getNegasList();
            responseGetNegaList.result = true;
            responseGetNegaList.errorCode = "";
        } catch (StatusRuntimeException e) {
            e.printStackTrace();
            responseGetNegaList.result = false;
            responseGetNegaList.errorCode = e.getStatus().getCode().toString();
        } finally {
            secureChannel.shutdown();
        }
        TimberLog("ネガリスト取得", responseGetNegaList.result, responseGetNegaList.errorCode);
        return responseGetNegaList;
    }

    /**
     * 端末撤去
     * @return 端末撤去結果
     */
    public TerminalUninstallation.Response uninstallTerminal() {
        TerminalUninstallation.Response responseTerminalUninstallation = new TerminalUninstallation.Response();
        TerminalUninstallationRequest requestTerminalUninstallation = TerminalUninstallationRequest.newBuilder().build();
        ManagedChannel secureChannel = getSecureChannelWithAccessToken();
        TerminalServiceGrpc.TerminalServiceBlockingStub secureStub = TerminalServiceGrpc.newBlockingStub(secureChannel).withDeadline(Deadline.after(30, TimeUnit.SECONDS));
        try {
            // 端末設置解除
            // 戻り値は使わないので無視する
            // TerminalUninstallationResult replyTerminalUninstallation = secureStub.uninstallTerminal(requestTerminalUninstallation);
            secureStub.uninstallTerminal(requestTerminalUninstallation);
            responseTerminalUninstallation.result = true;
            responseTerminalUninstallation.errorCode = "";
        } catch (StatusRuntimeException e) {
            e.printStackTrace();
            responseTerminalUninstallation.result = false;
            responseTerminalUninstallation.errorCode = e.getStatus().getCode().toString();
        } finally {
            secureChannel.shutdown();
        }
        TimberLog("端末撤去", responseTerminalUninstallation.result, responseTerminalUninstallation.errorCode);
        return responseTerminalUninstallation;
    }

    private static void TimberLog(String requestName, boolean result, String errorCode) {
        String strResult = "";

        if (result == true) {
            strResult = requestName + "成功";
            Timber.i(strResult);
        } else {
            strResult = requestName + "異常 (" + errorCode + ":";
            /* gRPCエラーコード一覧は、下記のサイトを参照
            https://zenn.dev/hsaki/books/golang-grpc-starting/viewer/errorcode
            */
            if (errorCode.equals(Status.OK.getCode().toString())) {
                strResult += "正常";
            } else if (errorCode.equals(Status.CANCELLED.getCode().toString())) {
                strResult += "処理がキャンセルされた";
            } else if (errorCode.equals(Status.UNKNOWN.getCode().toString())) {
                strResult += "不明なエラー";
            } else if (errorCode.equals(Status.INVALID_ARGUMENT.getCode().toString())) {
                strResult += "無効な引数でメソッドを呼び出した";
            } else if (errorCode.equals(Status.DEADLINE_EXCEEDED.getCode().toString())) {
                strResult += "タイムアウト";
            } else if (errorCode.equals(Status.NOT_FOUND.getCode().toString())) {
                strResult += "要求されたエンティティが存在しなかった";
            } else if (errorCode.equals(Status.ALREADY_EXISTS.getCode().toString())) {
                strResult += "既に存在しているエンティティを作成するようなリクエストだったため失敗";
            } else if (errorCode.equals(Status.PERMISSION_DENIED.getCode().toString())) {
                strResult += "そのメソッドを実行するための権限がない";
            } else if (errorCode.equals(Status.RESOURCE_EXHAUSTED.getCode().toString())) {
                strResult += "リクエストを処理するためのquotaが枯渇した";
            } else if (errorCode.equals(Status.FAILED_PRECONDITION.getCode().toString())) {
                strResult += "処理を実行できる状態ではないため、リクエストが拒否された";
            } else if (errorCode.equals(Status.ABORTED.getCode().toString())) {
                strResult += "トランザクションがコンフリクトしたなどして、処理が異常終了させられた";
            } else if (errorCode.equals(Status.OUT_OF_RANGE.getCode().toString())) {
                strResult += "有効範囲外の操作をリクエストされた";
            } else if (errorCode.equals(Status.UNIMPLEMENTED.getCode().toString())) {
                strResult += "サーバーに実装されていないサービス・メソッドを呼び出そうとした";
            } else if (errorCode.equals(Status.INTERNAL.getCode().toString())) {
                strResult += "サーバー内で重大なエラーが発生した";
            } else if (errorCode.equals(Status.UNAVAILABLE.getCode().toString())) {
                strResult += "メソッドを実行するための用意ができていない";
            } else if (errorCode.equals(Status.DATA_LOSS.getCode().toString())) {
                strResult += "NWの問題で伝送中にパケットが失われた";
            } else if (errorCode.equals(Status.UNAUTHENTICATED.getCode().toString())) {
                strResult += "ユーザー認証に失敗した";
            } else  {
                strResult += "未定義エラーコード";
            }
            Timber.e(strResult + ")");
        }
    }
}
