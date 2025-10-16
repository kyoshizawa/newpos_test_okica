package jp.mcapps.android.multi_payment_terminal.ui.update;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.BaseFragment;
import jp.mcapps.android.multi_payment_terminal.CommonClickEvent;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.NavigationWrapper;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.ScreenData;
import jp.mcapps.android.multi_payment_terminal.SharedViewModel;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentUpdateBinding;
import jp.mcapps.android.multi_payment_terminal.databinding.VersionDialogBinding;
import jp.mcapps.android.multi_payment_terminal.ui.dialog.BaseDialogFragment;
import jp.mcapps.android.multi_payment_terminal.ui.dialog.ConfirmDialog;

public class UpdateFragment extends BaseFragment implements UpdateEventHandlers {
    private static final String SCREEN_NAME = "更新画面";
    private static final int TAP_COUNT_TO_DEVELOPER = 10;
    private static Toast _toast;
    private int _tapCount = 0;

    public static UpdateFragment newInstance() {
        return new UpdateFragment();
    }
    private UpdateViewModel _updateViewModel;
    private SharedViewModel _sharedViewModel;
    private Handler _handler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        final FragmentUpdateBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_update, container, false);

        _updateViewModel = new ViewModelProvider(this, MainApplication.getViewModelFactory()).get(UpdateViewModel.class);
        _sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        binding.setHandlers(this);
        binding.setViewModel(_updateViewModel);
        binding.setLifecycleOwner(getViewLifecycleOwner());
        binding.versionAppInfo.setOnTouchListener((v, event) -> !AppPreference.isDeveloperMode());  //開発者モードに入れる設定の時のみタッチイベントを有効化

        _updateViewModel.getPrepaidAppVersion();

        ScreenData.getInstance().setScreenName(SCREEN_NAME);

        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();

        _updateViewModel.setScreenTimeout(true); //インストール画面でキャンセルした場合はここで画面OFF有効
        // アプリ更新画面でキャンセルされてしまった時のための処理
        _updateViewModel.checkUpdateCancel();
        _sharedViewModel.setBackVisibleFlag(_updateViewModel.getStage().getValue() == UpdateViewModel.Stages.Top);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        _updateViewModel.getStage().observe(getViewLifecycleOwner(), stage -> {
            _sharedViewModel.setBackVisibleFlag(stage == UpdateViewModel.Stages.Top);
        });
    }

    @Override
    public void onVersionClick(View view) {
        if (_toast != null) _toast.cancel();

        final FragmentActivity activity = getActivity();
        if (activity == null) return;

        _tapCount += 1;

        if (_tapCount >= TAP_COUNT_TO_DEVELOPER) {
            view.post(() -> {
                NavigationWrapper.navigate(view, R.id.action_navigation_update_to_developer_menu_navigation);
            });
        } else {
            String msg = String.format(
                    "あと%s回タップすると開発者画面に遷移します", TAP_COUNT_TO_DEVELOPER - _tapCount);

            _toast = Toast.makeText(activity, msg, Toast.LENGTH_LONG);
            _toast.show();
        }
    }

    @Override
    public void onOnlineUpdate(View view, boolean isPreUpdate) {
        if (getChildFragmentManager().getFragments().size() > 0) return;

        CommonClickEvent.RecordButtonClickOperation(view, true);

        _updateViewModel.setScreenTimeout(false);
        _updateViewModel.updateCheck(isPreUpdate);
//        ConfirmDialog.newInstance("更新しますか？", "この操作は取り消すことができません", () -> {
//            new Thread(() -> {
//                _updateViewModel.updateCheck(isPreUpdate);
//            }).start();
//        }).show(getChildFragmentManager(), null);
    }
    @Override
    public void onPrepaidAppOnlineUpdate(View view, boolean isPreUpdate) {
        if (getChildFragmentManager().getFragments().size() > 0) return;

        CommonClickEvent.RecordButtonClickOperation(view, true);

        _updateViewModel.setScreenTimeout(false);
        _updateViewModel.prepaidAppUpdateCheck(isPreUpdate);
//        ConfirmDialog.newInstance("更新しますか？", "この操作は取り消すことができません", () -> {
//            new Thread(() -> {
//                _updateViewModel.updateCheck(isPreUpdate);
//            }).start();
//        }).show(getChildFragmentManager(), null);
    }

    @Override
    public void onUSBUpdate(View view) {
        CommonClickEvent.RecordButtonClickOperation(view, true);
        _updateViewModel.setScreenTimeout(false);
        final String packageName = "com.newpos.appmanager";
        final String className = "com.newpos.appmanager.Main";

        final Intent intent = new Intent()
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)  // 多重起動防止 SINGLE_TOPはダメだった
                .setClassName(packageName, className);

        startActivity(intent);
    }

    @Override
    public void onClose(View view) {
        CommonClickEvent.RecordButtonClickOperation(view, true);
        _updateViewModel.setScreenTimeout(true);
        _updateViewModel.setStage(UpdateViewModel.Stages.Top);
        _updateViewModel.setResultMessage(null);
    }

    @Override
    public void onVersionDetailClick(View view) {
        if (getChildFragmentManager().getFragments().size() > 0) return;

        CommonClickEvent.RecordButtonClickOperation(view, true);

        new VersionDialog().show(getChildFragmentManager(), null);
    }

    @Override
    public void onUpdateClick(View view) {
        _updateViewModel.update();
    }

    @Override
    public void onCancelClick(View view) {
        CommonClickEvent.RecordButtonClickOperation(view, true);
        _updateViewModel.setScreenTimeout(true);
        _updateViewModel.setStage(UpdateViewModel.Stages.Top);
        _updateViewModel.setResultMessage(null);
    }

    static public class VersionDialog extends BaseDialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            // 画面外のタップを無効化
            this.setCancelable(false);

            final VersionDialogBinding binding = DataBindingUtil.inflate(
                    LayoutInflater.from(getActivity()), R.layout.version_dialog, null, false);

            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                    .setView(binding.getRoot())
                    .setPositiveButton("閉じる", (dialog, which) -> { dialog.dismiss(); });

            return builder.create();
        }
    }
}