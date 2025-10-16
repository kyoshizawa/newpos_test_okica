package jp.mcapps.android.multi_payment_terminal.model;

import com.amazonaws.services.cognitoidentityprovider.model.ExpiredCodeException;
import com.google.gson.Gson;
import com.pos.device.SDKException;
import com.pos.device.magcard.MagCardCallback;
import com.pos.device.magcard.MagCardReader;
import com.pos.device.magcard.MagneticCard;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.data.Amount;
import jp.mcapps.android.multi_payment_terminal.data.OptionService;
import jp.mcapps.android.multi_payment_terminal.data.trans_param.SurveyParam;
import jp.mcapps.android.multi_payment_terminal.database.DBManager;
import jp.mcapps.android.multi_payment_terminal.database.validation_check.ValidationCheckDao;
import jp.mcapps.android.multi_payment_terminal.database.validation_check.ValidationCheckHistoryData;
import jp.mcapps.android.multi_payment_terminal.database.validation_check.ValidationCheckPaymentData;
import jp.mcapps.android.multi_payment_terminal.error.McException;
import jp.mcapps.android.multi_payment_terminal.error.OptionServiceErrorMap;
import jp.mcapps.android.multi_payment_terminal.util.Crypto;
import jp.mcapps.android.multi_payment_terminal.util.McUtils;
import jp.mcapps.android.multi_payment_terminal.webapi.validation_check.ValidationCheckApi;
import jp.mcapps.android.multi_payment_terminal.webapi.validation_check.ValidationCheckApiImpl;
import jp.mcapps.android.multi_payment_terminal.webapi.validation_check.data.CardValidation;
import jp.mcapps.android.multi_payment_terminal.webapi.validation_check.data.ProcessResult;
import timber.log.Timber;

// NOTE: POSセンター未開局状態でインスタンスを作ると例外が発生する
public class Validator {
    public static class ValidatorException extends Throwable {
        private final int _reason;
        public int getReason() {
            return _reason;
        }
        public ValidatorException(int reason) {
            _reason = reason;
        }
    }

    private final MagCardReader _magCardReader = MagCardReader.getInstance();
    private final MainApplication _app = MainApplication.getInstance();
    private final Gson _gson = new Gson();
    private final String _demo_validation_name = "有効性確認";
    private final String _demo_merchant_name = "ＭＣモバイルクリエイト";
    private final String _demo_merchant_office = "大分本社";
    private final String _demo_merchant_telnumber = "123-456-7890";
    private final String _demo_term_id = "3080600099999";
    private final Integer _demo_driver_id = 999;
    private final String _demo_validation_id = "12345678-90AB-CDEF-1234-0305E82C3301";
    private final Integer _demo_trans_amount = 100;
    private final String _demo_card_id = "123456789";
    private final Integer _demo_car_id = 1;

    private ValidationCheckApi _api = null; // デモモードでインスタンス生成させないためvalidate(), sendResult()時に生成

    public static class ValidationResult {
        @NonNull
        public CardValidation.Request req;
        public CardValidation.Response res;
        public Throwable ex;
    }

    public Single<String[]> searchCard() {
        return searchCard(30 * 1000);
    }

    public Single<String[]> searchCard(int timeoutMills) {
        Timber.d("start search card");
        return Single.create(emitter -> {
            _magCardReader.startSearchCard(timeoutMills, (result, magneticCard) -> {
                if (result == MagCardCallback.SUCCESS) {
                    if (!emitter.isDisposed()) {
                        emitter.onSuccess(new String[] {
                                magneticCard.getTrackInfos((MagneticCard.TRACK_1)).getData(),
                                magneticCard.getTrackInfos((MagneticCard.TRACK_2)).getData(),
                                magneticCard.getTrackInfos((MagneticCard.TRACK_3)).getData(),
                        });
                    }
                } else {
                    Timber.d("search card error. reason: %s", result);
                    if (!emitter.isDisposed()) {
                        emitter.onError(new ValidatorException(result));
                    }
                }
            });
        });
    }

    public void stopSearchCard() {
        try {
            if (_magCardReader != null) {
                _magCardReader.stopSearchCard();
            }
        } catch (SDKException e) {
            e.printStackTrace();
        }
    }

