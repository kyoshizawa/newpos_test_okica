package jp.mcapps.android.multi_payment_terminal.encoding.pos.gs1_128;

public class CheckDigitGenerator {

    final String code;

    public CheckDigitGenerator(String code) {
        this.code = code;
    }

    public int generate() {
        return calculateDigit(code);
    }

    private static int calculateDigit(String code) {

        int a = 0;
        int b = 0;

        // 偶数番目をsum（最右端が1）x3
        for (int i=code.length()-2; 0 <= i; i-=2) {
            int n = Integer.parseInt(String.valueOf(code.charAt(i)));
            a += n;
        }
        a *= 3;

        // 奇数番目をsum（最右端が1）
        for (int i=code.length()-3; 0 <= i; i-=2) {
            int n = Integer.parseInt(String.valueOf(code.charAt(i)));
            b += n;
        }

        // チェックディジットを求める
        int c = (a + b) % 10;
        if (c == 0) {
            return 0;
        }
        return 10 - c;
    }
}
