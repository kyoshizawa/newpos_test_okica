package jp.mcapps.android.multi_payment_terminal.httpserver.events;


import io.reactivex.rxjava3.subjects.PublishSubject;
import jp.mcapps.android.multi_payment_terminal.webapi.tablet.data.ConnectionInfo;

public class EventBroker {
    public static PublishSubject<Types.SignIn> signIn = PublishSubject.create();
    public static PublishSubject<ConnectionInfo> ima820 = PublishSubject.create();
}
