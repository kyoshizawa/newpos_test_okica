package jp.mcapps.android.multi_payment_terminal.ui.pos;

import android.annotation.SuppressLint;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import jp.mcapps.android.multi_payment_terminal.CommonClickEvent;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.NavigationWrapper;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.ScreenData;
import jp.mcapps.android.multi_payment_terminal.SharedViewModel;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentPosProductSelectBinding;
import timber.log.Timber;

public class ProductSelectFragment extends PosBaseFragment {
    private final String SCREEN_NAME = "商品選択";
    private Typeface _typeface = null;

    private List<ProductCategorySelectModel> _productCategorySelectModelList;
    private ProductSelectViewModel _productSelectViewModel;
    private ProductSelectAdapter adapter;

    // 外付けバーコードリーダーで商品追加した場合のエラーメッセージ表示用
    private LiveData<String> errorMessageLiveData;
    private Observer<String> errorMessageObserver;

    private View.OnKeyListener onKeyListener;
    private String barcodeString = "";  // バーコードで読み取った文字列

    private final long _delayMillis = 1000;
    private long _pushedMillis = 0;

    public static ProductSelectFragment newInstance() {
        return new ProductSelectFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FontHelper.initialize(getActivity());
        _typeface = FontHelper.getFont();

        Timber.d("onCreateView");

        final FragmentPosProductSelectBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_pos_product_select, container, false);

        // viewModel from viewModelProvider
        SharedViewModel sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        PosViewModel posViewModel = new ViewModelProvider(requireActivity()).get(PosViewModel.class);
        _productSelectViewModel = new ViewModelProvider(this).get(ProductSelectViewModel.class);
        errorMessageLiveData = _productSelectViewModel.getErrorMessages();

        sharedViewModel.setScreenMode(SharedViewModel.ScreenMode.POS);

        // data binding
        binding.setSharedViewModel(sharedViewModel);
        binding.setPosHandlers(new PosEventHandlersImpl(this));
        binding.setPosViewModel(posViewModel);
        binding.setProductSelectViewModel(_productSelectViewModel);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        adapter = new ProductSelectAdapter(_typeface);
        _productCategorySelectModelList = new ArrayList<>();
        adapter.submitList(_productCategorySelectModelList);
        _productSelectViewModel.initFetchData()
                .subscribe(
                        result -> {
                            Timber.d("on init fetch data");
                            _productCategorySelectModelList = result;
                            adapter.submitList(_productCategorySelectModelList);
                        },
                        error -> {

                        }
                );


        RecyclerView.LayoutManager rLayoutManager = new LinearLayoutManager(getContext());
        binding.listProductSelect.setLayoutManager(rLayoutManager);
        binding.listProductSelect.setAdapter(adapter);

        ScreenData.getInstance().setScreenName(SCREEN_NAME);

