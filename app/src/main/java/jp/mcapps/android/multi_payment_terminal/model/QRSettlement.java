package jp.mcapps.android.multi_payment_terminal.model;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.data.Amount;
import jp.mcapps.android.multi_payment_terminal.data.QRPayTypeCodes;
import jp.mcapps.android.multi_payment_terminal.data.QRPayTypeNameMap;
import jp.mcapps.android.multi_payment_terminal.data.TransactionResults;
import jp.mcapps.android.multi_payment_terminal.error.GmoErrorCodes;
import jp.mcapps.android.multi_payment_terminal.error.GmoErrorMap;
import jp.mcapps.android.multi_payment_terminal.error.McPosCenterErrorMap;
import jp.mcapps.android.multi_payment_terminal.util.McUtils;
import jp.mcapps.android.multi_payment_terminal.webapi.HttpStatusException;
import jp.mcapps.android.multi_payment_terminal.webapi.gmo.GmoApi;
import jp.mcapps.android.multi_payment_terminal.webapi.gmo.GmoApiImpl;
import jp.mcapps.android.multi_payment_terminal.webapi.gmo.data.*;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.McPosCenterApi;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.McPosCenterApiImpl;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.Echo;
import timber.log.Timber;

public class QRSettlement {
    private static final int POLLING_CYCLE_MS = 3000;  // GMOの推奨値は2～3秒
    private static final int MAX_RESULT_CHECK_COUNT = 4;  // 結果確認の実行回数
    private final MainApplication _app = MainApplication.getInstance();
    private final GmoApi _apiClient = new GmoApiImpl();
    private final Gson _gson = new Gson();

    public static class ResultSummary {
        public TransactionResults result = TransactionResults.UNKNOWN;
        public String code = null;
        public String orderId = null;
        public String refundId = null;
        public String payType = null;
        public String totalFee = null;
        public String balanceAmount = null;
        public String payTime = null;
        public long startTime = 0;  // ミリ秒までのUNIX時間
        public long endTime = 0;  // ミリ秒までのUNIX時間
        public int procTime = 0;  // endTime - startTime
        public int resultCheckCount = 0;
        public String wallet = null; // Alipay+のみ それ以外はnull
    }

    private static class HeaderFields {
        public static final String TIME = "X-GCP-Time";  // ミリ秒で表される現在の時間
        public static final String NONCE_STR = "X-GCP-NonceStr";  // ランダム文字列
        public static final String SIGN = "X-GCP-Sign";  // 通信トークンハッシュ値
        public static final String LOGIN_ID = "X-GCP-loginId";  // ログイン ID
        public static final String SERIAL_NO = "X-GCP-serialNo";  // 端末識別番号
    }

    private static class ReturnCodes {
        public static final String SUCCESS = "SUCCESS";  // 固定
    }

    private static class ResultCodes {
        public static final String PAY_SUCCESS = "PAY_SUCCESS";  // 支払成功
        public static final String PAYING = "PAYING";  // 支払い待ち
        public static final String SUCCESS = "SUCCESS";  // 成功
        public static final String FINISHED = "FINISHED";  // 完了
        public static final String WAITING = "WAITING";  // 返金待ち
        public static final String PARTIAL_REFUND = "PARTIAL_REFUND";  // 一部返金済
        public static final String FULL_REFUND = "FULL_REFUND";  // 全額返金済
        public static final String PAY_TERMINATION = "PAY_ TERMINATION";  // 決済中止
        public static final String CHK_PAYFAIL = "PAY_FAIL"; // 支払確認NG
        public static final String CHK_FAILED = "FAILED"; // 取消確認NG
    }

