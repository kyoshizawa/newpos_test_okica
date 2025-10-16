package jp.mcapps.android.multi_payment_terminal.ui.menu;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.CommonClickEvent;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.SharedViewModel;
import jp.mcapps.android.multi_payment_terminal.data.CurrentRadio;
import jp.mcapps.android.multi_payment_terminal.data.IFBoxAppModels;
import jp.mcapps.android.multi_payment_terminal.database.DBManager;
import jp.mcapps.android.multi_payment_terminal.database.history.error.ErrorStackingData;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentMenuHeadBinding;
//import jp.mcapps.android.multi_payment_terminal.model.IFBoxManager;
import jp.mcapps.android.multi_payment_terminal.ui.error.CommonErrorDialog;
import jp.mcapps.android.multi_payment_terminal.ui.error.CommonErrorEventHandlers;
import jp.mcapps.android.multi_payment_terminal.ui.error.ErrorStackingEventHandler;
import timber.log.Timber;

public class MenuHeadFragment extends Fragment implements ErrorStackingEventHandler {

    public static MenuHeadFragment newInstance() {
        return new MenuHeadFragment();
    }

    public static MenuViewModel _menuViewModel;
    private CommonErrorDialog _commonErrorDialog;
    private Disposable _disposable;
    private final CompositeDisposable _compositeDisposable = new CompositeDisposable();
    private BroadcastReceiver _receiver;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        final Fragment menuFragment = getParentFragment().getParentFragment();

        final FragmentMenuHeadBinding binding =
                DataBindingUtil.inflate(inflater, R.layout.fragment_menu_head, container, false);

        _menuViewModel = new ViewModelProvider(menuFragment, MainApplication.getViewModelFactory()).get(MenuViewModel.class);

        _menuViewModel.setHeadType(MenuTypes.HEAD);



        binding.setViewModel(_menuViewModel);
        binding.setHandlers(new MenuEventHandlersImpl(this, _menuViewModel));
        binding.setLifecycleOwner(getViewLifecycleOwner());

        // スタックエラー発生時のイベントハンドラ設定
        _commonErrorDialog = new CommonErrorDialog();
        _commonErrorDialog.setErrorStackingEventHandler(this);

        if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D)) {          //フタバ双方向時 コールドスタートを実施（起動時初回１回　or 接続が切れるたび）
            final Fragment fragment = getParentFragment().getParentFragment();
            MenuViewModel menuViewModel = new ViewModelProvider(fragment, MainApplication.getViewModelFactory()).get(MenuViewModel.class);
            _commonErrorDialog.setCommonErrorEventHandlers(new CommonErrorEventHandlers() {
                @Override
                public void onPositiveClick(String errorCode) {
//                    if (errorCode.equals(getString(R.string.error_type_FutabaD_FareUp_Warning))) {
//                        menuViewModel.getIFBoxManager().send820_KeyCode(IFBoxManager.SendMeterDataStatus_FutabaD.RECEIPT_PRINT, 34, false); // 現金キーを送信
//                    }
                }

                @Override
                public void onNegativeClick(String errorCode) {
                }

                @Override
                public void onNeutralClick(String errorCode) {
                }

                @Override
                public void onDismissClick(String errorCode) {
                }
            });
        }

        // ワーニング画像をタッチしたときのイベント
        final ImageView imageView = binding.getRoot().findViewById(R.id.image_menu_head_warning);
        imageView.setOnTouchListener( new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(_menuViewModel.getWarningImage().getValue() != null) {
                    CommonClickEvent.RecordClickOperation("スタックエラー", true);
                    _commonErrorDialog.ShowErrorStackMessage(getContext());
                }
                return false;
            }
        });

        int[] radioImageResources = {R.drawable.ic_radio_level_low, R.drawable.ic_radio_level_middle, R.drawable.ic_radio_level_high, R.drawable.ic_airplane_mode};
        _menuViewModel.setRadioImageResource(AppPreference.isDemoMode()
                ? radioImageResources[2]                                //デモモードは強固定
                : radioImageResources[CurrentRadio.getImageLevel()]);   //電波レベルの初期画像
        _receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                _menuViewModel.setRadioImageResource(radioImageResources[CurrentRadio.getImageLevel()]);
            }
        };

        getLifecycle().addObserver(_menuViewModel);

        SharedViewModel _sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        binding.setSharedViewModel(_sharedViewModel);

        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onPause() {
        _compositeDisposable.clear();
        Timber.d("onPause()->_compositeDisposable.clear()");
        if (!AppPreference.isDemoMode()) {
            LocalBroadcastManager.getInstance(requireActivity()).unregisterReceiver(_receiver); //通常モードのみ電波レベルの変更通知を受け取るためここで解除
        }
        super.onPause();
    }

    @Override
    public void onResume() {

        //エラー・警告画像の設定
        _disposable = DBManager.getErrorStackingDao().getAllFlowable()
                .subscribeOn(Schedulers.io())
                .subscribe(errorStackingDataList -> getActivity().runOnUiThread(() -> {
                            if (errorStackingDataList == null || errorStackingDataList.size() == 0) {
                                // エラー画像非表示
                                _menuViewModel.resetWarningImage();
                                Timber.d("onResume()->エラー画像非表示");
                            } else {
                                boolean hasError = false;
                                for (ErrorStackingData data : errorStackingDataList) {
                                    if (data.level == Integer.parseInt(getString(R.string.level_error))) {
                                        hasError = true;
                                        break;
                                    }
                                }
                                if (hasError) {
                                    // エラー画像表示
                                    _menuViewModel.setErrorImage();
                                    Timber.d("onResume()->エラー画像表示");
                                } else {
                                    // ワーニング画像表示
                                    _menuViewModel.setWarningImage();
                                    Timber.d("onResume()->ワーニング画像表示");
                                }
                            }
                        })
                );

        _compositeDisposable.add(_disposable);
        Timber.d("onResume()->_compositeDisposable.add(_disposable)");

        if (!AppPreference.isDemoMode()) {
            IntentFilter intentFilter = new IntentFilter("CHANGE_RADIO_LEVEL_IMAGE");
            intentFilter.addAction("CHANGE_RADIO_DATA");
            LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(_receiver, intentFilter); //通常モードのみ電波レベルの変更通知を受け取る
        }
        super.onResume();
    }

    @Override
    public void onStacking() {
        if (_commonErrorDialog.hasError()) {
            // エラー画像表示
            _menuViewModel.setErrorImage();
        } else {
            // ワーニング画像表示
            _menuViewModel.setWarningImage();
        }
    }

    @Override
    public void onStackClear() {
        // ワーニング画像非表示
        _menuViewModel.resetWarningImage();
    }
}
