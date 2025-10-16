package jp.mcapps.android.multi_payment_terminal.webapi.utils;

import java.io.IOException;

import jp.mcapps.android.multi_payment_terminal.webapi.HttpStatusException;

public interface AuthenticationRepository {

    /**
     * 現在の認証トークンを返します
     *
     * @return リクエストに使用する認証トークン
     */
    String getToken();

    /**
     * 認証トークンを更新し、新しいトークンを返します
     *
     * @return 更新された認証トークン
     * @throws IOException
     */
    String refreshToken() throws IOException, HttpStatusException;
}
