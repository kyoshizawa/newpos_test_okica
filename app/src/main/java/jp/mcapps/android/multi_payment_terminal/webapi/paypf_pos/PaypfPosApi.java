package jp.mcapps.android.multi_payment_terminal.webapi.paypf_pos;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import jp.mcapps.android.multi_payment_terminal.webapi.HttpStatusException;
import jp.mcapps.android.multi_payment_terminal.webapi.paypf_pos.data.AuthTest;
import jp.mcapps.android.multi_payment_terminal.webapi.paypf_pos.data.CreateManyTransaction;
import jp.mcapps.android.multi_payment_terminal.webapi.paypf_pos.data.ListTenantProduct;
import jp.mcapps.android.multi_payment_terminal.webapi.paypf_pos.data.ListTenantProductCategory;
import jp.mcapps.android.multi_payment_terminal.webapi.paypf_pos.type.Tenant;

public interface PaypfPosApi {

    /**
     * アクセストークンを検証する
     *
     * @return
     * @throws IOException
     * @throws HttpStatusException
     */
    AuthTest.Response authTest() throws IOException, HttpStatusException;

    /**
     * 取引先コードから店舗情報を取得します
     *
     * @param serviceInstanceID サービスインスタンスID
     * @param customerCode      取引先コード
     * @return
     * @throws IOException
     * @throws HttpStatusException
     */
    Tenant getTenantByCustomerCode(@NotNull String serviceInstanceID, @NotNull String customerCode) throws IOException, HttpStatusException, PaypfStatusException;

    /**
     * 店舗取扱商品の一覧を取得する
     *
     * @param serviceInstanceID サービスインスタンスID
     * @param tenantID          店舗ID
     * @param limit             最大取得件数
     * @param offset            取得開始位置
     * @return
     * @throws IOException
     * @throws HttpStatusException
     */
    ListTenantProductCategory.ResponseData listTenantProductCategories(@NotNull String serviceInstanceID, long tenantID, int limit, int offset) throws IOException, HttpStatusException, PaypfStatusException;

    /**
     * 店舗取扱商品の一覧を取得する
     *
     * @param serviceInstanceID サービスインスタンスID
     * @param tenantID          店舗ID
     * @param limit             最大取得件数
     * @param offset            取得開始位置
     * @return
     * @throws IOException
     * @throws HttpStatusException
     */
    ListTenantProduct.ResponseData listTenantProducts(@NotNull String serviceInstanceID, long tenantID, int limit, int offset) throws IOException, HttpStatusException, PaypfStatusException;

    /**
     * 取引データおよび、取消しデータの登録を行う
     * 一度に複数のデータの登録を行えるインターフェースとなっている ※１回に最大５件とする
     *
     * @param request 取引データおよび、取消しデータの内容
     * @return
     * @throws IOException
     * @throws HttpStatusException
     */
    CreateManyTransaction.Response createTransactions(@NotNull CreateManyTransaction.Request request) throws IOException, HttpStatusException;
}