    public String login() {
        Timber.i("QRユーザー認証実行");
        String errorCode = null;

        String packageName = "UNKNOWN";
        String versionName = "UNKNOWN";

        try {
            PackageInfo packageInfo = MainApplication.getInstance().getPackageManager()
                    .getPackageInfo(MainApplication.getInstance().getPackageName(),
                            PackageManager.PackageInfoFlags.of(0));

            packageName = packageInfo.packageName;
            versionName = packageInfo.versionName;
        } catch (Exception ignore) {
            Timber.e(ignore);
        }

        try {
            PostLogin.Request request = new PostLogin.Request();
            request.loginId = AppPreference.getQrUserId();
            request.userPassword = AppPreference.getQrPassword();
            request.osName = packageName;
            request.osVersion = versionName;
            request.serialNo = String.valueOf(AppPreference.getMcTermId());

            _app.setQREnabledFlags(0);

            PostLogin.Response response = _apiClient.postLogin(request);
            Timber.d("response: %s", _gson.toJson(response));

            if (response.msgSummaryCode != null) {
                errorCode = GmoErrorMap.get(response.msgSummaryCode);
                Timber.tag("QR").e(response.returnMessage);
            } else if(response.result == null) {
                errorCode = GmoErrorMap.INTERNAL_ERROR_CODE;
            } else {
                saveHeaderParams(response.result.credentialKey);

                int flags = 0;

                for (final PostLogin.PayTypeList payType : response.result.payTypeList) {
                    /**/ if (payType.payTypeCode.equals(QRPayTypeCodes.Wechat))     flags |= QRPayTypeCodes.Flags.WECHAT;
                    else if (payType.payTypeCode.equals(QRPayTypeCodes.Alipay))     flags |= QRPayTypeCodes.Flags.ALIPAY;
                    else if (payType.payTypeCode.equals(QRPayTypeCodes.Docomo))     flags |= QRPayTypeCodes.Flags.DOCOMO;
                    else if (payType.payTypeCode.equals(QRPayTypeCodes.auPAY))      flags |= QRPayTypeCodes.Flags.AUPAY;
                    else if (payType.payTypeCode.equals(QRPayTypeCodes.PayPay))     flags |= QRPayTypeCodes.Flags.PAYPAY;
                    else if (payType.payTypeCode.equals(QRPayTypeCodes.LINEPay))    flags |= QRPayTypeCodes.Flags.LINEPAY;
                    else if (payType.payTypeCode.equals(QRPayTypeCodes.RakutenPay)) flags |= QRPayTypeCodes.Flags.RAKUTENPAY;
                    else if (payType.payTypeCode.equals(QRPayTypeCodes.GinkoPay))   flags |= QRPayTypeCodes.Flags.GINKOPAY;
                    else if (payType.payTypeCode.equals(QRPayTypeCodes.merpay))     flags |= QRPayTypeCodes.Flags.MERPAY;
                    else if (payType.payTypeCode.equals(QRPayTypeCodes.QUOPay))     flags |= QRPayTypeCodes.Flags.QUOPAY;
                    else if (payType.payTypeCode.equals(QRPayTypeCodes.AlipayPlus)) flags |= QRPayTypeCodes.Flags.ALIPAYPLUS;
                    else if (payType.payTypeCode.equals(QRPayTypeCodes.JCoinPay))   flags |= QRPayTypeCodes.Flags.JCOINPAY; // 2025/03/08 ADD t.wada
                    else if (payType.payTypeCode.equals(QRPayTypeCodes.AEONPay))    flags |= QRPayTypeCodes.Flags.AEONPAY; // 2025/03/08 ADD t.wada
                    // 決済種別コード⇔種別名称の対応を保存
                    QRPayTypeNameMap.set(payType.payTypeCode, payType.payTypeName);
                }

                _app.setQREnabledFlags(flags);
            }
        } catch (IOException e) {
            Timber.e(e);
            errorCode = GmoErrorMap.INTERNAL_ERROR_CODE;
        } catch (HttpStatusException e) {
            Timber.e("HttpStatusCode: %s", e.getStatusCode());
//            errorCode = GmoErrorMap.INTERNAL_ERROR_CODE;
            errorCode = _app.getString(R.string.error_type_qr_3321) + "@@@" + e.getStatusCode() + "@@@";
        }

        if (errorCode == null) {
            Timber.i("QRユーザー認証成功");
        } else {
            Timber.e("QRユーザー認証失敗(エラーコード:%s)", errorCode);
        }

        return errorCode;
    }

