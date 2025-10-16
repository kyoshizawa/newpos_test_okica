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
import jp.mcapps.android.multi_payment_terminal.CommonClickEvent;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.NavigationWrapper;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.ScreenData;
import jp.mcapps.android.multi_payment_terminal.SharedViewModel;
import jp.mcapps.android.multi_payment_terminal.data.Amount;
import jp.mcapps.android.multi_payment_terminal.data.OptionService;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentMenuSeparationWithTicketBinding;
import jp.mcapps.android.multi_payment_terminal.ui.amount_input.AmountInputSeparationPayFDViewModel;
import jp.mcapps.android.multi_payment_terminal.ui.dialog.ConfirmDialog;
import timber.log.Timber;

public class MenuSeparationWithTicketFragment extends BaseFragment {

    public static MenuSeparationWithTicketFragment newInstance() {
        return new MenuSeparationWithTicketFragment();
    }

    private SharedViewModel _sharedViewModel;
    private final String SCREEN_NAME = "分別メニュー：チケット＆現金クレカ電子マネーQR";
    private final MainApplication _app = MainApplication.getInstance();
    private MenuEventHandlersImpl _handlers;
    private View _view;
    private MenuViewModel _menuViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        AppPreference.setAmountInputCancel(true);              //金額入力を行えないようにする

        //final Fragment menuFragment = getParentFragment().getParentFragment();
        //final MenuViewModel menuViewModel = new ViewModelProvider(menuFragment, MainApplication.getViewModelFactory()).get(MenuViewModel.class);
        _menuViewModel = new ViewModelProvider(getActivity(), MainApplication.getViewModelFactory()).get(MenuViewModel.class);


        _menuViewModel.setBodyType(MenuTypes.OTHERS);

        final FragmentMenuSeparationWithTicketBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_menu_separation_with_ticket, container, false);

        binding.setViewModel(_menuViewModel);
        _handlers = new MenuEventHandlersImpl(this, _menuViewModel);
        binding.setHandlers(_handlers);

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

        //ADD-S BMT S.Oyama 2025/01/07 フタバ双方向向け改修
        _view = this.getView();
        _sharedViewModel.setBackAction(() -> {
            showCancelConfirmDialog();
        });
        //ADD-E BMT S.Oyama 2025/01/07 フタバ双方向向け改修


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
        (view.findViewById(R.id.btn_menu_separationwithticket_money)).setEnabled(moneyCheckEnabled());
        (view.findViewById(R.id.btn_menu_separationwithticket_credit)).setEnabled(creditCheckEnabled());
        (view.findViewById(R.id.btn_menu_separationwithticket_emoney)).setEnabled(emoneyCheckEnabled());
        (view.findViewById(R.id.btn_menu_separationwithticket_qr)).setEnabled(qrCheckEnabled());
        (view.findViewById(R.id.btn_menu_separationwithticket_prepaid)).setEnabled(prepaidCheckEnabled());
        (view.findViewById(R.id.btn_menu_separationwithticket_cancel)).setEnabled(cancelCheckEnabled());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        _sharedViewModel.setBackAction(null);
    }

    /******************************************************************************/
    /*!
     * @brief  現金を使用するか？
     * @note   現金を使用するか返す
     * @param [in] なし
     * @retval なし
     * @return　boolean
     * @private
     */
    /******************************************************************************/
    private boolean moneyCheckEnabled() {
        //final OptionService service = _app.getOptionService();
        //return (service != null && service.isAvailable() && service.indexOfFunc(OptionService.FuncIds.MAGNETIC_CARD_VALIDATION) >= 0) || AppPreference.isDemoMode();
        final boolean isMoney = true;
        return isMoney || AppPreference.isDemoMode();

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
        final boolean isCredit = true;
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
        final boolean isEmoney = true;
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
        //final boolean isPrepaid = true;
        //return isPrepaid || AppPreference.isDemoMode();
        return false;
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
        final boolean isQR = true;
        return isQR || AppPreference.isDemoMode();
    }

    /******************************************************************************/
    /*!
     * @brief  本画面中戻るボタンを押下時に続行確認するダイアログ表示
     * @note   中段確認ダイアログを表示させる．中断時は入力内容を破棄する
     * @param [in] View view
     * @retval なし
     * @return　なし
     * @private
     */
    /******************************************************************************/
    private void showCancelConfirmDialog() {

        Timber.i("分割チケット払いを取消します．よろしいですか？");
        final String message =
                "分割チケット払いを取消します\nよろしいですか？";
        ConfirmDialog.newInstance("【分割チケット払い取消確認】",message, () -> {
            CommonClickEvent.RecordClickOperation("はい", "取消確認", false);
            //disposables.clear();
            _sharedViewModel.setBackAction(null);

            boolean fl = _handlers.navigateToSeparationPayCancelSendCancel(this.getView());    //分割払いキャンセル送信
            if (fl == false)
            {
                return;
            }

            Amount.setTotalChangeAmount(0);         // 変更金額を初期化
            Amount.setTicketAmount(0);              // チケット金額を初期化

            _menuViewModel.onResumeExt();      // Amount系変数を整える

            NavigationWrapper.popBackStack(this);
            NavigationWrapper.popBackStack(this);

        },() ->{
            CommonClickEvent.RecordClickOperation("いいえ", "取消確認", false);
        }).show(getChildFragmentManager(), null);
    }

}
//ADD-E BMT S.Oyama 2024/09/05 フタバ双方向向け改修
