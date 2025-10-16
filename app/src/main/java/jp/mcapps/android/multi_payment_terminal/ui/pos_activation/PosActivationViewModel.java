package jp.mcapps.android.multi_payment_terminal.ui.pos_activation;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pos.device.sys.SystemManager;

import java.io.IOException;
import java.net.UnknownHostException;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.BuildConfig;
import jp.mcapps.android.multi_payment_terminal.util.DeviceUtils;
import jp.mcapps.android.multi_payment_terminal.webapi.HttpStatusException;
import jp.mcapps.android.multi_payment_terminal.webapi.paypf_activation.PaypfActivationApi;
import jp.mcapps.android.multi_payment_terminal.webapi.paypf_activation.PaypfActivationApiImpl;
import jp.mcapps.android.multi_payment_terminal.webapi.paypf_activation.data.GetToken;
import jp.mcapps.android.multi_payment_terminal.webapi.paypf_activation.data.Activate;
import timber.log.Timber;

;

public class PosActivationViewModel  extends ViewModel {
    public enum Statuses{
        Loading,
        Error,
        Finish
    }
    public enum Phases{
        None,
        Activate,
        getToken
    }

    private final Handler _handler = new Handler(Looper.getMainLooper());

    private final PaypfActivationApi _api = new PaypfActivationApiImpl();

    private final MutableLiveData<Statuses> _status = new MutableLiveData<>(Statuses.Loading);
    public MutableLiveData<Statuses> getState() {
        return _status;
    }

    private final MutableLiveData<Integer> _errorCode = new MutableLiveData<>(0);
    public MutableLiveData<Integer> getErrorCode() {
        return _errorCode;
    }


    private final MutableLiveData<String> _errorMessageDetail = new MutableLiveData<>(null);
    public MutableLiveData<String> getErrorMessageDetail() {
        return _errorMessageDetail;
    }

    public void setStatus(Statuses status) {
        setStatus(status , 0 , "");
    }
    public void setStatus(Statuses status ,  int errorCode) {
        setStatus(status , errorCode , "");
    }
    public void setStatus(Statuses status ,  int errorCode , String errorDetail) {
        Timber.i("ステータス%s -> %s / エラーコード:%d -> %d / エラー詳細:%s", _status.getValue() , status , _errorCode.getValue() , errorCode , errorDetail);
        _handler.post(() -> {
            _status.setValue(status);
            viewState(status);
            _errorCode.setValue(errorCode);
            _errorMessageDetail.setValue(errorDetail);
        });
    }

    private MutableLiveData<Boolean> _isLoading = new MutableLiveData<Boolean>(true);
    public MutableLiveData<Boolean> get_isLoading(){
        return _isLoading;
    }

    private MutableLiveData<Boolean> _isError = new MutableLiveData<Boolean>(false);
    public MutableLiveData<Boolean> get_isError(){
        return _isError;
    }
    private MutableLiveData<Boolean> _isFinish = new MutableLiveData<Boolean>(false);
    public MutableLiveData<Boolean> get_isFinish(){
        return _isFinish;
    }

    private final MutableLiveData<Phases> _phase = new MutableLiveData<>(Phases.None);
    private final MutableLiveData<String> _phaseText = new MutableLiveData<>("");
    public void setPhase(Phases phase) {
        Timber.d("POS フェーズ:%s => %s", _phase , phase);
        _handler.post(() -> {
            _phase.setValue(phase);
            viewPhase(phase);
        });
    }
    public MutableLiveData<String> getPhaseText(){
        return _phaseText;
    }


    private Thread thread;