    public ResultSummary order(String authCode) {
        if (AppPreference.isDemoMode()) return demoPayment();

        final ResultSummary summary = new ResultSummary();

        summary.orderId = makeSlipId();
        summary.startTime = new Date().getTime();

        final Map<String, String> header = loadHeaderParams();

        try {
            final PutOrders.Request request = new PutOrders.Request();
            request.orderId = summary.orderId;
            request.serialNo = header.get(HeaderFields.SERIAL_NO);
            request.description = ""; // null不可
            request.price = String.valueOf(Amount.getFixedAmount());
            request.authCode = authCode;
            request.currency = "JPY";  // JPY固定
            request.operator = header.get(HeaderFields.LOGIN_ID);

            // 売上に必要なデータを初期化（応答受信時は応答の内容で上書きする）
            summary.payType = null;
            summary.totalFee = String.valueOf(Amount.getFixedAmount());
            summary.payTime = null;

            Timber.d("QR支払(CMP)開始 支払伝票番号: %s", summary.orderId);
            PutOrders.Response response = _apiClient.putOrders(header, request);
            Timber.d("response: %s", _gson.toJson(response));

            if (response.msgSummaryCode != null) {
                summary.code = GmoErrorMap.get(response.msgSummaryCode);
                summary.result = TransactionResults.FAILURE;
                Timber.tag("QR").e(response.returnMessage);
            } else if(response.result == null) {
                summary.code = GmoErrorMap.INTERNAL_ERROR_CODE;
                summary.result = TransactionResults.FAILURE;
            } else {
                final PutOrders.Result result = response.result;

                if (!result.returnCode.equals(ReturnCodes.SUCCESS)) {
                    summary.code = GmoErrorMap.INTERNAL_ERROR_CODE;
                    summary.result = TransactionResults.FAILURE;
                } else {
                    if (result.resultCode.equals(ResultCodes.PAY_SUCCESS)) {
                        summary.result = TransactionResults.SUCCESS;
                        summary.code = null;
                        summary.balanceAmount = response.balanceAmount;
                    }
                    else if (result.resultCode.equals(ResultCodes.PAYING)) {
                        summary.balanceAmount = response.balanceAmount;
                        return checkOrder(header, summary);
                    }
                    else {
                        summary.code = GmoErrorMap.INTERNAL_ERROR_CODE;
                        summary.result = TransactionResults.FAILURE;
                    }

                    summary.payType = result.channel;
                    summary.totalFee = result.totalFee;
                    summary.payTime = replacePayTime(result.payTime);
                }
                summary.wallet = result.wallet;
            }
        } catch (HttpStatusException e) {
            Timber.e("HttpStatusCode: %s", e.getStatusCode());
//            summary.code = GmoErrorMap.INTERNAL_ERROR_CODE;
            summary.code = _app.getString(R.string.error_type_qr_3322) + "@@@" + e.getStatusCode() + "@@@";
            // ステータスコードがおかしい場合は明らかに取引失敗してるので状態確認しない
            summary.result = TransactionResults.FAILURE;
        } catch (SocketTimeoutException e) {
            Timber.e(e);
            Timber.tag("QR").i("Timeout Error");
            summary.result = TransactionResults.UNKNOWN;
        } catch (SocketException | UnknownHostException e) {
                Timber.e(e);
                Timber.tag("QR").i("SocketException Error");
                summary.result = TransactionResults.UNKNOWN;
        } catch (IOException e) {
            Timber.e(e);
            summary.code = GmoErrorMap.INTERNAL_ERROR_CODE;
            summary.result = TransactionResults.FAILURE;
        } catch (Exception e) {
            Timber.e(e);
            Timber.tag("QR").i("Unexpected Error");
            summary.result = TransactionResults.UNKNOWN;
        }

        final Date date = new Date();
        summary.endTime = date.getTime();
        summary.procTime = (int) (summary.endTime - summary.startTime);

        if (summary.payTime == null) {
            summary.payTime = createPayTime(date);
        }

        return summary;
    }


    public ResultSummary checkOrder(ResultSummary summary) {
        summary.resultCheckCount = 0;
        return checkOrder(loadHeaderParams(), summary);
    }

    public ResultSummary checkOrder(String orderId) {
        final ResultSummary summary = new ResultSummary();
        summary.orderId = orderId;
        summary.startTime = new Date().getTime();
        return checkOrder(loadHeaderParams(), summary);
    }

