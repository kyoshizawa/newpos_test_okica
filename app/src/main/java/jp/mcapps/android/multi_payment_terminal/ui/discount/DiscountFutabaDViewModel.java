package jp.mcapps.android.multi_payment_terminal.ui.discount;
//ADD-S BMT S.Oyama 2024/09/06 フタバ双方向向け改修

import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.net.UnknownHostException;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.BuildConfig;
import jp.mcapps.android.multi_payment_terminal.model.IFBoxManager;
import jp.mcapps.android.multi_payment_terminal.util.DeviceUtils;
import jp.mcapps.android.multi_payment_terminal.webapi.HttpStatusException;
//import jp.mcapps.android.multi_payment_terminal.webapi.paypf_activation.PaypfActivationApi;
//import jp.mcapps.android.multi_payment_terminal.webapi.paypf_activation.PaypfActivationApiImpl;
//import jp.mcapps.android.multi_payment_terminal.webapi.paypf_activation.data.Activate;
//import jp.mcapps.android.multi_payment_terminal.webapi.paypf_activation.data.GetToken;
import timber.log.Timber;

public class DiscountFutabaDViewModel extends ViewModel {

    public enum JOBSTATUS{          //画面動線ステータス
        Started,                    //カード挿入前画面
        Connecting,                 //センター通信中画面
        Error,                      //エラー終了画面
        Finish                      //OK終了画面
    }

    public enum DIACOUNT_ERRORMODE
    {
        None,                       //なし
        NoBalance,                  //残高不足
        CardError,                  //カードエラー
    }

    private final Handler _handler = new Handler(Looper.getMainLooper());

    //private final PaypfActivationApi _api = new PaypfActivationApiImpl();


    private final MutableLiveData<Integer> _errorCode = new MutableLiveData<>(0);
    public MutableLiveData<Integer> getErrorCode() {
        return _errorCode;
    }


