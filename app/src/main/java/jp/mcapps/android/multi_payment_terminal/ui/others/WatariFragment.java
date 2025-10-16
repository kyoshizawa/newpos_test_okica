package jp.mcapps.android.multi_payment_terminal.ui.others;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.BaseFragment;
import jp.mcapps.android.multi_payment_terminal.CommonClickEvent;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.NavigationWrapper;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.ScreenData;
import jp.mcapps.android.multi_payment_terminal.SharedViewModel;
import jp.mcapps.android.multi_payment_terminal.data.ActionBarColors;
import jp.mcapps.android.multi_payment_terminal.data.Amount;
import jp.mcapps.android.multi_payment_terminal.data.BusinessType;
import jp.mcapps.android.multi_payment_terminal.data.TransactionResults;
import jp.mcapps.android.multi_payment_terminal.data.trans_param.RefundParam;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentWatariBinding;
import jp.mcapps.android.multi_payment_terminal.model.SoundManager;
import jp.mcapps.android.multi_payment_terminal.model.TransLogger;
import jp.mcapps.android.multi_payment_terminal.thread.emv.Constants;
import jp.mcapps.android.multi_payment_terminal.ui.common_head.CommonHeadViewModel;
import jp.mcapps.android.multi_payment_terminal.ui.others.WatariViewModel.UIStates;
import timber.log.Timber;

@RequiresApi(api = Build.VERSION_CODES.M)
public class WatariFragment extends BaseFragment implements WatariEventHandlers {

    private final MainApplication _app = MainApplication.getInstance();
    private static final String SCREEN_NAME = "ポイントカード読み取り";

    private SharedViewModel _sharedViewModel;
    private CommonHeadViewModel _commonHeadViewModel;
    private WatariViewModel _watariViewModel;

    public static WatariFragment newInstance() {
        return new WatariFragment();
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        final FragmentWatariBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_watari, container, false);

        _sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        binding.setSharedViewModel(_sharedViewModel);

        _watariViewModel =
                new ViewModelProvider(this).get(WatariViewModel.class);

        binding.setLifecycleOwner(getViewLifecycleOwner());
        binding.setViewModel(_watariViewModel);
        binding.setHandlers(this);

        _commonHeadViewModel = new ViewModelProvider(this).get(CommonHeadViewModel.class);
        binding.setCommonHeadViewModel(_commonHeadViewModel);

        getLifecycle().addObserver(_watariViewModel);
        getLifecycle().addObserver(_commonHeadViewModel);

        ScreenData.getInstance().setScreenName(SCREEN_NAME);

        _watariViewModel.setResult(TransactionResults.None);

        final Bundle args = getArguments();

        switch (MainApplication.getInstance().getBusinessType()) {
            case POINT_ADD:
                _watariViewModel.setBusinessType("ポイント付与");
                break;
            case POINT_REFUND:
                _watariViewModel.setBusinessType("ポイント取消");
                break;
        }

        if (MainApplication.getInstance().getBusinessType() == BusinessType.POINT_REFUND) {
            final Integer slipId = args.getInt("slipId");
            _watariViewModel.setRefundSlipId(slipId);
            _watariViewModel.setIsRefund(true);
            final TransLogger transLogger = new TransLogger();
            transLogger.setRefundParam(slipId);
            RefundParam refundParam = transLogger.getRefundParam();
            _commonHeadViewModel.setAmount(refundParam.oldTransAmount);
        }

        _watariViewModel.getResult().observe(getViewLifecycleOwner(), result -> {
            if (result == TransactionResults.None) {
                _sharedViewModel.setActionBarColor(ActionBarColors.Normal);
            }
            else if (result == TransactionResults.SUCCESS) {
                _sharedViewModel.setActionBarColor(ActionBarColors.Success);
            }
            else if (result == TransactionResults.FAILURE) {
                _sharedViewModel.setActionBarColor(ActionBarColors.Error);
            }
            else if (result == TransactionResults.UNKNOWN) {
                _sharedViewModel.setActionBarColor(ActionBarColors.Unknown);
            }
        });

        _watariViewModel.getState().observe(getViewLifecycleOwner(), state -> {
            if (state == UIStates.Success || state == UIStates.Failure) {
                final Bundle params = new Bundle();
                params.putInt("slipId", _watariViewModel.getSlipId());

                final Completable wait5sec = Completable.create(emitter -> {
                    try {
                        Thread.sleep(5000);
                    } catch (Exception ignore) { }

                    emitter.onComplete();
                });

                Disposable subscribe = Completable.mergeArray(
                        wait5sec.subscribeOn(Schedulers.newThread()))
                        .observeOn(AndroidSchedulers.mainThread())
                        .doFinally(() -> {
                            _sharedViewModel.setActionBarColor(ActionBarColors.Normal);
                            _watariViewModel.setResult(TransactionResults.None);
                            Amount.reset();
                            NavigationWrapper.navigate(
                                    this, R.id.action_navigation_watari_to_navigation_menu, params);
                        })
                        .subscribe(() -> {}, e -> {});
            }
            else if (state == UIStates.ReadTimeout) {
                _watariViewModel.setResult(TransactionResults.None);
                NavigationWrapper.navigate(
                        this, R.id.action_navigation_watari_to_navigation_menu);
            }
        });

        _watariViewModel.start();
        return binding.getRoot();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        _sharedViewModel.setScreenInversion(false);
    }

    @Override
    public void onCancelClick(View view) {
        Timber.d("onCancelClick");
        CommonClickEvent.RecordButtonClickOperation(view, true);

        UIStates state = _watariViewModel.getState().getValue();
        if (state == UIStates.Read) {
            _watariViewModel.stopSerchCard();
            _watariViewModel.setResult(TransactionResults.None);
            NavigationWrapper.popBackStack(this);
        }
        else if (state == UIStates.Unknown) {
            Disposable subscribe = _watariViewModel.suspend()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe(d -> {
                        _sharedViewModel.setLoading(true);
                    })
                    .doFinally(() -> {
                        _sharedViewModel.setLoading(false);
                        _sharedViewModel.setActionBarColor(ActionBarColors.Normal);
                        _watariViewModel.setResult(TransactionResults.None);
                        NavigationWrapper.navigate(
                                this, R.id.action_navigation_watari_to_navigation_menu);
                    })
                    .subscribe(() -> {}, e -> {});
        }
    }

    @Override
    public void onCheckResultClick(View view) {
        switch (MainApplication.getInstance().getBusinessType()) {
            case POINT_ADD:
                _watariViewModel.pointAdd();
                break;
            case POINT_REFUND:
                _watariViewModel.pointCancel();
                break;
        }
    }

}