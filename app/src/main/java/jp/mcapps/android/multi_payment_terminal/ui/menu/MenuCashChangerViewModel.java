package jp.mcapps.android.multi_payment_terminal.ui.menu;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.Locale;
import java.util.Map;

import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;

public class MenuCashChangerViewModel extends ViewModel {
    private final MutableLiveData<String> _chStatusStr = new MutableLiveData<>(null);
    private final MutableLiveData<Integer> _chStatusInt = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> _enableReConnect = new MutableLiveData<>(null);

    private final MutableLiveData<String> _value10000 = new MutableLiveData<>(null);
    private final MutableLiveData<String> _value5000 = new MutableLiveData<>(null);
    private final MutableLiveData<String> _value2000 = new MutableLiveData<>(null);
    private final MutableLiveData<String> _value1000 = new MutableLiveData<>(null);
    private final MutableLiveData<String> _value500 = new MutableLiveData<>(null);
    private final MutableLiveData<String> _value100 = new MutableLiveData<>(null);
    private final MutableLiveData<String> _value50 = new MutableLiveData<>(null);
    private final MutableLiveData<String> _value10 = new MutableLiveData<>(null);
    private final MutableLiveData<String> _value5 = new MutableLiveData<>(null);
    private final MutableLiveData<String> _value1 = new MutableLiveData<>(null);

    private final MutableLiveData<String> _value10000Total = new MutableLiveData<>(null);
    private final MutableLiveData<String> _value5000Total = new MutableLiveData<>(null);
    private final MutableLiveData<String> _value2000Total = new MutableLiveData<>(null);
    private final MutableLiveData<String> _value1000Total = new MutableLiveData<>(null);
    private final MutableLiveData<String> _value500Total = new MutableLiveData<>(null);
    private final MutableLiveData<String> _value100Total = new MutableLiveData<>(null);
    private final MutableLiveData<String> _value50Total = new MutableLiveData<>(null);
    private final MutableLiveData<String> _value10Total = new MutableLiveData<>(null);
    private final MutableLiveData<String> _value5Total = new MutableLiveData<>(null);
    private final MutableLiveData<String> _value1Total = new MutableLiveData<>(null);
    private final MutableLiveData<String> _valueTotal = new MutableLiveData<>(null);

    public MenuCashChangerViewModel() {}

    public static class AmountValue {
        public int value10000;
        public int value5000;
        public int value2000;
        public int value1000;
        public int value500;
        public int value100;
        public int value50;
        public int value10;
        public int value5;
        public int value1;
        public int value10000Total;
        public int value5000Total;
        public int value2000Total;
        public int value1000Total;
        public int value500Total;
        public int value100Total;
        public int value50Total;
        public int value10Total;
        public int value5Total;
        public int value1Total;
        public int valueTotal;
        public String cashDate; // 日時
    }

    public static AmountValue _amount = new AmountValue();

    public static AmountValue getAmountValue(){
        return _amount;
    }

    public void setAmountValue(@NonNull Map<String, Integer> data, String date) {
        _amount.cashDate = date;

        _amount.value10000 = data.get("jpy10000");
        _amount.value5000 = data.get("jpy5000");
        _amount.value2000 = data.get("jpy2000");
        _amount.value1000 = data.get("jpy1000");
        _amount.value500 = data.get("jpy500");
        _amount.value100 = data.get("jpy100");
        _amount.value50 = data.get("jpy50");
        _amount.value10 = data.get("jpy10");
        _amount.value5 = data.get("jpy5");
        _amount.value1 = data.get("jpy1");
        _amount.value10000Total = _amount.value10000 * 10000;
        _amount.value5000Total = _amount.value5000 * 5000;
        _amount.value2000Total = _amount.value2000 * 2000;
        _amount.value1000Total = _amount.value1000 * 1000;
        _amount.value500Total = _amount.value500 * 500;
        _amount.value100Total = _amount.value100 * 100;
        _amount.value50Total = _amount.value50 * 50;
        _amount.value10Total = _amount.value10 * 10;
        _amount.value5Total = _amount.value5 * 5;
        _amount.value1Total = _amount.value1 * 1;
        _amount.valueTotal = _amount.value10000Total + _amount.value5000Total + _amount.value2000Total
                + _amount.value1000Total + _amount.value500Total + _amount.value100Total + _amount.value50Total
                + _amount.value10Total + _amount.value5Total + _amount.value1Total;

        _value10000.setValue(String.format(Locale.JAPANESE, "%,d", _amount.value10000));
        _value5000.setValue(String.format(Locale.JAPANESE, "%,d", _amount.value5000));
        _value2000.setValue(String.format(Locale.JAPANESE, "%,d", _amount.value2000));
        _value1000.setValue(String.format(Locale.JAPANESE, "%,d", _amount.value1000));
        _value500.setValue(String.format(Locale.JAPANESE, "%,d", _amount.value500));
        _value100.setValue(String.format(Locale.JAPANESE, "%,d", _amount.value100));
        _value50.setValue(String.format(Locale.JAPANESE, "%,d", _amount.value50));
        _value10.setValue(String.format(Locale.JAPANESE, "%,d", _amount.value10));
        _value5.setValue(String.format(Locale.JAPANESE, "%,d", _amount.value5));
        _value1.setValue(String.format(Locale.JAPANESE, "%,d", _amount.value1));

        _value10000Total.setValue(String.format(Locale.JAPANESE, "%,d", _amount.value10000Total));
        _value5000Total.setValue(String.format(Locale.JAPANESE, "%,d", _amount.value5000Total));
        _value2000Total.setValue(String.format(Locale.JAPANESE, "%,d", _amount.value2000Total));
        _value1000Total.setValue(String.format(Locale.JAPANESE, "%,d", _amount.value1000Total));
        _value500Total.setValue(String.format(Locale.JAPANESE, "%,d", _amount.value500Total));
        _value100Total.setValue(String.format(Locale.JAPANESE, "%,d", _amount.value100Total));
        _value50Total.setValue(String.format(Locale.JAPANESE, "%,d", _amount.value50Total));
        _value10Total.setValue(String.format(Locale.JAPANESE, "%,d", _amount.value10Total));
        _value5Total.setValue(String.format(Locale.JAPANESE, "%,d", _amount.value5Total));
        _value1Total.setValue(String.format(Locale.JAPANESE, "%,d", _amount.value1Total));

        _valueTotal.setValue(String.format(Locale.JAPANESE, "%,d", _amount.valueTotal));
    }
    