    public ResultSummary refund(String orderId, int fee) {
        if (AppPreference.isDemoMode()) return demoRefund(orderId,fee);

        final ResultSummary summary = new ResultSummary();

        summary.startTime = new Date().getTime();
        summary.refundId = makeSlipId();
        summary.orderId = orderId;

        final Map<String, String> header = loadHeaderParams();


        try {
            final PutRefunds.Request request = new PutRefunds.Request();
            request.refundId = summary.refundId;
            request.orderId = summary.orderId;
            request.serialNo = header.get(HeaderFields.SERIAL_NO);
            request.fee = String.valueOf(fee);

            // 売上に必要なデータを初期化（応答受信時は応答の内容で上書きする）
            summary.payType = null;
            summary.totalFee = request.fee;
            summary.payTime = null;

            Timber.d("QR返金開始 返金伝票金額:%s, 対象支払伝票番号: %s, 返金金額: %s", summary.refundId, summary.orderId, fee);
            PutRefunds.Response response = _apiClient.putRefunds(header, request);
            Timber.d("response: %s", _gson.toJson(response));

            if (response.msgSummaryCode != null) {
                summary.code = GmoErrorMap.get(response.msgSummaryCode);
                summary.result = TransactionResults.FAILURE;
                Timber.tag("QR").e(response.returnMessage);
            } else if(response.result == null) {
                summary.code = GmoErrorMap.INTERNAL_ERROR_CODE;
                summary.result = TransactionResults.FAILURE;
            } else {
                final PutRefunds.Result result = response.result;

                if (!result.returnCode.equals(ReturnCodes.SUCCESS)) {
                    summary.code = GmoErrorMap.INTERNAL_ERROR_CODE;
                    summary.result = TransactionResults.FAILURE;
                } else {
                    if (result.resultCode.equals(ResultCodes.SUCCESS) || result.resultCode.equals(ResultCodes.FINISHED)) {
                        summary.result = TransactionResults.SUCCESS;
                        summary.code = null;
                        summary.balanceAmount = response.balanceAmount;
                    }
                    else if (result.resultCode.equals(ResultCodes.WAITING))  {
                        summary.balanceAmount = response.balanceAmount;
                        return checkRefund(header, summary);
                    }
                    else {
                        summary.code = GmoErrorMap.INTERNAL_ERROR_CODE;
                        summary.result = TransactionResults.FAILURE;
                    }

                    summary.payType = result.channel;
                    summary.totalFee = result.totalFee;
                }
                summary.wallet = result.wallet;
            }
        } catch (HttpStatusException e) {
            Timber.e("HttpStatusCode: %s", e.getStatusCode());
//            summary.code = GmoErrorMap.INTERNAL_ERROR_CODE;
            summary.code = _app.getString(R.string.error_type_qr_3323) + "@@@" + e.getStatusCode() + "@@@";
            // ステータスコードがおかしい場合は明らかに取引失敗してるので状態確認しない
            summary.result = TransactionResults.FAILURE;
        } catch (SocketTimeoutException e) {
            Timber.e(e);
            Timber.tag("QR").i("Timeout Error");
            summary.result = TransactionResults.UNKNOWN;
        } catch (SocketException | UnknownHostException e) {
            Timber.e(e);
            Timber.tag("QR").i("SocketException Error");
            summary.result = TransactionResults.UNKNOWN;
        } catch (IOException e) {
            Timber.e(e);
            summary.code = GmoErrorMap.INTERNAL_ERROR_CODE;
            summary.result = TransactionResults.FAILURE;
        } catch (Exception e) {
            Timber.e(e);
            Timber.tag("QR").i("Unexpected Error");
            summary.result = TransactionResults.UNKNOWN;
        }

        final Date date = new Date();
        summary.endTime = date.getTime();
        summary.procTime = (int) (summary.endTime - summary.startTime);

        if (summary.payTime == null) {
            summary.payTime = createPayTime(date);
        }

        return summary;
    }

    public ResultSummary checkRefund(ResultSummary summary) {
        summary.resultCheckCount = 0;
        return checkRefund(loadHeaderParams(), summary);
    }

