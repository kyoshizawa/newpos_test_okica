package jp.mcapps.android.multi_payment_terminal;

public class ScreenData {
    private String _name;
    private ScreenData() {_name = "";}

    public static ScreenData getInstance() {
        return MainApplication.getInstance().getScreenData();
    }

    public String getScreenName() {
        return _name;
    }
    public void setScreenName(String name) {
        _name = name;
    }
}
