package jp.mcapps.android.multi_payment_terminal.data;

import android.text.TextUtils;

public class OptionService {
    public static class FuncIds {
        public static final int MAGNETIC_CARD_VALIDATION = 1;  // 有効性確認(磁気)
        public static final int PREPAID_POINT = 21;  // プリペイドポイント
    }

    public OptionService(String domain, String key, Func[] funcs) {
        _domain = domain;
        _serviceKey = key;
        _funcs = funcs;
    }

    private final String _domain;
    public String getDomain() {
        return _domain;
    }

    private final String _serviceKey;
    public String getServiceKey() {
        return _serviceKey;
    }

    private final Func[] _funcs;
    public Func[] getFuncs() {
        return _funcs;
    }

    public Func getFunc(int index) {
        if (_funcs.length <= index) {
            return null;
        }

        return _funcs[index];
    }

    public boolean isAvailable() {
        return _funcs != null && _funcs.length > 0
            && !TextUtils.isEmpty(_serviceKey)
            && !TextUtils.isEmpty(_domain);
    }

    // 指定したfuncIDのインデックス 存在しない場合は-1を返す
    public int indexOfFunc(int funcId) {
        if (_funcs == null) return -1;

        for (int i = 0; i < _funcs.length; i++) {
            if (_funcs[i].getFuncID() == funcId) {
                return i;
            }
        }

        return -1;
    }

    public static class Func {
        public Func(int funcID, String displayName) {
            _funcID = funcID;
            _displayName = displayName;
        }

        private final int _funcID;
        public int getFuncID() {
            return _funcID;
        }

        private final String _displayName;
        public String getDisplayName() {
            return _displayName;
        }
    }
}