    public Single<ValidationResult> validate(String[] tracks) {
        return Single.create(emitter -> {
            final CardValidation.Request request = new CardValidation.Request();

            // 端末番号
            request.terminalNo = AppPreference.getMcTermId();
            // 車番
            request.carNo = AppPreference.getMcCarId();
            // 乗務員コード
            request.driverCd = AppPreference.getMcDriverId();

            // 有効性確認端末通番
            request.procNo = AppPreference.getNextValidationCheckTermSequence();
            // 操作日時
            SimpleDateFormat dateFmt = new SimpleDateFormat("yyyyMMddHHmmss", Locale.JAPANESE);
            request.operationTime = dateFmt.format(new Date());
            // 媒体種別 (磁気トラック情報: 1)
            request.mediaType = 1;

            // トラック情報
            final CardValidation.MediaInfo mediaInfo = new CardValidation.MediaInfo();
            mediaInfo.encryptType = 1;
            mediaInfo.encryptTrack1 = encrypt(tracks[0]);
            mediaInfo.encryptTrack2 = encrypt(tracks[1]);
            mediaInfo.encryptTrack3 = encrypt(tracks[2]);
            request.mediaInfo = mediaInfo;

            /* APIのリクエストには含めないがDBに保存する項目 */
            // 乗務員名
            request.driverName = AppPreference.getDriverName();
            // 金額
            request.fare = Amount.getFixedAmount();

            // 位置・ネットワーク情報
            SurveyParam surveyParam = new SurveyParam();
            surveyParam.setAntennaLevel();
            surveyParam.setLocation();
            request.termLatitude = surveyParam.termLatitude;
            request.termLongitude = surveyParam.termLongitude;
            request.termNetworkType = surveyParam.termNetworkType;
            request.termRadioLevel = surveyParam.termRadioLevel;

            try {
                if (_api == null) {
                    _api = new ValidationCheckApiImpl();
                }
                CardValidation.Response response = _api.cardValidation(request);

                Timber.d("validation result %s", _gson.toJson(response));

                final ValidationResult result = new ValidationResult() {
                    {
                        req = request;
                        res = response;
                        ex = null;
                    }
                };
                if (!emitter.isDisposed()) {
                    Timber.d("save validation data successful");
                    emitter.onSuccess(result);
                }

            } catch (Exception e) {
                Timber.d(e);
                final ValidationResult result = new ValidationResult() {
                    {
                        req = request;
                        res = null;
                        ex = e;
                    }
                };
                if (!emitter.isDisposed()) {
                    emitter.onSuccess(result);
                }
            }
        });
    }

    public Completable sendResult() {
        return Completable.create(emitter -> {
            ValidationCheckDao dao = DBManager.getValidationCheckDao();
            final List<ValidationCheckPaymentData> records = dao.getUnsentPaymentWithLimit(10);

            if (records.size() <= 0) {
                if (!emitter.isDisposed()) {
                    emitter.onComplete();
                }
                Timber.i("有効性確認結果なし(未送信件数:0)");
                return;
            }

            Timber.i("有効性確認結果送信: %s件", records.size());
            ProcessResult.Request request = new ProcessResult.Request();

            List<ProcessResult.RequestRecord> recordList = new ArrayList<>();

            for (ValidationCheckPaymentData record : records) {
                ProcessResult.RequestRecord one = new ProcessResult.RequestRecord();
                one.recordType = 1;
                one.terminalNo = record.termId;
                one.carNo = record.carId;
                one.driverCd = record.driverId;
                one.driverName = record.driverName;
                one.procNo = record.termSequence;
                one.operationTime = invertDate(record.operationDate);
                one.validationID = record.validationId;
                one.terminalResult = record.termResult == 0;
                one.fare = record.transAmount;

                if (record.termResult != 0) {
                    final String reason = String.format("000%s", record.termResult);
                    one.reason = reason.substring(reason.length() - 4);
                } else {
                    one.reason = "";
                }

                recordList.add(one);
            }
            request.records = recordList.toArray(new ProcessResult.RequestRecord[0]);

            try {
                if (_api == null) {
                    _api = new ValidationCheckApiImpl();
                }
                ProcessResult.Response response = _api.processResult(request);
                Timber.d("processResult %s", _gson.toJson(response));

                if (response.result) {
                    // センター側の処理が成功なら1件毎の成否にかかわらず削除する
                    for (ValidationCheckPaymentData record : records) {
                        dao.deletePaymentById(record.paymentId);
                    }
                    Timber.i("送信結果成功");
                } else {
                    if (!emitter.isDisposed()) {
                        emitter.onError(new McException(
                                OptionServiceErrorMap.get(response.errorCode),
                                response.errorCode));
                    }
                    Timber.e("送信結果失敗(エラーコード:%s)", response.errorCode);
                }

                if (!emitter.isDisposed()) {
                    emitter.onComplete();
                }

            } catch (Exception e) {
                Timber.d(e);
                if (!emitter.isDisposed()) {
                    emitter.onError(e);
                }
            }
        });
    }

