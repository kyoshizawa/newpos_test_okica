package jp.mcapps.android.multi_payment_terminal.devices;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import androidx.annotation.RawRes;
import androidx.annotation.RequiresApi;

import com.epson.epos2.ConnectionListener;
import com.epson.epos2.Epos2CallbackCode;
import com.epson.epos2.Epos2Exception;
import com.epson.epos2.cashchanger.CashChanger;
import com.epson.epos2.cashchanger.CashChangerStatusInfo;
import com.epson.epos2.cashchanger.CashCountListener;
import com.epson.epos2.cashchanger.CommandReplyListener;
import com.epson.epos2.cashchanger.DepositListener;
import com.epson.epos2.cashchanger.DirectIOCommandReplyListener;
import com.epson.epos2.cashchanger.DispenseListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Single;
import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.NavigationWrapper;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.SharedViewModel;
import jp.mcapps.android.multi_payment_terminal.data.MoneyType;
import jp.mcapps.android.multi_payment_terminal.data.TransactionResults;
import jp.mcapps.android.multi_payment_terminal.model.DeviceConnectivityManager;
import jp.mcapps.android.multi_payment_terminal.model.OptionalTransFacade;
import jp.mcapps.android.multi_payment_terminal.model.SoundManager;
import jp.mcapps.android.multi_payment_terminal.model.TransLogger;
import jp.mcapps.android.multi_payment_terminal.thread.printer.PrinterManager;
import jp.mcapps.android.multi_payment_terminal.ui.error.CommonErrorDialog;
import jp.mcapps.android.multi_payment_terminal.ui.error.CommonErrorEventHandlers;
import jp.mcapps.android.multi_payment_terminal.ui.pos.CashChangerPaymentViewModel;
import timber.log.Timber;

public class GloryCashChanger implements CommonErrorEventHandlers {
    private Handler _handler = new Handler(Looper.getMainLooper());
    private static GloryCashChanger mGloryCashChanger = null;
    private CashChanger mCashChanger = null;
    private Map<String, Integer> _countData = null;
    private View errView = null;
    private final Object obj = new Object();
    // 処理完了待ちのためにobjを使い回しているが、目的ごとに変数を分けた方がよいか？

    private DepositAmountListener mDepositAmountListener = null;
    private int mOposErrorCode = 0;

    // OPOSエラー
    private final int OPOS_ECHAN_OVERDISPENSE = 201;

    private final String LOGTAG = "GloryCashChanger";

    private GloryCashChanger() throws Epos2Exception {
        try {
            mCashChanger = new CashChanger(MainApplication.getInstance().getApplicationContext());
        } catch (Epos2Exception e) {
            throw e;
        }
        // Listener登録は一度でよいのでここで登録しておく
        mCashChanger.setDirectIOCommandReplyEventListener(mDirectCommandListener);
        mCashChanger.setConnectionEventListener(mConnectionListener);
    }

    public static GloryCashChanger getInstance() {
        if (mGloryCashChanger == null) {
            try {
                mGloryCashChanger = new GloryCashChanger();
            } catch (Epos2Exception e) {
                mGloryCashChanger = null;
            }
        }
        return mGloryCashChanger;
    }

    public boolean connect() {
        if (mCashChanger == null) {
            Timber.tag(LOGTAG).d("mCashChanger is NULL in connect()");
            return false;
        }
        CashChangerStatusInfo cashChangerStatusInfo = mCashChanger.getStatus();
        if (cashChangerStatusInfo.getConnection() == CashChanger.TRUE) {
            Timber.tag(LOGTAG).d("already connect in connect()");
            // 接続中なので接続処理はせず接続完了とする
            return true;
        }
        String target = DiscoverDevice.getCashChangerTarget();
        if (target == null) {
            Timber.tag(LOGTAG).d("target is NULL in connect()");
            return false;
        }
        Context appContext = MainApplication.getInstance();
        ConnectivityManager connectivityManager = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network defaultNetwork = connectivityManager.getBoundNetworkForProcess();
        connectivityManager.bindProcessToNetwork(null);
        try {
            Timber.tag(LOGTAG).d("connect exec");
            mCashChanger.connect(target, CashChanger.PARAM_DEFAULT);
        } catch (Epos2Exception e) {
            Timber.tag(LOGTAG).e("Exception in connect() %d", e.getErrorStatus());
            return false;
        } finally {
//            ConnectivityManager.setProcessDefaultNetwork(defaultNetwork);
            DeviceConnectivityManager.setProcessMobileNetwork(defaultNetwork);
        }
        return true;
    }

