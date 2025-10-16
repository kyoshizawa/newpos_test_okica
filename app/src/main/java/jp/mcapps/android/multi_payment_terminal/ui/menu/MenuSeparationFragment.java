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
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentMenuSeparationBinding;
import jp.mcapps.android.multi_payment_terminal.ui.amount_input.AmountInputSeparationPayFDViewModel;

public class MenuSeparationFragment extends BaseFragment {

    public static MenuSeparationFragment newInstance() {
        return new MenuSeparationFragment();
    }

    private SharedViewModel _sharedViewModel;
    private final String SCREEN_NAME = "分別メニュー";
    private final MainApplication _app = MainApplication.getInstance();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        AppPreference.setAmountInputCancel(true);              //金額入力を行えないようにする

        final Fragment menuFragment = getParentFragment().getParentFragment();
        final MenuViewModel menuViewModel = new ViewModelProvider(menuFragment, MainApplication.getViewModelFactory()).get(MenuViewModel.class);

        menuViewModel.setBodyType(MenuTypes.OTHERS);

        final FragmentMenuSeparationBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_menu_separation, container, false);

        binding.setViewModel(menuViewModel);
        binding.setHandlers(new MenuEventHandlersImpl(this, menuViewModel));

        final FragmentActivity activity = getActivity();

        if (activity != null) {
            _sharedViewModel = new ViewModelProvider(activity).get(SharedViewModel.class);
            _sharedViewModel.setScreenMode(SharedViewModel.ScreenMode.MAIN);
            binding.setSharedViewModel(_sharedViewModel);
        }

        ScreenData.getInstance().setScreenName(SCREEN_NAME);

//DEL-S BMT S.Oyama 2024/09/05 フタバ双方向向け改修
        ////有効性確認の名称設定
        //binding.textMenuOtherValidation.setText(validationCheckName());
//DEL-E BMT S.Oyama 2024/09/05 フタバ双方向向け改修

        //ADD-S BMT S.Oyama 2025/01/08 フタバ双方向向け改修
        _sharedViewModel.setBackAction(() -> {
            _sharedViewModel.setBackAction(null);
        });
        //ADD-E BMT S.Oyama 2025/01/08 フタバ双方向向け改修

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
//        (view.findViewById(R.id.btn_menu_separation_ticket)).setEnabled(ticketCheckEnabled());
        (view.findViewById(R.id.btn_menu_separation_credit)).setEnabled(creditCheckEnabled());
        (view.findViewById(R.id.btn_menu_separation_emoney)).setEnabled(emoneyCheckEnabled());
        (view.findViewById(R.id.btn_menu_separation_qr)).setEnabled(qrCheckEnabled());
        (view.findViewById(R.id.btn_menu_separation_prepaid)).setEnabled(prepaidCheckEnabled());
        (view.findViewById(R.id.btn_menu_separation_cancel)).setEnabled(cancelCheckEnabled());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        //ADD-S BMT S.Oyama 2025/01/08 フタバ双方向向け改修
        _sharedViewModel.setBackAction(null);
        //ADD-E BMT S.Oyama 2025/01/08 フタバ双方向向け改修
    }


    /******************************************************************************/
    /*!
     * @brief  チケットを使用するか？
     * @note   チケットを使用するか返す
     * @param [in] なし
     * @retval なし
     * @return　boolean
     * @private
     */
    /******************************************************************************/
    private boolean ticketCheckEnabled() {
        //final OptionService service = _app.getOptionService();
        //return (service != null && service.isAvailable() && service.indexOfFunc(OptionService.FuncIds.MAGNETIC_CARD_VALIDATION) >= 0) || AppPreference.isDemoMode();
        final boolean isTicket = true;
        return isTicket || AppPreference.isDemoMode();

    }

    /******************************************************************************/
    /*!
     * @brief  クレジットを使用するか？
     * @note   クレジットを使用するか返す
     * @param [in] なし
     * @retval なし
     * @return　boolean
     * @private
     */
    /******************************************************************************/
    private boolean creditCheckEnabled() {
        //final boolean isWatari = AppPreference.isWatariPoint();
        //return isWatari || AppPreference.isDemoMode();
        final boolean isCredit = AppPreference.isMoneyCredit();
        return isCredit || AppPreference.isDemoMode();
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
     * @brief  電子マネーを使用するか？
     * @note   電子マネーを使用するか返す
     * @param [in] なし
     * @retval なし
     * @return　boolean
     * @private
     */
    /******************************************************************************/
    private boolean emoneyCheckEnabled() {
        final boolean isEmoney = AppPreference.isMoneySuica() || AppPreference.isMoneyId() || AppPreference.isMoneyEdy() ||
                                 AppPreference.isMoneyNanaco() || AppPreference.isMoneyQuicpay();
        return isEmoney || AppPreference.isDemoMode();
    }

    /******************************************************************************/
    /*!
     * @brief  プリペイドを使用するか？
     * @note   プリペイドを使用するか返す
     * @param [in] なし
     * @retval なし
     * @return　boolean
     * @private
     */
    /******************************************************************************/
    private boolean prepaidCheckEnabled() {
        final boolean isPrepaid = AppPreference.getIsPrepaid();
        return isPrepaid || AppPreference.isDemoMode();
    }

    /******************************************************************************/
    /*!
     * @brief  分別取消を使用するか？
     * @note   分別取消を使用するか返す
     * @param [in] なし
     * @retval なし
     * @return　boolean
     * @private
     */
    /******************************************************************************/
    private boolean cancelCheckEnabled() {
        final boolean isCancel = true;
        return isCancel || AppPreference.isDemoMode();
    }

    /******************************************************************************/
    /*!
     * @brief  QRを使用するか？
     * @note   QRを使用するか返す
     * @param [in] なし
     * @retval なし
     * @return　boolean
     * @private
     */
    /******************************************************************************/
    private boolean qrCheckEnabled() {
        final boolean isQR = AppPreference.isMoneyQr();
        return isQR || AppPreference.isDemoMode();
    }

}
//ADD-E BMT S.Oyama 2024/09/05 フタバ双方向向け改修
