package jp.mcapps.android.multi_payment_terminal.ui.ticket;

import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jp.mcapps.android.multi_payment_terminal.database.DBManager;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketClassDao;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketClassData;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketEmbarkDao;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketEmbarkData;

public class TicketEmbarkViewModel extends ViewModel {
    public TicketEmbarkViewModel() {
        super();
    }

    private final TicketEmbarkDao _ticketEmbarkDao = DBManager.getTicketEmbarkDao();

    List<TicketEmbarkData> getTicketEmbarkData(long ticket_class_id) {
        return _ticketEmbarkDao.getAllTicketEmbarkByTicketId(ticket_class_id, "embark");
    }

    public Observable<List<TicketEmbarkData>> initFetchData(long ticket_class_id) {

        return Observable.fromCallable(() -> {
                    return getTicketEmbarkData(ticket_class_id);
                })
                .subscribeOn(Schedulers.single())
                .observeOn(AndroidSchedulers.mainThread());
    }
}
