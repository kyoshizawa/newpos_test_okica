package jp.mcapps.android.multi_payment_terminal.model;

import android.text.TextUtils;
import com.google.gson.Gson;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.data.OptionService;
import jp.mcapps.android.multi_payment_terminal.database.DBManager;
import jp.mcapps.android.multi_payment_terminal.database.LocalDatabase;
import jp.mcapps.android.multi_payment_terminal.database.history.slip.SlipDao;
import jp.mcapps.android.multi_payment_terminal.database.history.slip.SlipData;
import jp.mcapps.android.multi_payment_terminal.database.history.uri.UriDao;
import jp.mcapps.android.multi_payment_terminal.database.history.uri.UriData;
import jp.mcapps.android.multi_payment_terminal.database.history.uriOkica.ISOUtil;
import jp.mcapps.android.multi_payment_terminal.database.history.uriOkica.UriOkicaDao;
import jp.mcapps.android.multi_payment_terminal.database.history.uriOkica.UriOkicaData;
import jp.mcapps.android.multi_payment_terminal.database.pos.TerminalDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.TerminalData;
import jp.mcapps.android.multi_payment_terminal.error.McPosCenterErrorCodes;
import jp.mcapps.android.multi_payment_terminal.error.McPosCenterErrorMap;
//import jp.mcapps.android.multi_payment_terminal.thread.emv.ParamManage;
//import jp.mcapps.android.multi_payment_terminal.thread.emv.utils.ISOUtil;
import jp.mcapps.android.multi_payment_terminal.webapi.HttpStatusException;
import jp.mcapps.android.multi_payment_terminal.webapi.grpc.McOkicaCenterApiImpl;
import jp.mcapps.android.multi_payment_terminal.webapi.grpc.data.SendDtl;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.McPosCenterApi;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.McPosCenterApiImpl;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.Car;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.Driver;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.Echo;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.PostTerminalInfo;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.TerminalInfo;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.payment.Payment;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.payment.RequestCr;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.payment.RequestEd;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.payment.RequestId;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.payment.RequestJr;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.payment.RequestNn;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.payment.RequestQp;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.payment.RequestQr;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.payment.RequestWn;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.payment.ResultInfo;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.TicketSalesApi;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.TicketSalesApiImpl;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.TicketSalesStatusException;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data.TicketPurchasedCancel;
import timber.log.Timber;

public class McTerminal {
    private static final int MAX_RETRY_COUNT = 3;
    private static final int OKICA_DETAIL_SIZE = 192;
    private McPosCenterApi _apiClient = new McPosCenterApiImpl();
    private McOkicaCenterApiImpl _apiOkicaClient = new McOkicaCenterApiImpl();
    private static boolean postOkicaPaymentRunning = false;

    public String echo() {
        String errCode = null;
        try {
            Echo.Response echoResponse = _apiClient.echo(AppPreference.isDetachJR(), AppPreference.isDetachQR());
            AppPreference.execMcEcho(); //echo実行フラグON
            //resultがfalseの場合は無視する
            if (echoResponse.result) {
                AppPreference.setIsAvailable(echoResponse.useable); //利用許可状態をRAMに保持
                if (!echoResponse.useable) {
                    //利用不可検知
                    errCode = MainApplication.getInstance().getString(R.string.error_type_not_available);
                }
            }
        } catch (UnknownHostException | SocketTimeoutException e) {
            Timber.e(e);
            // エラー表示は行わない
        } catch (IOException | IllegalStateException e) {
            Timber.e(e);
            errCode = McPosCenterErrorMap.INTERNAL_ERROR_CODE;
        } catch (HttpStatusException e) {
            Timber.d("echo HttpStatusCode: %s", e.getStatusCode());
            errCode = McPosCenterErrorMap.INTERNAL_ERROR_CODE;
        }

        return errCode;
    }

