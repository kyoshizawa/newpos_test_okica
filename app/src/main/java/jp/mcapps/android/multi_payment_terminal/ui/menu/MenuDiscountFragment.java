package jp.mcapps.android.multi_payment_terminal.ui.menu;
//ADD-S BMT S.Oyama 2024/09/05 フタバ双方向向け改修

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import java.util.List;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.BaseFragment;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.ScreenData;
import jp.mcapps.android.multi_payment_terminal.SharedViewModel;
import jp.mcapps.android.multi_payment_terminal.data.Amount;
import jp.mcapps.android.multi_payment_terminal.data.IFBoxAppModels;
import jp.mcapps.android.multi_payment_terminal.data.OptionService;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentMenuDiscountBinding;
import jp.mcapps.android.multi_payment_terminal.model.DiscountInfo;
import jp.mcapps.android.multi_payment_terminal.model.DiscountMenuInfo;
import jp.mcapps.android.multi_payment_terminal.model.IFBoxManager;
import jp.mcapps.android.multi_payment_terminal.thread.printer.PrinterConst;
import jp.mcapps.android.multi_payment_terminal.thread.printer.PrinterManager;
import timber.log.Timber;

public class MenuDiscountFragment extends BaseFragment {

    public static MenuDiscountFragment newInstance() {
        return new MenuDiscountFragment();
    }

    private SharedViewModel _sharedViewModel;
    private final String SCREEN_NAME = "割引ボタンメニュー";
    private final MainApplication _app = MainApplication.getInstance();
    public static final int DISCOUNT_MENU_INFO_REQUEST_CODE = 1110;
    public static final int DISCOUNT_INFO_REQUEST_CODE = 1111;
    private MenuViewModel _menuViewModel;
    private MenuEventHandlersImpl _menuEventHandlers;

    private List<DiscountMenuInfo> _discountMenuInfo = null;
    private FragmentMenuDiscountBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        AppPreference.setAmountInputCancel(true);              //金額入力を行えないようにする

        final Fragment menuFragment = getParentFragment().getParentFragment();
        _menuViewModel = new ViewModelProvider(menuFragment, MainApplication.getViewModelFactory()).get(MenuViewModel.class);

        _menuViewModel.setBodyType(MenuTypes.OTHERS);

        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_menu_discount, container, false);

        binding.setViewModel(_menuViewModel);
        _menuEventHandlers = new MenuEventHandlersImpl(this, _menuViewModel);
        binding.setHandlers(_menuEventHandlers);

        final FragmentActivity activity = getActivity();

        if (activity != null) {
            _sharedViewModel = new ViewModelProvider(activity).get(SharedViewModel.class);
            _sharedViewModel.setScreenMode(SharedViewModel.ScreenMode.MAIN);
            binding.setSharedViewModel(_sharedViewModel);
//            CreateDiscountJobInfo(_sharedViewModel);                                    //割日関連情報の取得メソッド　サーバと通信して情報を取得する
        }

        ScreenData.getInstance().setScreenName(SCREEN_NAME);

//DEL-S BMT S.Oyama 2024/09/05 フタバ双方向向け改修
        ////有効性確認の名称設定
        //binding.textMenuOtherValidation.setText(validationCheckName());
