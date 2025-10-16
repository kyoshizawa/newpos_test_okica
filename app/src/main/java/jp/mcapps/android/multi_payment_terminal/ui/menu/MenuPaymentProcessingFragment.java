package jp.mcapps.android.multi_payment_terminal.ui.menu;
//ADD-S BMT S.Oyama 2024/09/05 フタバ双方向向け改修

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
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentMenuPaymentprocessingBinding;

public class MenuPaymentProcessingFragment extends BaseFragment {

    public static MenuPaymentProcessingFragment newInstance() {
        return new MenuPaymentProcessingFragment();
    }

    private SharedViewModel _sharedViewModel;
    private final String SCREEN_NAME = "決済業務ボタンメニュー";
    private final MainApplication _app = MainApplication.getInstance();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        AppPreference.setAmountInputCancel(true);              //金額入力を行えないようにする

        final Fragment menuFragment = getParentFragment().getParentFragment();
        final MenuViewModel menuViewModel = new ViewModelProvider(menuFragment, MainApplication.getViewModelFactory()).get(MenuViewModel.class);

        menuViewModel.setBodyType(MenuTypes.OTHERS);

        final FragmentMenuPaymentprocessingBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_menu_paymentprocessing, container, false);

        binding.setViewModel(menuViewModel);
        binding.setHandlers(new MenuEventHandlersImpl(this, menuViewModel));

        final FragmentActivity activity = getActivity();

        if (activity != null) {
            _sharedViewModel = new ViewModelProvider(activity).get(SharedViewModel.class);
            _sharedViewModel.setScreenMode(SharedViewModel.ScreenMode.MAIN);
            binding.setSharedViewModel(_sharedViewModel);
        }

        ScreenData.getInstance().setScreenName(SCREEN_NAME);

        ////有効性確認の名称設定
        //binding.textMenuOtherValidation.setText(validationCheckName());

        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //分別メニューの有効/無効設定に合わせてボタンを有効/無効化
        (view.findViewById(R.id.btn_menu_paymentprocessing_receipt)).setEnabled(receiptCheckEnabled());
        (view.findViewById(R.id.btn_menu_paymentprocessing_ticket)).setEnabled(ticketCheckEnabled());
        (view.findViewById(R.id.btn_menu_paymentprocessing_discount)).setEnabled(discountCheckEnabled());
        (view.findViewById(R.id.btn_menu_paymentprocessing_empty4)).setEnabled(false);
        (view.findViewById(R.id.btn_menu_paymentprocessing_empty5)).setEnabled(false);
        (view.findViewById(R.id.btn_menu_paymentprocessing_empty6)).setEnabled(false);
    }

    /******************************************************************************/
    /*!
     * @brief  領収書を使用するか？
     * @note   領収書を使用するか返す
     * @param [in] なし
     * @retval なし
     * @return　boolean
     * @private
     */
    /******************************************************************************/
    private boolean receiptCheckEnabled() {
        final boolean isReceipt = true;
        return isReceipt || AppPreference.isDemoMode();

    }

    /******************************************************************************/
    /*!
     * @brief  チケット伝票を使用するか？
     * @note   チケット伝票を使用するか返す
     * @param [in] なし
     * @retval なし
     * @return　boolean
     * @private
     */
    /******************************************************************************/
    private boolean ticketCheckEnabled() {
        final boolean isTicket = true;
        return isTicket || AppPreference.isDemoMode();
    }

//    private String validationCheckName() {
//        final OptionService service = _app.getOptionService();
//
//        int index = service != null ? service.indexOfFunc(OptionService.FuncIds.MAGNETIC_CARD_VALIDATION) : -1;
//
//        return index >= 0
//                ? service.getFunc(index).getDisplayName()
//                : _app.getString(R.string.btn_other_validation);
//    }

    /******************************************************************************/
    /*!
     * @brief  割引を使用するか？
     * @note   割引を使用するか返す
     * @param [in] なし
     * @retval なし
     * @return　boolean
     * @private
     */
    /******************************************************************************/
    private boolean discountCheckEnabled() {
        final boolean isDiscount = true;
        return isDiscount || AppPreference.isDemoMode();
    }


}
//ADD-E BMT S.Oyama 2024/09/05 フタバ双方向向け改修
