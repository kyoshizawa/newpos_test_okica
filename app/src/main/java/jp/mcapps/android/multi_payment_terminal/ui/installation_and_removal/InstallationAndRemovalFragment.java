package jp.mcapps.android.multi_payment_terminal.ui.installation_and_removal;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.common.base.Strings;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import io.grpc.Status;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.BaseFragment;
import jp.mcapps.android.multi_payment_terminal.CommonClickEvent;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.NavigationWrapper;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.ScreenData;
import jp.mcapps.android.multi_payment_terminal.SharedViewModel;
import jp.mcapps.android.multi_payment_terminal.data.Amount;
import jp.mcapps.android.multi_payment_terminal.data.okica.ICMaster;
import jp.mcapps.android.multi_payment_terminal.data.okica.OkicaNegaFile;
import jp.mcapps.android.multi_payment_terminal.database.DBManager;
import jp.mcapps.android.multi_payment_terminal.database.LocalDatabase;
import jp.mcapps.android.multi_payment_terminal.databinding.DialogTerminalInfoBinding;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentInstallationAndRemovalBinding;
import jp.mcapps.android.multi_payment_terminal.devices.SamManagementUtils;
import jp.mcapps.android.multi_payment_terminal.ui.dialog.AlertDialogFragment;
import jp.mcapps.android.multi_payment_terminal.ui.dialog.BaseDialogFragment;
import jp.mcapps.android.multi_payment_terminal.ui.dialog.ConfirmDialog;
import jp.mcapps.android.multi_payment_terminal.ui.dialog.ErrorDialog;
import jp.mcapps.android.multi_payment_terminal.ui.dialog.RadioDialogFragment;
import jp.mcapps.android.multi_payment_terminal.ui.dialog.SuccessDialog;
import jp.mcapps.android.multi_payment_terminal.ui.error.CommonErrorDialog;
import jp.mcapps.android.multi_payment_terminal.ui.pin.PinInputEventHandlers;
import jp.mcapps.android.multi_payment_terminal.webapi.grpc.McOkicaCenterApi;
import jp.mcapps.android.multi_payment_terminal.webapi.grpc.McOkicaCenterApiImpl;
import jp.mcapps.android.multi_payment_terminal.webapi.grpc.data.TerminalUninstallation;
import timber.log.Timber;

public class InstallationAndRemovalFragment extends BaseFragment implements InstallationAndRemovalEventHandlers, PinInputEventHandlers, RadioDialogFragment.RadioDialogListener {
    private final String SCREEN_NAME = "保守";
    private static final HashMap<String, String> _errorTable = new HashMap<>();
    private final MainApplication _app = MainApplication.getInstance();
    private Handler _handler = new Handler(Looper.getMainLooper());
    private final LocalDatabase _db = LocalDatabase.getInstance();

    public static InstallationAndRemovalFragment newInstance() {
        return new InstallationAndRemovalFragment();
    }

    private InstallationAndRemovalViewModel _installationAndRemovalViewModel;
    private SharedViewModel _sharedViewModel;
    private int _externalDeviceType;
    private int _test;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d("pos_screen_status" , "onCreate");

        final FragmentInstallationAndRemovalBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_installation_and_removal, container, false);

        _installationAndRemovalViewModel =
                new ViewModelProvider(this, MainApplication.getViewModelFactory()).get(InstallationAndRemovalViewModel.class);

        binding.setLifecycleOwner(getViewLifecycleOwner());

        binding.setHandlers(this);

        binding.setViewModel(_installationAndRemovalViewModel);

        FragmentActivity activity = getActivity();

        if (activity != null) {
            _sharedViewModel = new ViewModelProvider(activity).get(SharedViewModel.class);

            _installationAndRemovalViewModel.fetchDrivers(_db.driverDao());

            _installationAndRemovalViewModel.hasError().observe(getViewLifecycleOwner(), b -> {
                if (b) {
                    CommonErrorDialog dialog = new CommonErrorDialog();

                    dialog.ShowErrorMessage(
                            activity, _installationAndRemovalViewModel.getErrorCode());

                    _installationAndRemovalViewModel.hasError(false);
                }
            });
        }

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
        final boolean showDialog = args.getBoolean("showSuccessDialog");

        if (showDialog) {
            _installationAndRemovalViewModel.isPinSuccess(true);
            SuccessDialog.show(requireContext(), "登録しました");
        }
    }

    @Override
    public void onStart() {
        _installationAndRemovalViewModel.setDeviceInterlocking();
        _installationAndRemovalViewModel.isOkicaInstalled(!Strings.isNullOrEmpty(AppPreference.getOkicaAccessToken()));
        _installationAndRemovalViewModel.setPosActivate(AppPreference.isServicePos());
        _installationAndRemovalViewModel.setTicketActivate(AppPreference.isServiceTicket());
        _installationAndRemovalViewModel.setExternal(AppPreference.getIsOnCradle());
        super.onStart();
    }

    @Override
    public void onInputNumber(View view, String number) {
        CommonClickEvent.RecordClickOperation(number,"暗証番号入力画面", false);
        _installationAndRemovalViewModel.inputNumber(number);
    }

    @Override
    public void onCorrection(View view) {
        CommonClickEvent.RecordClickOperation("CLEAR","暗証番号入力画面", false);
        _installationAndRemovalViewModel.correct();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onEnter(View view) {
        CommonClickEvent.RecordClickOperation("ENTER","暗証番号入力画面", false);

        if (getChildFragmentManager().getFragments().size() >= 1) return;

        if (!_installationAndRemovalViewModel.enter()) {

            new PinErrorDialogFragment().show(getChildFragmentManager(), null);
        } else {
            Timber.i("保守メニュー画面が表示されました。");
        }
    }

    @Override
    public void onCancel(View view) {
        CommonClickEvent.RecordClickOperation("CANCEL","暗証番号入力画面", false);
        view.post(() -> {
            NavigationWrapper.popBackStack(this);
        });
    }

    static public class PinErrorDialogFragment extends BaseDialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            // 画面外のタップを無効化
            this.setCancelable(false);

            Timber.e("暗証番号エラー画面:暗証番号が違います。");
            final AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                    .setTitle("暗証番号エラー")
                    .setMessage("暗証番号が違います。")
                    .setPositiveButton("閉じる", (dialog, which) -> {
                        CommonClickEvent.RecordClickOperation("閉じる", "暗証番号エラー", false);
                        dialog.dismiss(); });

            return builder.create();
        }
    }

    //同じ名称のボタンが複数あるため操作ログで判別出来るように設定
    //ハードコードしているので別の方法を検討

    /* 号機番号 */
    public void onRegisterCarNumberClick(View view) {
        CommonClickEvent.RecordClickOperation("登録", "号機番号", false);
        ConfirmDialog.newInstance("【号機番号登録確認】","登録しますか？", () -> {
            CommonClickEvent.RecordClickOperation("はい", "号機番号登録確認", false);
            final Bundle params = new Bundle();

            params.putString("destination", getClass().getName());
            view.post(() -> {
                NavigationWrapper.navigate(view, R.id.action_navigation_installation_and_removal_to_navigation_car_id, params);
            });
        },() ->{
            CommonClickEvent.RecordClickOperation("いいえ", "号機番号登録確認", false);
        }).show(getChildFragmentManager(), null);

    }

    /* 電子マネー認証 */
    // 注意！　電子マネー認証ボタン自体を削除したため今のところ呼ばれることはありません
    @Override
    public void onActivateClick(View view) {
        CommonClickEvent.RecordClickOperation("認証", "電子マネー認証", true);
        final Runnable run = () -> {
            _sharedViewModel.setLoading(true);
            if (_installationAndRemovalViewModel.install()) {
                _handler.post(() -> {
                    SuccessDialog.show(requireContext(), "認証しました");
                });
            }
            _sharedViewModel.setLoading(false);
        };
        new Thread(run).start();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onDeactivateClick(View view) {
        CommonClickEvent.RecordClickOperation("解除", "電子マネー認証", false);
        ConfirmDialog.newInstance("【電子マネー認証解除確認】","解除しますか？", () -> {
            CommonClickEvent.RecordClickOperation("はい", "電子マネー認証解除確認", false);
            new Thread(() -> {
                _sharedViewModel.setLoading(true);
                String errCode = _installationAndRemovalViewModel.uninstall();
                _sharedViewModel.setLoading(false);

                _handler.post(() -> {
                    _installationAndRemovalViewModel.setJremActivateIdEnabled(AppPreference.getJremActivateId());
                    if (errCode != null && errCode.equals("Exception") == false) {
                        CommonErrorDialog errorDialog = new CommonErrorDialog();
                        Timber.e("異常終了:解除失敗(エラーコード:%s)", errCode);
                        errorDialog.ShowErrorMessage(
                                requireActivity(), errCode);
                    } else if (errCode != null && errCode.equals("Exception")) {
                        CommonErrorDialog errorDialog = new CommonErrorDialog();
                        Timber.e("例外発生(4910):認証解除に失敗しました。管理者に連絡してください。");
                        errorDialog.ShowErrorMessage(
                                requireActivity(), _app.getString(R.string.error_type_jrem_activate_uninstall_error));
                    } else {
                        Timber.i("正常終了:解除しました");
                        SuccessDialog.show(requireContext(), "解除しました");
                    }
                });
            }).start();
        },() ->{
            CommonClickEvent.RecordClickOperation("いいえ", "電子マネー認証解除確認", false);
        }).show(getChildFragmentManager(), null);
    }

    /* コード決済認証 */
    // 注意！　コード決済認証 解除ボタン自体を削除したため今のところ呼ばれることはありません
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onDeactivateQRClick(View view) {
        CommonClickEvent.RecordClickOperation("解除", "コード決済認証", true);

        ConfirmDialog.newInstance("解除しますか？", () -> {
            new Thread(() -> {
                _sharedViewModel.setLoading(true);
                String errCode = _installationAndRemovalViewModel.uninstallQR();
                _sharedViewModel.setLoading(false);

                _handler.post(() -> {
                    if (errCode != null) {
                        CommonErrorDialog errorDialog = new CommonErrorDialog();

                        errorDialog.ShowErrorMessage(
                                requireActivity(), errCode);
                    } else {
                        SuccessDialog.show(requireContext(), "解除しました");
                    }
                });
            }).start();
        }).show(getChildFragmentManager(), null);
    }

    /* Edy業務 */
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onEdyActivation(View view) {
        CommonClickEvent.RecordClickOperation("初回通信", "Edy業務", false);

        ConfirmDialog.newInstance("【Edy業務初回通信確認】","初回通信しますか？", () -> {
            CommonClickEvent.RecordClickOperation("はい", "Edy業務初回通信確認", false);
            new Thread(() -> {
                _sharedViewModel.setLoading(true);
//                _installationAndRemovalViewModel.edyInitCommunication();
                _handler.post(() -> {
                    if (_installationAndRemovalViewModel.isEdyInitCommunicated().getValue()) {
                        Timber.i("正常終了:Edy初回通信が完了しました");
                        SuccessDialog.show(requireContext(), "Edy初回通信が完了しました");
                    } else {
                        CommonErrorDialog errorDialog = new CommonErrorDialog();
                        Timber.e("異常終了:Edy初回通信が失敗しました");
                        errorDialog.ShowErrorMessage(
                                requireActivity(), _app.getString(R.string.error_type_edy_init_communication_error));
                    }
                });
                _sharedViewModel.setLoading(false);
            }).start();
        },() ->{
            CommonClickEvent.RecordClickOperation("いいえ", "Edy業務初回通信確認", false);
        }).show(getChildFragmentManager(), null);
    }

    // コードは残して置くが端末から撤去業務はしないことになったのでコールしたらダメ
    @Override
    public void onEdyDeactivation(View view) {
        CommonClickEvent.RecordButtonClickOperation(view, true);
        ConfirmDialog.newInstance("撤去しますか？", () -> {
            _sharedViewModel.setLoading(true);
//            _installationAndRemovalViewModel.edyRemove();
            if (!_installationAndRemovalViewModel.isEdyInitCommunicated().getValue()) {
                SuccessDialog.show(requireContext(), "撤去しました");
            } else {
            }
            _sharedViewModel.setLoading(false);
        }).show(getChildFragmentManager(), null);
    }

    /* OKICA */
    @Override
    public void onOkicaActivation(View view) {
        CommonClickEvent.RecordClickOperation("設置", "OKICA", false);
        ConfirmDialog.newInstance("【OKICA設置確認】","設置しますか？", () -> {
            CommonClickEvent.RecordClickOperation("はい", "OKICA設置確認", false);
            NavigationWrapper.navigate(this, R.id.action_navigation_installation_and_removal_to_navigation_installation_okica);
        },() ->{
            CommonClickEvent.RecordClickOperation("いいえ", "OKICA設置確認", false);
        }).show(getChildFragmentManager(), null);
    }

    @Override
    public void onOkicaDeactivation(View view) {
        CommonClickEvent.RecordClickOperation("撤去", "OKICA", false);
        ConfirmDialog.newInstance("【OKICA撤去確認】","撤去しますか？", () -> {
            CommonClickEvent.RecordClickOperation("はい", "OKICA撤去確認", false);
            new Thread(() -> {
                if (DBManager.getUriOkicaDao().getUnsentCnt() > 0) {
                    Timber.e("【撤去不可】未送信の売上が存在します。業務終了を行ってください。");
                    // 未送信売上データが存在する場合は撤去できない
                    _handler.post(() -> {
                        ErrorDialog.show(requireContext(), "【撤去不可】" + "\n" + "未送信の売上が存在します。" + "\n" + "業務終了を行ってください。");
                    });
                } else if (DBManager.getSlipDao().getAggregate().size() > 0) {
                    Timber.e("【撤去不可】未印刷の集計が存在します。業務終了を行ってください。");
                    // 未印刷集計データが存在する場合は撤去できない
                    _handler.post(() -> {
                        ErrorDialog.show(requireContext(), "【撤去不可】" + "\n" + "未印刷の集計が存在します。" + "\n" + "業務終了を行ってください。");
                    });
                } else {
                    _sharedViewModel.setLoading(true);

                    final McOkicaCenterApi api = new McOkicaCenterApiImpl();
                    final TerminalUninstallation.Response response = api.uninstallTerminal();

                    if (response.result || response.errorCode.equals(Status.UNAUTHENTICATED.getCode().toString())) {
                        // 強制撤去されている場合も同様に撤去処理を行う

                        if (!_app.isInitFeliCaSAM() || SamManagementUtils.reset() == false) {
                            Timber.e("【SAMリセット失敗】管理者に連絡してください。");
                            _handler.post(() -> {
                                ErrorDialog.show(requireContext(), "【SAMリセット失敗】\n" + "管理者に連絡してください。");
                            });
                        } else if (ICMaster.delete() == false) {
                            Timber.e("【IC運用マスタファイル削除失敗】管理者に連絡してください。");
                            _handler.post(() -> {
                                ErrorDialog.show(requireContext(), "【IC運用マスタファイル削除失敗】\n" + "管理者に連絡してください。");
                            });
                        } else if (OkicaNegaFile.delete() == false) {
                            Timber.e("【ネガファイル削除失敗】管理者に連絡してください。");
                            _handler.post(() -> {
                                ErrorDialog.show(requireContext(), "【ネガファイル削除失敗】\n" + "管理者に連絡してください。");
                            });
                        } else {
                            AppPreference.clearOkica();
                            Timber.i("【撤去完了】撤去成功しました。");
                            _handler.post(() -> {
                                SuccessDialog.show(requireContext(), "【撤去完了】\n" + "撤去成功しました。");
                                _installationAndRemovalViewModel.isOkicaInstalled(false);
                            });
                        }
                    } else {
                        if (response.errorCode.equals(Status.DEADLINE_EXCEEDED.getCode().toString())) {
                            // タイムアウト
                            Timber.e("【タイムアウト】電波の良い場所に移動して再度撤去してください。");
                            _handler.post(() -> {
                                ErrorDialog.show(requireContext(), "【タイムアウト】\n" + "電波の良い場所に移動して再度撤去してください。");
                            });
                        } else {
                            Timber.e("【撤去失敗】管理者に連絡してください。");
                            _handler.post(() -> {
                                ErrorDialog.show(requireContext(), "【撤去失敗】\n" + "管理者に連絡してください。");
                            });
                        }
                    }

                    _sharedViewModel.setLoading(false);
                }
            }).start();
        },() ->{
            CommonClickEvent.RecordClickOperation("いいえ", "OKICA撤去確認", false);
        }).show(getChildFragmentManager(), null);
    }

    /* IM-A820 */
    @Override
    public void onIFBoxSetupClick(View view) {
        CommonClickEvent.RecordClickOperation("登録", "IM-A820", false);
        ConfirmDialog.newInstance("【IM-A820登録確認】","登録しますか？", () -> {
            CommonClickEvent.RecordClickOperation("はい", "IM-A820登録確認", false);
            if (AppPreference.getTabletLinkInfo() != null) {
                Timber.e("タブレット連動中です　この機能を利用する場合はタブレット連動の解除を行ってください");
                final String title = "タブレット連動中です";
                final String msg = "この機能を利用する場合はタブレット連動の解除を行ってください";
                AlertDialogFragment.newInstance(title, msg, () -> {
                }).show(getChildFragmentManager(), null);
            } else {
                NavigationWrapper.navigate(this, R.id.action_navigation_installation_and_removal_to_navigation_ifbox_setup);
            }
        },() ->{
            CommonClickEvent.RecordClickOperation("いいえ", "IM-A820登録確認", false);
        }).show(getChildFragmentManager(), null);
    }

    @Override
    public void onDisconnectIFBox(View view) {
        CommonClickEvent.RecordClickOperation("解除", "IM-A820", false);
        if (getChildFragmentManager().getFragments().size() > 0) {
            Timber.e("getChildFragmentManager().getFragments().size():%d > 0", getChildFragmentManager().getFragments().size());
            return;
        }

        ConfirmDialog.newInstance("【IM-A820解除確認】","解除しますか？", () -> {
            CommonClickEvent.RecordClickOperation("はい", "IM-A820解除確認", false);
            Amount.setMeterCharge(0);
            _installationAndRemovalViewModel.disconnectIFBox();
//            _installationAndRemovalViewModel.removeWifiP2pGroup()
//                    .subscribeOn(Schedulers.io())
//                    .observeOn(AndroidSchedulers.mainThread())
//                    .doFinally(() -> {
//                        _installationAndRemovalViewModel.setDeviceInterlocking();
//                        Timber.i("正常終了:解除しました IM-A820のSSID確認画面で電源ボタンを長押しし、設定のクリアを行ってください");
//                        SuccessDialog.show(requireContext(), "解除しました\n\nIM-A820のSSID確認画面で電源ボタンを長押しし、設定のクリアを行ってください");
//                    })
//                    .subscribe(() -> { }, e -> {
//                        Timber.e("異常終了:解除失敗しました");
//                        Timber.e(e);
//                        ErrorDialog.show(requireContext(), "解除失敗しました");
//                    });
        },() ->{
            CommonClickEvent.RecordClickOperation("いいえ", "IM-A820解除確認", false);
        }).show(getChildFragmentManager(), null);
    }

    /* タブレット連動 */
    @Override
    public void onTabletLinkSetupClick(View view) {
        CommonClickEvent.RecordClickOperation("登録", "タブレット連動", false);
        ConfirmDialog.newInstance("【タブレット連動登録確認】","登録しますか？", () -> {
            CommonClickEvent.RecordClickOperation("はい", "タブレット連動登録確認", false);
            if (AppPreference.getTabletLinkInfo() == null && AppPreference.getIFBoxOTAInfo() != null) {
                Timber.e("IM-A820連動中です この機能を利用する場合はIM-A820の解除を行ってください");
                final String title = "IM-A820連動中です";
                final String msg = "この機能を利用する場合はIM-A820の解除を行ってください";
                AlertDialogFragment.newInstance(title, msg, () -> {
                }).show(getChildFragmentManager(), null);
            } else {
//                NavigationWrapper.navigate(this, R.id.action_navigation_installation_and_removal_to_navigation_tablet_link_setup);
            }
        },() ->{
            CommonClickEvent.RecordClickOperation("いいえ", "タブレット連動登録確認", false);
        }).show(getChildFragmentManager(), null);
    }

    @Override
    public void onTabletUnlinkClick(View view) {
        CommonClickEvent.RecordClickOperation("解除", "タブレット連動", false);
        ConfirmDialog.newInstance("【タブレット連動解除確認】","解除しますか？", () -> {
            CommonClickEvent.RecordClickOperation("はい", "タブレット連動解除確認", false);
            Amount.setMeterCharge(0);
//            _installationAndRemovalViewModel.removeWifiP2pGroup()
//                    .subscribeOn(Schedulers.io())
//                    .observeOn(AndroidSchedulers.mainThread())
//                    .doFinally(() -> {
//                        _installationAndRemovalViewModel.setDeviceInterlocking();
//                        Timber.i("正常終了:解除しました");
//                        SuccessDialog.show(requireContext(), "解除しました");
//                    })
//                    .subscribe(() -> { }, e -> {
//                        Timber.e("異常終了:解除失敗しました");
//                        Timber.e(e);
//                        ErrorDialog.show(requireContext(), "解除失敗しました");
//                    });
        },() ->{
            CommonClickEvent.RecordClickOperation("いいえ", "タブレット連動解除確認", false);
        }).show(getChildFragmentManager(), null);
    }

    /* つり銭機連動 */
    @Override
    public void onCashChangerSetupClick(View view) {
        CommonClickEvent.RecordClickOperation("登録", "つり銭機連動", false); //LOG
        ConfirmDialog.newInstance("【外部機器連動登録確認】","登録しますか？", () -> {
            requireActivity().runOnUiThread(() -> {
            checkExternalDeviceType();
            String[] applications = {"つり銭機", "ドロア+プリンター連動", "プリンターのみ", "ドロアーのみ"};	// ★ここで外部機器タイプのリストを指定

            RadioDialogFragment dialog = RadioDialogFragment.newInstance("外部機器連動タイプ", applications, _externalDeviceType);
            dialog.show(getChildFragmentManager(), "");
        });
        },() ->{
            CommonClickEvent.RecordClickOperation("いいえ", "つり銭機連動登録確認", false); //LOG
        }).show(getChildFragmentManager(), null);
    }

    @Override
    public void onDisconnectCashChanger(View view) {
        CommonClickEvent.RecordClickOperation("解除", "つり銭機連動", false);
        ConfirmDialog.newInstance("【外部機器連動解除確認】","解除しますか？", () -> {
            CommonClickEvent.RecordClickOperation("はい", "つり銭機連動解除確認", false);
            AppPreference.setIsCashChanger(false);
            AppPreference.setCashDrawerType(0);
            _installationAndRemovalViewModel.setExternal(false);
            SuccessDialog.show(requireContext(), "外部機器連動を解除しました。");
        },() ->{
            CommonClickEvent.RecordClickOperation("いいえ", "つり銭機連動解除確認", false);
        }).show(getChildFragmentManager(), null);
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        switch (_externalDeviceType) {
            case 0:
                AppPreference.setIsCashChanger(true);
                AppPreference.setCashDrawerType(0);
                break;
            case 1:
                AppPreference.setIsCashChanger(false);
                AppPreference.setCashDrawerType(3);
                break;
            case 2:
                AppPreference.setIsCashChanger(false);
                AppPreference.setCashDrawerType(1);
                break;
            case 3:
                AppPreference.setIsCashChanger(false);
                AppPreference.setCashDrawerType(2);
                break;
            default:
                AppPreference.setIsCashChanger(true);
                AppPreference.setCashDrawerType(0);
        }
        _installationAndRemovalViewModel.setExternal(true);
        dialog.dismiss();
    }

    @Override
    public void onRadioButtonClick(DialogFragment dialog, int which)
    {
        _externalDeviceType = which;
    }

    public void checkExternalDeviceType() {
        _externalDeviceType = 0;
        if (AppPreference.getIsCashChanger()) {
            _externalDeviceType = 0;
        }
        else if (AppPreference.getCashDrawerType() == 1) {
            _externalDeviceType = 2;
        }
        else if (AppPreference.getCashDrawerType() == 2) {
            _externalDeviceType = 3;
        }
        else if (AppPreference.getCashDrawerType() == 3) {
            _externalDeviceType = 1;
        }
    }

    /* 係員番号履歴 */
    @Override
    public void onRemoveDriverCodeClick(View view) {
        if (getChildFragmentManager().getFragments().size() > 0) return;
        CommonClickEvent.RecordClickOperation("削除", "係員番号履歴", false);

        ConfirmDialog.newInstance("【係員番号履歴解除確認】","削除しますか？", () -> {
            CommonClickEvent.RecordClickOperation("はい", "係員番号履歴解除確認", false);
            Thread thread = new Thread(() -> {
                DBManager.getDriverDao().deleteAll();
            });
            thread.start();
            try {
                thread.join();
                _installationAndRemovalViewModel.fetchDrivers(_db.driverDao());
                Timber.i("正常終了:削除しました");
                SuccessDialog.show(requireContext(), "削除しました");
            } catch (InterruptedException e) {
                e.printStackTrace();
                Timber.e("異常終了:削除失敗しました");
                ErrorDialog.show(requireContext(), "削除失敗しました");
            }
            //AppPreference.setDriverCode(String.valueOf(getResources().getInteger(R.integer.setting_default_mc_driverid)));
            //AppPreference.setDriverName("");
        },() ->{
            CommonClickEvent.RecordClickOperation("いいえ", "係員番号履歴解除確認", false);
        }).show(getChildFragmentManager(), null);
    }

    /* 端末設定画面 */
    @Override
    public void onSettingsClick(View view) {
        CommonClickEvent.RecordClickOperation("開く", "端末設定画面", false);

        final Intent intent = new Intent();
        intent.setAction(Settings.ACTION_SETTINGS);
        startActivity(intent);
        Timber.i("端末設定画面が表示されました");
    }

    /* デモモード */
    @Override
    public void onEnableDemoClick(View view) {

        if (AppPreference.isServicePos() && AppPreference.isServiceTicket()) {
            // POS機能、チケット販売機能のデモモードは未実装のため
            Toast.makeText(requireContext(), "POS機能とチケット販売機能が有効な場合、デモモードの設定できません", Toast.LENGTH_SHORT).show();
        } else if(AppPreference.isServicePos()) {
            // POS機能のデモモードは未実装のため
            Toast.makeText(requireContext(), "POS機能が有効な場合、デモモードの設定できません", Toast.LENGTH_SHORT).show();
        } else if (AppPreference.isServiceTicket()) {
            // チケット販売機能のデモモードは未実装のため
            Toast.makeText(requireContext(), "チケット販売機能が有効な場合、デモモードの設定できません", Toast.LENGTH_SHORT).show();
        } else {
            CommonClickEvent.RecordClickOperation("設定", "デモモード", false);
            ConfirmDialog.newInstance("【デモモード設定確認】", "設定しますか？", () -> {
                CommonClickEvent.RecordClickOperation("はい", "デモモード設定確認", false);
                AppPreference.set_isDemoMode(true);
                _installationAndRemovalViewModel.setDemoEnabled(true);
                _sharedViewModel.setUpdatedFlag(true);
                Timber.i("正常終了:デモモードに切り替わりました。カードへの書き込みは行いません。");
                SuccessDialog.show(requireContext(), "デモモードに切り替わりました。\nカードへの書き込みは行いません。");
            }, () -> {
                CommonClickEvent.RecordClickOperation("いいえ", "デモモード設定確認", false);
            }).show(getChildFragmentManager(), null);
        }
    }

    @Override
    public void onDisableDemoClick(View view) {
        CommonClickEvent.RecordClickOperation("解除", "デモモード", false);
        ConfirmDialog.newInstance("【デモモード解除確認】","解除しますか？", () -> {
            CommonClickEvent.RecordClickOperation("はい", "デモモード解除確認", false);
            AppPreference.set_isDemoMode(false);
            _installationAndRemovalViewModel.setDemoEnabled(false);
            _sharedViewModel.setUpdatedFlag(true);
            Timber.i("正常終了:デモモードを解除しました。カードへの書き込みは行います。");
            SuccessDialog.show(requireContext(), "デモモードを解除しました。\nカードへの書き込みは行います。");
        },() ->{
            CommonClickEvent.RecordClickOperation("いいえ", "デモモード解除確認", false);
        }).show(getChildFragmentManager(), null);
    }

    /* センター設定 */
    @Override
    public void onTerminalInfoClick(View view) {
        CommonClickEvent.RecordClickOperation("開く", "センター設定", false);
        if (getChildFragmentManager().getFragments().size() > 0) {
            Timber.e("getChildFragmentManager().getFragments().size():%d > 0", getChildFragmentManager().getFragments().size());
            return;
        }

        new CenterInfoDialog().show(getChildFragmentManager(), null);
        Timber.i("センター配信設定画面が表示されました");
    }

    /* OKICA初期化 */
    @Override
    public void onOkicaInitializeClick(View view) {
        CommonClickEvent.RecordClickOperation("初期化", "OKICA", false);
        ConfirmDialog.newInstance("【OKICA初期化確認】","初期化しますか？", () -> {
            CommonClickEvent.RecordClickOperation("はい", "OKICA初期化確認", false);
            new Thread(() -> {
                if (DBManager.getUriOkicaDao().getUnsentCnt() > 0) {
                    Timber.e("【初期化不可】未送信の売上が存在します。業務終了を行ってください。");
                    // 未送信売上データが存在する場合は初期化できない
                    _handler.post(() -> {
                        ErrorDialog.show(requireContext(), "【初期化不可】" + "\n" + "未送信の売上が存在します。" + "\n" + "業務終了を行ってください。");
                    });
                } else if (DBManager.getSlipDao().getAggregate().size() > 0) {
                    Timber.e("【初期化不可】未印刷の集計が存在します。業務終了を行ってください。");
                    // 未印刷集計データが存在する場合は初期化できない
                    _handler.post(() -> {
                        ErrorDialog.show(requireContext(), "【初期化不可】" + "\n" + "未印刷の集計が存在します。" + "\n" + "業務終了を行ってください。");
                    });
                } else {
                    _sharedViewModel.setLoading(true);

                    if (!_app.isInitFeliCaSAM() || SamManagementUtils.reset() == false) {
                        Timber.e("【SAMリセット失敗】管理者に連絡してください。");
                        _handler.post(() -> {
                            ErrorDialog.show(requireContext(), "【SAMリセット失敗】\n" + "管理者に連絡してください。");
                        });
                    } else if (ICMaster.delete() == false) {
                        Timber.e("【IC運用マスタファイル削除失敗】管理者に連絡してください。");
                        _handler.post(() -> {
                            ErrorDialog.show(requireContext(), "【IC運用マスタファイル削除失敗】\n" + "管理者に連絡してください。");
                        });
                    } else if (OkicaNegaFile.delete() == false) {
                        Timber.e("【ネガファイル削除失敗】管理者に連絡してください。");
                        _handler.post(() -> {
                            ErrorDialog.show(requireContext(), "【ネガファイル削除失敗】\n" + "管理者に連絡してください。");
                        });
                    } else {
                        AppPreference.clearOkica();
                        Timber.i("【初期化完了】初期化に成功しました。");
                        _handler.post(() -> {
                            SuccessDialog.show(requireContext(), "【初期化完了】\n" + "初期化に成功しました。");
                            _installationAndRemovalViewModel.isOkicaInstalled(false);
                        });
                    }

                    _sharedViewModel.setLoading(false);
                }
            }).start();
        },() ->{
            CommonClickEvent.RecordClickOperation("いいえ", "OKICA初期化確認", false);
        }).show(getChildFragmentManager(), null);
    }

    /* POS機能認証 */
    @Override
    public void onPosAuthenticationClick(View view) {
        if(AppPreference.isDemoMode()) {
            Toast.makeText(requireContext(), "デモモードではPOS機能認証できません。", Toast.LENGTH_SHORT).show();
        } else if (AppPreference.isServiceTicket()) {
            // POS機能とチケット販売機能を同時に有効設定での試験未実施のため、ガード処理を入れてます。
            // 試験実施するときには、ガード処理を無効にしてください。
            Toast.makeText(requireContext(), "チケット販売機能認証済みではPOS機能認証できません。", Toast.LENGTH_SHORT).show();
        } else {
            Timber.d("isPos : %s / ActivationToken : %s / RefreshToken : %s", AppPreference.isServicePos(), AppPreference.get_servicePosAccessToken(), AppPreference.get_servicePosRefreshToken());
            CommonClickEvent.RecordClickOperation("認証", "POS機能", false);
            ConfirmDialog.newInstance("【POS機能設定】", "認証しますか？", () -> {
                CommonClickEvent.RecordClickOperation("はい", "POS機能認証", false);
                //NavigationWrapper.navigate(this, R.id.action_navigation_installation_and_removal_to_navigation_pos_activation);
            }, () -> {
                CommonClickEvent.RecordClickOperation("いいえ", "POS機能認証", false);
            }).show(getChildFragmentManager(), null);
        }
    }

    @Override
    public  void onPosClearClick(View view) {
        Timber.d("isPos : %s / ActivationToken : %s / RefreshToken : %s" , AppPreference.isServicePos() , AppPreference.get_servicePosAccessToken() , AppPreference.get_servicePosRefreshToken());
        CommonClickEvent.RecordClickOperation("クリア", "POS機能", false);
        ConfirmDialog.newInstance("【POS機能設定】", "内部データをクリアしますか？", () -> {
            CommonClickEvent.RecordClickOperation("はい", "POS機能クリア", false);
            // 内部データをクリアする
            AppPreference.clearServicePos();
            _installationAndRemovalViewModel.setPosActivate(false);
            if(AppPreference.isServiceTicket()) {
                Toast.makeText(view.getContext(),"内部データをクリアしました。" , Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(view.getContext(),"内部データをクリアしました。\nFIGコンソールにてディアクティベーション処理を行ってください" , Toast.LENGTH_LONG).show();
            }
        }, () -> {
            CommonClickEvent.RecordClickOperation("いいえ", "POS機能クリア", false);
        }).show(getChildFragmentManager(), null);
    }

    /* チケット販売機能認証 */
    @Override
    public void onTicketAuthenticationClick(View view) {
        if(AppPreference.isDemoMode()) {
            Toast.makeText(requireContext(), "デモモードではチケット販売機能認証できません。", Toast.LENGTH_SHORT).show();
        } else if (AppPreference.isServicePos()) {
            // POS機能とチケット販売機能を同時に有効設定での試験未実施のため、ガード処理を入れてます。
            // 試験実施するときには、ガード処理を無効にしてください。
            Toast.makeText(requireContext(), "POS機能認証済みではチケット販売機能認証できません。", Toast.LENGTH_SHORT).show();
        } else {
            Timber.d("isTicket : %s / ActivationToken : %s / RefreshToken : %s", AppPreference.isServiceTicket(), AppPreference.get_serviceTicketAccessToken(), AppPreference.get_serviceTicketRefreshToken());
            CommonClickEvent.RecordClickOperation("認証", "チケット販売機能", false);
            ConfirmDialog.newInstance("【チケット販売機能設定】", "認証しますか？", () -> {
                CommonClickEvent.RecordClickOperation("はい", "チケット販売機能認証", false);
//                NavigationWrapper.navigate(this, R.id.action_navigation_installation_and_removal_to_navigation_ticket_activation);
            }, () -> {
                CommonClickEvent.RecordClickOperation("いいえ", "チケット販売機能認証", false);
            }).show(getChildFragmentManager(), null);
        }
    }

    @Override
    public  void onTicketClearClick(View view) {
        Timber.d("isTicket : %s / ActivationToken : %s / RefreshToken : %s" , AppPreference.isServiceTicket() , AppPreference.get_serviceTicketAccessToken() , AppPreference.get_serviceTicketRefreshToken());
        CommonClickEvent.RecordClickOperation("クリア", "チケット販売機能", false);
        ConfirmDialog.newInstance("【チケット販売機能設定】", "内部データをクリアしますか？", () -> {
            CommonClickEvent.RecordClickOperation("はい", "チケット販売機能クリア", false);
            // 内部データをクリアする
            AppPreference.clearServiceTicket();
            _installationAndRemovalViewModel.setTicketActivate(false);
            if(AppPreference.isServicePos()) {
                Toast.makeText(view.getContext(),"内部データをクリアしました。" , Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(view.getContext(),"内部データをクリアしました。\nFIGコンソールにてディアクティベーション処理を行ってください" , Toast.LENGTH_LONG).show();
            }
        }, () -> {
            CommonClickEvent.RecordClickOperation("いいえ", "チケット販売機能クリア", false);
        }).show(getChildFragmentManager(), null);
    }

    @Override
    public void onPause() {
        super.onPause();
        AppPreference.load();
        if(AppPreference.isDemoMode()) {
            //業務開始日時の設定
            SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.JAPANESE);
            new Thread(() -> {
                String date = dateFmt.format((new Date()));
                DBManager.getAggregateDao().insertAggregateStart(date);
            }).start();
        }
        _sharedViewModel.setUpdatedFlag(true);
    }

    public static class CenterInfoDialog extends BaseDialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            // 画面外のタップを無効化
            this.setCancelable(false);

            final DialogTerminalInfoBinding binding = DataBindingUtil.inflate(
                    LayoutInflater.from(getActivity()), R.layout.dialog_terminal_info, null, false);

            List<String[]> info = AppPreference.getTerminalInfo();

            ListView listView = binding.getRoot().findViewById(R.id.list_terminal_info);
            listView.setAdapter(new ListViewAdapter(binding.getRoot().getContext(), R.layout.item_terminal_info, info));

            final AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity())
                    .setView(binding.getRoot())
                    .setPositiveButton("閉じる", (dialog, which) -> {
                        CommonClickEvent.RecordClickOperation("閉じる", "センター配信設定画面", false);
                        dialog.dismiss();
                    });

            return builder.create();
        }
    }

    private static class ListViewAdapter extends ArrayAdapter<String[]> {
        private final LayoutInflater inflater;
        private final int itemLayout;

        private static class ViewHolder {
            TextView numberView;
            TextView nameView;
            TextView valueView;
            TextView valueView2;
        }

        ListViewAdapter(Context context, int itemLayout, List<String[]> list) {
            super(context, 0, list);
            this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            this.itemLayout = itemLayout;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = inflater.inflate(itemLayout, parent, false);
                holder = new ViewHolder();
                holder.numberView = convertView.findViewById(R.id.info_number);
                holder.nameView = convertView.findViewById(R.id.info_name);
                holder.valueView = convertView.findViewById(R.id.info_value);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            String[] data = getItem(position);
            if(data != null) {
                holder.numberView.setText(String.format(Locale.JAPANESE, "%d. ", position + 1));
                holder.nameView.setText(data[0]);
                holder.valueView.setText(data[1]);
            }

            return convertView;
        }
    }
}