    public boolean disconnect() {
        if (mCashChanger == null) {
            Timber.tag(LOGTAG).d("mCashChanger is NULL in disconnect()");
            return false;
        }
        CashChangerStatusInfo cashChangerStatusInfo = mCashChanger.getStatus();
        if (cashChangerStatusInfo.getConnection() == CashChanger.FALSE) {
            Timber.tag(LOGTAG).d("already disconnect in disconnect()");
            // 念のためlistener登録は削除しておく
            mDepositAmountListener = null;
            // 未接続なので何もしないで成功を返す
            return true;
        }

        Context appContext = MainApplication.getInstance();
        ConnectivityManager connectivityManager = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network defaultNetwork = connectivityManager.getBoundNetworkForProcess();
        connectivityManager.bindProcessToNetwork(null);
        try {
            Timber.tag(LOGTAG).d("disconnect exec");
            mCashChanger.disconnect();
        } catch (Epos2Exception e) {
            Timber.tag(LOGTAG).e("Exception in disconnect() %d", e.getErrorStatus());
            return false;
        } finally {
            DeviceConnectivityManager.setProcessMobileNetwork(defaultNetwork);
        }
        // 登録したListenerを削除しておく
        mDepositAmountListener = null;
        return true;
    }

    public void beginDeposit() {
        if (mCashChanger == null) {
            Timber.tag(LOGTAG).d("mCashChanger is NULL in beginDeposit()");
            // 何もできない
            return;
        }
        try {
            Timber.tag(LOGTAG).d("beginDeposit exec");
            mCashChanger.setDepositEventListener(mDepositListener);
            mCashChanger.beginDeposit();
        } catch (Epos2Exception e) {
            Timber.tag(LOGTAG).e("Exception in beginDeposit() %d", e.getErrorStatus());
            // エラーが発生したらユーザに通知すべきか？
        }
        // 何か操作するまでは入金処理のままでよいので、完了待ち合わせはしない
    }

    private static class DepositInfo {
        private static int _config = 0;
        private static View _view = null;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public boolean endDeposit(int config, View view, SharedViewModel sharedViewModel) {
        boolean ret = false;
        int epos2ExceptionCode = 0;
        int epos2CallbackCode = Epos2CallbackCode.CODE_ERR_FAILURE;

        mOposErrorCode = 0;

        DepositInfo._config = config;
        DepositInfo._view = view;

        if (mCashChanger == null) {
            Timber.tag(LOGTAG).d("mCashChanger is NULL in endDeposit()");
            // 何もできない
            epos2CallbackCode = Epos2CallbackCode.CODE_ERR_FAILURE;
        } else {
            @NonNull Single<Object> single = Single.create(emitter -> {
                        mCashChanger.setDepositEventListener(new DepositListener() {
                            @Override
                            public void onCChangerDeposit(CashChanger cchangerObj, int code, int status, int amount, Map<String, Integer> data) {
                                Timber.tag(LOGTAG).i("onCChangerDeposit status : %d code = %d",status, code);
                                if (status == CashChanger.STATUS_END) {
                                    Timber.tag(LOGTAG).i("確定入金額" + amount + "円");
                                    mDepositAmountListener.onCChangerDeposit(amount);
                                    emitter.onSuccess(code);
                                }
                            }
                        });
                    })
                    .timeout(10, TimeUnit.SECONDS)
                    .onErrorReturnItem(Epos2CallbackCode.CODE_ERR_TIMEOUT);
            try {
                Timber.tag(LOGTAG).d("endDeposit exec");
                mCashChanger.endDeposit(config);
                epos2CallbackCode = (int) single.blockingGet();
                Timber.tag(LOGTAG).i("onCChangerDeposit epos2CallbackCode = %x", epos2CallbackCode);
            } catch (Epos2Exception e) {
                Timber.tag(LOGTAG).d("Exception in endDeposit()");
                epos2ExceptionCode = e.getErrorStatus();
                Timber.tag(LOGTAG).e("onCChangerDeposit epos2ExceptionCode = %x", epos2ExceptionCode);
            }

            if (epos2CallbackCode == Epos2CallbackCode.CODE_SUCCESS) {
                //　正常
                Timber.tag(LOGTAG).i("onCChangerDeposit OK");
                ret = true;
            } else  {
                //　異常
                mOposErrorCode = mCashChanger.getOposErrorCode();
                Timber.tag(LOGTAG).e("onCChangerDeposit NG oposErrorCode = %d", mOposErrorCode);
                endDepositError(mOposErrorCode, view);
            }
        }

        return ret;
    }

