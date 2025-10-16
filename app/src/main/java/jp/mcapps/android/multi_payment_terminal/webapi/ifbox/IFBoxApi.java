package jp.mcapps.android.multi_payment_terminal.webapi.ifbox;

import java.io.IOException;

import javax.net.SocketFactory;

import jp.mcapps.android.multi_payment_terminal.webapi.ifbox.data.Chip;
import jp.mcapps.android.multi_payment_terminal.webapi.ifbox.data.Ems;
import jp.mcapps.android.multi_payment_terminal.webapi.ifbox.data.Meter;
import jp.mcapps.android.multi_payment_terminal.webapi.ifbox.data.Version;
import jp.mcapps.android.multi_payment_terminal.webapi.ifbox.data.Wifi;

public interface IFBoxApi {
    void setBaseUrl(String address);
    Version.Response getVersion() throws IOException;
    String getHeep() throws IOException;
    Chip.Response getChip() throws IOException;
    Meter.Response getMeter() throws IOException;
    Meter.ResponseStatusFutabaD getMeterFutabaD() throws IOException;
    Ems.Response getEms() throws IOException;
    void postWifi(Wifi.Request request, SocketFactory factory) throws IOException;
    void postUpdate(String filePath) throws IOException;
}
