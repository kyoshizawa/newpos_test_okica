package jp.mcapps.android.multi_payment_terminal.model.device_network_manager;

public class Constants {
    public static final int MY_SERVICE_PORT = 25395;
    public static final int TABLET_SERVICE_PORT = 25395;

    public enum Devices {
        Self("PT-750", ServiceTypes.HTTP_TCP),
        IFBox("IM-A820", ServiceTypes.HTTP_TCP),
        Tablet("TAPP-REST", ServiceTypes.HTTP_TCP),
        ; // コンストラクタやメソッドを定義する場合は列挙子の最後にセミコロンが必要

        private final String _serviceName;

        public String getServiceName() {
            return _serviceName;
        }

        private final ServiceTypes _serviceType;
        public ServiceTypes getServiceType() {
            return _serviceType;
        }

        Devices(String serviceName, ServiceTypes serviceType) {
            _serviceName = serviceName;
            _serviceType = serviceType;
        }

        // 自分以外のサービス名のいずれかに一致するかどうか
        public static boolean findServiceName(String serviceName) {
            for (Devices device : Devices.values()) {
                if (device != Self && device.getServiceName().equals(serviceName)) return true;
            }

            return false;
        }
    }

    public enum ServiceTypes {
        HTTP_TCP("_http._tcp."),
        ; // コンストラクタやメソッドを定義する場合は列挙子の最後にセミコロンが必要

        private final String _serviceType;
        public String get() {
            return _serviceType;
        }

        ServiceTypes(String serviceType) {
            _serviceType = serviceType;
        }
    }
}