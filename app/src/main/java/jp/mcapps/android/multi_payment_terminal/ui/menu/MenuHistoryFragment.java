package jp.mcapps.android.multi_payment_terminal.ui.menu;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.BaseFragment;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.ScreenData;
import jp.mcapps.android.multi_payment_terminal.data.IFBoxAppModels;
import jp.mcapps.android.multi_payment_terminal.data.OptionService;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentMenuHistoryBinding;
import jp.mcapps.android.multi_payment_terminal.model.IFBoxManager;

public class MenuHistoryFragment extends BaseFragment {

    public static MenuHistoryFragment newInstance() {
        return new MenuHistoryFragment();
    }

    private final String SCREEN_NAME = "履歴メニュー";
    private final MainApplication _app = MainApplication.getInstance();

    //ADD-S BMT S.Oyama 2025/03/14 フタバ双方向向け改修
    private MenuViewModel _menuViewModel;
    private final String BUTTON_BACKCOLOR_ENABLED = "#004D63";
    //ADD-E BMT S.Oyama 2025/03/14 フタバ双方向向け改修

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        final Fragment menuFragment = getParentFragment().getParentFragment();
        //CHG-S BMT S.Oyama 2025/03/14 フタバ双方向向け改修
        _menuViewModel = new ViewModelProvider(menuFragment, MainApplication.getViewModelFactory()).get(MenuViewModel.class);

        _menuViewModel.setBodyType(MenuTypes.HISTORY);

        final FragmentMenuHistoryBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_menu_history, container, false);

        binding.setViewModel(_menuViewModel);
        binding.setHandlers(new MenuEventHandlersImpl(this, _menuViewModel));
        //CHG-E BMT S.Oyama 2025/03/14 フタバ双方向向け改修

        ScreenData.getInstance().setScreenName(SCREEN_NAME);

        // 有効性確認履歴の表示/非表示と名称の設定
        binding.btnMenuHistoryValidationCheck.setVisibility(validationCheckEnabled() ? View.VISIBLE : View.GONE); binding.btnMenuHistoryValidationCheck.setText(String.format("%s履歴", validationCheckName()));

        //集計履歴の表示/非表示の設定（LT-27双方向,OKB双方向連動時は非表示）
        binding.btnMenuHistoryDailyTotal.setVisibility((IFBoxAppModels.isMatch(IFBoxAppModels.YAZAKI_LT27_D) || IFBoxAppModels.isMatch(IFBoxAppModels.OKABE_MS70_D)) ? View.GONE : View.VISIBLE);

        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) { super.onActivityCreated(savedInstanceState);
        //ADD-S BMT S.Oyama 2025/03/14 フタバ双方向向け改修
        if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == true) {
            if (IFBoxManager.meterDataV4Disposable_HistoryMenu == null) {
                IFBoxManager.meterDataV4Disposable_HistoryMenu = _menuViewModel.getIFBoxManager().getMeterInfo()
                        .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(meter -> {
                            ButtonEnabledFromTariff();
                        });
            }
            ButtonEnabledFromTariff();
        }
        //ADD-E BMT S.Oyama 2025/03/14 フタバ双方向向け改修
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    private boolean validationCheckEnabled() {
        final OptionService service = _app.getOptionService();
        return (service != null && service.isAvailable() && service.indexOfFunc(OptionService.FuncIds.MAGNETIC_CARD_VALIDATION) >= 0) || AppPreference.isDemoMode(); }

    private String validationCheckName() {
        final OptionService service = _app.getOptionService();

        int index = service != null ? service.indexOfFunc(OptionService.FuncIds.MAGNETIC_CARD_VALIDATION) : -1;

        return index >= 0
                ? service.getFunc(index).getDisplayName()
                : _app.getString(R.string.btn_other_validation);
    }

    //ADD-S BMT S.Oyama 2025/03/14 フタバ双方向向け改修
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        //レシーバーの削除

        if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == true) {
            if (IFBoxManager.meterDataV4Disposable_HistoryMenu != null) {
                IFBoxManager.meterDataV4Disposable_HistoryMenu.dispose();
                IFBoxManager.meterDataV4Disposable_HistoryMenu = null;
            }
        }
    }

    /******************************************************************************/
    /*!
     * @brief  タリフの変化に合わせてボタンのEnabledを変更する
     * @note   タリフの変化に合わせてボタンのEnabledを変更する
     * @param [in]
     * @retval なし
     * @return　なし
     * @private
     */

    /******************************************************************************/
    private void ButtonEnabledFromTariff() {
        if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == false) {
            return;
        }

        String tmpTariffStatus = _menuViewModel.getIFBoxManager().getMeterStatus();         //現在

        View view = this.getView();

        if (tmpTariffStatus.equals("KUUSYA") == true)           //空車時
        {
            (view.findViewById(R.id.btn_menu_history_daily_total)).setEnabled(true);         //集計印字ボタン有効
            (view.findViewById(R.id.btn_menu_history_daily_total)).setBackgroundColor(Color.parseColor(BUTTON_BACKCOLOR_ENABLED));
        } else {
            (view.findViewById(R.id.btn_menu_history_daily_total)).setEnabled(false);        //集計印字ボタン無効
            (view.findViewById(R.id.btn_menu_history_daily_total)).setBackgroundColor(getResources().getColor(R.color.gray, MainApplication.getInstance().getTheme()));
        }
    }
    //ADD-E BMT S.Oyama 2025/03/14 フタバ双方向向け改修
}