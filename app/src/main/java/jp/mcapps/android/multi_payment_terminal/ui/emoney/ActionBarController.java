package jp.mcapps.android.multi_payment_terminal.ui.emoney;

import android.app.Application;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import com.google.android.material.shadow.ShadowRenderer;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.SharedViewModel;

public class ActionBarController {
    static final int BLINK_ON_MILLS = 500;
    static final int BLINK_OFF_MILLS = 500;
    static final int BLINK_CYCLE_MILLS = BLINK_ON_MILLS + BLINK_OFF_MILLS;

    static public class ControlCodes {
        public static final int OFF = 0x00;
        public static final int ON = 0x01;
        public static final int BLINK = 0x02;
    }

    static public class ColorCodes {
        public static final int NONE = 0x00;
        public static final int BLUE = 0x01;
        public static final int RED = 0x02;
        public static final int GREEN = 0x03;
    }

    private final ActionBar _actionBar;
    private final SharedViewModel _sharedViewModel;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Map<Integer, Integer> colorMap = new HashMap();
    private boolean _showIndicatorArrow = false;

    ActionBarController(AppCompatActivity activity, SharedViewModel sharedViewModel) {

        if (activity == null) {
            throw new IllegalArgumentException("activity is null");
        }

        _actionBar = activity.getSupportActionBar();
        if (_actionBar == null) {
            throw new IllegalArgumentException("actionBar is null");
        }

        _sharedViewModel = sharedViewModel;

        initialize();
    };

    public ActionBarController(@NonNull Fragment fragment, SharedViewModel sharedViewModel) {
        this((AppCompatActivity) fragment.getActivity(), sharedViewModel);
    }

    public void cleanup() {
        _sharedViewModel.setIndicatorArrow(SharedViewModel.IndicatorArrows.NONE);
        _sharedViewModel.setShowMarkFelica(false);
        handler.removeCallbacksAndMessages(null);
    }

    public void setStatus(int controlCode, int colorCode, int termSec) {
        _sharedViewModel.setShowMarkFelica(true);
        handler.removeCallbacksAndMessages(null);
        Runnable runner = new RingActionRunner(controlCode, colorCode, termSec);
        handler.post(runner);
    }

    public void setShowIndicatorArrow(boolean b) {
        _showIndicatorArrow = b;
        if (!b) {
            _sharedViewModel.setIndicatorArrow(SharedViewModel.IndicatorArrows.NONE);
        }
    }

    private void initialize() {
        Application application = MainApplication.getInstance();

        colorMap.put(ColorCodes.NONE, R.color.bar_emoney_none);
        colorMap.put(ColorCodes.BLUE, R.color.bar_emoney_blue);
        colorMap.put(ColorCodes.RED, R.color.bar_emoney_red);
        colorMap.put(ColorCodes.GREEN, R.color.bar_emoney_green);
    }

    private int getColorSafely(int colorCode) {
        return colorMap.get(colorCode) != null ? colorMap.get(colorCode) : R.color.bar_emoney_none;
    }

    private void setActionBarColor(int colorCode) {
        final int colorId = getColorSafely(colorCode);
        Context appContext = MainApplication.getInstance();
        final Drawable drawable = appContext.getResources().getDrawable(colorId, appContext.getTheme());

        _actionBar.setBackgroundDrawable(drawable);
    }

    private class RingActionRunner implements Runnable {
        private final int controlCode;
        private final int colorCode;
        private final int termSec;
        private int blinkCount = 0;
        private boolean isStarted = false;
        private boolean isLightOn = false;

        RingActionRunner(int controlCode, int colorCode, int termSec) {
            this.controlCode = controlCode;
            this.colorCode = colorCode;
            this.termSec = termSec;
            if (termSec > 0 && controlCode == ControlCodes.BLINK) {
                blinkCount = (termSec * 1000) / BLINK_CYCLE_MILLS * 2;
            }
        }

        @Override
        public void run() {
            // 想定してないカラーコードの場合は何もしない
            if (colorMap.get(colorCode) == null) return;

            switch (controlCode) {
                case ControlCodes.OFF:
                    off();
                    break;
                case ControlCodes.ON:
                    on();
                    break;
                case ControlCodes.BLINK:
                    blink();
                    break;
                default:
                    break;
            }
        }

        private void on() {
            if (!isStarted) {
                setActionBarColor(colorCode);
                isStarted = true;
                if (termSec > 0) {
                    handler.postDelayed(this, termSec*1000);
                }
            } else {
                setActionBarColor(ColorCodes.NONE);
            }
        }

        private void off() {
            if (!isStarted) {
                setActionBarColor(ColorCodes.NONE);
                isStarted = true;
                if (termSec > 0) {
                    handler.postDelayed(this, termSec*1000);
                }
            } else {
                setActionBarColor(colorCode);
            }
        }

        private void blink() {
            if (termSec > 0) {
                if (blinkCount <= 0) {
                    setActionBarColor(ColorCodes.NONE);
                    _sharedViewModel.setIndicatorArrow(SharedViewModel.IndicatorArrows.NONE);
                    return;
                } else {
                    blinkCount -= 1;
                }
            }

            if (!isLightOn) {
                setActionBarColor(colorCode);
                if (_showIndicatorArrow) {
                    _sharedViewModel.setIndicatorArrow(SharedViewModel.IndicatorArrows.WHITE);
                }
                isLightOn = true;
                handler.postDelayed(this, BLINK_ON_MILLS);
            } else {
                setActionBarColor(ColorCodes.NONE);
                if (_showIndicatorArrow) {
                    _sharedViewModel.setIndicatorArrow(SharedViewModel.IndicatorArrows.BLUE);
                }
                isLightOn = false;
                handler.postDelayed(this, BLINK_OFF_MILLS);
            }
        }
    }
}