    public String getTerminalInfo() {
        String errCode = null;
        try {
            final TerminalInfo.Response response = _apiClient.getTerminalInfo();
            if (!response.result) {
                //errCode = response.errorCode;
                errCode = McPosCenterErrorMap.get(response.errorCode);
            } else {
                // アクティベーションIDが変わっていたら証明書は削除する
                if (!response.activateId.equals(AppPreference.getJremActivateId())) {
//                    Timber.i("JREMアクティベーションID変更 before: %s, after: %s",
//                            AppPreference.getJremActivateId(), response.activateId);
//
//                    new JremActivator().removeCertFile();
                }
                Timber.d("terminalInfo: %s", new Gson().toJson(response));
                AppPreference.save(response);
                setOptionService(response);
//                ParamManage pm = new ParamManage();
//                pm.saveTerminalNo(AppPreference.getMcTermId());
            }
        } catch (UnknownHostException | SocketTimeoutException e) {
            Timber.e(e);
            errCode = MainApplication.getInstance().getString(R.string.error_type_startup_comm_reception);
        } catch (IOException | IllegalStateException e) {
            Timber.e(e);
            errCode = McPosCenterErrorMap.INTERNAL_ERROR_CODE;
        } catch (HttpStatusException e) {
            Timber.d("getTerminalInfo HttpStatusCode: %s", e.getStatusCode());
            errCode = McPosCenterErrorMap.INTERNAL_ERROR_CODE;
        }

        return errCode;
    }

    //type：送信種別
    //      0：電マネ開局(自動/手動)
    //      1：業務終了
    public String postTerminalInfo(int type) {
        String errCode = null;
        boolean result = false;

        Timber.i("端末稼働情報連携実行");
        try {
            final PostTerminalInfo.Response response = _apiClient.postTerminalInfo(type);
            if (!response.result) {
                errCode = response.errorCode;
            } else {
                result = true;
            }
        } catch (UnknownHostException | SocketTimeoutException e) {
            Timber.e(e);
            // エラー表示は行わない
        } catch (IOException | IllegalStateException e) {
            Timber.e(e);
            errCode = McPosCenterErrorMap.INTERNAL_ERROR_CODE;
        } catch (HttpStatusException e) {
            Timber.d("getDriver HttpStatusCode: %s", e.getStatusCode());
            errCode = McPosCenterErrorMap.INTERNAL_ERROR_CODE;
        }

        if (result == true) {
            Timber.i("端末稼働情報連携成功");
        } else {
            Timber.e("端末稼働情報連携失敗(エラーコード:%s)", errCode);
        }
        return errCode;
    }


    public String getDriver(int driverCode, boolean update, String driverName) {
        String errCode = null;

        try {
            final Driver.Response response = _apiClient.getDriver(driverCode, update, driverName);
            if (!response.result) {
                errCode = response.errorCode;
            } else {
                AppPreference.setDriverCode(String.valueOf(driverCode));
                AppPreference.setDriverName(response.driverName);
            }
        } catch (UnknownHostException | SocketTimeoutException e) {
            Timber.e(e);
            errCode = MainApplication.getInstance().getString(R.string.error_type_comm_reception);
        } catch (IOException | IllegalStateException e) {
            Timber.e(e);
            errCode = McPosCenterErrorMap.INTERNAL_ERROR_CODE;
        } catch (HttpStatusException e) {
            Timber.d("getDriver HttpStatusCode: %s", e.getStatusCode());
            errCode = McPosCenterErrorMap.INTERNAL_ERROR_CODE;
        }

        return errCode;
    }

    public String setCar(int carId) {
        String errCode = null;

        try {
            final Car.Response response = _apiClient.setCar(carId);
            if (!response.result) {
                errCode = response.errorCode;
            } else {
                AppPreference.setMcCarId(carId);
            }
        } catch (UnknownHostException | SocketTimeoutException e) {
            Timber.e(e);
            errCode = MainApplication.getInstance().getString(R.string.error_type_comm_reception);
        } catch (IOException | IllegalStateException e) {
            Timber.e(e);
            errCode = McPosCenterErrorMap.INTERNAL_ERROR_CODE;
        } catch (HttpStatusException e) {
            Timber.d("getDriver HttpStatusCode: %s", e.getStatusCode());
            errCode = McPosCenterErrorMap.INTERNAL_ERROR_CODE;
        }

        return errCode;
    }

