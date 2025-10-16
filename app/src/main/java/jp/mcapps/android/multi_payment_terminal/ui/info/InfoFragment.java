package jp.mcapps.android.multi_payment_terminal.ui.info;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import jp.mcapps.android.multi_payment_terminal.BaseFragment;
import jp.mcapps.android.multi_payment_terminal.NavigationWrapper;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.ScreenData;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentInfoBinding;

public class InfoFragment extends BaseFragment implements InfoEventHandlers {
    private final String SCREEN_NAME = "設定情報";
    private static final int TAP_COUNT_TO_DEVELOPER = 10;
    private static Toast mToast;

    public static InfoFragment newInstance() {
        return new InfoFragment();
    }

    private int tapCount = 0;

    @SuppressLint("ClickableViewAccessibility")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        final InfoViewModel infoViewModel = new ViewModelProvider(this).get(InfoViewModel.class);

        final FragmentInfoBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_info, container, false);

        binding.setViewModel(infoViewModel);
        binding.setHandlers(this);
//        binding.versionAppInfo.setOnTouchListener((v, event) -> !infoViewModel.isDeveloperMode());  //開発者モードに入れる設定の時のみタッチイベントを有効化

        ScreenData.getInstance().setScreenName(SCREEN_NAME);

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
    public void onVersionClick(View view) {
        if (mToast != null) mToast.cancel();

        final FragmentActivity activity = getActivity();
        if (activity == null) return;

        tapCount += 1;

        if (tapCount >= TAP_COUNT_TO_DEVELOPER) {
            view.post(() -> {
                NavigationWrapper.navigate(view, R.id.action_navigation_info_to_developer_menu_navigation);
            });
        } else {
            String msg = String.format(
                    "あと%s回タップすると開発者画面に遷移します", TAP_COUNT_TO_DEVELOPER - tapCount);

            mToast = Toast.makeText(activity, msg, Toast.LENGTH_LONG);
            mToast.show();
        }
    }
}