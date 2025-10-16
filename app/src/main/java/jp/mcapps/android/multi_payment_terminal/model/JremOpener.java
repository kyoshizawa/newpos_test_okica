package jp.mcapps.android.multi_payment_terminal.model;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.google.gson.Gson;

import org.json.JSONException;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.data.EmoneyOpeningInfo;
import jp.mcapps.android.multi_payment_terminal.database.LocalDatabase;
import jp.mcapps.android.multi_payment_terminal.database.history.error.ErrorData;
import jp.mcapps.android.multi_payment_terminal.database.history.error.ErrorStackingDao;
import jp.mcapps.android.multi_payment_terminal.database.history.error.ErrorStackingData;
import jp.mcapps.android.multi_payment_terminal.error.ErrorManage;
import jp.mcapps.android.multi_payment_terminal.error.JremRasErrorCodes;
import jp.mcapps.android.multi_payment_terminal.error.JremRasErrorMap;
import jp.mcapps.android.multi_payment_terminal.iCAS.BusinessParameter;
import jp.mcapps.android.multi_payment_terminal.iCAS.IiCASClient;
import jp.mcapps.android.multi_payment_terminal.iCAS.data.DeviceClient;
import jp.mcapps.android.multi_payment_terminal.iCAS.iCASClient;
import jp.mcapps.android.multi_payment_terminal.webapi.HttpStatusException;
import jp.mcapps.android.multi_payment_terminal.webapi.jrem_ras.JremRasApi;
import jp.mcapps.android.multi_payment_terminal.webapi.jrem_ras.JremRasApiImpl;
import jp.mcapps.android.multi_payment_terminal.webapi.jrem_ras.data.MoneyCommon;
import jp.mcapps.android.multi_payment_terminal.webapi.jrem_ras.data.MoneyTypes;
import jp.mcapps.android.multi_payment_terminal.webapi.jrem_ras.data.OpeningEmoney;
import jp.mcapps.android.multi_payment_terminal.webapi.jrem_ras.data.OpeningInfoEdy;
import jp.mcapps.android.multi_payment_terminal.webapi.jrem_ras.data.OpeningInfoId;
import jp.mcapps.android.multi_payment_terminal.webapi.jrem_ras.data.OpeningInfoNanaco;
import jp.mcapps.android.multi_payment_terminal.webapi.jrem_ras.data.OpeningInfoQuicpay;
import jp.mcapps.android.multi_payment_terminal.webapi.jrem_ras.data.OpeningInfoWaon;
import jp.mcapps.android.multi_payment_terminal.webapi.jrem_ras.data.OpeningSuica;
import timber.log.Timber;

@SuppressWarnings("BusyWait")
public class JremOpener implements IiCASClient {
    private static final int MAX_RETRY_COUNT = 3;
    private static final int ICAS_TIMEOUT_MILLS = 10*1000;  // 正常時の通信速度は1秒ちょっと
    private static final String UNDEFINED_ERROR_CODE = JremRasErrorMap.EMONEY_OPENING_ERROR_CODE;
    private final Gson _gson = new Gson();
    private JremRasApi _apiClient;

    private boolean _isEdyInitCommunicateFinished = false;
    private boolean _isEdyRemoveFinished = false;
    private boolean _isEdyJournalFinished = false;
    private boolean _isNanacoFinished = false;
    private boolean _isQUICPayFinished = false;

    private boolean _isEdyInitCommunicationSuccess = false;
    private boolean _isEdyRemoveSuccess = false;

    private int currentBusinessId;

    private boolean _isEdyJournalErr = false;
    private boolean _isNanacoJournalErr = false;
    private boolean _isQuicpayJournalErr = false;

    public JremOpener() {
        try {
            _apiClient = new JremRasApiImpl();
        } catch (Exception e) {
            _apiClient = null;
        }
    }

    public String openingSuica() {
        if (_apiClient == null) {
            return JremRasErrorMap.get(JremRasErrorCodes.E4901);
        }

        return openingSuica(1);
    }
    private String openingSuica(int tryCount) {
        if (tryCount > MAX_RETRY_COUNT) {
            return UNDEFINED_ERROR_CODE;
        }

        Timber.i("交通系開局処理: %s回目", tryCount++);

        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.JAPANESE);
        final Date invalidDate = new Date(0);

