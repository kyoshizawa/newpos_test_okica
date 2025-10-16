package jp.mcapps.android.multi_payment_terminal.ui.amount_input;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
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
import jp.mcapps.android.multi_payment_terminal.data.BusinessType;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentChargeAmountInputBinding;
import timber.log.Timber;

public class ChargeAmountInputFragment extends BaseFragment implements ChargeAmountInputEventHandlers {
    private final String SCREEN_NAME = "チャージ金額入力";
    public static ChargeAmountInputFragment newInstance() {
        return new ChargeAmountInputFragment();
    }

    private final MainApplication _app = MainApplication.getInstance();
    private ChargeAmountInputViewModel _chargeAmountInputViewModel;
    private SharedViewModel _sharedViewModel;

    private String _brand;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        final FragmentChargeAmountInputBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_charge_amount_input, container, false);

        _chargeAmountInputViewModel =
                new ViewModelProvider(this).get(ChargeAmountInputViewModel.class);

        _sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        binding.setViewModel(_chargeAmountInputViewModel);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        binding.setHandlers(this);

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

        final Bundle args = getArguments();
        final String brand = args.getString("brand");

        if (TextUtils.isEmpty(brand)) {
            throw new IllegalArgumentException();
        }

        boolean isValid = brand.equals(_app.getString(R.string.money_brand_suica))
                || brand.equals(_app.getString(R.string.money_brand_id))
                || brand.equals(_app.getString(R.string.money_brand_waon))
                || brand.equals(_app.getString(R.string.money_brand_nanaco))
                || brand.equals(_app.getString(R.string.money_brand_qp))
                || brand.equals(_app.getString(R.string.money_brand_edy))
                || brand.equals(_app.getString(R.string.money_brand_okica));

        if (!isValid) {
            throw new IllegalArgumentException();
        }

        _brand = brand;
        Timber.d("ブランド: %s", brand);

        final AppCompatActivity activity = (AppCompatActivity) view.getContext();

        if (activity != null && !AppPreference.isServicePos()) {
            _sharedViewModel.isActionBarLock(false);
            final ActionBar actionBar = activity.getSupportActionBar();
            actionBar.setBackgroundDrawable(
                    getResources().getDrawable(R.color.menu_charge, null));
            _sharedViewModel.isActionBarLock(true);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        _sharedViewModel.setUpdatedFlag(true);
    }

    @Override
    public void onInputNumber(View view, String number) {
        CommonClickEvent.RecordButtonClickOperation(view, false);
        _chargeAmountInputViewModel.inputNumber(number);
    }

    @Override
    public void onCorrection(View view) {
        CommonClickEvent.RecordButtonClickOperation(view, false);
        _chargeAmountInputViewModel.correct();
    }

    @Override
    public void onEnter(View view) {
        CommonClickEvent.RecordButtonClickOperation(view, false);
        _app.setBusinessType(BusinessType.CHARGE);

        final Bundle params = new Bundle();
        params.putInt("amount", _chargeAmountInputViewModel.getAmount().getValue());

        if (_brand.equals(_app.getString(R.string.money_brand_suica))) {
        }
        else if (_brand.equals(_app.getString(R.string.money_brand_id))) {
        }
        else if (_brand.equals(_app.getString(R.string.money_brand_waon))) {
        }
        else if (_brand.equals(_app.getString(R.string.money_brand_nanaco))) {
        }
        else if (_brand.equals(_app.getString(R.string.money_brand_qp))) {
        }
        else if (_brand.equals(_app.getString(R.string.money_brand_edy))) {
        }
        else if (_brand.equals(_app.getString(R.string.money_brand_okica))) {
            NavigationWrapper.navigate(this, R.id.action_navigation_charge_amount_input_to_navigation_emoney_okica, params);
        }
    }
}