    public String uninstall() {
        final File dir = MainApplication.getInstance().getFilesDir();
        final McPosCenterApi mcPosCenterApi = new McPosCenterApiImpl();

        try {
            AppPreference.execMcEcho(); //echo実行フラグON
            AppPreference.setDetachQR(true); //QRの設置解除指示をON
            final Echo.Response echoResponse = mcPosCenterApi.echo(AppPreference.isDetachJR(), AppPreference.isDetachQR());

            if (!echoResponse.result) {
                Timber.d("QR紐付け解除失敗");
                return McPosCenterErrorMap.get(echoResponse.errorCode);
            } else {
                Timber.d("QR紐付け解除成功");
                AppPreference.setIsAvailable(echoResponse.useable); //利用許可状態をRAMに保持
            }
        } catch (IOException | IllegalStateException e) {
            Timber.d(e);
            // 紐付け解除時のエラー表示は行わない
            return null;
        } catch (HttpStatusException e) {
            Timber.e("HttpStatusCode: %s", e.getStatusCode());
            // 紐付け解除時のエラー表示は行わない
            return null;
        }

        AppPreference.clearQrUserId();
        AppPreference.clearQrPassword();
        AppPreference.setGmoHeaderTime("");
        AppPreference.setGmoHeaderNonceStr("");
        AppPreference.setGmoHeaderSign("");

        return null;
    }

    public ResultSummary checkRefund(String refundId, String orderId) {
        final ResultSummary summary = new ResultSummary();
        summary.refundId = refundId;
        summary.orderId = orderId;
        summary.startTime = new Date().getTime();
        return checkRefund(loadHeaderParams(), summary);
    }

    private ResultSummary checkOrder(Map<String, String> header, ResultSummary summary) {
        if (summary.resultCheckCount >= MAX_RESULT_CHECK_COUNT) {
            setSummaryParams(summary);
            return summary;
        }

        summary.resultCheckCount++;

        try {
            Thread.sleep(POLLING_CYCLE_MS);
        } catch (InterruptedException e) {
            Timber.e(e);
        }

        try {
            final GetCheckOrder.Request request = new GetCheckOrder.Request();
            request.storeOrderId = summary.orderId;

            Timber.d("QR支払結果確認確認 支払伝票番号: %s", summary.orderId);
            GetCheckOrder.Response response = _apiClient.getCheckOrder(header, request.toMap());
            Timber.d("response: %s", _gson.toJson(response));

            if (response.msgSummaryCode != null) {
                summary.code = GmoErrorMap.get(response.msgSummaryCode);
                summary.result = TransactionResults.FAILURE;
                Timber.tag("QR").e(response.returnMessage);
            } else if(response.result == null) {
                summary.code = GmoErrorMap.INTERNAL_ERROR_CODE;
                summary.result = TransactionResults.FAILURE;
            } else {
                final GetCheckOrder.Result result = response.result;

                if (result.resultCode.equals(ResultCodes.PAY_SUCCESS)) {
                    summary.result = TransactionResults.SUCCESS;
                    summary.code = null;
                }
                else if (result.resultCode.equals(ResultCodes.PAYING)) {
                    return checkOrder(header, summary);
                }
                else if (result.resultCode.equals(ResultCodes.CHK_PAYFAIL)) {
                    summary.code = _app.getString(R.string.error_type_qr_3315);
                    summary.result = TransactionResults.FAILURE;
                }
                else {
                    summary.code = GmoErrorMap.INTERNAL_ERROR_CODE;
                    summary.result = TransactionResults.FAILURE;
                }

                summary.payType = result.channel;
                summary.totalFee = result.totalFee;
                summary.payTime = replacePayTime(result.payTime);
                summary.wallet = result.wallet;
            }
        } catch (HttpStatusException e) {
            Timber.e("HttpStatusCode: %s", e.getStatusCode());
//            summary.code = GmoErrorMap.INTERNAL_ERROR_CODE;
            summary.code = _app.getString(R.string.error_type_qr_3324) + "@@@" + e.getStatusCode() + "@@@";
            // ステータスコードがおかしい場合は明らかに取引失敗してるので状態確認しない
            summary.result = TransactionResults.FAILURE;
        } catch (SocketTimeoutException e) {
            Timber.e(e);
            summary.result = TransactionResults.UNKNOWN;
        } catch (IOException e) {
            Timber.e(e);
            // 結果が不明なので状態確認する
            summary.result = TransactionResults.UNKNOWN;
        }

        final Date date = new Date();
        summary.endTime = date.getTime();
        summary.procTime = (int) (summary.endTime - summary.startTime);

        if (summary.payTime == null) {
            summary.payTime = createPayTime(date);
        }

        return summary;
    }

