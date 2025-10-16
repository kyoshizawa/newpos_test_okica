package jp.mcapps.android.multi_payment_terminal.model;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.disposables.Disposable;

public class Disposer {
    private List<Disposable> _disposables = new ArrayList<>();

    public void addTo(Disposable d) {
        _disposables.add(d);
    }

    public void dispose() {
        for (Disposable d : _disposables) {
            if (!d.isDisposed()) {
                d.dispose();
            }
        }

        _disposables.clear();
    }
}
