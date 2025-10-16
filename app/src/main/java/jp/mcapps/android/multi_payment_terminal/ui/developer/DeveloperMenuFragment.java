package jp.mcapps.android.multi_payment_terminal.ui.developer;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jp.mcapps.android.multi_payment_terminal.BaseFragment;
import jp.mcapps.android.multi_payment_terminal.BuildConfig;
import jp.mcapps.android.multi_payment_terminal.CommonClickEvent;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.NavigationWrapper;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.ScreenData;
import jp.mcapps.android.multi_payment_terminal.SharedViewModel;
import jp.mcapps.android.multi_payment_terminal.database.DBManager;
import jp.mcapps.android.multi_payment_terminal.database.LocalDatabase;
import jp.mcapps.android.multi_payment_terminal.database.history.gps.GpsDao;
import jp.mcapps.android.multi_payment_terminal.database.history.radio.RadioDao;
import jp.mcapps.android.multi_payment_terminal.database.history.slip.SlipDao;
import jp.mcapps.android.multi_payment_terminal.database.history.uri.UriDao;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentDeveloperMenuBinding;
import timber.log.Timber;

public class DeveloperMenuFragment extends BaseFragment {

    private final String SCREEN_NAME = "開発者メニュー";
    public static DeveloperMenuFragment newInstance() {
        return new DeveloperMenuFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        FragmentDeveloperMenuBinding binding = DataBindingUtil.inflate(inflater, R.layout.fragment_developer_menu, container, false);

        List<String> menu = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.developer_menu_array)));
        List<Integer> navIds = new ArrayList<>(Arrays.asList(
                R.id.action_navigation_developer_menu_to_navigation_developer_realtime_gps,
                R.id.action_navigation_developer_menu_to_navigation_developer_history_gps,
                R.id.action_navigation_developer_menu_to_navigation_developer_realtime_radio,
                R.id.action_navigation_developer_menu_to_navigation_developer_history_radio,
                R.id.action_navigation_developer_menu_to_navigation_developer_add_dummy_transaction,
                R.id.action_navigation_developer_menu_to_navigation_developer_add_dummy_driver
        ));

        if (!BuildConfig.DEBUG) {
            //releaseビルドの場合、ダミー決済追加をメニューから除外
            menu.remove(4);
            navIds.remove(4);
        }

        ListView listView = binding.listDeveloperMenu;
        ArrayAdapter<String> arrayAdapter =
                new ArrayAdapter<>(getContext(), R.layout.item_developer_menu, menu);

        listView.setAdapter(arrayAdapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            CommonClickEvent.RecordClickOperation(menu.get(position), false);
            if (position < navIds.size()) {
                requireActivity().runOnUiThread(() -> {
                    NavigationWrapper.navigate(view, navIds.get(position));
                });
            } else {
                String dialogName = "取引履歴削除確認";
                new AlertDialog.Builder(view.getContext())
                        .setTitle("削除確認")
                        .setMessage("取引履歴を削除します。\nよろしいですか？")
                        .setPositiveButton("はい", (dialogInterface, i) -> {
                            CommonClickEvent.RecordClickOperation("はい", dialogName, true);
                            deleteTransHistory(view);
                        })
                        .setNegativeButton("いいえ", (dialogInterface, i) -> CommonClickEvent.RecordClickOperation("いいえ", dialogName, true))
                        .show();
            }
        });

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
    }

    private void deleteTransHistory(View view) {
        final SharedViewModel sharedViewModel =
                new ViewModelProvider(getActivity()).get(SharedViewModel.class);
        sharedViewModel.setLoading(true);

        Thread thread = new Thread(() -> {
            DBManager.getSlipDao().deleteAll();
            DBManager.getUriDao().deleteAll();
        });
        thread.start();

        try {
            thread.join();

            sharedViewModel.setLoading(false);
            new AlertDialog.Builder(view.getContext())
                    .setTitle("削除成功")
                    .setMessage("取引履歴を削除しました")
                    .setPositiveButton("確認", (dialogInterface, i) -> CommonClickEvent.RecordClickOperation("確認", "取引履歴削除成功", true))
                    .show();
        } catch (Exception e) {
            e.printStackTrace();

            sharedViewModel.setLoading(false);
            new AlertDialog.Builder(view.getContext())
                    .setTitle("削除失敗")
                    .setMessage("取引履歴の削除に失敗しました")
                    .setPositiveButton("確認", (dialogInterface, i) -> CommonClickEvent.RecordClickOperation("確認", "取引履歴削除失敗", true))
                    .show();
        }
    }
}
