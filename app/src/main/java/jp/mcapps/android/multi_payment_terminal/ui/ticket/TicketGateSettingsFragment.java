package jp.mcapps.android.multi_payment_terminal.ui.ticket;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.adapters.SwitchBindingAdapter;
import androidx.lifecycle.ViewModelProvider;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jp.mcapps.android.multi_payment_terminal.BaseFragment;
import jp.mcapps.android.multi_payment_terminal.CommonClickEvent;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.NavigationWrapper;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.ScreenData;
import jp.mcapps.android.multi_payment_terminal.SharedViewModel;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketGateSettingsData;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentTicketGateSettingsBinding;
import jp.mcapps.android.multi_payment_terminal.webapi.HttpStatusException;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.TicketSalesStatusException;
import timber.log.Timber;

public class TicketGateSettingsFragment extends BaseFragment implements TicketGateSettingsHandlers{

    private final String SCREEN_NAME = "改札設定画面";
    private TicketGateSettingsViewModel _ticketGateSettingsViewModel;
    private SharedViewModel _sharedViewModel;
    private TicketGateSettingsLocationAdapter _adapter;
    private TicketGateSettingsRouteAdapter _routeAdapter;

    public static TicketGateSettingsFragment newInstance() {
        return new TicketGateSettingsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        final FragmentTicketGateSettingsBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_ticket_gate_settings, container, false);

        _ticketGateSettingsViewModel = new ViewModelProvider(this).get(TicketGateSettingsViewModel.class);
        _sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        ArrayAdapter<String> gateStartAdapter = new ArrayAdapter<String>(getActivity(), R.layout.item_ticket_gate_settings, getResources().getStringArray(R.array.ticket_gate_start));
        gateStartAdapter.setDropDownViewResource(R.layout.item_ticket_gate_settings_dropdown);
        ArrayAdapter<String> gateUpdateIntervalAdapter = new ArrayAdapter<String>(getActivity(), R.layout.item_ticket_gate_settings, getResources().getStringArray(R.array.ticket_gate_update_interval));
        gateStartAdapter.setDropDownViewResource(R.layout.item_ticket_gate_settings_dropdown);

