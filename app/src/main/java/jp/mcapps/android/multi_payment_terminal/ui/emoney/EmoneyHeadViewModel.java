package jp.mcapps.android.multi_payment_terminal.ui.emoney;

import android.graphics.drawable.Drawable;
import android.view.View;

import java.text.NumberFormat;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.data.BusinessType;

public class EmoneyHeadViewModel extends ViewModel {
    private final MutableLiveData<String> _amount = new MutableLiveData<>("");
    public MutableLiveData<String> getAmount() { return _amount; }
    public void setAmount(int amount) {
        final NumberFormat nf = NumberFormat.getNumberInstance();
        _amount.setValue(nf.format(amount));
    }

    private MutableLiveData<Drawable> _warningImage = new MutableLiveData<Drawable>(null);
    public MutableLiveData<Drawable> getWarningImage() {
        return _warningImage;
    }
    public void setWarningImage() {
        _warningImage.setValue(MainApplication.getInstance().getDrawable(R.drawable.ic_warning));
    }
    public void resetWarningImage() {
        _warningImage.setValue(null);
    }

    private final MutableLiveData<Integer> _radioLevelImage = new MutableLiveData<>(null);

    public int getAmountVisibility() {
        return MainApplication.getInstance().getBusinessType() != BusinessType.BALANCE
                ? View.VISIBLE
                : View.INVISIBLE;
    }

    private final MutableLiveData<Integer> _radioImageResource = new MutableLiveData<>();
    public MutableLiveData<Integer> getRadioImageResource() {
        return _radioImageResource;
    }
    public void setRadioImageResource(int radioImageResource) {
        _radioImageResource.setValue(radioImageResource);
    }
}