    public String sendResultSync() {
        try {
            sendResult().subscribeOn(Schedulers.io()).timeout(30, TimeUnit.SECONDS).blockingAwait();
            return null;
        } catch (McException e) {
            return e.getMcErrorCode();
        } catch (Exception e) {
            return OptionServiceErrorMap.UNDEFINED_CONNECTION_ERROR_CODE;
        }
    }

    private String encrypt(String data) {
        if (data == null || data.equals("")) return "";

        final OptionService service = _app.getOptionService();

        if (service == null) {
            throw new IllegalStateException("option service is null");
        }

        final byte[] key = McUtils.hexStringToBytes(service.getServiceKey());
        final byte[] iv = new byte[16];
        Arrays.fill(iv, (byte) 0);

        return McUtils.bytesToHexString(Crypto.AES256.encrypt(data, key, iv));
    }

    private String getDecryptCardNo(CardValidation.MediaResultInfo mediaResultInfo) {
        if (mediaResultInfo == null || mediaResultInfo.encryptType == null || mediaResultInfo.encryptCardNo == null) {
            return "";
        }

        final int encryptType = mediaResultInfo.encryptType;
        final String encrypted = mediaResultInfo.encryptCardNo;

        final OptionService service = _app.getOptionService();

        if (encrypted.equals("")) {
            return "";
        }

        if (encryptType == 0) {
            return encrypted;
        }
        else if (encryptType == 1) {
            final byte[] key = McUtils.hexStringToBytes(service.getServiceKey());
            final byte[] iv = new byte[16];
            Arrays.fill(iv, (byte) 0);

            try {
                return new String(Crypto.AES256.decrypt(McUtils.hexStringToBytes(encrypted), key, iv));
            } catch (Exception ex) {
                return "";
            }
        } else {
            Timber.e("Unknown EncryptType %s", encryptType);
            return "";
        }
    }

    public Completable addRecord(@NonNull ValidationResult result, Boolean isDemo) {
        return Completable.create(emitter -> {
            ValidationCheckDao dao = DBManager.getValidationCheckDao();

            if (isDemo) {
                dao.addRecord(createDemoHistoryData());
            } else {
                dao.addRecord(createPaymentData(result), createHistoryData(result));
            }
            AppPreference.incrementValidationCheckTermSequence();

            if (!emitter.isDisposed()) {
                emitter.onComplete();
            }
        });
    }

    private ValidationCheckPaymentData createPaymentData(@NonNull ValidationResult result) {
        ValidationCheckPaymentData data = new ValidationCheckPaymentData();
        data.posSend = 0; //送信状態
        data.termId = result.req.terminalNo; //端末番号
        data.carId = result.req.carNo; //車番
        data.driverId = result.req.driverCd; //乗務員コード
        data.driverName = result.req.driverName; //乗務員名
        data.termSequence = result.req.procNo; //機器通番
        data.operationDate = invertDate(result.req.operationTime); //操作日時
        data.transAmount = result.req.fare; //取引金額
        data.termLatitude = result.req.termLatitude; //位置情報(緯度)
        data.termLongitude = result.req.termLongitude; //位置情報(経度)
        data.termNetworkType = result.req.termNetworkType; //ネットワーク種別
        data.termRadioLevel = result.req.termRadioLevel; //電波状況(レベル)
        data.termResult = getTermResult(result); //処理結果

        if (result.ex == null && result.res != null) {
            data.validationId = result.res.validationID; //有効性確認ID
            data.errorCode = result.res.errorCode; // エラーコード

            data.invalidReason = result.res.mediaResultInfo != null ? result.res.mediaResultInfo.reason : null;  //無効理由
        }

        return data;
    }