        _adapter = new TicketGateSettingsLocationAdapter(binding.getRoot().getContext());
        _routeAdapter = new TicketGateSettingsRouteAdapter(binding.getRoot().getContext());
        _ticketGateSettingsViewModel.fetch(_adapter, _routeAdapter, getResources().getStringArray(R.array.ticket_gate_start), getResources().getStringArray(R.array.ticket_gate_update_interval));
        binding.spinnerGateStart.setAdapter(gateStartAdapter);
        binding.spinnerUpdateInterval.setAdapter(gateUpdateIntervalAdapter);
        binding.spinnerLocation.setAdapter(_adapter);
        binding.spinnerRoute.setAdapter(_routeAdapter);
        final Handler handler = new Handler();
        binding.spinnerLocation.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            // アイテム選択時に呼び出される。
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
            int position, long id) {
                CommonClickEvent.RecordClickOperation(((TicketGateLocation) parent.getSelectedItem()).stopName, "設置場所", true);
                // 選択が変わったので路線情報、便情報を取得しなおし
                _ticketGateSettingsViewModel.changeTripTimeList();
                _ticketGateSettingsViewModel.changeLocation(((TicketGateLocation) parent.getSelectedItem()).stopIds, ((TicketGateLocation) parent.getSelectedItem()).stopName)
                        .subscribe(
                                result -> {
                                    Timber.d("change location");
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            _routeAdapter.clear();
                                            _routeAdapter.addAll(result);
                                            _ticketGateSettingsViewModel.updateSettingModified(binding.spinnerLocation, binding.spinnerRoute, binding.spinnerGateStart, binding.spinnerUpdateInterval);
                                        }
                                    });
                                },
                                error -> {
                                    _ticketGateSettingsViewModel.isInitResult(false);
                                    Resources resources = MainApplication.getInstance().getResources();

                                    if (error.getClass() == TicketSalesStatusException.class) {
                                        Timber.e(error, "changeLocation error %s %s",String.valueOf(((TicketSalesStatusException) error).getCode()), error.getMessage());
                                        _ticketGateSettingsViewModel.setErrorCode(resources.getString(R.string.error_type_ticket_8210));
                                        _ticketGateSettingsViewModel.setErrorMessage(resources.getString(R.string.error_message_ticket_8210));
                                        _ticketGateSettingsViewModel.setErrorMessageInformation(String.format(resources.getString(R.string.error_detail_ticket_8210), String.valueOf(((TicketSalesStatusException) error).getCode())));
                                    } else if (error.getClass() == HttpStatusException.class) {
                                        Timber.e(error, "changeLocation error %s %s", String.valueOf(((HttpStatusException) error).getStatusCode()), error.getMessage());
                                        _ticketGateSettingsViewModel.setErrorCode(resources.getString(R.string.error_type_ticket_8210));
                                        _ticketGateSettingsViewModel.setErrorMessage(resources.getString(R.string.error_message_ticket_8210));
                                        _ticketGateSettingsViewModel.setErrorMessageInformation(String.format(resources.getString(R.string.error_detail_ticket_8210), String.valueOf(((HttpStatusException) error).getStatusCode())));
                                    } else {
                                        Timber.e(error, "changeLocation error %s", error.getMessage());
                                        _ticketGateSettingsViewModel.setErrorCode(resources.getString(R.string.error_type_ticket_8097));
                                        _ticketGateSettingsViewModel.setErrorMessage(resources.getString(R.string.error_message_ticket_8097));
                                        _ticketGateSettingsViewModel.setErrorMessageInformation(resources.getString(R.string.error_detail_ticket_8097));
                                    }
                                }
                        );

            }
            // 何も選択されなかったときに呼び出される。
            @Override
            public void onNothingSelected(AdapterView<?> arg0)
            {
            }
        });

        binding.spinnerRoute.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            // アイテム選択時に呼び出される。
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
//                if(parent.getCount() <= 1){
//                    return;
//                }
                String routeId = ((TicketGateRoute) parent.getSelectedItem()).routeId;
                String routeName = ((TicketGateRoute) parent.getSelectedItem()).routeName;
                CommonClickEvent.RecordClickOperation(routeName, "経路", true);
                // 選択が変わったので便情報を取得しなおし
                _ticketGateSettingsViewModel.changeTripTimeList();
                _ticketGateSettingsViewModel.changeRoute(routeId, routeName)
                        .subscribe(
                                result -> {
                                    Timber.d("change route");
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            _ticketGateSettingsViewModel.updateSettingModified(null, null, null, null);
                                        }
                                    });
                                },
                                error -> {
                                    _ticketGateSettingsViewModel.isInitResult(false);
                                    Resources resources = MainApplication.getInstance().getResources();

                                    if (error.getClass() == TicketSalesStatusException.class) {
                                        Timber.e(error, "changeRoute error %s %s", String.valueOf(((TicketSalesStatusException) error).getCode()), error.getMessage());
                                        _ticketGateSettingsViewModel.setErrorCode(resources.getString(R.string.error_type_ticket_8210));
                                        _ticketGateSettingsViewModel.setErrorMessage(resources.getString(R.string.error_message_ticket_8210));
                                        _ticketGateSettingsViewModel.setErrorMessageInformation(String.format(resources.getString(R.string.error_detail_ticket_8210), String.valueOf(((TicketSalesStatusException) error).getCode())));
                                    } else if (error.getClass() == HttpStatusException.class) {
                                        Timber.e(error, "changeRoute error %s %s", String.valueOf(((HttpStatusException) error).getStatusCode()), error.getMessage());
                                        _ticketGateSettingsViewModel.setErrorCode(resources.getString(R.string.error_type_ticket_8210));
                                        _ticketGateSettingsViewModel.setErrorMessage(resources.getString(R.string.error_message_ticket_8210));
                                        _ticketGateSettingsViewModel.setErrorMessageInformation(String.format(resources.getString(R.string.error_detail_ticket_8210), String.valueOf(((HttpStatusException) error).getStatusCode())));
                                    } else {
                                        Timber.e(error, "changeRoute error %s", error.getMessage());
                                        _ticketGateSettingsViewModel.setErrorCode(resources.getString(R.string.error_type_ticket_8097));
                                        _ticketGateSettingsViewModel.setErrorMessage(resources.getString(R.string.error_message_ticket_8097));
                                        _ticketGateSettingsViewModel.setErrorMessageInformation(resources.getString(R.string.error_detail_ticket_8097));
                                    }
                                }
                        );
            }
            // 何も選択されなかったときに呼び出される。
            @Override
            public void onNothingSelected(AdapterView<?> arg0)
            {
            }
        });

        binding.setViewModel(_ticketGateSettingsViewModel);
        binding.setLifecycleOwner(getViewLifecycleOwner());
        binding.setHandlers(this);

        ScreenData.getInstance().setScreenName(SCREEN_NAME);

        return binding.getRoot();
    }

    @Override
    public void navigateToTicketGateQrScanByChange(View view) {

        CommonClickEvent.RecordButtonClickOperation(view, true);
        Activity activity = (Activity) view.getContext();
        if (activity == null) return;

        // 改札設定データを保存
        _ticketGateSettingsViewModel.saveGateSettings()
                .subscribe(
                        result -> {
                            Timber.d("save success GateSettings");

                            _sharedViewModel.setTopBarView(false);
                            Bundle args = new Bundle();
                            args.putSerializable("ticketRouteIds", (Serializable) _ticketGateSettingsViewModel.getRouteIds());
                            // QRかざし待ち画面に遷移する
                            NavigationWrapper.navigate(activity, R.id.fragment_main_nav_host, R.id.action_navigation_menu_to_fragment_ticket_qr_scan, args);
                        },
                        error -> {
                            Timber.e(error, "save failure GateSettings");
                            _ticketGateSettingsViewModel.isInitResult(false);
                            Resources resources = MainApplication.getInstance().getResources();
                            _ticketGateSettingsViewModel.setErrorCode(resources.getString(R.string.error_type_ticket_8049));
                            _ticketGateSettingsViewModel.setErrorMessage(resources.getString(R.string.error_message_ticket_8049));
                            _ticketGateSettingsViewModel.setErrorMessageInformation(resources.getString(R.string.error_detail_ticket_8049));
                        }
                );
    }

    @Override
    public void navigateToTicketGateQrScan(View view) {

        CommonClickEvent.RecordButtonClickOperation(view, true);
        Activity activity = (Activity) view.getContext();
        if (activity == null) return;

        _sharedViewModel.setTopBarView(false);
        Bundle args = new Bundle();
        args.putSerializable("ticketRouteIds", (Serializable) _ticketGateSettingsViewModel.getRouteIdsBackup());
        // QRかざし待ち画面に遷移する
        NavigationWrapper.navigate(activity, R.id.fragment_main_nav_host, R.id.action_navigation_menu_to_fragment_ticket_qr_scan, args);
    }

    @Override
    public void selectTripOne(View view) {
        _ticketGateSettingsViewModel.TripOneSelect();
        CommonClickEvent.RecordClickOperation(_ticketGateSettingsViewModel.getTripOneTimeInfo(), "便の選択", true);
    }

    @Override
    public void selectTripTwo(View view) {
        _ticketGateSettingsViewModel.TripTwoSelect();
        CommonClickEvent.RecordClickOperation(_ticketGateSettingsViewModel.getTripTwoTimeInfo(), "便の選択", true);
    }

    @Override
    public void selectTripThree(View view) {
        _ticketGateSettingsViewModel.TripThreeSelect();
        CommonClickEvent.RecordClickOperation(_ticketGateSettingsViewModel.getTripThreeTimeInfo(), "便の選択", true);
    }

    @Override
    public void arrowUp(View view) {
        CommonClickEvent.RecordClickOperation("前便(arrowUp)", "便の選択", true);
        _ticketGateSettingsViewModel.initTripSelect();
        _ticketGateSettingsViewModel.arrowUp();
    }

    @Override
    public void arrowDown(View view) {
        CommonClickEvent.RecordClickOperation("次便(arrowDown)", "便の選択", true);
        _ticketGateSettingsViewModel.initTripSelect();
        _ticketGateSettingsViewModel.arrowDown();
    }
}
