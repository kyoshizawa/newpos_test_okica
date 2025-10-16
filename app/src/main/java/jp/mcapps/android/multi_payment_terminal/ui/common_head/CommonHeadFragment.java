package jp.mcapps.android.multi_payment_terminal.ui.common_head;

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
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.CommonClickEvent;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.SharedViewModel;
import jp.mcapps.android.multi_payment_terminal.data.CurrentRadio;
import jp.mcapps.android.multi_payment_terminal.database.DBManager;
import jp.mcapps.android.multi_payment_terminal.database.history.error.ErrorStackingData;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentCommonHeadBinding;
import jp.mcapps.android.multi_payment_terminal.ui.error.CommonErrorDialog;
import jp.mcapps.android.multi_payment_terminal.ui.error.ErrorStackingEventHandler;

public class CommonHeadFragment extends Fragment implements ErrorStackingEventHandler {

    public static CommonHeadFragment newInstance() {
        return new CommonHeadFragment();
    }

    private CommonHeadViewModel _commonHeadViewModel;
    private SharedViewModel _sharedViewModel;
    private CommonErrorDialog _commonErrorDialog;
    private Disposable _disposable;
    private final CompositeDisposable _compositeDisposable = new CompositeDisposable();
    private BroadcastReceiver _receiver;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        _commonHeadViewModel = new ViewModelProvider(this).get(CommonHeadViewModel.class);

        final FragmentCommonHeadBinding binding =
                DataBindingUtil.inflate(inflater, R.layout.fragment_common_head, container, false);

        binding.setViewModel(_commonHeadViewModel);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        // スタックエラー発生時のイベントハンドラ設定
        _commonErrorDialog = new CommonErrorDialog();
        _commonErrorDialog.setErrorStackingEventHandler(this);

        // ワーニング画像をタッチしたときのイベント
        final ImageView imageView = binding.getRoot().findViewById(R.id.image_common_head_warning);
        imageView.setOnTouchListener( new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(_commonHeadViewModel.getWarningImage().getValue() != null) {
                    CommonClickEvent.RecordClickOperation("スタックエラー", true);
                    _commonErrorDialog.ShowErrorStackMessage(getContext());
                }
                return false;
            }
        });

        final FragmentActivity activity = getActivity();
        if (activity != null) {
            _sharedViewModel =
                    new ViewModelProvider(activity).get(SharedViewModel.class);

            binding.setSharedViewModel(_sharedViewModel);
        }

        int[] radioImageResources = {R.drawable.ic_radio_level_low, R.drawable.ic_radio_level_middle, R.drawable.ic_radio_level_high, R.drawable.ic_airplane_mode};
        _commonHeadViewModel.setRadioImageResource(AppPreference.isDemoMode()
                ? radioImageResources[2]                                //デモモードは強固定
                : radioImageResources[CurrentRadio.getImageLevel()]);   //電波レベルの初期画像
        _receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                _commonHeadViewModel.setRadioImageResource(radioImageResources[CurrentRadio.getImageLevel()]);
            }
        };

        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //エラー・警告画像の設定
        _disposable = DBManager.getErrorStackingDao().getAllFlowable()
                .subscribeOn(Schedulers.io())
                .subscribe(errorStackingDataList -> requireActivity().runOnUiThread(() -> {
                            if (errorStackingDataList == null || errorStackingDataList.size() == 0) {
                                // エラー画像非表示
                                _commonHeadViewModel.resetWarningImage();
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
                                    _commonHeadViewModel.setErrorImage();
                                } else {
                                    // ワーニング画像表示
                                    _commonHeadViewModel.setWarningImage();
                                }
                            }
                        })
                );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        _sharedViewModel.setScreenInversion(false);
    }

    @Override
    public void onPause() {
        _compositeDisposable.clear();
        if (!AppPreference.isDemoMode()) {
            LocalBroadcastManager.getInstance(requireActivity()).unregisterReceiver(_receiver); //通常モードのみ電波レベルの変更通知を受け取るためここで解除
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        _compositeDisposable.add(_disposable);
        if (!AppPreference.isDemoMode()) {
            IntentFilter intentFilter = new IntentFilter("CHANGE_RADIO_LEVEL_IMAGE");
            LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(_receiver, intentFilter); //通常モードのみ電波レベルの変更通知を受け取る
        }
        super.onResume();
    }

    @Override
    public void onStacking() {
        if (_commonErrorDialog.hasError()) {
            // エラー画像表示
            _commonHeadViewModel.setErrorImage();
        } else {
            // ワーニング画像表示
            _commonHeadViewModel.setWarningImage();
        }
    }

    @Override
    public void onStackClear() {
        // ワーニング画像非表示
        _commonHeadViewModel.resetWarningImage();
    }
}