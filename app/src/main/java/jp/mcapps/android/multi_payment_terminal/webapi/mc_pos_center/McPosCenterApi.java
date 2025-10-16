package jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center;

import java.io.IOException;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import jp.mcapps.android.multi_payment_terminal.webapi.HttpStatusException;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.CAKey;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.Car;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.CardAnalyze;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.CreditGetKey;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.Driver;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.Echo;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.Exchange1;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.Exchange2;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.GetKey;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.OnlineAuth;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.OnlineAuthCancel;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.PostTerminalInfo;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.RiskParameterContactless;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.TerminalInfo;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.WatariPoint;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.WatariPointCancel;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.payment.Payment;

public interface McPosCenterApi {
    // 認証用公開鍵取得
    GetKey.Response getKey() throws IOException;
    // 相互認証１
    Exchange1.Response exchange1(String key1, String sessionKey1) throws IOException, HttpStatusException;
    // 相互認証２
    Exchange2.Response exchange2(String sessionKey2) throws IOException, HttpStatusException;
    // 疎通確認
    Echo.Response echo(boolean detachJR, boolean detachQR) throws IOException, HttpStatusException, IllegalStateException;
    // クレジットCA公開鍵DL
    // xxx throws IOException
    // カードデータ保護用公開鍵取得
    CreditGetKey.Response creditGetKey() throws IOException, HttpStatusException, IllegalStateException;
    // カード判定
    CardAnalyze.Response cardAnalyze(CardAnalyze.Request request) throws IOException, HttpStatusException, IllegalStateException;
    // オンラインオーソリ
    OnlineAuth.Response onlineAuth(OnlineAuth.Request request) throws IOException, HttpStatusException, IllegalStateException;
    // オンラインオーソリ取消
    OnlineAuthCancel.Response onlineAuthCancel(OnlineAuthCancel.Request request) throws IOException, HttpStatusException, IllegalStateException;
    // 端末情報取得
    TerminalInfo.Response getTerminalInfo() throws IOException, HttpStatusException, IllegalStateException;
    //端末稼働情報連携
    PostTerminalInfo.Response postTerminalInfo(int type) throws IOException, HttpStatusException, IllegalStateException;
    // 乗務員情報取得
    Driver.Response getDriver(int driverCode, boolean update, String driverName) throws IOException, HttpStatusException, IllegalStateException;
    // 号機番号設定
    Car.Response setCar(int carId) throws IOException, HttpStatusException, IllegalStateException;
    // 売上情報連携
    Payment.Response postPayment(Payment.Request request) throws IOException, HttpStatusException, IllegalStateException;
    // CA公開鍵取得
    CAKey.Response getCAKey() throws IOException, HttpStatusException, IllegalStateException;
    // 異常売上情報連携
    Payment.Response postInvalidPayment(Payment.Request request) throws IOException, HttpStatusException, IllegalStateException;
    // 非接触リスク管理パラメータ取得
    RiskParameterContactless.Response getRiskParameterContactless(RiskParameterContactless.Request request) throws IOException, HttpStatusException, IllegalStateException;
    // 和多利ポイント付与
    WatariPoint.Response watariAdd(WatariPoint.Request request) throws IOException, HttpStatusException, IllegalStateException;
    // 和多利ポイント取消
    WatariPointCancel.Response watariCancel(WatariPointCancel.Request request) throws IOException, HttpStatusException, IllegalStateException;
}