    public void start() {
        final String modelCode = BuildConfig.MODEL_CODE;
        final String serialNo = DeviceUtils.getSerial();
        final String  unitId = String.valueOf(AppPreference.getMcCarId());  // 車番を設定
        final boolean useInTest = false; //TODO デモモードの仕様未確定
        final boolean usePayment = true;
        final String tid = String.valueOf(AppPreference.getMcTermId());
        final String supplierCd = String.valueOf(AppPreference.getSupplierCd());

        thread = new Thread(() -> {
            if(!AppPreference.isServiceTicket()) {
                //アクティベーション要求
                setPhase(Phases.Activate);
                Timber.i("アクティベーション要求");
                try {
                    _api.activate(
                            modelCode,
                            serialNo,
                            unitId,
                            usePayment,
                            tid,
                            supplierCd
                    );
                } catch (HttpStatusException e) {
                    Timber.e(e);
                    try {
                        Activate.Response reqActivateResponse = new Activate.Response();
                        final Gson _gson = new GsonBuilder().disableHtmlEscaping().create();
                        final String json = e.getBody();
                        final Activate.Response errorReqActivation = _gson.fromJson(json, Activate.Response.class);
                        reqActivateResponse = errorReqActivation;
                        Timber.e("reqActivateResponse:%d", reqActivateResponse.code);
                        if (reqActivateResponse.code == 4003) {
                            setStatus(
                                    Statuses.Error,
                                    reqActivateResponse.code,
                                    "すでにアクティベーションされています"
                            );
                        } else if (reqActivateResponse.code == 4020) {
                            setStatus(
                                    Statuses.Error,
                                    reqActivateResponse.code,
                                    "この端末では取引先コード（" + supplierCd + "）でのアクティベーションはできません"
                            );
                        } else {
                            setStatus(Statuses.Error, e.getStatusCode());
                        }
                        return;
                    } catch (Exception ex) {
                        Timber.e(ex);
                        setStatus(Statuses.Error, e.getStatusCode());
                        return;
                    }
                } catch (UnknownHostException e) {
                    Timber.e(e);
                    setStatus(Statuses.Error, 0, "ネットワークに異常があります");
                    return;
                } catch (IOException e) {
                    Timber.e(e);
                    setStatus(Statuses.Error);
                    return;
                }
            }
            //トークン取得
            setPhase(Phases.getToken);
            while (true){
                try {
                    Thread.sleep(5000);

                    final GetToken.Response getTokenResponse;
                    Timber.i("トークン要求");
                    try {
                        getTokenResponse = _api.getToken(modelCode, serialNo, useInTest);
                        AppPreference.set_servicePos(getTokenResponse.access_token,getTokenResponse.refresh_token);
                        setStatus(Statuses.Finish);
                        return;
                    } catch (HttpStatusException e) {
                        //500は認証待ち
                        if (e.getStatusCode() != 500) {
                            Timber.e(e);
                            setStatus(Statuses.Error,e.getStatusCode());
                            return;
                        } else {
                            Timber.i("FIGコンソールからの承認待機中・・・");
                        }
                    } catch (IOException e) {
                        Timber.e(e);
                        setStatus(Statuses.Error);
                        return;
                    }
                } catch (InterruptedException e) {
                    Timber.e(e);
                    setStatus(Statuses.Error);
                    return;
                }
            }
        });

        Timber.d("アクティベーション開始");
        // アクティベーション中は画面オフにしない
        SystemManager.setScreenTimeOut(60 * 60 * 24);
        thread.start();

    }

    public void stop() {
        Timber.d("アクティベーション終了");
        SystemManager.setScreenTimeOut(AppPreference.getTimeoutScreen());
        thread.interrupt();
    }

    protected void viewPhase(Phases phases){

        if(phases == Phases.None) {
            _phaseText.setValue("アクティベーション要求前");
        } else if(phases == Phases.Activate) {
            _phaseText.setValue("アクティベーション要求");
        } else if(phases == Phases.getToken) {
            _phaseText.setValue("トークン取得");
        }
    }
    protected void viewState(Statuses status){
        boolean loading = false;
        boolean error = false;
        boolean finish = false;
        if(status == Statuses.Loading) {
            loading = true;
        }else if(status == Statuses.Error) {
            error = true;
            Timber.e("アクティベーション失敗しました");
        }else if(status == Statuses.Finish) {
            finish = true;
            Timber.i("アクティベーション成功しました");
        }
        _isLoading.setValue(loading);
        _isError.setValue(error);
        _isFinish.setValue(finish);
    }
}