    private ResultSummary checkRefund(Map<String, String> header, ResultSummary summary) {
        if (summary.resultCheckCount >= MAX_RESULT_CHECK_COUNT) {
            setSummaryParams(summary);
            return summary;
        }

        summary.resultCheckCount++;

        try {
            Thread.sleep(POLLING_CYCLE_MS);
        } catch (InterruptedException e) {
            Timber.e(e);
        }

        try {
            final GetCheckRefunds.Request request = new GetCheckRefunds.Request();

            request.storeRefundId = summary.refundId;
            request.storeOrderId = summary.orderId;

            Timber.d("QR返金結果確認開始 返金伝票番号:%s, 対象支払伝票番号: %s", summary.refundId, summary.orderId);
            GetCheckRefunds.Response response = _apiClient.getCheckRefunds(header, request.toMap());
            Timber.d("response: %s", _gson.toJson(response));

            if (response.msgSummaryCode != null) {
                summary.code = GmoErrorMap.get(response.msgSummaryCode);
                summary.result = TransactionResults.FAILURE;
                Timber.tag("QR").e(response.returnMessage);
            } else if(response.result == null) {
                summary.code = GmoErrorMap.INTERNAL_ERROR_CODE;
                summary.result = TransactionResults.FAILURE;
            } else {
                final GetCheckRefunds.Result result = response.result;

                if (!result.returnCode.equals(ReturnCodes.SUCCESS)) {
                    summary.code = GmoErrorMap.INTERNAL_ERROR_CODE;
                    summary.result = TransactionResults.FAILURE;
                }
                else if (result.resultCode.equals(ResultCodes.CHK_FAILED)) {
                        summary.code = _app.getString(R.string.error_type_qr_3316);
                        summary.result = TransactionResults.FAILURE;
                } else {
                    // "SUCCESS" or "FINISHED":返金完了／"WAITING":返金待ち／前記以外：返金失敗
                    if (result.resultCode.equals(ResultCodes.SUCCESS) || result.resultCode.equals(ResultCodes.FINISHED)) {
                        summary.result = TransactionResults.SUCCESS;
                        summary.code = null;
                    }
                    else if (result.resultCode.equals(ResultCodes.WAITING))  {
                        return checkRefund(header, summary);
                    }
                    else {
                        summary.code = _app.getString(R.string.error_type_qr_3316);
                        summary.result = TransactionResults.FAILURE;
                    }

                    summary.payType = result.channel;
                    summary.totalFee = result.totalFee;
                }
                summary.wallet = result.wallet;
            }
        } catch (HttpStatusException e) {
            Timber.e("HttpStatusCode: %s", e.getStatusCode());
//            summary.code = GmoErrorMap.INTERNAL_ERROR_CODE;
            summary.code = _app.getString(R.string.error_type_qr_3324) + "@@@" + e.getStatusCode() + "@@@";
            // ステータスコードがおかしい場合は明らかに取引失敗してるので状態確認しない
            summary.result = TransactionResults.FAILURE;
        } catch (SocketTimeoutException e) {
            Timber.e(e);
            summary.result = TransactionResults.UNKNOWN;
        } catch (IOException e) {
            Timber.e(e);
            // 結果が不明なので状態確認する
            summary.result = TransactionResults.UNKNOWN;
        }

        final Date date = new Date();
        summary.endTime = date.getTime();
        summary.procTime = (int) (summary.endTime - summary.startTime);

        if (summary.payTime == null) {
            summary.payTime = createPayTime(date);
        }

        return summary;
    }

    // 支払伝票番号 or 返金伝票番号の生成
    private String makeSlipId() {
        final SimpleDateFormat df = new SimpleDateFormat("ddHHmmss", Locale.JAPANESE);
        final String termId = AppPreference.getMcTermId();

        final String slipId =
                termId.substring(termId.length() - 8) + df.format(new Date()) + getTermSequence();

        return slipId;
    }