//DEL-S BMT S.Oyama 2024/09/05 フタバ双方向向け改修

        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        _sharedViewModel.getDiscountJobName().observe(getViewLifecycleOwner(), name -> {

            Boolean[] discountJobActiveFl = _sharedViewModel.getDiscountJobActiveFl().getValue();

            //分別メニューの有効/無効設定に合わせてボタンを有効/無効化
            (view.findViewById(R.id.btn_menu_discount_confirmation)).setEnabled(confirmationCheckEnabled());

            for (int i = 0; i < name.length; i++) {
                TextView textView = null;
                ImageView imageView = null;
                //ADD-S BMT S.Oyama 2025/03/19 フタバ双方向向け改修
                Button button = null;
                //ADD-E BMT S.Oyama 2025/03/19 フタバ双方向向け改修
                switch(i) {
                    case 0:
                        (view.findViewById(R.id.btn_menu_discount_job1)).setEnabled(jobCheckEnabled(i + 1));
                        textView = view.findViewById(R.id.text_menu_discount_job1);
                        imageView = view.findViewById(R.id.img_menu_discount_job1);
                        //ADD-S BMT S.Oyama 2025/03/19 フタバ双方向向け改修
                        button = view.findViewById(R.id.btn_menu_discount_job1);
                        //ADD-E BMT S.Oyama 2025/03/19 フタバ双方向向け改修
                        break;
                    case 1:
                        (view.findViewById(R.id.btn_menu_discount_job2)).setEnabled(jobCheckEnabled(i + 1));
                        textView = view.findViewById(R.id.text_menu_discount_job2);
                        imageView = view.findViewById(R.id.img_menu_discount_job2);
                        //ADD-S BMT S.Oyama 2025/03/19 フタバ双方向向け改修
                        button = view.findViewById(R.id.btn_menu_discount_job2);
                        //ADD-E BMT S.Oyama 2025/03/19 フタバ双方向向け改修
                        break;
                    case 2:
                        (view.findViewById(R.id.btn_menu_discount_job3)).setEnabled(jobCheckEnabled(i + 1));
                        textView = view.findViewById(R.id.text_menu_discount_job3);
                        imageView = view.findViewById(R.id.img_menu_discount_job3);
                        //ADD-S BMT S.Oyama 2025/03/19 フタバ双方向向け改修
                        button = view.findViewById(R.id.btn_menu_discount_job3);
                        //ADD-E BMT S.Oyama 2025/03/19 フタバ双方向向け改修
                        break;
                    case 3:
                        (view.findViewById(R.id.btn_menu_discount_job4)).setEnabled(jobCheckEnabled(i + 1));
                        textView = view.findViewById(R.id.text_menu_discount_job4);
                        imageView = view.findViewById(R.id.img_menu_discount_job4);
                        //ADD-S BMT S.Oyama 2025/03/19 フタバ双方向向け改修
                        button = view.findViewById(R.id.btn_menu_discount_job4);
                        //ADD-E BMT S.Oyama 2025/03/19 フタバ双方向向け改修
                        break;
                    case 4:
                        (view.findViewById(R.id.btn_menu_discount_job5)).setEnabled(jobCheckEnabled(i + 1));
                        textView = view.findViewById(R.id.text_menu_discount_job5);
                        imageView = view.findViewById(R.id.img_menu_discount_job5);
                        //ADD-S BMT S.Oyama 2025/03/19 フタバ双方向向け改修
                        button = view.findViewById(R.id.btn_menu_discount_job5);
                        //ADD-E BMT S.Oyama 2025/03/19 フタバ双方向向け改修
                        break;
                }

                if (discountJobActiveFl[i] != null && discountJobActiveFl[i] == true)
                {
                    if (textView != null)
                    {
                        //DEL-S BMT S.Oyama 2025/03/19 フタバ双方向向け改修
                        //textView.setText(name[i]);
                        //textView.setVisibility(View.VISIBLE);
                        //DEL-E BMT S.Oyama 2025/03/19 フタバ双方向向け改修
                    }
                    if (imageView != null)
                    {
                        //DEL-S BMT S.Oyama 2025/03/19 フタバ双方向向け改修
                        //imageView.setVisibility(View.VISIBLE);
                        //DEL-E BMT S.Oyama 2025/03/19 フタバ双方向向け改修
                    }
                    //ADD-S BMT S.Oyama 2025/03/19 フタバ双方向向け改修
                    if (button != null)
                    {
                        button.setText(name[i]);
                    }
                    //ADD-E BMT S.Oyama 2025/03/19 フタバ双方向向け改修
                }
                else
                {
                    if (textView != null)
                    {
                        //DEL-S BMT S.Oyama 2025/03/19 フタバ双方向向け改修
                        //textView.setVisibility(View.INVISIBLE);
                        //DEL-E BMT S.Oyama 2025/03/19 フタバ双方向向け改修
                    }
                    if (imageView != null)
                    {
                        //DEL-S BMT S.Oyama 2025/03/19 フタバ双方向向け改修
                        //imageView.setVisibility(View.INVISIBLE);
                        //DEL-E BMT S.Oyama 2025/03/19 フタバ双方向向け改修
                    }
                    //ADD-S BMT S.Oyama 2025/03/19 フタバ双方向向け改修
                    if (button != null)
                    {
                        button.setText("");
                    }
                    //ADD-E BMT S.Oyama 2025/03/19 フタバ双方向向け改修
                }
            }
        });
    }

    /******************************************************************************/
    /*!
     * @brief  割引確認を使用するか？
     * @note   割引確認を使用するか返す
     * @param [in] なし
     * @retval なし
     * @return　boolean
     * @private
     */
    /******************************************************************************/
    private boolean confirmationCheckEnabled() {
        //final boolean isConfirmation = true;
        //return isConfirmation || AppPreference.isDemoMode();

        boolean isAmountNon0 = (_menuViewModel.getTotalAmount().getValue() > 0) ? true : false;;
        boolean isAvailabled = (Amount.getDiscountAvailable() == 0) ? true : false;     // 割引実施フラグ取得
        boolean isFutabaD = IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D);

        return isAmountNon0 && isAvailabled && isFutabaD;
    }

    /******************************************************************************/
    /*!
     * @brief  割引ジョブを使用するか？
     * @note   割引ジョブ
     * @param [in] int tmpJobNo   ジョブ番号　１～５
     * @retval なし
     * @return　boolean
     * @private
     */
    /******************************************************************************/
    private boolean jobCheckEnabled(int tmpJobNo) {
        boolean isJob = true;

        if (_sharedViewModel == null) {
            return false;
        }

        Boolean[] discountJobActiveFl = _sharedViewModel.getDiscountJobActiveFl().getValue();
        if (discountJobActiveFl == null) {
            return false;
        }

        if (tmpJobNo < 1 || tmpJobNo > discountJobActiveFl.length) {
            return false;
        }

        if (discountJobActiveFl[tmpJobNo - 1] == null) {
            return false;
        }

        isJob = discountJobActiveFl[tmpJobNo - 1];      //ジョブ番号は１～５なので、インデックスは０～４

        //return isJob || AppPreference.isDemoMode();

        boolean isAmountNon0 = (_menuViewModel.getTotalAmount().getValue() > 0) ? true : false;;
        boolean isAvailabled = (Amount.getDiscountAvailable() == 0) ? true : false;     // 割引実施フラグ取得
        boolean isFutabaD = IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D);

        return isJob && isAmountNon0 && isAvailabled && isFutabaD;
    }

    /******************************************************************************/
    /*!
     * @brief  割引関連情報をサーバより受取，SharedViewModel内配列に設定
     * @note   割引関連情報をサーバより受取，SharedViewModel内配列に設定
     * @param [in] View view
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    public void setDiscountMenuInfo(List<DiscountMenuInfo> discountMenuInfo) {
        _discountMenuInfo = discountMenuInfo;

        Boolean[] tmpdiscountJobActiveFlAry = _sharedViewModel.getDiscountJobActiveFl().getValue();
        String[]  tmpdiscountJobNameAry = _sharedViewModel.getDiscountJobName().getValue();
        Integer[] tmpdiscountJobTypeAry = _sharedViewModel.getDiscountJobType().getValue();

        if (tmpdiscountJobActiveFlAry == null || tmpdiscountJobNameAry == null || tmpdiscountJobTypeAry == null) return;
        if (tmpdiscountJobActiveFlAry.length != tmpdiscountJobNameAry.length || tmpdiscountJobActiveFlAry.length != tmpdiscountJobTypeAry.length) return;

        for(int i = 0; i < tmpdiscountJobActiveFlAry.length; i++)
        {
            if (discountMenuInfo.size() >= i + 1) {
                tmpdiscountJobActiveFlAry[i] = true;
                tmpdiscountJobNameAry[i] = _discountMenuInfo.get(i).getDiscountName();
                tmpdiscountJobTypeAry[i] = _discountMenuInfo.get(i).getDiscountType();
            } else {
                tmpdiscountJobActiveFlAry[i] = null;
                tmpdiscountJobNameAry[i] = null;
                tmpdiscountJobTypeAry[i] = null;
            }
        }

        _sharedViewModel.setDiscountJobName(tmpdiscountJobNameAry);
        _sharedViewModel.setDiscountJobActiveFl(tmpdiscountJobActiveFlAry);
        _sharedViewModel.setDiscountJobType(tmpdiscountJobTypeAry);
    }

    /******************************************************************************/
    /*!
     * @brief  プリペイドアプリからの割引情報を受取　エントリポイント
     * @note   プリペイドアプリからの割引情報を受取　エントリポイント
     * @param [in] DiscountInfo discountInfo
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    public void setDiscountInfo(DiscountInfo discountInfo) {
        // TODO: handle discount info
        Timber.tag("[FUTABA-D]").d(discountInfo.toString());
        Timber.i("[FUTABA-D]setDiscountInfo()");

        if ( (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == false)) {
            return;
        }

        PrinterManager printerManager = PrinterManager.getInstance();
        printerManager.setView(this.getView());

        if (_menuViewModel.getIFBoxManager() == null) {
            printerManager.PrinterDuplexError(PrinterConst.DuplexPrintStatus_IFBOXERROR);       //IFBOX接続エラー
            return;
        }

        if (_menuViewModel.getIFBoxManager().getIsConnected820() == false)             //820未接続の場合
        {
            printerManager.PrinterDuplexError(PrinterConst.DuplexPrintStatus_IFBOXERROR);       //IFBOX接続エラー
            return;
        }

        _menuEventHandlers.navigateToDiscountCard(this.getView(), _sharedViewModel, discountInfo);

        (this.getView().findViewById(R.id.btn_menu_discount_confirmation)).setEnabled(confirmationCheckEnabled());

        for (int i = 0; i < 5; i++) {
            switch(i) {
                case 0:
                    (this.getView().findViewById(R.id.btn_menu_discount_job1)).setEnabled(jobCheckEnabled(i + 1));
                    break;
                case 1:
                    (this.getView().findViewById(R.id.btn_menu_discount_job2)).setEnabled(jobCheckEnabled(i + 1));
                    break;
                case 2:
                    (this.getView().findViewById(R.id.btn_menu_discount_job3)).setEnabled(jobCheckEnabled(i + 1));
                    break;
                case 3:
                    (this.getView().findViewById(R.id.btn_menu_discount_job4)).setEnabled(jobCheckEnabled(i + 1));
                    break;
                case 4:
                    (this.getView().findViewById(R.id.btn_menu_discount_job5)).setEnabled(jobCheckEnabled(i + 1));
                    break;
            }
        }
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


}
//ADD-E BMT S.Oyama 2024/09/05 フタバ双方向向け改修
