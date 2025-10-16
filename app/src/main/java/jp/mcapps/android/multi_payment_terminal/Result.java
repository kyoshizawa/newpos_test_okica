package jp.mcapps.android.multi_payment_terminal;

public class Result<T, E> {

    public final T ok;
    public final E err;

    private Result(T ok, E err) {
        this.ok = ok;
        this.err = err;
    }

    public boolean isOk() {
        return ok != null;
    }

    public boolean isErr() {
        return err != null;
    }

    public static <T, E> Result<T, E> ok(T ok) {
        return new Result<>(ok, null);
    }

    public static <T, E> Result<T, E> err(E err) {
        return new Result<>(null, err);
    }
}