    public Map<String, Integer> readCashCount() {
        int epos2ExceptionCode = 0;
        int epos2CallbackCode = Epos2CallbackCode.CODE_ERR_FAILURE;
        int oposErrorCode = 0;

        _countData = null;

        if (mCashChanger == null) {
            Timber.tag(LOGTAG).d("mCashChanger is NULL in readCashCount()");
            // 何もできない
            return _countData;
        }

        @NonNull Single<Object> single = Single.create(emitter -> {
                    mCashChanger.setCashCountEventListener(new CashCountListener() {
                        @Override
                        public void onCChangerCashCount(CashChanger cchangerObj, int code, Map<String, Integer> data) {
                            Timber.tag(LOGTAG).i("onCChangerCashCount code: %d", code);
                            _countData = data;
                            emitter.onSuccess(code);
                        }
                    });
                })
                .timeout(10, TimeUnit.SECONDS)
                .onErrorReturnItem(Epos2CallbackCode.CODE_ERR_TIMEOUT);

        try {
            Timber.tag(LOGTAG).d("readCashCount exec");
            mCashChanger.readCashCount();
            epos2CallbackCode = (int) single.blockingGet();
            Timber.tag(LOGTAG).i("onCChangerCashCount epos2CallbackCode = %x", epos2CallbackCode);
        } catch (Epos2Exception e) {
            // エラーが発生したらユーザに通知すべきか？
            epos2ExceptionCode = e.getErrorStatus();
            Timber.tag(LOGTAG).e("onCChangerCashCount epos2ExceptionCode = %x", epos2ExceptionCode);
        }

        if (epos2CallbackCode == Epos2CallbackCode.CODE_SUCCESS) {
            //　正常
            Timber.tag(LOGTAG).i("onCChangerCashCount readCashCount OK");
        } else  {
            //　異常
            oposErrorCode = mCashChanger.getOposErrorCode();
            Timber.tag(LOGTAG).e("onCChangerCashCount readCashCount NG oposErrorCode = %x", oposErrorCode);
        }
        return  _countData;
    }

    public void sendDirectIOCommand(int command, int data,java.lang.String string) {
        try {
            mCashChanger.sendDirectIOCommand(command, data, string);
        } catch (Epos2Exception e) {
            // エラーが発生したらユーザに通知すべきか？
        }

        synchronized (obj) {
            try {
                obj.wait(3 * 1000);
            } catch (Exception e) {
                // 特に何もしない
            }
        }
    }

    private final DirectIOCommandReplyListener mDirectCommandListener = new DirectIOCommandReplyListener() {
        @Override
        public void onCChangerDirectIOCommandReply(CashChanger cchangerObj, int code, int command, int data, String string) {
            if (code == Epos2CallbackCode.CODE_SUCCESS) {

            } else {
                // error
            }
        }
    };

    private final DepositListener mDepositListener = new DepositListener() {
        @Override
        public void onCChangerDeposit(CashChanger cchangerObj, int code, int status, int amount, Map<String, Integer> data) {
            if (code == Epos2CallbackCode.CODE_SUCCESS) {
                switch (status) {
                    case CashChanger.STATUS_BUSY:
                        Timber.tag(LOGTAG).d("STATUS_BUSY in DepositListener");
                        // 開始時と入金時に呼ばれるはず
                        // 入金額を通知
                        if (mDepositAmountListener != null) {
                            Timber.tag(LOGTAG).i("入金額" + amount + "円");
                            mDepositAmountListener.onCChangerDeposit(amount);
                        }
                        break;
//                    case CashChanger.STATUS_END:
//                        // 終了時に呼ばれる
//                        // 釣銭出金指示が出せるようになるはずなので、操作完了待ちを解除
//                        synchronized (obj) {
//                            obj.notify();
//                        }
//                        break;
                    case CashChanger.STATUS_PAUSE:
                        Timber.tag(LOGTAG).d("STATUS_PAUSE in DepositListener");
                        // 一時停止を行うケースがあれば何か処理する
                        break;
                    case CashChanger.STATUS_ERR:
                        Timber.tag(LOGTAG).d("STATUS_ERR in DepositListener");
                        // エラー時はダイアログ等でユーザに通知すべきか？
                        break;
                    default:
                        Timber.tag(LOGTAG).d("unknown status in DepositListener");
                        // ありえないはずなので何もしない
                        break;
                }
            } else {
                Timber.tag(LOGTAG).d("code ERROR in DepositListener");
                // BUSYなら再実行するようにしたい
                // エラーはユーザに通知すべきか？
            }
        }
    };