    public String postPayment() {
        Timber.i("売上情報送信実行");
        UriDao dao = DBManager.getUriDao();
        List<UriData> uriDataList = dao.getUnsentData();

        if (uriDataList == null || uriDataList.size() == 0) {
            //売上データなし エラーではない
            Timber.i("売上情報なし(未送信件数:0)");
            return null;
        }

        final SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.JAPANESE);
        final SimpleDateFormat parseFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.JAPANESE);
        try {
            //10件毎に分割して送信。失敗した場合は2回までリトライ
            for (int i = 0; i < MAX_RETRY_COUNT; i++) {
                List<UriData> retryList = new ArrayList<>();
                Timber.i("売上情報送信：%d回目", i + 1);

                int postCnt = (int) Math.ceil(uriDataList.size() / 10.0);
                for (int j = 0; j < postCnt; j++) {
                    Timber.i("分割送信：%d回目", j + 1);
                    //subListは部分ビュー、newでディープコピー
                    List<UriData> splitList = j == postCnt - 1
                            ? new ArrayList<>(uriDataList.subList(j * 10, uriDataList.size()))
                            : new ArrayList<>(uriDataList.subList(j * 10, (j * 10) + 10));

                    Payment.Request request = makePaymentRequest(splitList);
                    if (request == null) {
                        Timber.e("未対応の売上情報発生");
                        continue;
                    }

                    Payment.Response response = _apiClient.postPayment(request);
                    if (!response.result) {
                        //処理失敗
                        Timber.e("売上情報送信失敗：エラーコード%s", response.errorCode);
                    } else {
                        //各ブランドの結果を結合
                        List<ResultInfo> resultInfoList = new ArrayList<>();
                        resultInfoList.addAll(response.cr);
                        resultInfoList.addAll(response.jr);
                        resultInfoList.addAll(response.qp);
                        resultInfoList.addAll(response.iD);
                        resultInfoList.addAll(response.ed);
                        resultInfoList.addAll(response.wn);
                        resultInfoList.addAll(response.nn);
                        resultInfoList.addAll(response.up);
                        resultInfoList.addAll(response.qr);

                        for (ResultInfo result : resultInfoList) {
                            //成功：売上データ削除 失敗：一件ごとの送信に回すためここでは処理しない
                            if (result.tranResult) {
                                try {
                                    // yyyyMMddHHmmss -> yyyy/MM/dd HH:mm:ss に変換
                                    Date paymentDate = parseFormat.parse(result.paymentDateTime);
                                    if (paymentDate != null) {
                                        String paymentDateString = dateFmt.format(paymentDate);
                                        dao.posSendCompleted(paymentDateString, result.procNo);
                                        splitList.remove(checkUriData(splitList, paymentDateString, result.procNo));  //送信成功した売上データを送信対象から除外
                                    } else {
                                        //日時のフォーマットが異常な状態。送信成功かつフォーマット異常はない想定
                                        Timber.e("payment date is empty");
                                    }
                                } catch (ParseException e) {
                                    //日時のフォーマットが異常な状態。送信成功かつフォーマット異常はない想定
                                    Timber.e("parse error : %s", result.paymentDateTime);
                                }
                            }
                        }
                    }
                    retryList.addAll(splitList); //送信失敗した売上データをリトライ対象に追加
                }

                if (retryList.size() == 0) {
                    //全て送信成功した場合
                    Timber.i("売上情報送信成功");
                    return null;
                }

                uriDataList = new ArrayList<>(retryList); //そのまま代入するとシャローコピーになる
                Thread.sleep(1000); //送信間隔は1秒空ける
            }

            //失敗した売上を1件ずつ送信
            String errCode = null;
            for (UriData uriData : uriDataList) {
                Payment.Request requestSinglePayment = makePaymentRequest(Collections.singletonList(uriData));
                if (requestSinglePayment == null) {
                    Timber.e("未対応の売上情報発生");
                    continue;
                }

                Payment.Response responseSinglePayment = _apiClient.postPayment(requestSinglePayment);
                String err = confirmSinglePaymentResponse(responseSinglePayment, uriData);
                if (err == null) {
                    dao.posSendCompleted(uriData.transDate, uriData.termSequence);
                } else {
                    Timber.e("売上情報送信失敗：エラーコード %s", err);
                    errCode = err;
                    Thread.sleep(1000); //送信間隔は1秒空ける

                    //1件毎でも失敗した売上を異常売上として送信
                    Timber.i("異常売上情報送信実行");
                    Payment.Response responseInvalidPayment = _apiClient.postInvalidPayment(requestSinglePayment);
                    String invalidErr = confirmSinglePaymentResponse(responseInvalidPayment, uriData);
                    if (invalidErr == null) {
                        Timber.i("異常売上情報送信成功");
                        dao.posSendCompleted(uriData.transDate, uriData.termSequence);
                    } else {
                        //異常売上送信では異常応答は返ってこない想定
                        Timber.e("異常売上情報送信失敗：エラーコード %s", invalidErr);
                    }
                }

                Thread.sleep(1000); //送信間隔は1秒空ける
            }

            Timber.i("未送信売上件数：%d", dao.getUnsentData().size());
            return errCode;
        } catch(IOException | HttpStatusException | InterruptedException | IllegalStateException e){
            Timber.e("post payment failed : %s", e.getMessage());
            Timber.e("未送信売上件数：%d", dao.getUnsentData().size());
            return McPosCenterErrorMap.INTERNAL_ERROR_CODE;
        }
    }

    private Payment.Request makePaymentRequest(List<UriData> uriDataList) {
        MainApplication app = MainApplication.getInstance();
        UriDao dao = DBManager.getUriDao();
        Payment.Request request = new Payment.Request();
        int cnt = 0;

        List<UriData> removeList = new ArrayList<>();
        for (UriData uriData : uriDataList) {
            if (uriData.transBrand == null) {
                Timber.e("ブランド名なし");
                removeList.add(uriData);
                continue;
            }

            if (uriData.transBrand.equals(app.getString(R.string.money_brand_credit))) {
                cnt++;
                request.cr.add(new RequestCr(uriData));
            } else if (uriData.transBrand.equals(app.getString(R.string.money_brand_suica))) {
                cnt++;
                request.jr.add(new RequestJr(uriData));
            } else if (uriData.transBrand.equals(app.getString(R.string.money_brand_nanaco))) {
                cnt++;
                request.nn.add(new RequestNn(uriData));
            } else if (uriData.transBrand.equals(app.getString(R.string.money_brand_waon))) {
                cnt++;
                request.wn.add(new RequestWn(uriData));
            } else if (uriData.transBrand.equals(app.getString(R.string.money_brand_edy))) {
                cnt++;
                request.ed.add(new RequestEd(uriData));
            } else if (uriData.transBrand.equals(app.getString(R.string.money_brand_id))) {
                cnt++;
                request.id.add(new RequestId(uriData));
            } else if (uriData.transBrand.equals(app.getString(R.string.money_brand_qp))) {
                cnt++;
                request.qp.add(new RequestQp(uriData));
            } else if (uriData.transBrand.equals(app.getString(R.string.money_brand_codetrans))) {
                cnt++;
                request.qr.add(new RequestQr(uriData));
            } else {
                // 銀聯、MCプリペイド、LANSはSTEP2対応
                Timber.e("未対応のブランド名：%s", uriData.transBrand);
                removeList.add(uriData);
            }
        }

        if (!removeList.isEmpty()) {
            //不正なブランド名を送信できるフォーマットがないため削除する。実運用では起こらない想定
            dao.deleteUriDataList(removeList);
            uriDataList.removeAll(removeList);
        }

        if (cnt == 0) {
            return null;    //送信対象データなし
        }

        return request;
    }

    private String confirmSinglePaymentResponse(Payment.Response response, UriData uriData) {
        if (!response.result) {
            return response.errorCode; //処理失敗
        } else {
            MainApplication app = MainApplication.getInstance();

            ResultInfo result = null;
            //一件ごとの送信なのでレスポンスも一件のみ
            if (uriData.transBrand.equals(app.getString(R.string.money_brand_credit))) {
                result = response.cr.get(0);
            } else if (uriData.transBrand.equals(app.getString(R.string.money_brand_suica))) {
                result = response.jr.get(0);
            } else if (uriData.transBrand.equals(app.getString(R.string.money_brand_qp))) {
                result = response.qp.get(0);
            } else if (uriData.transBrand.equals(app.getString(R.string.money_brand_id))) {
                result = response.iD.get(0);
            } else if (uriData.transBrand.equals(app.getString(R.string.money_brand_edy))) {
                result = response.ed.get(0);
            } else if (uriData.transBrand.equals(app.getString(R.string.money_brand_waon))) {
                result = response.wn.get(0);
            } else if (uriData.transBrand.equals(app.getString(R.string.money_brand_nanaco))) {
                result = response.nn.get(0);
            } else if (uriData.transBrand.equals(app.getString(R.string.print_brand_unionpay))) {
                result = response.up.get(0);
            } else if (uriData.transBrand.equals(app.getString(R.string.money_brand_codetrans))) {
                result = response.qr.get(0);
            }

            if (result != null && result.tranResult) {
                return null;
            } else {
                return McPosCenterErrorCodes.E0901;     // センターNG応答はE0901に集約
            }
        }
    }

    private UriData checkUriData(List<UriData>uriDataList, String date, int seq) {
        for (UriData uriData : uriDataList) {
            if (uriData.termSequence == seq && uriData.transDate.equals(date)) {
                return uriData;
            }
        }
        return null;
    }

    private void setOptionService(TerminalInfo.Response info) {
        if (info == null) {
            MainApplication.getInstance().setOptionService(null);
            return;
        }

        List<OptionService.Func> funcs = new ArrayList<>();

        if (info.optionServiceFuncs != null) {
            for (TerminalInfo.OptionServiceFunc f : info.optionServiceFuncs) {
                if (f.funcID == OptionService.FuncIds.MAGNETIC_CARD_VALIDATION) {
                    if (TextUtils.isEmpty(f.displayName)) {
                        String defaultDisplayName = "有効性確認";
                        Timber.i("有効性確認の表示名が空のためデフォルトを使用します '%s'", defaultDisplayName);
                        f.displayName = defaultDisplayName;
                    } else if (f.displayName.length() > 7) {
                        Timber.i("文字数オーバーのため8文字目以降を切り捨てます '%s'", f.displayName);
                        f.displayName = f.displayName.substring(0, 7);
                    }
                }
                funcs.add(new OptionService.Func(f.funcID, f.displayName));
            }
        }

        MainApplication.getInstance().setOptionService(
                new OptionService(info.optionServiceDomain, info.optionServiceKey, funcs.toArray(new OptionService.Func[0])));
    }

    synchronized public String postOkicaPayment() {
        if(postOkicaPaymentRunning == true) {
            Timber.i("OKICA売上情報送信実行中");
            return null;
        }
        postOkicaPaymentRunning = true;
        Timber.i("OKICA売上情報送信実行");
        UriOkicaDao dao = DBManager.getUriOkicaDao();
        List<UriOkicaData> uriOkicaDataList = dao.getUnsentData();

        if (uriOkicaDataList == null || uriOkicaDataList.size() == 0) {
            //売上データなし エラーではない
            Timber.i("OKICA売上情報なし(未送信件数:0)");
            postOkicaPaymentRunning = false;
            return null;
        }

        try {
            //10件毎に分割して送信。失敗した場合は2回までリトライ
            for (int i = 0; i < MAX_RETRY_COUNT; i++) {
                Timber.i("OKICA売上情報送信：%d回目", i + 1);

                int postCnt = (int) Math.ceil(uriOkicaDataList.size() / 10.0);
                for (int j = 0; j < postCnt; j++) {
                    Timber.i("OKICA分割送信：%d回目", j + 1);
                    //subListは部分ビュー、newでディープコピー
                    List<UriOkicaData> splitList = j == postCnt - 1
                            ? new ArrayList<>(uriOkicaDataList.subList(j * 10, uriOkicaDataList.size()))
                            : new ArrayList<>(uriOkicaDataList.subList(j * 10, (j * 10) + 10));

                    byte[] dtls = makeOkicaPaymentRequest(splitList);

                    SendDtl.Response response = _apiOkicaClient.sendDtl(dtls);
                    if (true != response.result) {
                        // 処理失敗
                        Timber.e("OKICA売上情報送信失敗：エラーコード %s", response.errorCode);
                    } else {
                        // 送信成功した売上データを削除
                        for (UriOkicaData uriOkicaData : splitList) {
                            dao.setOkicaSent(uriOkicaData.okicaTransDate, uriOkicaData.okicaSequence);
                            dao.posSendCompleted(uriOkicaData.okicaTransDate, uriOkicaData.okicaSequence);
                        }
                    }
                }

                uriOkicaDataList = dao.getUnsentData();
                if (uriOkicaDataList == null || uriOkicaDataList.size() == 0) {
                    //全て送信成功した場合
                    Timber.i("OKICA売上情報送信成功");
                    postOkicaPaymentRunning = false;
                    return null;
                }
                Thread.sleep(1000); //送信間隔は1秒空ける
            }

            Timber.i("OKICA未送信売上件数：%d", dao.getUnsentData().size());
            postOkicaPaymentRunning = false;
            return null;
        } catch(HttpStatusException | InterruptedException | IllegalStateException e){
            Timber.e("post OKICA payment failed : %s", e.getMessage());
            Timber.e("OKICA未送信売上件数：%d", dao.getUnsentData().size());
            postOkicaPaymentRunning = false;
            return McPosCenterErrorMap.INTERNAL_ERROR_CODE;
        }
    }

    public static byte[] makeOkicaPaymentRequest(List<UriOkicaData> uriDataList) {
        byte[] detail = new byte[OKICA_DETAIL_SIZE * uriDataList.size()];  // 0で初期化される
        int pos = 0;    // データ設定位置

        for (UriOkicaData uriOkicaData : uriDataList) {
            // 売上データからIDiを取得してdetailにコピー
            System.arraycopy(uriOkicaData.okicaCardIdi, 0, detail, pos, uriOkicaData.okicaCardIdi.length);
            pos += uriOkicaData.okicaCardIdi.length;

            // リサイクルID（未使用0x00固定）
            pos++;

            // 一件明細レビジョン（物販共通0x33固定）
            detail[pos] = (byte) 0x33;
            pos++;

            // 設定ビットマスク
            if (uriOkicaData.okicaProcessCode == 0x2100 || uriOkicaData.okicaProcessCode == 0x2110) {
                // ネガヒット時
                byte[] setBitMask = {(byte)0xFF, (byte)0xFE, 0x09, 0x00, 0x40, 0x00, 0x00, 0x00};
                System.arraycopy(setBitMask, 0, detail, pos, 8);
            } else {
                byte[] setBitMask = {(byte) 0xFF, (byte) 0xFE, 0x7F, (byte) 0x80, 0x40, 0x00, 0x00, 0x00};
                System.arraycopy(setBitMask, 0, detail, pos, 8);
            }
            pos += 8;

            // 売上データから一件明細IDを取り出してコピー
            short detailId = uriOkicaData.okicaDetailId.shortValue();
            detail[pos] = (byte) (detailId >> 8);
            detail[pos + 1] = (byte) detailId;
            pos += 2;

            // 売上データから処理コードを取り出してコピー
            short processCode = uriOkicaData.okicaProcessCode.shortValue();
            detail[pos] = (byte) (processCode >> 8);
            detail[pos + 1] = (byte) processCode;
            pos += 2;

            // 売上データから年月日と日時を取り出してBCDに変換して設定
            ISOUtil.str2bcd(uriOkicaData.okicaTransDate, false, detail, pos);
            pos += 7;

            // 売上データから機能種別を取り出してコピー
            short functionType = uriOkicaData.okicaFunctionType.shortValue();
            detail[pos] = (byte) (functionType >> 8);
            detail[pos + 1] = (byte) functionType;
            pos += 2;

            // 売上データからカード制御コードを取り出してコピー
            detail[pos] = uriOkicaData.okicaControlCode.byteValue();
            pos++;

            // 売上データから処理未了フラグを取り出してコピー
            detail[pos] = uriOkicaData.okicaUnfinishedFlg.byteValue();
            pos++;

            // 売上データから機器事業者コードを取り出してコピー
            detail[pos] = (byte) (0xFF & (uriOkicaData.okicaBusinessCode >> 8));
            detail[pos + 1] = (byte) (0xFF & uriOkicaData.okicaBusinessCode);
            pos += 2;

            // 売上データから機種コードを取り出してコピー
            ISOUtil.str2bcd(uriOkicaData.okicaModelCode, false, detail, pos);
            pos++;

            // 売上データから機器IDを取り出してコピー
            //byte[] mid = getMachineId(uriOkicaData.okicaMachineId);
            System.arraycopy(getMachineId(uriOkicaData.okicaMachineId), 0, detail, pos, 4);
            pos += 4;

            // 売上データからIC取扱通番を取り出してコピー
            short sequence = uriOkicaData.okicaSequence.shortValue();
            detail[pos] = (byte) (sequence >> 8);
            detail[pos + 1] = (byte) sequence;
            pos += 2;

            // 利用駅1, 利用駅2は0固定
            pos += (2 + 2);

            // 利用駅1・2地域識別コード（0固定）
            pos++;

            // 売上データから処理種別を取り出してコピー
            detail[pos] = uriOkicaData.okicaProcessType.byteValue();
            pos++;

            // 利用駅種別（未使用なので0x00固定）
            pos++;

            // 売上データからSFログIDを取り出してコピー
            short sfLogId = uriOkicaData.okicaSfLogId.shortValue();
            detail[pos] = (byte) (sfLogId >> 8);
            detail[pos + 1] = (byte) sfLogId;
            pos += 2;

            // 売上データから事業者コード（SF1）を取り出してコピー
            // 最新の積み増し事業者なのでチャージ以外はカード読み取り情報になる点に注意
            short sf1BusinessId = uriOkicaData.okicaSf1BusinessId.shortValue();
            detail[pos] = (byte) (sf1BusinessId >> 8);
            detail[pos + 1] = (byte) sf1BusinessId;
            pos += 2;

            // 売上データから利用金額（SF1）を取り出して設定
            int sf1Amount = uriOkicaData.okicaSf1Amount;
            detail[pos] = (byte) (sf1Amount >> 16);
            detail[pos + 1] = (byte) (sf1Amount >> 8);
            detail[pos + 2] = (byte) sf1Amount;
            pos += 3;

            // 売上データから残額（SF1）を取り出して設定
            int sf1Balance = uriOkicaData.okicaSf1Balance;
            detail[pos] = (byte) (sf1Balance >> 16);
            detail[pos + 1] = (byte) (sf1Balance >> 8);
            detail[pos + 2] = (byte) sf1Balance;
            pos += 3;

            // 売上データから入金区分（SF1）を取り出して設定
            detail[pos] = uriOkicaData.okicaSf1Category.byteValue();
            pos++;

            // チェックサム（0x00を加算するのは無駄なので、ここまでで計算しておく）
            int sum = 0;
            for (int index = 0; index < pos; index++) {
                sum += Byte.toUnsignedInt(detail[index]);
            }

            // デポジット額～予備（業務情報）は設定対象外なので0x00固定
            pos += (2 + 1 + 8 + 1 + 8 + 6 + 3 + 37);    // 各フィールドのバイト数を加算

            // チェックサムを設定
            detail[pos] = (byte) (sum >> 8);
            pos++;
            detail[pos] = (byte) sum;
            pos++;

            // 項目設定情報～予備は0x00固定
            pos += (4 + 1 + 1 + 58);    // 各フィールドのバイト数を加算
        }
        return detail;
    }

    // 売上データから機器IDを取り出す
    public static byte[] getMachineId(String machineId) {
        byte[] rtn = new byte[4];
        if (machineId.length() == 10) {
            int mid = 0;
            // 10進数（3桁）
            try {
                mid = Integer.valueOf(machineId.substring(0, 3));
                rtn[0] = (byte)mid;
            } catch (NumberFormatException nfe) {
                Timber.w("OKICA一件明細 機器ID異常1");
            }
            // 10進数（3桁）
            try {
                mid = Integer.valueOf(machineId.substring(3, 6));
                rtn[1] = (byte)mid;
            } catch (NumberFormatException nfe) {
                Timber.w("OKICA一件明細 機器ID異常2");
            }

            // 16進数（1桁） + BCD（1桁）
            byte[] midB = new byte[2];
            mid = Integer.parseInt(machineId.substring(6, 7), 16);
            midB[0] = (byte)(mid);
            ISOUtil.str2bcd(machineId.substring(7, 8), false, midB, 1);
            rtn[2] = (byte)((midB[0] & 0x0F) << 4 | (midB[1] & 0xF0) >> 4);

            // BCD（2桁）
            ISOUtil.str2bcd(machineId.substring(8, 10), false, rtn, 3);
        }
        return rtn;
    }

    public String postTicketCancel() {

        SlipDao dao = DBManager.getSlipDao();
        List<SlipData> slipDataList = dao.getUnsentCancelPurchasedTicketData();

        Timber.i("チケット購入取消実行：%s件",slipDataList.size());

        final TicketSalesApi ticketSalesApiClient = TicketSalesApiImpl.getInstance();
        final TerminalDao terminalDao = LocalDatabase.getInstance().terminalDao();
        final TerminalData terminalData = terminalDao.getTerminal();

        try {
            //1件ずつ送信。失敗した場合は次回実施。その場でのリトライはなし。
            for (SlipData data : slipDataList) {

                if (data.purchasedTicketDealId == null || data.purchasedTicketDealId.equals("")) {
                    Timber.e("チケット購入取消：id=%s", data.purchasedTicketDealId);
                    // チケット購入IDが存在しない場合は、送信せずに送信済みとして扱う
                    dao.updateSentCancelPurchasedTicketData(data.id);
                    continue;
                } else {
                    Timber.i("チケット購入取消：id=%s", data.purchasedTicketDealId);
                }

                try {
                    // ABTに取消実施
                    TicketPurchasedCancel.Response cancelResponse = ticketSalesApiClient.TicketPurchasedCancel(terminalData.service_instance_abt, data.purchasedTicketDealId);
                    if (cancelResponse != null && cancelResponse.error != null) {
                        Timber.e("チケット購入の取消応答エラー（ErrorCode:%s ErrorMessage:%s）", cancelResponse.error.code, cancelResponse.error.message);
                    }
                    // チケット購入の取消を送信済み更新
                    dao.updateSentCancelPurchasedTicketData(data.id);
                } catch(TicketSalesStatusException e){
                    Timber.e("post ticket cancel failed : %s", e.getCode() + " " + e.getMessage());
                    // エラー応答のものも送信済みとして扱う
                    dao.updateSentCancelPurchasedTicketData(data.id);
                    // 次のものを送信するためエラー終了はしない
//                    return MainApplication.getInstance().getString(R.string.error_type_ticket_8097) + "@@@" + e.getCode().toString() + "@@@";
                } catch(IOException | HttpStatusException | IllegalStateException e){
                    Timber.e(e);
                    // その他エラーが発生した場合は送信自体を中止。フラグも更新しない。
                    return MainApplication.getInstance().getString(R.string.error_type_ticket_8098);
                }
                Thread.sleep(1000); //送信間隔は1秒空ける
            }
        } catch(Exception e){
            Timber.e("post payment failed : %s", e.getMessage());
            Timber.e("未送信件数：%d", dao.getUnsentCancelPurchasedTicketData().size());
            // その他エラーが発生した場合は送信自体を中止。フラグも更新しない。
            return MainApplication.getInstance().getString(R.string.error_type_ticket_8098);
        }

        return null;
    }
}