        adapter.setOnItemClickListener(new ProductSelectAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(ProductCategorySelectModel item) {
                CommonClickEvent.RecordClickOperation(item.name, true);

                Timber.d("Product item clicked: %s (id: %s, category: %s)", item.name, item.id, item.isCategory);
                if (item.isCategory) {
                    _productSelectViewModel.addQue(item.id);
                    _productSelectViewModel.fetchDataWithParentId(item.id);
                } else {
                    // 連続タップ防止
                    long timeMillis = System.currentTimeMillis();
                    if (timeMillis - _pushedMillis < _delayMillis)
                    {
                        Timber.e("１秒以内に連続タップ発生");
                        return;
                    }
                    _pushedMillis = timeMillis;
                    //Timber.d("push time millis:%s",_pushedMillis);

                    _productSelectViewModel.insertDataIntoDatabase(item)
                            .subscribe(
                                    result -> {
                                        if (result) {
                                            // 挿入が成功した場合の処理
                                            NavigationWrapper.navigate(getParentFragment(), R.id.action_navigation_product_select_to_cartConfirmFragment);
                                        } else {
                                            Timber.e("「処理失敗しました」表示");
                                            Toast.makeText(MainApplication.getInstance(), "処理失敗しました", Toast.LENGTH_LONG).show();
                                        }
                                    },
                                    error -> {
                                        // エラーハンドリングの処理 Toastでも出す？
                                        Timber.e(error);
                                        Timber.e("「処理失敗しました」表示");
                                        Toast.makeText(MainApplication.getInstance(), "処理失敗しました", Toast.LENGTH_LONG).show();
                                    }
                            );
                }

            }
        });

        // OnKeyListenerのインスタンスを作成（外付けバーコードリーダー用）
        onKeyListener = new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent keyEvent) {
                if(keyEvent.getDevice() != null) {
                    if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                        switch (keyCode) {
                            case KeyEvent.KEYCODE_ENTER:
                                Timber.i("バーコードで読み取った文字列:%s", barcodeString);
                                ProductCategorySelectModel item = new ProductCategorySelectModel();
                                item.code = barcodeString;
                                _productSelectViewModel.insertDataIntoDatabase(item)
                                        .subscribe(
                                                result -> {
                                                    if (result) {
                                                        // 挿入が成功した場合の処理
                                                        NavigationWrapper.navigate(getParentFragment(), R.id.action_navigation_product_select_to_cartConfirmFragment);
                                                    }
                                                },
                                                error -> {
                                                    // エラーハンドリングの処理 Toastでも出す？
                                                    Timber.e(error);
                                                }
                                        );
                                barcodeString = "";
                                break;
                            case KeyEvent.KEYCODE_SHIFT_LEFT:
                            case KeyEvent.KEYCODE_SHIFT_RIGHT:
                                // 大文字の場合、シフトキーも来るため破棄
                                break;
                            case KeyEvent.KEYCODE_UNKNOWN:
                                // 連続で読み取っていると入る場合があったので破棄
                                break;
                            default:
                                char pressedChar = (char) keyEvent.getUnicodeChar(keyEvent.getMetaState());
                                String pressedKey = Character.toString(pressedChar);
                                barcodeString += pressedKey;
                                break;
                        }
                        return true;
                    }
                }
                return false;
            }
        };

        return binding.getRoot();
    }

    @SuppressLint("TimberArgCount")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Timber.d("Android Lifecycle onViewCreated");
        _productSelectViewModel.getData().observe(getViewLifecycleOwner(), dataList -> {
            if (dataList == null) return;
            Timber.d("on observe data: %s", dataList.size());

            // データの変更を監視してUIへの反映を行います
            _productCategorySelectModelList = dataList;
            Runnable commitCallBack = new Runnable() {
                @Override
                public void run() {
                    // adapter.notifyDataSetChanged(); // RecyclerViewの更新を行います
                }
            };
            adapter.submitList(_productCategorySelectModelList, commitCallBack);
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        View view = getView();
        if(view != null && onKeyListener != null){
            Timber.d("onKeyListenerの登録");
            view.setOnKeyListener(onKeyListener);
            view.setFocusableInTouchMode(true);
            view.requestFocus();
        }

        // Observableの登録
        errorMessageObserver = new Observer<String>() {
            @Override
            public void onChanged(String s) {
                if(!s.equals("")) {
                    Toast.makeText(getContext(), s, Toast.LENGTH_SHORT).show();
                    _productSelectViewModel.setErrorMessages("");
                }
            }
        };
        errorMessageLiveData.observe(getViewLifecycleOwner(), errorMessageObserver);
    }

    @Override
    public void onPause() {
        super.onPause();

        View view = getView();
        if (view != null && onKeyListener != null) {
            Timber.d("onKeyListenerの登録解除");
            view.setOnKeyListener(null);
        }

        // Observableの登録解除
        errorMessageLiveData.removeObserver(errorMessageObserver);
    }
}
