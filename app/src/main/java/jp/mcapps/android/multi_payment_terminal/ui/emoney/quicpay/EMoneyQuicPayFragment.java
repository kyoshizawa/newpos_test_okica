package jp.mcapps.android.multi_payment_terminal.ui.emoney.quicpay;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.json.JSONException;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.BaseFragment;
import jp.mcapps.android.multi_payment_terminal.CommonClickEvent;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.NavigationWrapper;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.ScreenData;
import jp.mcapps.android.multi_payment_terminal.SharedViewModel;
import jp.mcapps.android.multi_payment_terminal.data.Amount;
import jp.mcapps.android.multi_payment_terminal.data.BusinessType;
import jp.mcapps.android.multi_payment_terminal.data.IFBoxAppModels;
import jp.mcapps.android.multi_payment_terminal.data.JremLcdTextMap;
import jp.mcapps.android.multi_payment_terminal.data.MoneyType;
import jp.mcapps.android.multi_payment_terminal.data.TransMap;
import jp.mcapps.android.multi_payment_terminal.database.DBManager;
import jp.mcapps.android.multi_payment_terminal.database.history.slip.SlipData;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentEmoneyQuicpayBinding;
import jp.mcapps.android.multi_payment_terminal.error.JremRasErrorCodes;
import jp.mcapps.android.multi_payment_terminal.error.JremRasErrorMap;
import jp.mcapps.android.multi_payment_terminal.error.iCASErrorMap;
//import jp.mcapps.android.multi_payment_terminal.iCAS.BusinessParameter;
//import jp.mcapps.android.multi_payment_terminal.iCAS.IiCASClient;
//import jp.mcapps.android.multi_payment_terminal.iCAS.data.DeviceClient;
//import jp.mcapps.android.multi_payment_terminal.iCAS.iCASClient;
//import jp.mcapps.android.multi_payment_terminal.model.IFBoxManager;
//import jp.mcapps.android.multi_payment_terminal.model.OptionalTicketTransFacade;
import jp.mcapps.android.multi_payment_terminal.model.OptionalTransFacade;
import jp.mcapps.android.multi_payment_terminal.model.TransLogger;
import jp.mcapps.android.multi_payment_terminal.thread.felica.FelicaManager;
import jp.mcapps.android.multi_payment_terminal.thread.printer.PrinterManager;
import jp.mcapps.android.multi_payment_terminal.ui.Converters;
import jp.mcapps.android.multi_payment_terminal.ui.PostPaymentProcess;
import jp.mcapps.android.multi_payment_terminal.ui.amount_input.AmountInputSeparationPayFDViewModel;
import jp.mcapps.android.multi_payment_terminal.ui.emoney.ActionBarController;
import jp.mcapps.android.multi_payment_terminal.ui.emoney.LcdController;
import jp.mcapps.android.multi_payment_terminal.ui.emoney.SoundController;
import timber.log.Timber;

public class EMoneyQuicPayFragment extends BaseFragment /*implements EMoneyQuicPayEventHandlers, IiCASClient*/ {
    public static EMoneyQuicPayFragment newInstance() {
        return new EMoneyQuicPayFragment();
    }
    private final String SCREEN_NAME = "QuicPay";

    public static int FORCE_CANCEL_TIMEOUT = 60*1000;  // 中止ボタンを押してから強制的に画面を抜けるまでの時間
    // private final iCASClient _icasClient = iCASClient.getInstance();
    private final Handler _handler = new Handler(Looper.getMainLooper());
    // private EMoneyQuicPayViewModel _eMoneyQuicPayViewModel;
    private SharedViewModel _sharedViewModel;
    private ActionBarController _actionBarController;
    private LcdController _lcd1Controller;
    private LcdController _lcd2Controller;
    private LcdController _lcd3Controller;
    private SoundController _soundController;
    private int _t = 0;
    private Observer<Boolean> _cancelObserver = null;
    private int _balance = 0;
    private String _currentStatus = "";
    private boolean _hasError = false;
    private boolean _backFlag = false;
    private boolean _firstContact = true;
    private int _amount = 0;
    private Integer[] _bar;
    private boolean _isIcasWorking = false;
    private String _sid = null;
    private Integer _slipId = null;
    private boolean _usePrinter = false;
    private int _businessId = -1;
    private Integer _value;
    private TransLogger _transLogger;
    private Integer _currentSound = null;
    private boolean _isChancel = false;
    private boolean _isNoEndError = false;   // 処理未了フラグ（OnError時に未了が発生していたか判別）
    //ADD-S BMT S.Oyama 2025/02/07 フタバ双方向向け改修
    private boolean _is820ResetAbort = false;  // 820側でリセット応答を750へ送付してきた場合（スキャン中の空車等）
    //ADD-E BMT S.Oyama 2025/02/07 フタバ双方向向け改修

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        final FragmentEmoneyQuicpayBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_emoney_quicpay, container, false);

        final FragmentActivity activity = getActivity();
        if (activity != null) {
            _sharedViewModel =
                    new ViewModelProvider(activity).get(SharedViewModel.class);

            binding.setSharedViewModel(_sharedViewModel);
        }

//        _eMoneyQuicPayViewModel = new ViewModelProvider(this).get(EMoneyQuicPayViewModel.class);
//
//        binding.setViewModel(_eMoneyQuicPayViewModel);

        binding.setLifecycleOwner(getViewLifecycleOwner());

//        binding.setHandlers(this);
//
//        _lcd1Controller = new LcdController(_eMoneyQuicPayViewModel.getLcd1());
//        _lcd2Controller = new LcdController(_eMoneyQuicPayViewModel.getLcd2());
//        _lcd3Controller = new LcdController(_eMoneyQuicPayViewModel.getLcd3());

        HashMap<Integer, Integer> soundMap = new HashMap<Integer, Integer>();

        soundMap.put(0x00, R.raw.emoney_touch_default);  // かざし待ち
        soundMap.put(0x03, R.raw.stopsettlement);        // 警告音
        soundMap.put(0x04, R.raw.complete_qp);           // 決済音（"quicpay”）
        soundMap.put(0x06, R.raw.unfinished);            // エラー発生（注意音）
        soundMap.put(0x63, null);  // 鳴動停止

        _soundController = new SoundController(soundMap);

        _transLogger = new TransLogger();

        ScreenData.getInstance().setScreenName(SCREEN_NAME);

        return binding.getRoot();
    }

}