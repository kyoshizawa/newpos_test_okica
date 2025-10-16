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
import io.reactivex.rxjava3.schedulers.Schedulers;
import jp.mcapps.android.multi_payment_terminal.BaseFragment;
import jp.mcapps.android.multi_payment_terminal.CommonClickEvent;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.NavigationWrapper;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.ScreenData;
import jp.mcapps.android.multi_payment_terminal.SharedViewModel;
import jp.mcapps.android.multi_payment_terminal.data.ActionBarColors;
import jp.mcapps.android.multi_payment_terminal.data.OptionService;
import jp.mcapps.android.multi_payment_terminal.data.TransactionResults;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentValidationCheckBinding;
import jp.mcapps.android.multi_payment_terminal.model.SoundManager;
import jp.mcapps.android.multi_payment_terminal.model.TransLogger;
import jp.mcapps.android.multi_payment_terminal.ui.common_head.CommonHeadViewModel;
import jp.mcapps.android.multi_payment_terminal.ui.others.ValidationCheckViewModel.UIStates;
import timber.log.Timber;

@SuppressWarnings("ResultOfMethodCallIgnored")
@RequiresApi(api = Build.VERSION_CODES.M)
public class ValidationCheckFragment extends BaseFragment implements ValidationCheckEventHandlers {

    private final MainApplication _app = MainApplication.getInstance();
    private final String SCREEN_NAME = validationCheckName();

    private SharedViewModel _sharedViewModel;
    private CommonHeadViewModel _commonHeadViewModel;
    private SoundManager _soundManager = SoundManager.getInstance();
    private ValidationCheckViewModel _validationCheckViewModel;
    private TransLogger _transLogger;

    public static ValidationCheckFragment newInstance() {
        return new ValidationCheckFragment();
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        final FragmentValidationCheckBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_validation_check, container, false);

        _sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        binding.setSharedViewModel(_sharedViewModel);

        _validationCheckViewModel =
                new ViewModelProvider(this).get(ValidationCheckViewModel.class);

        binding.setLifecycleOwner(getViewLifecycleOwner());
        binding.setViewModel(_validationCheckViewModel);
        binding.setHandlers(this);

        _commonHeadViewModel = new ViewModelProvider(this).get(CommonHeadViewModel.class);
        binding.setCommonHeadViewModel(_commonHeadViewModel);

        getLifecycle().addObserver(_validationCheckViewModel);
        getLifecycle().addObserver(_commonHeadViewModel);

        ScreenData.getInstance().setScreenName(SCREEN_NAME);

        _transLogger = new TransLogger();

        _validationCheckViewModel.setResult(TransactionResults.None);
        _validationCheckViewModel.getResult().observe(getViewLifecycleOwner(), result -> {
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

        _validationCheckViewModel.getState().observe(getViewLifecycleOwner(), state -> {
            if (state == UIStates.Success || state == UIStates.Failure) {
                final Completable wait5sec = Completable.create(emitter -> {
                    try {
                        Thread.sleep(5000);
                    } catch (Exception ignore) { }

                    emitter.onComplete();
                });

                // エラーを無視したいのでCompletableでラップする
                final Completable sendTask = Completable.create(emitter -> {
                    _validationCheckViewModel.sendResult()
                            .subscribeOn(Schedulers.io())
                            .doFinally(emitter::onComplete)
                            .subscribe(() -> {}, e -> {});
                });

                Completable.mergeArray(
                        sendTask,
                        wait5sec.subscribeOn(Schedulers.newThread()))
                        .observeOn(AndroidSchedulers.mainThread())
                        .doFinally(() -> {
                            _validationCheckViewModel.setResult(TransactionResults.None);
                            NavigationWrapper.navigate(
                                    this, R.id.action_navigation_validation_check_to_navigation_menu);
                        })
                        .subscribe(() -> {}, e -> {});
            }
            else if (state == UIStates.ReadTimeout) {
                _validationCheckViewModel.setResult(TransactionResults.None);
                NavigationWrapper.popBackStack(this);
            }
        });

        _validationCheckViewModel.start();
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

        UIStates state = _validationCheckViewModel.getState().getValue();
        if (state == UIStates.Read) {
            _validationCheckViewModel.stopSerchCard();
            _validationCheckViewModel.setResult(TransactionResults.None);
            NavigationWrapper.popBackStack(this);
        }
        else if (state == UIStates.Unknown) {
            _validationCheckViewModel.suspend()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe(d -> {
                        _sharedViewModel.setLoading(true);
                    })
                    .doFinally(() -> {
                        _sharedViewModel.setLoading(false);
                        _validationCheckViewModel.setResult(TransactionResults.None);
                        NavigationWrapper.navigate(
                                this, R.id.action_navigation_validation_check_to_navigation_menu);
                    })
                    .subscribe(() -> {}, e -> {});
        }
    }

    @Override
    public void onCheckResultClick(View view) {
        _validationCheckViewModel.validate();
    }

    private String validationCheckName() {
        final OptionService service = _app.getOptionService();

        int index = service != null ? service.indexOfFunc(OptionService.FuncIds.MAGNETIC_CARD_VALIDATION) : -1;

        // Fragment生成前の為MainApplicationからgetString
        return index >= 0
                ? service.getFunc(index).getDisplayName()
                : _app.getString(R.string.btn_other_validation);
    }
}