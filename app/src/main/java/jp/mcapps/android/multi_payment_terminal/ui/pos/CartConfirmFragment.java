package jp.mcapps.android.multi_payment_terminal.ui.pos;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

import java.util.List;
import java.util.logging.Logger;

import jp.mcapps.android.multi_payment_terminal.CommonClickEvent;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.ScreenData;
import jp.mcapps.android.multi_payment_terminal.SharedViewModel;
import jp.mcapps.android.multi_payment_terminal.database.pos.CartData;
import jp.mcapps.android.multi_payment_terminal.database.pos.IncludedTaxTypes;
import jp.mcapps.android.multi_payment_terminal.database.pos.ProductTaxTypes;
import jp.mcapps.android.multi_payment_terminal.database.pos.ReducedTaxTypes;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentPosCartConfirmBinding;
import jp.mcapps.android.multi_payment_terminal.ui.dialog.ErrorDialog;
import timber.log.Timber;

public class CartConfirmFragment extends PosBaseFragment {
    private final String SCREEN_NAME = "商品確認";
    private CartConfirmViewModel cartConfirmViewModel;
    private CartConfirmAdapter adapter;

    private List<CartModel> _cartModelList; // 表示用モデルクラスリスト
    private List<CartData> _cartOverdueItems; // お支払い期限切れ商品リスト

    // 外付けバーコードリーダーで商品追加した場合のエラーメッセージ表示用
    private LiveData<String> errorMessageLiveData;
    private Observer<String> errorMessageObserver;

    private View.OnKeyListener onKeyListener;
    private String barcodeString = "";  // バーコードで読み取った文字列

    private final Logger logger = Logger.getLogger("test");

    public static CartConfirmFragment newInstance() {
        return new CartConfirmFragment();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Timber.d("onCreateView");
        FontHelper.initialize(getActivity());

        final FragmentPosCartConfirmBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_pos_cart_confirm, container, false);

        final PosEventHandlers _PosEventHandlers = new PosEventHandlersImpl(this); // イベントハンドラ
        final CartConfirmHandlers _cartConfirmHandlers = new CartConfirmHandlersImpl(); // イベントハンドラ

        SharedViewModel sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        cartConfirmViewModel = new ViewModelProvider(this).get(CartConfirmViewModel.class);
        PosViewModel posViewModel = new ViewModelProvider(requireActivity()).get(PosViewModel.class);
        errorMessageLiveData = cartConfirmViewModel.getErrorMessage();

        // 共通UI処理
//        sharedViewModel.setLoading(true);
        sharedViewModel.setScreenMode(SharedViewModel.ScreenMode.POS);

        // data binding
        binding.setPosViewModel(posViewModel);
        binding.setCartConfirmViewModel(cartConfirmViewModel);
        binding.setPosHandlers(_PosEventHandlers);
        binding.setCartConfirmHandlers(_cartConfirmHandlers);
        binding.setLifecycleOwner(getViewLifecycleOwner());
        binding.setSharedViewModel(sharedViewModel);

        adapter = new CartConfirmAdapter();

        RecyclerView.LayoutManager rLayoutManager = new LinearLayoutManager(getContext());
        binding.listCartList.setLayoutManager(rLayoutManager);
        binding.listCartList.setAdapter(adapter);

        ScreenData.getInstance().setScreenName(SCREEN_NAME);

