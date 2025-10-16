package jp.mcapps.android.multi_payment_terminal.ui.discount;

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
//import jp.mcapps.android.multi_payment_terminal.model.IFBoxManager;
import jp.mcapps.android.multi_payment_terminal.webapi.HttpStatusException;
//import jp.mcapps.android.multi_payment_terminal.webapi.paypf_activation.PaypfActivationApi;
//import jp.mcapps.android.multi_payment_terminal.webapi.paypf_activation.PaypfActivationApiImpl;
//import jp.mcapps.android.multi_payment_terminal.webapi.paypf_activation.data.Activate;
//import jp.mcapps.android.multi_payment_terminal.webapi.paypf_activation.data.GetToken;
import timber.log.Timber;

public class DiscountJobFutabaDViewModel extends ViewModel {


    public static final int  DISCOUNTMODE_NONE = 0;            //なし(あるいはエラー終了)
    public static final int  DISCOUNTMODE_JOB1 = 1;            //割引１  (サーバとの通信で内容は確定する)
    public static final int  DISCOUNTMODE_JOB2 = 2;            //割引２
    public static final int  DISCOUNTMODE_JOB3 = 3;            //割引３
    public static final int  DISCOUNTMODE_JOB4 = 4;            //割引４
    public static final int  DISCOUNTMODE_JOB5 = 5;            //割引５


//    private final IFBoxManager _ifBoxManager;
//    public IFBoxManager getIfBoxManager(){
//        return _ifBoxManager;
//    }

    public DiscountJobFutabaDViewModel() {

        // _ifBoxManager = ifBoxManager;
    }


    //private final Handler _handler = new Handler(Looper.getMainLooper());

    //private final PaypfActivationApi _api = new PaypfActivationApiImpl();

    private MutableLiveData<Boolean> _isError = new MutableLiveData<Boolean>(false);
    public MutableLiveData<Boolean> get_isError(){
        return _isError;
    }
    private MutableLiveData<Boolean> _isFinish = new MutableLiveData<Boolean>(false);
    public MutableLiveData<Boolean> get_isFinish(){
        return _isFinish;
    }

    private MutableLiveData<Integer> _discountJobMode = new MutableLiveData<Integer>(0);
    public MutableLiveData<Integer> getdiscountJobMode(){
        return _discountJobMode;
    }
    public void setdiscountJobMode(int mode){       //割引JOBモード配列員デックス値と同等０～４
        _discountJobMode.setValue(mode);
    }

    //OK時のメッセージ取得
//    public MutableLiveData<String> getDiscountOKStr(){
//        MutableLiveData<String> ret = new MutableLiveData<String>("");
//        if(_jobStatus.getValue() == JOBSTATUS.Finish) {
//            switch (_discountMode.getValue()) {
//                case DiscountJobFutabaDViewModel.DISCOUNTMODE_JOB1:
//                    ret.setValue("ゆうゆう\n割引しても\nよろしいですか？");
//                    break;
//                case DiscountJobFutabaDViewModel.DISCOUNTMODE_JOB2:
//                    ret.setValue("すこやか\n割引しても\nよろしいですか？");
//                    break;
//                default:
//                    ret.setValue("");
//                    break;
//            }
//        }
//        return ret;
//    }


//    private Thread thread;

//    public void start() {
//        final String modelCode = BuildConfig.MODEL_CODE;
//        final String serialNo = Build.SERIAL;
//        final String  unitId = String.valueOf(AppPreference.getMcCarId());  // 車番を設定
//        final boolean useInTest = false; //TODO デモモードの仕様未確定
//        final boolean usePayment = true;
//        final String tid = String.valueOf(AppPreference.getMcTermId());
//        final String supplierCd = String.valueOf(AppPreference.getSupplierCd());
//
//        thread = new Thread(() -> {
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
//        });
//        thread.start();
//
//    }
//
//    public void stop() {
//        thread.interrupt();
//    }

}