    private void saveHeaderParams(String credentialKey) {
        final String loginId = AppPreference.getQrUserId();
        final String time = String.valueOf(new Date().getTime());
        final String nonceStr = McUtils.generateRandomString(15);

        /*
            ログインID、ミリ秒で表される現在の時間、ランダム文字列、認証キーを"&"で連結した文字列の
            sha256ハッシュ値の16進数表記文字列を小文字に変換
         */
        final String plainText = String.format("%s&%s&%s&%s", loginId, time, nonceStr, credentialKey);
        String sign = "";
        try {
            final MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            sign = McUtils.bytesToHexString(sha256.digest(plainText.getBytes())).toLowerCase();
        } catch (NoSuchAlgorithmException e) {
            Timber.e(e);
        }

        AppPreference.setGmoHeaderTime(time);
        AppPreference.setGmoHeaderNonceStr(nonceStr);
        AppPreference.setGmoHeaderSign(sign);
    }

    private Map<String, String> loadHeaderParams() {
        final String loginId = AppPreference.getQrUserId();
        final String time = AppPreference.getGmoHeaderTime();
        final String nonceStr = AppPreference.getGmoHeaderNonceStr();
        final String sign = AppPreference.getGmoHeaderSign();
        final String serialNo = String.valueOf(AppPreference.getMcCarId());

        final HashMap<String, String> header = new HashMap<>();

        header.put(HeaderFields.TIME, time);
        header.put(HeaderFields.NONCE_STR, nonceStr);
        header.put(HeaderFields.SIGN, sign);
        header.put(HeaderFields.LOGIN_ID, loginId);
        header.put(HeaderFields.SERIAL_NO, serialNo);

        return header;
    }

    private String getTermSequence() {
        int termSequence = AppPreference.getTermSequence();
            termSequence = termSequence < 999
                    ? termSequence + 1
                    : 1;

        String termSeqStr = String.format("0000%s", termSequence);
        return termSeqStr.substring(termSeqStr.length() - 4);
    }

    private String replacePayTime(String payTime) {
        SimpleDateFormat beforeDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.JAPANESE);
        SimpleDateFormat afterDF = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.JAPANESE);

        try {
            // すでにフォーマットされた状態ならそのまま返す
            afterDF.parse(payTime);
            return payTime;
        } catch (Exception ignore) { }

        try {
            return afterDF.format(Objects.requireNonNull(beforeDF.parse(payTime)));
        } catch (Exception e) {
            return null;
        }
    }

    private String createPayTime(Date date) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.JAPANESE);

        try {
            return df.format(date);
        } catch (Exception e) {
            return null;
        }
    }

    private void setSummaryParams(ResultSummary summary) {
        final Date date = new Date();
        summary.endTime = date.getTime();
        summary.procTime = (int) (summary.endTime - summary.startTime);

        if (summary.payTime == null) {
            summary.payTime = createPayTime(date);
        }
    }

    private ResultSummary demoPayment() {
        Timber.d("QRデモ決済");
        final ResultSummary summary = new ResultSummary();
        summary.result = TransactionResults.SUCCESS;
        summary.orderId = makeSlipId();
        summary.payType = QRPayTypeCodes.PayPay;
        summary.payTime = createPayTime(new Date());
        summary.totalFee = String.valueOf(Amount.getFixedAmount());
        summary.code = null;
        summary.procTime = 2000;

        // 2秒くらいUIを処理中画面にするためスリープを入れる
        try { Thread.sleep(2000); } catch (InterruptedException ignore) {}
        return summary;
    }

    private ResultSummary demoRefund(String orderId,int fee) {
        Timber.d("QRデモ返金");
        final ResultSummary summary = new ResultSummary();
        summary.result = TransactionResults.SUCCESS;
        summary.orderId = orderId;
        summary.refundId = makeSlipId();
        summary.payType = QRPayTypeCodes.PayPay;
        summary.payTime = createPayTime(new Date());
        summary.totalFee = String.valueOf(fee);
        summary.code = null;
        summary.procTime = 2000;

        // 2秒くらいUIを処理中画面にするためスリープを入れる
        try { Thread.sleep(2000); } catch (InterruptedException ignore) {}
        return summary;
    }
}
