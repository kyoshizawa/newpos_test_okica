package jp.mcapps.android.multi_payment_terminal.webapi.grpc;

import jp.mcapps.android.multi_payment_terminal.webapi.grpc.data.GetAccessToken;
import jp.mcapps.android.multi_payment_terminal.webapi.grpc.data.GetMasterFile;
import jp.mcapps.android.multi_payment_terminal.webapi.grpc.data.GetMasterFileVerion;
import jp.mcapps.android.multi_payment_terminal.webapi.grpc.data.GetNegaList;
import jp.mcapps.android.multi_payment_terminal.webapi.grpc.data.GetNegaVersion;
import jp.mcapps.android.multi_payment_terminal.webapi.grpc.data.SendDtl;
import jp.mcapps.android.multi_payment_terminal.webapi.grpc.data.TerminalInfo;
import jp.mcapps.android.multi_payment_terminal.webapi.grpc.data.TerminalInstallation;
import jp.mcapps.android.multi_payment_terminal.webapi.grpc.data.TerminalUninstallation;

public interface McOkicaCenterApi {
    // 端末設置認証データ取得
    TerminalInstallation.Response installTerminal(String terminalInstallId);

    // アクセストークン取得
    GetAccessToken.Response getAccessToken(String terminalInstallId, String code);

    // 端末情報取得
    TerminalInfo.Response getTerminalInfo();

    // 一件明細送信
    SendDtl.Response sendDtl(byte[] dtls);

    // マスターファイルバージョン取得
    GetMasterFileVerion.Response getMasterFileVersion(String[] fileIds);

    // マスターファイル取得
    GetMasterFile.Response getMasterFile(String fileId);

    // ネガバージョン取得
    GetNegaVersion.Response getNegaVersion();

    // ネガリスト取得
    GetNegaList.Response getNegaList();

    // 端末設置解除
    TerminalUninstallation.Response uninstallTerminal();
}