    private final ConnectionListener mConnectionListener = new ConnectionListener() {
        @Override
        public void onConnection(Object deviceObj, int eventType) {
            new Thread(() -> {
                Timber.tag(LOGTAG).i("onConnection eventType: %d", eventType);
                switch (eventType) {
                    case CashChanger.EVENT_RECONNECTING:
                        Timber.tag(LOGTAG).i("onConnection 再接続開始イベント");
                        break;

                    case CashChanger.EVENT_RECONNECT:
                        Timber.tag(LOGTAG).i("onConnection 再接続完了イベント");
                        break;

                    case CashChanger.EVENT_DISCONNECT:
                        Timber.tag(LOGTAG).i("onConnection 切断イベント");
                        break;

                    default:
                        break;
                }
            }).start();
        }
    };

    private static class DispenseInfo {
        private static View _view = null;
        private static CashChangerPaymentViewModel _cashChangerPaymentActiveViewModel = null;
        private static SharedViewModel _sharedViewModel = null;
        private static int _amount = 0;
        private static int _over = 0;
        private static boolean _isRepay = true;
        private static int _cash = 0;
        private static TransLogger _transLogger;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public CashChangerDispenseError dispense(View view, CashChangerPaymentViewModel cashChangerPaymentActiveViewModel, SharedViewModel sharedViewModel, int amount, int over, boolean isRepay, int pay, TransLogger transLogger, int cash) {
        int epos2ExceptionCode = 0;
        int epos2CallbackCode = Epos2CallbackCode.CODE_ERR_FAILURE;
        DispenseInfo._view = view;
        DispenseInfo._cashChangerPaymentActiveViewModel = cashChangerPaymentActiveViewModel;
        DispenseInfo._sharedViewModel = sharedViewModel;
        DispenseInfo._amount = amount;
        DispenseInfo._over = over;
        DispenseInfo._isRepay = isRepay;
        DispenseInfo._cash = cash;
        DispenseInfo._transLogger= transLogger;

        mOposErrorCode = 0;

        if (mCashChanger == null) {
            Timber.tag(LOGTAG).d("mCashChanger is NULL in dispense()");
            // 何もできない
            epos2CallbackCode = Epos2CallbackCode.CODE_ERR_FAILURE;
        } else {
            @NonNull Single<Object> single = Single.create(emitter -> {
                mCashChanger.setDispenseEventListener(new DispenseListener() {
                    @Override
                    public void onCChangerDispense(CashChanger cchangerObj, int code) {
                        Timber.tag(LOGTAG).i("onCChangerDispense code : %d", code);
                        emitter.onSuccess(code);
                    }
                });
            })
            .timeout(10, TimeUnit.SECONDS)
            .onErrorReturnItem(Epos2CallbackCode.CODE_ERR_TIMEOUT);
            try {
                Timber.tag(LOGTAG).d("dispenseChange exec");
                mCashChanger.dispenseChange(pay);
                epos2CallbackCode = (int) single.blockingGet();
                Timber.tag(LOGTAG).i("onCChangerDispense epos2CallbackCode = %x", epos2CallbackCode);
            } catch (Epos2Exception e) {
                epos2ExceptionCode = e.getErrorStatus();
                Timber.tag(LOGTAG).e("onCChangerDispense epos2ExceptionCode = %x", epos2ExceptionCode);
            }

            if (epos2CallbackCode == Epos2CallbackCode.CODE_SUCCESS) {
                //　正常
                Timber.tag(LOGTAG).i("onCChangerDispense dispenseChange OK");
                print(view, cashChangerPaymentActiveViewModel, amount, over, isRepay, transLogger);
            } else  {
                //　異常
                mOposErrorCode = mCashChanger.getOposErrorCode();
                Timber.tag(LOGTAG).e("onCChangerDispense dispenseChange NG oposErrorCode = %d", mOposErrorCode);
                dispenseError(mOposErrorCode, view);
            }
        }
        return new CashChangerDispenseError(epos2ExceptionCode, epos2CallbackCode, mOposErrorCode);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void print(View view, CashChangerPaymentViewModel cashChangerPaymentActiveViewModel, int amount, int over, boolean isRepay, TransLogger transLogger) {
        Log.d("enter--------", "came");
        Date exDate = new Date();   // 2．日付（今回は現在の日時）を取得
        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.JAPANESE);
        String payTime = dateFmt.format(exDate);

        transLogger.cash(payTime, amount, false);
        int slipId = transLogger.insert();
        transLogger.updateCancelFlg();

        _handler.post(() -> {
            String termSequence = String.valueOf(AppPreference.getTermSequence());
            new Thread(() -> {
                OptionalTransFacade _optionalTransFacade = new OptionalTransFacade(MoneyType.CASH);
                _optionalTransFacade = transLogger.setDataForFacade(_optionalTransFacade);
                _optionalTransFacade.CreateReceiptData(slipId, payTime, termSequence, amount, over); // 取引明細書、取消票、領収書のデータを作成
                _optionalTransFacade.CreateCash(isRepay, payTime, termSequence);
            }).start();

            String msg = "";
            if (!isRepay) {
                makeSound(R.raw.credit_auth_ok);
                msg = "決済処理が完了しました\nおつり " + String.format("%,d", over) + "円";
                // 売上明細書の印刷
                PrinterManager.getInstance().print_trans_cash(view, slipId);
            } else {
                msg = "取消処理が完了しました";
                // 取消票の印刷
                PrinterManager.getInstance().print_cancel_ticket(view, slipId);
            }
            cashChangerPaymentActiveViewModel.setFinishedMessage(msg);
            cashChangerPaymentActiveViewModel.isFinished(true);
            cashChangerPaymentActiveViewModel.setResult(TransactionResults.SUCCESS);
        });
    }

