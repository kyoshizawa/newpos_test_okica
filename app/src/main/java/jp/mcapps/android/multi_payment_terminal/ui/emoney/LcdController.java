package jp.mcapps.android.multi_payment_terminal.ui.emoney;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.MutableLiveData;

public class LcdController {
    private final Handler _handler = new Handler(Looper.getMainLooper());
    private final MutableLiveData<String> _lcd;

    public LcdController(MutableLiveData<String> lcd) {
        _lcd = lcd;
    }

    public void cleanup() {
        _handler.removeCallbacksAndMessages(null);
    }

    public void setStatus(String message, String termSecString) {
        _handler.removeCallbacksAndMessages(null);

//        _lcd.setValue(message_han);
        _lcd.setValue(message);
        final int termSec = Integer.parseInt(termSecString);

        if (termSec > 0) {
            _handler.postDelayed(() -> {
                _lcd.setValue("");
            }, termSec*1000);
        }
    }
}