    private ValidationCheckHistoryData createHistoryData(@NonNull ValidationResult result) {
        final OptionService s = _app.getOptionService();

        ValidationCheckHistoryData data = new ValidationCheckHistoryData();
        data.validationName = s.getFunc(s.indexOfFunc(
                OptionService.FuncIds.MAGNETIC_CARD_VALIDATION)).getDisplayName();  //名称
        data.printFlg = 0;  //印刷可否
        data.printCnt = 0;  //印刷回数
        data.merchantName = AppPreference.getMerchantName();  //加盟店名
        data.merchantOffice = AppPreference.getMerchantOffice();  //加盟店営業所名
        data.merchantTelnumber = AppPreference.getMerchantTelnumber();  //加盟店電話番号
        data.termId = result.req.terminalNo; //端末番号
        data.driverId = result.req.driverCd; //乗務員コード
        data.termSequence = result.req.procNo; //機器通番
        data.operationDate = invertDate(result.req.operationTime); //操作日時
        data.transAmount = result.req.fare; //取引金額
        data.termResult = getTermResult(result); //処理結果

        if (result.ex == null && result.res != null) {
            data.validationId = result.res.validationID; //有効性確認ID
            data.errorCode = result.res.errorCode; // エラーコード
            data.encryptType = 1; //暗号化パターン
            data.cardId = getDecryptCardNo(result.res.mediaResultInfo); //カード番号
        }

        return data;
    }

    private ValidationCheckHistoryData createDemoHistoryData() {
        final OptionService s = _app.getOptionService();

        ValidationCheckHistoryData data = new ValidationCheckHistoryData();
        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyyMMddHHmmss", Locale.JAPANESE);

        data.validationName = _demo_validation_name;  //名称
        data.printFlg = 0;  //印刷可否
        data.printCnt = 0;  //印刷回数
        data.merchantName = _demo_merchant_name;  //加盟店名
        data.merchantOffice = _demo_merchant_office;  //加盟店営業所名
        data.merchantTelnumber = _demo_merchant_telnumber;  //加盟店電話番号
        data.termId = _demo_term_id; //端末番号
        data.driverId = _demo_driver_id; //乗務員コード
        data.termSequence = AppPreference.getNextValidationCheckTermSequence(); //機器通番
        data.operationDate = invertDate(dateFmt.format(new Date())); //操作日時
        data.transAmount = _demo_trans_amount; //取引金額
        data.termResult = 0; //処理結果
        data.validationId = _demo_validation_id; //有効性確認ID
        data.errorCode = ""; // エラーコード
        data.encryptType = 0; //暗号化パターン
        data.cardId = _demo_card_id; //カード番号

        return data;
    }

    private int getTermResult(ValidationResult result) {
        /*
            0: 成功
            1: 失敗
            2: 未確認(レスポンス異常)
            3: 未確認(通信タイムアウト)
         */

        if (result.ex != null || result.res == null) {
            return 3;
        }

        // この場合mediaResultInfoが返って来ないのでカード有効性確認の結果はわからない
        if (!result.res.result || result.res.mediaResultInfo == null) {
            return 2;
        }

        return result.res.mediaResultInfo.isValid ? 0 : 1;
    }

    // DBの日付文字列 <-> APIリクエストの日付文字列を入れ替える
    private String invertDate(String dateStr) {
        final SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy/MM/dd/ HH:mm:ss", Locale.JAPANESE);
        final SimpleDateFormat requestFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.JAPANESE);

        try {
            final Date date = dbFormat.parse(dateStr);
            if (date == null) throw new Exception();
            return requestFormat.format(date);
        } catch (Exception ignore) { }

        try {
            final Date date = requestFormat.parse(dateStr);
            if (date == null) throw new Exception();
            return dbFormat.format(date);
        } catch (Exception ignore) { }

        throw new IllegalArgumentException(String.format("Not allowed format '%s'", dateStr));
    }
}