    public void setAmountValue() {
        _value10000.setValue("－");
        _value5000.setValue("－");
        _value2000.setValue("－");
        _value1000.setValue("－");
        _value500.setValue("－");
        _value100.setValue("－");
        _value50.setValue("－");
        _value10.setValue("－");
        _value5.setValue("－");
        _value1.setValue("－");

        _value10000Total.setValue("－");
        _value5000Total.setValue("－");
        _value2000Total.setValue("－");
        _value1000Total.setValue("－");
        _value500Total.setValue("－");
        _value100Total.setValue("－");
        _value50Total.setValue("－");
        _value10Total.setValue("－");
        _value5Total.setValue("－");
        _value1Total.setValue("－");

        _valueTotal.setValue("－");
    }

    public void setChStatus(int stat)
    {
        // stat　0；取得中　1：接続正常　2：接続異常
        _chStatusInt.setValue(stat);
        if (stat == 0) {
            // 取得中
            _chStatusStr.setValue(MainApplication.getInstance().getString(R.string.status_cashchanger_getting));
        } else if (stat == 1) {
            // 接続正常
            _chStatusStr.setValue(MainApplication.getInstance().getString(R.string.status_cashchanger_connect_ok));
        } else {
            // 接続異常
            _chStatusStr.setValue(MainApplication.getInstance().getString(R.string.status_cashchanger_connect_ng));
        }
    }

    public void setEnableReConnect(boolean flg) { _enableReConnect.setValue(flg); }

    public MutableLiveData<String> getChStatusStr() { return _chStatusStr; }

    public MutableLiveData<Integer> getChStatusInt() { return _chStatusInt; }

    public MutableLiveData<Boolean> getEnableReConnect() { return _enableReConnect; }

    public MutableLiveData<String> getValue10000() { return _value10000; }

    public MutableLiveData<String> getValue5000() { return _value5000; }

    public MutableLiveData<String> getValue2000() { return _value2000; }

    public MutableLiveData<String> getValue1000() { return _value1000; }

    public MutableLiveData<String> getValue500() { return _value500; }

    public MutableLiveData<String> getValue100() { return _value100; }

    public MutableLiveData<String> getValue50() { return _value50; }

    public MutableLiveData<String> getValue10() { return _value10; }

    public MutableLiveData<String> getValue5() { return _value5; }

    public MutableLiveData<String> getValue1() { return _value1; }

    public MutableLiveData<String> getValue10000Total() { return _value10000Total; }

    public MutableLiveData<String> getValue5000Total() { return _value5000Total; }

    public MutableLiveData<String> getValue2000Total() { return _value2000Total; }

    public MutableLiveData<String> getValue1000Total() { return _value1000Total; }

    public MutableLiveData<String> getValue500Total() { return _value500Total; }

    public MutableLiveData<String> getValue100Total() { return _value100Total; }

    public MutableLiveData<String> getValue50Total() { return _value50Total; }

    public MutableLiveData<String> getValue10Total() { return _value10Total; }

    public MutableLiveData<String> getValue5Total() { return _value5Total; }

    public MutableLiveData<String> getValue1Total() { return _value1Total; }

    public MutableLiveData<String> getValueTotal() { return _valueTotal; }
}
