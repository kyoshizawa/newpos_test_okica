package jp.mcapps.android.multi_payment_terminal.webapi.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.base.Strings;

import java.io.IOException;

import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import timber.log.Timber;

public class TokenAuthenticator implements okhttp3.Authenticator, okhttp3.Interceptor {

    public TokenAuthenticator(AuthenticationRepository repository) {
        this.repository = repository;
    }

    private final AuthenticationRepository repository;

    @Nullable
    @Override
    public Request authenticate(@Nullable Route route, @NonNull Response response) throws IOException {

        final int retryCount = requestedCount(response);
        if (2 < retryCount) {
            // 認証リトライは１回のみとする
            return null;
        }

        try {
            // 認証トークンを更新する
            repository.refreshToken();
        } catch (Exception ex) {
            Timber.e(ex, "Failed to refresh token");
            return null;
        }

        return createSignedRequest(response.request());
    }

    private int requestedCount(Response resp) {
        int count = 0;
        Response cur = resp.priorResponse();
        while (cur != null) {
            count++;
            cur = cur.priorResponse();
        }
        return count;
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {

        // Bearerトークンをつけたリクエストを処理に渡す
        Request req = createSignedRequest(chain.request());
        return chain.proceed(req);
    }

    private Request createSignedRequest(Request req) {

        // 現在の認証トークンを取得する
        String token = repository.getToken();

        if (Strings.isNullOrEmpty(token)) {
            // トークンがまだない？のでそのままリクエスト
            return req;
        }

        // AuthorizationヘッダーにBearerトークンを付与する
        return req.newBuilder()
                .removeHeader("Authorization") // 二重付与されちゃうのでいったん消す
                .addHeader("Authorization", "Bearer " + token)
                .build();
    }
}