        adapter.setOnItemClickListener(new CartConfirmAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(CartModel item) {
            }

            @Override
            public void onInputClick(CartModel item) {
                String info = String.format("数量変更（%s %s円x%s）",item.name ,item.unitPrice ,item.count);
                CommonClickEvent.RecordClickOperation(info, true);
                _PosEventHandlers.navigateToInputCartCount(getParentFragment(), item.id, item.count);
            }

            @Override
            public void onSelectInputTypeClick(CartModel item) {
                String info = String.format("商品変更（%s %s円x%s）",item.name ,item.unitPrice ,item.count);
                CommonClickEvent.RecordClickOperation(info, true);
                _PosEventHandlers.navigateToSelectInputType(getParentFragment(), item);
            }
        });

        final Bundle args = getArguments();
        if(args != null){
            String productTaxTypeKey = args.getString("productTaxType");
            String reducedTaxTypeKey = args.getString("reducedTaxType");
            String includedTaxTypeKey= args.getString("includedTaxType");
            if(productTaxTypeKey != null){
                cartConfirmViewModel.setProductTaxType(ProductTaxTypes.fromKey(productTaxTypeKey));
            }
            if(reducedTaxTypeKey != null){
                cartConfirmViewModel.setReducedTaxType(ReducedTaxTypes.fromKey(reducedTaxTypeKey));
            }
            if(includedTaxTypeKey != null){
                cartConfirmViewModel.setIncludedTaxType(IncludedTaxTypes.fromKey(includedTaxTypeKey));
            }
        }

        // OnKeyListenerのインスタンスを作成（外付けバーコードリーダー用）
        onKeyListener = new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent keyEvent) {
                if(keyEvent.getDevice() != null) {
                    if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                        switch (keyCode) {
                            case KeyEvent.KEYCODE_ENTER:
                                Timber.i("バーコードで読み取った文字列:%s", barcodeString);
                                cartConfirmViewModel.setProduct(barcodeString);
                                // 追加してすぐに通知すると 1->2 の時だけなぜかうまくいかないので少し遅延させて通知する
                                Handler notifyHandler = new Handler(Looper.getMainLooper());
                                notifyHandler.postDelayed(() -> adapter.notifyDataSetChanged(), 100);
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

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Timber.d("onViewCreated");

        cartConfirmViewModel.getData().observe(getViewLifecycleOwner(), dataList -> {
            Timber.d("getData() observe");
            if (_cartModelList == dataList) {
                return;
            }
            _cartModelList = dataList;

            // データの変更を監視してUIへの反映を行います
            // Adapterにデータをセットしたり、RecyclerViewの更新を行います
            Timber.d("getData() submitList");
            adapter.submitList(dataList);
            // adapter.notifyDataSetChanged();
        });

        cartConfirmViewModel.getPaymentOverdueItems().observe(getViewLifecycleOwner(), items -> {
            if (items != null && !items.isEmpty()) {
                Timber.d("getPaymentOverdueItem() observe: %d", items.size());
                if (_cartOverdueItems != null) {
                    boolean isChanged = false;
                    for (CartData overDueItem : _cartOverdueItems) {
                        boolean isExist = false;
                        for (CartData item : items) {
                            if (overDueItem.id == item.id) {
                                isExist = true;
                                break;
                            }
                        }
                        if (!isExist) {
                            isChanged = true;
                            break;
                        }
                    }
                    if (!isChanged) {
                        return;
                    }
                }
                _cartOverdueItems = items;
                ErrorDialog.show(getContext(), "ご確認ください", "お支払い期限切れの商品が含まれています。");
            }
        });
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Timber.d("onCreate");
    }

    @Override
    public void onStart() {
        super.onStart();
        Timber.d("onStart");
    }

    @Override
    public void onResume() {
        super.onResume();
        Timber.d("onResume");
        cartConfirmViewModel.fetchData();

        View view = getView();
        if(view != null && onKeyListener != null){
            Timber.d("onKeyListenerの登録");
            view.setOnKeyListener(onKeyListener);
            view.setFocusableInTouchMode(true);
            view.requestFocus();
        }

        // Observerの登録
        errorMessageObserver = new Observer<String>() {
            @Override
            public void onChanged(String s) {
                if(!s.equals("")) {
                    Toast.makeText(getContext(), s, Toast.LENGTH_SHORT).show();
                    cartConfirmViewModel.setErrorMessage("");
                }
            }
        };
        errorMessageLiveData.observe(getViewLifecycleOwner(), errorMessageObserver);
    }

    @Override
    public void onPause() {
        super.onPause();
        Timber.d("onPause");

        View view = getView();
        if (view != null && onKeyListener != null) {
            Timber.d("onKeyListenerの登録解除");
            view.setOnKeyListener(null);
        }

        // Observerの登録解除
        errorMessageLiveData.removeObserver(errorMessageObserver);
    }

    @Override
    public void onStop() {
        super.onStop();
        Timber.d("onStop");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Timber.d("onDestroyView");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Timber.d("onDestroy");
    }
}
