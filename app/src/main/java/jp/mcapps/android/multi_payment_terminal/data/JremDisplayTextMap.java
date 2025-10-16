package jp.mcapps.android.multi_payment_terminal.data;

import java.util.HashMap;

public class JremDisplayTextMap {
    private static final HashMap<String, String> _textMap = new HashMap<>();
    public static final String DISPLAYED_HEAD = "@HEAD@";
    public static final String DISPLAYED_BALANCE = "@BALANCE@";
    public static final String DISPLAYED_LCD = "@LCD@";

    public static String get(String messageId) {
        String value = _textMap.get(messageId);
        return value != null ? value : "";
    }

    static {
        // iD
        // PIN入力画面に表示する
        _textMap.put("803", "暗証番号の入力を行ってください。");
        _textMap.put("804", "暗証番号が誤っています。\n再度、入力を行ってください。");
        _textMap.put("805", "暗証番号が誤っています。\n再度、入力を行ってください。残り1回");
        _textMap.put("806", "暗証番号の入力が完了しました。");

        _textMap.put("810", "");

        // Edy
        // lcd部に表示する
        // lcdに表示するメッセージには句読点がないので合わせる
        _textMap.put("811", "カードが違います\n最初のカードを\nタッチしてください");
        _textMap.put("812", "カードの読取が不十分です\nもう一度タッチしてください");
        _textMap.put("813", "カードをタッチしてください");
    }
}
