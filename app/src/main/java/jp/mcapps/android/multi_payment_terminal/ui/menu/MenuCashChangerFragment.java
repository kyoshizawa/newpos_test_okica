package jp.mcapps.android.multi_payment_terminal.ui.menu;

import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.epson.epos2.Epos2CallbackCode;
import com.epson.epos2.cashchanger.CashChanger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.ScreenData;
import jp.mcapps.android.multi_payment_terminal.SharedViewModel;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentMenuCashChangerBinding;
import jp.mcapps.android.multi_payment_terminal.devices.GloryCashChanger;
import jp.mcapps.android.multi_payment_terminal.model.DeviceConnectivityManager;
import jp.mcapps.android.multi_payment_terminal.thread.printer.PrinterManager;
import timber.log.Timber;

public class MenuCashChangerFragment extends Fragment implements MenuCashChangerEventHandlers {

    public static MenuCashChangerFragment newInstance() {
        return new MenuCashChangerFragment();
    }

    private final String SCREEN_NAME = "つり銭機メニュー";
    private SharedViewModel _sharedViewModel;
    private final MainApplication _app = MainApplication.getInstance();

    private MenuCashChangerViewModel _menuCashChangerViewModel;

    private Thread _thread = null;

    private final String LOGTAG = "MenuCashChangerFragment";

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        final FragmentMenuCashChangerBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_menu_cash_changer, container, false);

        _menuCashChangerViewModel = new ViewModelProvider(this).get(MenuCashChangerViewModel.class);

        binding.setViewModel(_menuCashChangerViewModel);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        binding.setHandlers(this);

        _sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        binding.setSharedViewModel(_sharedViewModel);

        ScreenData.getInstance().setScreenName(SCREEN_NAME);

        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) { super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getCashChangerInfo();
    }

    public void getCashChangerInfo() {
        _thread = new Thread(new Runnable() {
            @Override
            public void run () {
                requireActivity().runOnUiThread(() -> {
                    _menuCashChangerViewModel.setChStatus(0);
                    _menuCashChangerViewModel.setEnableReConnect(false);
                    _menuCashChangerViewModel.setAmountValue();
                });

                GloryCashChanger gloryCashChanger = GloryCashChanger.getInstance();
                if (gloryCashChanger == null) {
                    // これ以降の処理は何もできない
                    Timber.tag(LOGTAG).e("gloryCashChanger null");
                } else {
                    if (gloryCashChanger.connect() == false) {
                        Timber.tag(LOGTAG).e("connect failure");
                    }

                    Map<String, Integer> cashCount = gloryCashChanger.readCashCount();
                    gloryCashChanger.disconnect();

                    // 現在時刻(ミリ秒)を取得
                    long currentTimeMillis = System.currentTimeMillis();

                    // ミリ秒を日付時刻に変換
                    Date currentDate = new Date(currentTimeMillis);

                    // 日付時刻を指定のフォーマットで出力
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                    String cashDate = dateFormat.format(currentDate);

                    requireActivity().runOnUiThread(() -> {
                        if (cashCount != null) {
                            // 接続正常
                            _menuCashChangerViewModel.setChStatus(1);
                            _menuCashChangerViewModel.setEnableReConnect(false);
                            _menuCashChangerViewModel.setAmountValue(cashCount, cashDate);
                        } else {
                            // 接続異常
                            _menuCashChangerViewModel.setChStatus(2);
                            _menuCashChangerViewModel.setEnableReConnect(true);
                            _menuCashChangerViewModel.setAmountValue();
                        }
                    });
                }
            }
        });
        _thread.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (_thread != null) {
            try {
                //スレッドが終了するまで待機
                _thread.join();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onReConnect(View view) {
        getCashChangerInfo();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onCashReturn(View view) {
        Handler handler = new Handler(Looper.getMainLooper());

        new Thread(() -> {
            handler.post(() -> {
                _sharedViewModel.setLoading(true);
            });

            GloryCashChanger gloryCashChanger = GloryCashChanger.getInstance();
            if (gloryCashChanger == null) {
                // これ以降の処理は何もできない
                Timber.tag(LOGTAG).e("gloryCashChanger null in onCashReturn");
            } else {
                if (gloryCashChanger.connect() == false) {
                    Timber.tag(LOGTAG).e("connect failure in onCashReturn");
                }

                gloryCashChanger.endDeposit(CashChanger.DEPOSIT_REPAY, view, _sharedViewModel);
                gloryCashChanger.disconnect();
            }

            handler.post(() -> {
                _sharedViewModel.setLoading(false);
            });
        }).start();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onCashPrint(View view) {
        PrinterManager printerManager = new PrinterManager();
        MenuCashChangerViewModel.AmountValue amountValue = MenuCashChangerViewModel.getAmountValue();
        printerManager.print_cash_history(view, amountValue);
    }
}