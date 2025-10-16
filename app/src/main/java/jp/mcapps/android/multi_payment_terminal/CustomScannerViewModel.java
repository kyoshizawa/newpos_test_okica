package jp.mcapps.android.multi_payment_terminal;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayDeque;
import java.util.ArrayList;

import jp.mcapps.android.multi_payment_terminal.data.QRPayTypeCodes;

public class CustomScannerViewModel extends ViewModel {

    public CustomScannerViewModel() {
        _enabledBrands = MainApplication.getInstance().getQREnabledFlags();
        firstFlowIds = new ArrayDeque<>();
        secondFlowIds = new ArrayDeque<>();

        if (getFlag(QRPayTypeCodes.Flags.DOCOMO) != 0) firstFlowIds.add(R.drawable.ic_qr_d_payment);
        if (getFlag(QRPayTypeCodes.Flags.AUPAY) != 0) firstFlowIds.add(R.drawable.ic_qr_aupay);
        if (getFlag(QRPayTypeCodes.Flags.GINKOPAY) != 0) firstFlowIds.add(R.drawable.ic_qr_yuchopay);
        if (getFlag(QRPayTypeCodes.Flags.RAKUTENPAY) != 0) firstFlowIds.add(R.drawable.ic_qr_rpay);
        if (getFlag(QRPayTypeCodes.Flags.PAYPAY) != 0) firstFlowIds.add(R.drawable.ic_qr_paypay);
        if (getFlag(QRPayTypeCodes.Flags.MERPAY) != 0) firstFlowIds.add(R.drawable.ic_qr_merpay);
        if (getFlag(QRPayTypeCodes.Flags.WECHAT) != 0) firstFlowIds.add(R.drawable.ic_qr_wechatpay);

        if (getFlag(QRPayTypeCodes.Flags.ALIPAYPLUS) != 0) {
            int surplusCnt = firstFlowIds.size() % 3;
            if (surplusCnt != 0) {
                for (int i = 0; i < surplusCnt; i++) {
                    secondFlowIds.addFirst(firstFlowIds.pollLast());
                }
            }
            secondFlowIds.add(R.drawable.ic_qr_alipayplus);
        }
    }
    private MutableLiveData<Boolean> _useLight = new MutableLiveData<>(false);
    public MutableLiveData<Boolean> useLight() {
        return _useLight;
    }
    public void useLight(boolean b) {
        _useLight.setValue(b);
    }

    private final int _enabledBrands;
    public final ArrayDeque<Integer> firstFlowIds;
    public final ArrayDeque<Integer> secondFlowIds;

    public int getFlag(int flag) {
        return _enabledBrands & flag;
    }
}