    public void makeSound(@RawRes int id) {
        SoundManager soundManager = SoundManager.getInstance();
        float volume = 0f;
        soundManager.load(MainApplication.getInstance(), id, 1);
        volume =  AppPreference.getSoundPaymentVolume() / 10f;
        float leftVolume = volume;
        float rightVolume = volume;
        soundManager.setOnLoadCompleteListener((soundPool, soundId, status) -> {
            soundPool.play(soundId, leftVolume, rightVolume, 1, 0, 1);
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void dispenseError(int error_code, View view) {
        errView = view;
        _handler.post(() -> {
            CommonErrorDialog commonErrorDialog = new CommonErrorDialog();
            commonErrorDialog.setCommonErrorEventHandlers(this);
            if (error_code == OPOS_ECHAN_OVERDISPENSE) {
                commonErrorDialog.ShowErrorMessage(view.getContext(), MainApplication.getInstance().getString(R.string.error_type_cashchanger_balance_error));
            } else {
                commonErrorDialog.ShowErrorMessage(view.getContext(), MainApplication.getInstance().getString(R.string.error_type_cashchanger_connection_error));
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void endDepositError(int error_code, View view) {
        errView = view;
        _handler.post(() -> {
            CommonErrorDialog commonErrorDialog = new CommonErrorDialog();
            commonErrorDialog.setCommonErrorEventHandlers(this);
            commonErrorDialog.ShowErrorMessage(view.getContext(), MainApplication.getInstance().getString(R.string.error_type_cashchanger_enddeposit_error));
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void onPositiveClick(String errorCode) {
        // はいボタン押下時
        Timber.tag(LOGTAG).i("はいを選択 %s", errorCode);

        new Thread(() -> {
            if (MainApplication.getInstance().getString(R.string.error_type_cashchanger_balance_error).equals(errorCode)) {
                // つり銭不足エラーで「はい」が押下された場合
                Timber.tag(LOGTAG).i("dispense amount = %x", DispenseInfo._amount);
                _handler.post(() -> {
                    DispenseInfo._sharedViewModel.setLoading(true);
                });
                dispense(DispenseInfo._view, DispenseInfo._cashChangerPaymentActiveViewModel, DispenseInfo._sharedViewModel, DispenseInfo._amount, DispenseInfo._over, DispenseInfo._isRepay, DispenseInfo._over, DispenseInfo._transLogger, DispenseInfo._cash);
                _handler.post(() -> {
                    DispenseInfo._sharedViewModel.setLoading(false);
                });
            } else if (MainApplication.getInstance().getString(R.string.error_type_cashchanger_enddeposit_error).equals(errorCode)) {
                // 預り金払い出しエラーで「はい」が押下された場合
            }
        }).start();
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void onNegativeClick(String errorCode) {
        // いいえボタン押下時
        Timber.tag(LOGTAG).i("いいえを選択 %s", errorCode);
        if (MainApplication.getInstance().getString(R.string.error_type_cashchanger_balance_error).equals(errorCode)) {
            // つり銭不足エラーで「いいえ」が押下された場合
            _handler.post(() -> {
                NavigationWrapper.navigate(errView, R.id.action_global_navigation_menu);
            });
        }
    }
    public void onNeutralClick(String errorCode) {
        // キャンセルボタン押下時
    }
    public void onDismissClick(String errorCode) {
        // ボタンを押さずダイアログを閉じた時
//        ReprintStop(errorCode);
    }

    public void setDepositAmountListener(DepositAmountListener depositAmountListener) {
        mDepositAmountListener = depositAmountListener;
    }
}