        try {

            final OpeningSuica.Response response = _apiClient.openingSuica();

            if (response.code != null) {
                // 二重起動なら時間をおいてリトライする
                if (response.code == JremRasErrorCodes.E337) {
                    try {
                        Thread.sleep(5000);
                        return openingSuica(tryCount);
                    } catch (InterruptedException e) {
                        Timber.e(e);
                    }
                }
                AppPreference.setDatetimeOpeningSuica(dateFmt.format(invalidDate));

                return JremRasErrorMap.get(response.code);
            }

            Timber.d("money: suica, value: %s", _gson.toJson(response));

            EmoneyOpeningInfo.setSuica(response);

            final Date currentDate = new Date();
            AppPreference.setDatetimeOpeningSuica(dateFmt.format(currentDate));
            return null;
        } catch (IOException e) {
            Timber.e(e);
            return openingSuica(tryCount);
        } catch (HttpStatusException e) {
            Timber.e(e);
            if (e.getStatusCode() >= 500) {
                return openingSuica(tryCount);
            } else {
                AppPreference.setDatetimeOpeningSuica(dateFmt.format(invalidDate));
                return UNDEFINED_ERROR_CODE;
            }
        }
    }

    public ArrayList<String> openingEmoney() {
        if (_apiClient == null) {
            final ArrayList<String> errors = new ArrayList<>();
            Timber.e("他マネー開局失敗(エラーコード:4901)");
            errors.add(JremRasErrorMap.get(JremRasErrorCodes.E4901));
            return errors;
        }

        return openingEmoney(1);
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    private ArrayList<String> openingEmoney(int tryCount) {
        final ArrayList<String> errors = new ArrayList<>();
        final List<String> errorList = new ArrayList<>();
        final List<String> removeList = new ArrayList<>();

        if (tryCount > MAX_RETRY_COUNT) {
            errors.add(UNDEFINED_ERROR_CODE);
            return errors;
        }

        Timber.i("他マネー開局処理: %s回目", tryCount++);

        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.JAPANESE);
        final Date invalidDate = new Date(0);

        try {
            final OpeningEmoney.Response response = _apiClient.openingEmoney();

            if (!response.result) {
                try {
                    Thread.sleep(5000);
                    // 他マネーは二重起動エラーなのかわからないのでとりあえずリトライする
                    return openingEmoney(tryCount);
                } catch (InterruptedException e) {
                    Timber.e(e);
                }
                errors.add(UNDEFINED_ERROR_CODE);
                AppPreference.setDatetimeOpeningSuica(dateFmt.format(invalidDate));
                return errors;
            }

            if (response.money == null) {
                errors.add(UNDEFINED_ERROR_CODE);
                AppPreference.setDatetimeOpeningSuica(dateFmt.format(invalidDate));
                return errors;
            }


            // Stringに変換してから2回デコードしているのでもっといいやり方あれば修正したい
            for (Object object : response.money) {
                final String json = _gson.toJson(object);
                final MoneyCommon money = _gson.fromJson(json, MoneyCommon.class);

                Timber.d("money: %s, value: %s", money.moneyname, json);

                switch(money.moneyname) {
                    case MoneyTypes.WAON:
                        if (!AppPreference.isMoneyWaon()) {
                            AppPreference.setDatetimeOpeningWaon(dateFmt.format(invalidDate));
                            EmoneyOpeningInfo.setWaon(null);
                            break;
                        }

                        if (money.code != null) {
                            Timber.e("WAON開局失敗(エラーコード:%s)", money.code);
                            errors.add(JremRasErrorMap.get(money.code));
                            AppPreference.setDatetimeOpeningWaon(dateFmt.format(invalidDate));
                            EmoneyOpeningInfo.setWaon(null);
                            break;
                        }

                        Timber.i("WAON開局成功");
                        AppPreference.setDatetimeOpeningWaon(dateFmt.format(new Date()));
                        EmoneyOpeningInfo.setWaon(_gson.fromJson(json, OpeningInfoWaon.class));
                        break;
                    case MoneyTypes.NANACO:
                        if (!AppPreference.isMoneyNanaco()) {
                            AppPreference.setDatetimeOpeningNanaco(dateFmt.format(invalidDate));
                            EmoneyOpeningInfo.setNanaco(null);
                            break;
                        }

                        if (money.code != null) {
                            Timber.e("nanaco開局失敗(エラーコード:%s)", money.code);
                            errors.add(JremRasErrorMap.get(money.code));
                            AppPreference.setDatetimeOpeningNanaco(dateFmt.format(invalidDate));
                            EmoneyOpeningInfo.setNanaco(null);
                            break;
                        }

                        Timber.i("nanaco開局成功");
                        AppPreference.setDatetimeOpeningNanaco(dateFmt.format(new Date()));
                        EmoneyOpeningInfo.setNanaco(_gson.fromJson(json, OpeningInfoNanaco.class));
                        journalNanaco();

                        if(_isNanacoJournalErr) {
                            Timber.e("nanaco日計業務処理失敗(エラーコード:4405)");
                            errorList.add("4405");
                        } else {
                            Timber.i("nanaco日計業務処理成功");
                            removeList.add("4405");
                        }
                        break;
                    case MoneyTypes.EDY:
                        if (!AppPreference.isMoneyEdy()) {
                            AppPreference.setDatetimeOpeningEdy(dateFmt.format(invalidDate));
                            EmoneyOpeningInfo.setEdy(null);
                            break;
                        }

                        if (money.code != null) {
                            Timber.e("Edy開局失敗(エラーコード:%s)", money.code);
                            errors.add(JremRasErrorMap.get(money.code));
                            AppPreference.setDatetimeOpeningEdy(dateFmt.format(invalidDate));
                            EmoneyOpeningInfo.setEdy(null);
                            break;
                        }

                        Timber.i("Edy開局成功");
                        AppPreference.setDatetimeOpeningEdy(dateFmt.format(new Date()));
                        EmoneyOpeningInfo.setEdy(_gson.fromJson(json, OpeningInfoEdy.class));
                        journalEdy();

//                        edyInitCommunication();

                        if(_isEdyJournalErr) {
                            Timber.e("Edy日計業務処理失敗(エラーコード:4407)");
                            errorList.add("4507");
                        } else {
                            Timber.i("Edy日計業務処理成功");
                            removeList.add("4507");
                        }
                        break;
                    case MoneyTypes.ID:
                        if (!AppPreference.isMoneyId()) {
                            AppPreference.setDatetimeOpeningId(dateFmt.format(invalidDate));
                            EmoneyOpeningInfo.setId(null);
                            break;
                        }

                        if (money.code != null) {
                            Timber.e("iD開局失敗(エラーコード:%s)", money.code);
                            errors.add(JremRasErrorMap.get(money.code));
                            AppPreference.setDatetimeOpeningId(dateFmt.format(invalidDate));
                            EmoneyOpeningInfo.setId(null);
                            break;
                        }

                        Timber.i("iD開局成功");
                        AppPreference.setDatetimeOpeningId(dateFmt.format(new Date()));
                        EmoneyOpeningInfo.setId(_gson.fromJson(json, OpeningInfoId.class));
                        break;
                    case MoneyTypes.QUICPAY:
                        if (!AppPreference.isMoneyQuicpay()) {
                            AppPreference.setDatetimeOpeningQuicpay(dateFmt.format(invalidDate));
                            EmoneyOpeningInfo.setQuicpay(null);
                            break;
                        }

                        if (money.code != null) {
                            Timber.e("QUICPay開局失敗(エラーコード:%s)", money.code);
                            errors.add(JremRasErrorMap.get(money.code));
                            AppPreference.setDatetimeOpeningQuicpay(dateFmt.format(invalidDate));
                            EmoneyOpeningInfo.setQuicpay(null);
                            break;
                        }

                        Timber.i("QUICPay開局成功");
                        AppPreference.setDatetimeOpeningQuicpay(dateFmt.format(new Date()));
                        EmoneyOpeningInfo.setQuicpay(_gson.fromJson(json, OpeningInfoQuicpay.class));

                        journalQUICPay();

                        if(_isQuicpayJournalErr) {
                            Timber.e("QUICPay日計業務処理失敗(エラーコード:4604)");
                            errorList.add("4604");
                        } else {
                            Timber.i("QUICPay日計業務処理成功");
                            removeList.add("4604");
                        }

                        break;
                    default:
                        break;
                }
            }

            removeErrorStacking(removeList);
            errorStacking(errorList);
        } catch (IOException e) {
            Timber.e(e);
            return openingEmoney(tryCount);
        } catch (HttpStatusException e) {
            Timber.e(e);
            if (e.getStatusCode() >= 500) {
                return openingEmoney(tryCount);
            } else {
                AppPreference.setDatetimeOpeningWaon(dateFmt.format(invalidDate));
                AppPreference.setDatetimeOpeningNanaco(dateFmt.format(invalidDate));
                AppPreference.setDatetimeOpeningEdy(dateFmt.format(invalidDate));
                AppPreference.setDatetimeOpeningId(dateFmt.format(invalidDate));
                AppPreference.setDatetimeOpeningQuicpay(dateFmt.format(invalidDate));
                errors.add(UNDEFINED_ERROR_CODE);
            }
        }

        return errors;
    }

    @Override
    public void OnUIUpdate(DeviceClient.RWParam rwParam) {
    }

    @Override
    public void OnStatusChanged(DeviceClient.Status status) {
    }

    @Override
    public void OnDisplay(DeviceClient.Display display) {
    }

    @Override
    public void OnOperation(DeviceClient.Operation operation) {
    }

    @Override
    public void OnCancelDisable(boolean bDisable) {
    }

    @Override
    public void OnResultSuica(DeviceClient.Result resultSuica) {
    }

    @Override
    public void OnResultID(DeviceClient.ResultID resultID) {
    }

    @Override
    public void OnResultWAON(DeviceClient.ResultWAON resultWAON) {
    }

    @Override
    public void OnResultQUICPay(DeviceClient.ResultQUICPay resultQUICPay) {
        Timber.d("resultnanaco: %s", _gson.toJson(resultQUICPay));
    }

    @Override
    public void OnResultEdy(DeviceClient.ResultEdy resultEdy) {
        Timber.d("resultEdy: %s", _gson.toJson(resultEdy));
    }

    @Override
    public void OnResultnanaco(DeviceClient.Resultnanaco resultnanaco) {
        Timber.d("resultnanaco: %s", _gson.toJson(resultnanaco));
    }

    @Override
    public void OnJournalEdy(String daTermTo) {
        Timber.d("OnJournalEdy daTermTo: %s", daTermTo);
        // RASからのresultがfalseの場合はdaTermToがnullで渡される
        if(daTermTo != null) {
            AppPreference.setNanacoDaTermFrom(daTermTo);
            _isEdyJournalErr = false;
        }
    }

    @Override
    public void OnJournalnanaco(String daTermTo) {
        Timber.d("OnJournalnanaco daTermTo: %s", daTermTo);
        // RASからのresultがfalseの場合はdaTermToがnullで渡される
        if(daTermTo != null) {
            AppPreference.setNanacoDaTermFrom(daTermTo);
            _isNanacoJournalErr = false;
        }
    }

    @Override
    public void OnJournalQUICPay(String daTermTo) {
        Timber.d("OnJournalQUICPay daTermTo: %s", daTermTo);
        // RASからのresultがfalseの場合はdaTermToがnullで渡される
        if(daTermTo != null) {
            AppPreference.setQuicpayDaTermFrom(daTermTo);
            _isQuicpayJournalErr = false;
        }
    }

    @Override
    public void OnFinished(int statusCode) {
        Timber.d("%s OnFinished statusCode: %s", currentBusinessId, statusCode);

        switch (currentBusinessId) {
            case iCASClient.BUSINESS_ID_EDY_FIRST_COMM:
                _isEdyInitCommunicateFinished = true;
                _isEdyInitCommunicationSuccess = true;
                OpeningInfoEdy edyInfo = EmoneyOpeningInfo.getEdy();
                edyInfo.initCommunicationFlg = false;
                EmoneyOpeningInfo.setEdy(edyInfo);
                break;
            case iCASClient.BUSINESS_ID_EDY_REMOVAL:
                _isEdyRemoveFinished = true;
                _isEdyRemoveSuccess = true;
//                edyInfo = EmoneyOpeningInfo.getEdy();
//                edyInfo.initCommunicationFlg = true;
//                EmoneyOpeningInfo.setEdy(edyInfo);
                break;
            case iCASClient.BUSINESS_ID_EDY_JOURNAL:
                _isEdyJournalFinished = true;
                break;
            case iCASClient.BUSINESS_ID_NANACO_JOURNAL:
                _isNanacoFinished = true;
                break;
            case iCASClient.BUSINESS_ID_QUICPAY_JOURNAL:
                _isQUICPayFinished = true;
                break;
            default:
                break;
        }
    }

    @Override
    public void OnErrorOccurred(long lErrorType, String errorMessage) {
        Timber.d("%s OnErrorOccurred lErrorType: %s, errorMessage: %s", currentBusinessId, lErrorType, errorMessage);

        switch (currentBusinessId) {
            case iCASClient.BUSINESS_ID_EDY_FIRST_COMM:
                _isEdyInitCommunicateFinished = true;
                break;
            case iCASClient.BUSINESS_ID_EDY_REMOVAL:
                _isEdyRemoveFinished = true;
                break;
            case iCASClient.BUSINESS_ID_EDY_JOURNAL:
                _isEdyJournalFinished = true;
                break;
            case iCASClient.BUSINESS_ID_NANACO_JOURNAL:
                _isNanacoFinished = true;
                break;
            case iCASClient.BUSINESS_ID_QUICPAY_JOURNAL:
                _isQUICPayFinished = true;
                break;
            default:
                break;
        }
    }

    @Override
    public void OnRecovery(Object result) {
        DeviceClient.Result result1 = (DeviceClient.Result)result;
        result1.SFLogID = "0";
    }

    @Override
    public long OnTransmitRW(byte[] command, long timeout, byte[] response) {
        return 0;
    }

    public boolean edyInitCommunication() {
        final OpeningInfoEdy edyInfo = EmoneyOpeningInfo.getEdy();

        if (edyInfo != null) {
            if (!edyInfo.initCommunicationFlg) {
                Timber.i("Edy業務初回通信済み");
                return true;
            }

            Timber.i("Edy業務初回通信開始");

            final int businessId = iCASClient.BUSINESS_ID_EDY_FIRST_COMM;
            final iCASClient icasClient = iCASClient.getInstance();
            icasClient.SetRWUIEventListener(this);

            BusinessParameter businessParameter = new BusinessParameter();
            BusinessParameter.Edy edy = new BusinessParameter.Edy();
            businessParameter.businessId = String.valueOf(businessId);
            businessParameter.money = edy;

            try {
                currentBusinessId = businessId;
                icasClient.OnStart(businessParameter);

                final int SLEEP_TIME_MILLS = 50;
                int elapsedTimeMills = 0;
                _isEdyInitCommunicateFinished = false;

                while (!_isEdyInitCommunicateFinished) {
                    Thread.sleep(SLEEP_TIME_MILLS);
                    elapsedTimeMills += SLEEP_TIME_MILLS;

                    if (elapsedTimeMills >= ICAS_TIMEOUT_MILLS) {
                        Timber.e("Edy初回通信業務タイムアウト");
                        _isEdyInitCommunicateFinished = true;
                    }
                }

                icasClient.SetRWUIEventListener(null);
            } catch (JSONException | KeyStoreException | IOException | CertificateException | NoSuchAlgorithmException | InterruptedException e) {
                Timber.e(e);
            }
        } else {
            Timber.e("Edy未開局");
            _isEdyInitCommunicateFinished = true;
        }

        return _isEdyInitCommunicationSuccess;
    }

    public boolean edyRemove() {
        final OpeningInfoEdy edyInfo = EmoneyOpeningInfo.getEdy();

        if (edyInfo != null) {
            if (edyInfo.initCommunicationFlg) {
                return true;
            }

            Timber.d("Edy撤去開始");

            final int businessId = iCASClient.BUSINESS_ID_EDY_REMOVAL;
            final iCASClient icasClient = iCASClient.getInstance();
            icasClient.SetRWUIEventListener(this);

            BusinessParameter businessParameter = new BusinessParameter();
            BusinessParameter.Edy edy = new BusinessParameter.Edy();
            businessParameter.businessId = String.valueOf(businessId);
            businessParameter.money = edy;

            try {
                currentBusinessId = businessId;
                icasClient.OnStart(businessParameter);

                final int SLEEP_TIME_MILLS = 50;
                int elapsedTimeMills = 0;

                while (!_isEdyRemoveFinished) {
                    Thread.sleep(SLEEP_TIME_MILLS);
                    elapsedTimeMills += SLEEP_TIME_MILLS;

                    if (elapsedTimeMills >= ICAS_TIMEOUT_MILLS) {
                        Timber.e("Edy撤去処理タイムアウト");
                        _isEdyRemoveFinished = true;
                    }
                }

                icasClient.SetRWUIEventListener(null);
            } catch (JSONException | KeyStoreException | IOException | CertificateException | NoSuchAlgorithmException | InterruptedException e) {
                Timber.e(e);
            }
        } else {
            Timber.d("Edy未開局");
            _isEdyRemoveFinished = true;
        }

        return _isEdyRemoveSuccess;
    }

    public boolean journalEdy() {
        Timber.i("Edy日計業務処理実行");
        final OpeningInfoEdy edyInfo = EmoneyOpeningInfo.getEdy();

        if (edyInfo != null) {
            if (edyInfo.initCommunicationFlg) {
                Timber.e("Edy初回通信業務処理 未実施");
                _isEdyJournalFinished = true;
                _isEdyJournalErr = false;    // 初回通信業務未実施は日計エラーを表示しない
            } else {
                Timber.d("Edy日計開始");

                final int businessId = iCASClient.BUSINESS_ID_EDY_JOURNAL;
                final iCASClient icasClient = iCASClient.getInstance();
                icasClient.SetRWUIEventListener(this);

                BusinessParameter businessParameter = new BusinessParameter();
                BusinessParameter.Edy edy = new BusinessParameter.Edy();
                businessParameter.businessId = String.valueOf(businessId);
                edy.daTermFrom = AppPreference.getNanacoDaTermFrom();
                edy.training = "OFF";
                businessParameter.money = edy;

                _isEdyJournalErr = true;

                try {
                    currentBusinessId = businessId;
                    icasClient.OnStart(businessParameter);

                    final int SLEEP_TIME_MILLS = 50;
                    int elapsedTimeMills = 0;

                    while (!_isEdyJournalFinished) {
                        Thread.sleep(SLEEP_TIME_MILLS);
                        elapsedTimeMills += SLEEP_TIME_MILLS;

                        if (elapsedTimeMills >= ICAS_TIMEOUT_MILLS) {
                            Timber.e("Edy日計業務処理タイムアウト");
                            _isEdyJournalFinished = true;
                        }
                    }

                    icasClient.SetRWUIEventListener(null);
                } catch (JSONException | KeyStoreException | IOException | CertificateException | NoSuchAlgorithmException | InterruptedException e) {
                    Timber.e(e);
                }
            }
        } else {
            Timber.e("Edy未開局状態");
            _isEdyJournalFinished = true;
            _isEdyJournalErr = false;    // 未開局時は日計エラーは表示しない
        }

        return _isEdyJournalFinished;
    }

    public void  journalNanaco() {
        Timber.i("nanaco日計業務処理実行");
        final iCASClient icasClient = iCASClient.getInstance();
        icasClient.SetRWUIEventListener(this);

        final int businessId = iCASClient.BUSINESS_ID_NANACO_JOURNAL;

        BusinessParameter businessParameter = new BusinessParameter();
        BusinessParameter.nanaco nanaco = new BusinessParameter.nanaco();
        businessParameter.businessId = String.valueOf(businessId);
        businessParameter.money = nanaco;
        nanaco.daTermFrom = AppPreference.getNanacoDaTermFrom();
        nanaco.training = "OFF";

        Timber.d(_gson.toJson(nanaco));

        _isNanacoJournalErr = true;

        try {
            currentBusinessId = businessId;
            icasClient.OnStart(businessParameter);

            final int SLEEP_TIME_MILLS = 50;
            int elapsedTimeMills = 0;

            while (!_isNanacoFinished) {
                Thread.sleep(SLEEP_TIME_MILLS);
                elapsedTimeMills += SLEEP_TIME_MILLS;

                if (elapsedTimeMills >= ICAS_TIMEOUT_MILLS) {
                    Timber.e("nanaco日計業務処理タイムアウト");
                    _isNanacoFinished = true;
                }
            }

            icasClient.SetRWUIEventListener(null);
        } catch (JSONException | KeyStoreException | IOException | CertificateException | NoSuchAlgorithmException | InterruptedException e) {
            Timber.e(e);
        }
    }

    public void  journalQUICPay() {
        Timber.i("QUICPay日計業務処理実行");
        final iCASClient icasClient = iCASClient.getInstance();
        icasClient.SetRWUIEventListener(this);

        final int businessId = iCASClient.BUSINESS_ID_QUICPAY_JOURNAL;

        BusinessParameter businessParameter = new BusinessParameter();
        BusinessParameter.QUICPay qp = new BusinessParameter.QUICPay();
        businessParameter.businessId = String.valueOf(businessId);
        businessParameter.money = qp;
        qp.daTermForm = AppPreference.getQuicpayDaTermFrom();
        qp.training = "OFF";

        Timber.d(_gson.toJson(qp));

        _isQuicpayJournalErr = true;

        try {
            currentBusinessId = businessId;
            icasClient.OnStart(businessParameter);

            final int SLEEP_TIME_MILLS = 50;
            int elapsedTimeMills = 0;

            while (!_isQUICPayFinished) {
                Thread.sleep(SLEEP_TIME_MILLS);
                elapsedTimeMills += SLEEP_TIME_MILLS;

                if (elapsedTimeMills >= ICAS_TIMEOUT_MILLS) {
                    Timber.e("QUICPay日計業務処理タイムアウト");
                    _isQUICPayFinished = true;
                }
            }

            icasClient.SetRWUIEventListener(null);
        } catch (JSONException | KeyStoreException | IOException | CertificateException | NoSuchAlgorithmException | InterruptedException e) {
            Timber.e(e);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void errorStacking(List<String> errorList) {
        ErrorManage errorManage = ErrorManage.getInstance();
        ErrorStackingDao dao = LocalDatabase.getInstance().errorStackingDao();

        for (String errorCode : errorList) {
            // エラー情報を取得
            ErrorData errorData = errorManage.getErrorData(errorCode);

            if(errorData == null) {
                Timber.d("error dialog errorCode not found");
                continue;
            }

            // 送信用のログはスタックされるたびに作成
            String message = String.format("MCエラーコード：%s", errorData.errorCode);
            if (errorData.detail.contains("詳細コード：")){
                message = message.concat(",").concat(errorData.detail.split("\n")[0]);
            }
            Timber.tag("エラー履歴").i(message);

            ErrorStackingData errorStackingData = dao.getErrorStackingData(errorData.errorCode);

            Date date = new Date();
            //同じエラーがスタックされていない場合 新たにスタック
            if (errorStackingData == null) {
                ErrorStackingData newErrorStackingData = new ErrorStackingData();
                newErrorStackingData.errorCode = errorData.errorCode;
                newErrorStackingData.title = errorData.title;
                newErrorStackingData.message = errorData.message;
                newErrorStackingData.detail = errorData.detail;
                newErrorStackingData.level = errorData.level;

                newErrorStackingData.date = date;
                dao.insertErrorData(newErrorStackingData);
                //Timber.d("error stacking insert : code = %s", newErrorStackingData.errorCode);
                //同じエラーがスタックされている場合 時間を更新
            } else {
                dao.updateErrorStackingData(errorStackingData.id, date);
                //Timber.d("error stacking update : code = %s", errorStackingData.errorCode);
            }
        }
    }

    private void removeErrorStacking(List<String> errorCodeList) {
        ErrorStackingDao dao = LocalDatabase.getInstance().errorStackingDao();

        for (String errorCode : errorCodeList) {
            ErrorStackingData errorStackingData = dao.getErrorStackingData(errorCode);
            if (errorStackingData != null) {
                dao.deleteErrorStackingData(errorStackingData.id);
                //Timber.d("error stacking remove : code = %s", errorCode);
            }
        }
    }

    public void journalEdyResult() {
        if(_isEdyJournalErr) {
            Timber.e("Edy日計業務処理失敗(エラーコード:4407)");
        } else {
            Timber.i("Edy日計業務処理成功");
        }
    }

    public void journalNanacoResult() {
        if(_isNanacoJournalErr) {
            Timber.e("nanaco日計業務処理失敗(エラーコード:4405)");
        } else {
            Timber.i("nanaco日計業務処理成功");
        }
    }

    public void journalQUICPayResult() {
        if(_isQuicpayJournalErr) {
            Timber.e("QUICPay日計業務処理失敗(エラーコード:4604)");
        } else {
            Timber.i("QUICPay日計業務処理成功");
        }
    }
}