    private final MutableLiveData<String> _errorMessageDetail = new MutableLiveData<>(null);
    public MutableLiveData<String> getErrorMessageDetail() {
        return _errorMessageDetail;
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

    //画面動線ステータス
    public MutableLiveData<JOBSTATUS> _jobStatus = new MutableLiveData<>(JOBSTATUS.Started);
    public MutableLiveData<JOBSTATUS> getJobStatus(){
        return _jobStatus;
    }
    public void setJobStatus(JOBSTATUS status){
        _jobStatus.setValue(status);
    }
    //エラーモード
    public MutableLiveData<DIACOUNT_ERRORMODE> _discountErrorMode = new MutableLiveData<>(DIACOUNT_ERRORMODE.None);
    public MutableLiveData<DIACOUNT_ERRORMODE> getDiscountErrorMode(){
        return _discountErrorMode;
    }
    public void setDiscountErrorMode(DIACOUNT_ERRORMODE mode){
        _discountErrorMode.setValue(mode);
    }

    private final IFBoxManager _ifBoxManager;
    public IFBoxManager getIfBoxManager(){
        return _ifBoxManager;
    }

    public DiscountFutabaDViewModel(IFBoxManager ifBoxManager) {
        _ifBoxManager = ifBoxManager;
    }


    //エラー時のエラーメッセージ取得
    public MutableLiveData<String> getDiscountErrorStr(){
        MutableLiveData<String> ret = new MutableLiveData<String>("");
        if(_jobStatus.getValue() == JOBSTATUS.Error) {
            switch (_discountErrorMode.getValue()) {
                case NoBalance:
                    ret.setValue("割引出来ません");
                    break;
                case CardError:
                    ret.setValue("割引不可なカードです\nお取り扱いできません");
                    break;
                default:
                    ret.setValue("");
                    break;
            }
        }
        return ret;
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
//            if(!AppPreference.isServicePos()) {
//                //アクティベーション要求
//                setPhase(DiscountFutabaDViewModel.Phases.Activate);
//                Timber.i("アクティベーション要求");
//                try {
//                    _api.activate(
//                            modelCode,
//                            serialNo,
//                            unitId,
//                            usePayment,
//                            tid,
//                            supplierCd
//                    );
//                } catch (HttpStatusException e) {
//                    Timber.e(e);
//                    try {
//                        Activate.Response reqActivateResponse = new Activate.Response();
//                        final Gson _gson = new GsonBuilder().disableHtmlEscaping().create();
//                        final String json = e.getBody();
//                        final Activate.Response errorReqActivation = _gson.fromJson(json, Activate.Response.class);
//                        reqActivateResponse = errorReqActivation;
//                        Timber.e("reqActivateResponse:%d", reqActivateResponse.code);
//                        if (reqActivateResponse.code == 4003) {
//                            setStatus(
//                                    DiscountFutabaDViewModel.Statuses.Error,
//                                    reqActivateResponse.code,
//                                    "すでにアクティベーションされています"
//                            );
//                        } else if (reqActivateResponse.code == 4020) {
//                            setStatus(
//                                    DiscountFutabaDViewModel.Statuses.Error,
//                                    reqActivateResponse.code,
//                                    "この端末では取引先コード（" + supplierCd + "）でのアクティベーションはできません"
//                            );
//                        } else {
//                            setStatus(DiscountFutabaDViewModel.Statuses.Error, e.getStatusCode());
//                        }
//                        return;
//                    } catch (Exception ex) {
//                        Timber.e(ex);
//                        setStatus(DiscountFutabaDViewModel.Statuses.Error, e.getStatusCode());
//                        return;
//                    }
//                } catch (UnknownHostException e) {
//                    Timber.e(e);
//                    setStatus(DiscountFutabaDViewModel.Statuses.Error, 0, "ネットワークに異常があります");
//                    return;
//                } catch (IOException e) {
//                    Timber.e(e);
//                    setStatus(DiscountFutabaDViewModel.Statuses.Error);
//                    return;
//                }
//            }
            //トークン取得
//            setPhase(DiscountFutabaDViewModel.Phases.getToken);
//            while (true){
//                try {
//                    Thread.sleep(5000);
//
//                    final GetToken.Response getTokenResponse;
//                    Timber.i("トークン要求");
//                    try {
//                        getTokenResponse = _api.getToken(modelCode, serialNo, useInTest);
//                        AppPreference.set_serviceTicket(getTokenResponse.access_token,getTokenResponse.refresh_token);
//                        setStatus(DiscountFutabaDViewModel.Statuses.Finish);
//                        return;
//                    } catch (HttpStatusException e) {
//                        //500は認証待ち
//                        if (e.getStatusCode() != 500) {
//                            Timber.e(e);
//                            setStatus(DiscountFutabaDViewModel.Statuses.Error,e.getStatusCode());
//                            return;
//                        } else {
//                            Timber.i("FIGコンソールからの承認待機中・・・");
//                        }
//                    } catch (IOException e) {
//                        Timber.e(e);
//                        setStatus(DiscountFutabaDViewModel.Statuses.Error);
//                        return;
//                    }
//                } catch (InterruptedException e) {
//                    Timber.e(e);
//                    setStatus(DiscountFutabaDViewModel.Statuses.Error);
//                    return;
//                }
//            }
        });
        thread.start();

    }

    public void stop() {
        thread.interrupt();
    }

//    protected void viewPhase(DiscountFutabaDViewModel.Phases phases){
//
//        if(phases == DiscountFutabaDViewModel.Phases.None) {
//            _phaseText.setValue("アクティベーション要求前");
//        } else if(phases == DiscountFutabaDViewModel.Phases.Activate) {
//            _phaseText.setValue("アクティベーション要求");
//        } else if(phases == DiscountFutabaDViewModel.Phases.getToken) {
//            _phaseText.setValue("トークン取得");
//        }
//    }
//    protected void viewState(DiscountFutabaDViewModel.Statuses status){
//        boolean loading = false;
//        boolean error = false;
//        boolean finish = false;
//        if(status == DiscountFutabaDViewModel.Statuses.Loading) {
//            loading = true;
//        }else if(status == DiscountFutabaDViewModel.Statuses.Error) {
//            error = true;
//            Timber.e("アクティベーション失敗しました");
//        }else if(status == DiscountFutabaDViewModel.Statuses.Finish) {
//            finish = true;
//            Timber.i("アクティベーション成功しました");
//        }
//        _isLoading.setValue(loading);
//        _isError.setValue(error);
//        _isFinish.setValue(finish);
//    }
}
//ADD-E BMT S.Oyama 2024/09/06 フタバ双方向向け改修
