package jp.mcapps.android.multi_payment_terminal.httpserver.events;

public class Types {
    public static class SignIn {
        public String driverCode;
        public String driverName;

        @Override
        public String toString() {
            return "SignIn{" +
                    "driverCode='" + driverCode + '\'' +
                    ", driverName='" + driverName + '\'' +
                    '}';
        }
    }
}
