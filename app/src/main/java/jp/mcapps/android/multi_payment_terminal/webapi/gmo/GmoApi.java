package jp.mcapps.android.multi_payment_terminal.webapi.gmo;

import java.io.IOException;
import java.util.Map;

import jp.mcapps.android.multi_payment_terminal.webapi.HttpStatusException;
import jp.mcapps.android.multi_payment_terminal.webapi.gmo.data.GetCheckOrder;
import jp.mcapps.android.multi_payment_terminal.webapi.gmo.data.GetCheckRefunds;
import jp.mcapps.android.multi_payment_terminal.webapi.gmo.data.GetOrders;
import jp.mcapps.android.multi_payment_terminal.webapi.gmo.data.PostLogin;
import jp.mcapps.android.multi_payment_terminal.webapi.gmo.data.PostOrdersForPartner;
import jp.mcapps.android.multi_payment_terminal.webapi.gmo.data.PutCancelOrders;
import jp.mcapps.android.multi_payment_terminal.webapi.gmo.data.PutCreateQRCode;
import jp.mcapps.android.multi_payment_terminal.webapi.gmo.data.PutOrders;
import jp.mcapps.android.multi_payment_terminal.webapi.gmo.data.PutRefunds;
import jp.mcapps.android.multi_payment_terminal.webapi.ifbox.data.Chip;
import jp.mcapps.android.multi_payment_terminal.webapi.ifbox.data.Ems;
import jp.mcapps.android.multi_payment_terminal.webapi.ifbox.data.Meter;
import jp.mcapps.android.multi_payment_terminal.webapi.ifbox.data.Version;

public interface GmoApi {
    PostLogin.Response postLogin(PostLogin.Request request) throws IOException, HttpStatusException;
    PutOrders.Response putOrders(Map<String, String> header, PutOrders.Request request) throws IOException, HttpStatusException;
    GetCheckOrder.Response getCheckOrder(Map<String, String> header, Map<String, String> query) throws IOException, HttpStatusException;
    PutRefunds.Response putRefunds(Map<String, String> header, PutRefunds.Request request) throws IOException, HttpStatusException;
    GetCheckRefunds.Response getCheckRefunds(Map<String, String> header, Map<String, String> queryParams) throws IOException, HttpStatusException;
    GetOrders.Response getOrders(Map<String, String> header, Map<String, String> queryParams) throws IOException, HttpStatusException;
    PostOrdersForPartner.Response postOrdersForPartner(Map<String, String> header, PostOrdersForPartner.Request request) throws IOException, HttpStatusException;
    PutCreateQRCode.Response putCreateQRCode(Map<String, String> header, PutCreateQRCode.Request request) throws IOException, HttpStatusException;
    PutCancelOrders.Response PutCancelOrders(Map<String, String> header, PutCancelOrders.Request request) throws IOException, HttpStatusException;
}
