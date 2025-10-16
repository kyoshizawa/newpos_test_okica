package jp.mcapps.android.multi_payment_terminal.ui.menu;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.BaseFragment;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.ScreenData;
import jp.mcapps.android.multi_payment_terminal.SharedViewModel;
import jp.mcapps.android.multi_payment_terminal.data.OptionService;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentMenuOthersBinding;

public class MenuOthersFragment extends BaseFragment {

    public static MenuOthersFragment newInstance() {
        return new MenuOthersFragment();
    }

    private SharedViewModel _sharedViewModel;
    private final String SCREEN_NAME = "その他メニュー";
    private final MainApplication _app = MainApplication.getInstance();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
//ADD-S BMT S.Oyama 2024/09/10 フタバ双方向向け改修
        AppPreference.setAmountInputCancel(false);              //金額入力を行えるようにする
//ADD-E BMT S.Oyama 2024/09/10 フタバ双方向向け改修

        final Fragment menuFragment = getParentFragment().getParentFragment();
        final MenuViewModel menuViewModel = new ViewModelProvider(menuFragment, MainApplication.getViewModelFactory()).get(MenuViewModel.class);

        menuViewModel.setBodyType(MenuTypes.OTHERS);

        final FragmentMenuOthersBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_menu_others, container, false);

        binding.setViewModel(menuViewModel);
        binding.setHandlers(new MenuEventHandlersImpl(this, menuViewModel));

        final FragmentActivity activity = getActivity();

        if (activity != null) {
            _sharedViewModel = new ViewModelProvider(activity).get(SharedViewModel.class);
            _sharedViewModel.setScreenMode(SharedViewModel.ScreenMode.MAIN);
            binding.setSharedViewModel(_sharedViewModel);
        }

        ScreenData.getInstance().setScreenName(SCREEN_NAME);

        //有効性確認の名称設定
        binding.textMenuOtherValidation.setText(validationCheckName());

        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //その他決済の有効/無効設定に合わせてボタンを有効/無効化
        (view.findViewById(R.id.btn_menu_other_validation)).setEnabled(validationCheckEnabled());
        (view.findViewById(R.id.btn_menu_other_watari)).setEnabled(validationCheckEnabledWatari());
        (view.findViewById(R.id.btn_menu_other_fixed_amount_postal_order)).setEnabled(validationCheckEnabledFixedAmountPostalOrder());
        (view.findViewById(R.id.btn_menu_other_discount)).setEnabled(false); // 現状、割引の機能がリリースされないため非活性
        (view.findViewById(R.id.btn_menu_other_prepaid)).setEnabled(validationCheckEnabledPrepaid());
    }

    private boolean validationCheckEnabled() {
        final OptionService service = _app.getOptionService();
        return (service != null && service.isAvailable() && service.indexOfFunc(OptionService.FuncIds.MAGNETIC_CARD_VALIDATION) >= 0) || AppPreference.isDemoMode();
    }

    private boolean validationCheckEnabledWatari() {
        final boolean isWatari = AppPreference.isWatariPoint();
        return isWatari || AppPreference.isDemoMode();
    }

    private boolean validationCheckEnabledPrepaid() {
        final boolean isPrepaid = AppPreference.getIsPrepaid();
        return isPrepaid || AppPreference.isDemoMode();
    }

    private boolean validationCheckEnabledFixedAmountPostalOrder() {
        final boolean isFixedAmountPostalOrder = AppPreference.isFixedAmountPostalOrder();
        final boolean isServicePos = AppPreference.isServicePos();
        return ( isFixedAmountPostalOrder && isServicePos ) || AppPreference.isDemoMode();
    }

    private String validationCheckName() {
        final OptionService service = _app.getOptionService();

        int index = service != null ? service.indexOfFunc(OptionService.FuncIds.MAGNETIC_CARD_VALIDATION) : -1;

        return index >= 0
                ? service.getFunc(index).getDisplayName()
                : _app.getString(R.string.btn_other_validation);
    }

//ADD-S BMT S.Oyama 2024/09/03 フタバ双方向向け改修
    /******************************************************************************/
    /*!
     * @brief  立替払いを使用するか？
     * @note   立替払いを使用するか返す
     * @param [in] なし
     * @retval なし
     * @return　boolean
     * @private
     */
    /******************************************************************************/
//    private boolean AdvancePayCheckEnabled() {
//        final boolean isAdvancePay = true;
//        return isAdvancePay || AppPreference.isDemoMode();
//    }

    /******************************************************************************/
    /*!
     * @brief  分割払いを使用するか？
     * @note   分割払いを使用するか返す
     * @param [in] なし
     * @retval なし
     * @return　boolean
     * @private
     */
    /******************************************************************************/
//    private boolean InstallmentPayCheckEnabled() {
//        final boolean isInstallmentPay = true;
//        return isInstallmentPay || AppPreference.isDemoMode();
//    }
//ADD-E BMT S.Oyama 2024/09/03 フタバ双方向向け改修

}