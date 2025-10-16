package jp.mcapps.android.multi_payment_terminal.ui.discount;
//ADD-S BMT S.Oyama 2024/09/06 フタバ双方向向け改修

import android.os.Bundle;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import jp.mcapps.android.multi_payment_terminal.BaseFragment;
import jp.mcapps.android.multi_payment_terminal.CommonClickEvent;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.NavigationWrapper;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.SharedViewModel;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentDiscountjobFutabadBinding;
import jp.mcapps.android.multi_payment_terminal.ui.amount_input.AmountInputAdvancePayFDViewModel;
import jp.mcapps.android.multi_payment_terminal.ui.dialog.ConfirmDialog;
import jp.mcapps.android.multi_payment_terminal.ui.menu.MenuViewModel;
import timber.log.Timber;

public class DiscountJobFutabaDFragment extends BaseFragment {
    private DiscountJobFutabaDViewModel _discountJobFutabaDViewModel;
    private SharedViewModel _sharedViewModel;

    private final String MESSAGE_COMMONSTR = "\n割引しても\nよろしいですか？";

    public static DiscountJobFutabaDFragment newInstance() {
        return new DiscountJobFutabaDFragment();
    }

    private DiscountJobFutabaDEventHandlersImpl _eventHandlers = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        final FragmentDiscountjobFutabadBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_discountjob_futabad, container, false);

        ViewModelProvider viewModelProvider = new ViewModelProvider(this, MainApplication.getViewModelFactory());
        _discountJobFutabaDViewModel  = viewModelProvider.get(DiscountJobFutabaDViewModel.class);

        _sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        String[] tmpNameArray = _sharedViewModel.getDiscountJobName().getValue();
        int tmpJobMode = _sharedViewModel.getDiscountJobMode().getValue();

        _discountJobFutabaDViewModel.setdiscountJobMode(tmpJobMode);

        if ((tmpNameArray != null) && (tmpJobMode >= 0) && (tmpJobMode < tmpNameArray.length) && (tmpNameArray[tmpJobMode] != null))
        {
            String tmpMessage = tmpNameArray[tmpJobMode] + MESSAGE_COMMONSTR;                    //メッセージ文字列の組み立て
            TextView textViewMessage = binding.getRoot().findViewById(R.id.discountjob_message_lbl);
            textViewMessage.setText(tmpMessage);
        }

        binding.setSharedViewModel(_sharedViewModel);
        binding.setViewModel(_discountJobFutabaDViewModel);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        //_discountJobFutabaDViewModel.start();

        _eventHandlers = new DiscountJobFutabaDEventHandlersImpl(this);
        binding.setHandlers(_eventHandlers);

        _sharedViewModel.setBackAction(() -> {
            clickModoruButton();
        });

        return binding.getRoot();
    }

    @Override
    public void onStop() {
        super.onStop();
        //_discountJobFutabaDViewModel.stop();
    }

    /******************************************************************************/
    /*!
     * @brief  本画面中戻るボタンを押下時に820へキャンセルコードを送出
     * @note   本画面中戻るボタンを押下時に820へキャンセルコードを送出
     * @param [in]
     * @retval なし
     * @return　なし
     * @private
     */
    /******************************************************************************/
    private void clickModoruButton() {

        _eventHandlers.onCancelClick(this.getView(), _discountJobFutabaDViewModel, _sharedViewModel);
        _sharedViewModel.setBackAction(null);

    }

}
//ADD-E BMT S.Oyama 2024/09/06 フタバ双方向向け改修
