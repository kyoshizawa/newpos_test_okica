package jp.mcapps.android.multi_payment_terminal.ui.ticket;

import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jp.mcapps.android.multi_payment_terminal.database.DBManager;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketEmbarkDao;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketEmbarkData;

public class TicketDisembarkViewModel extends ViewModel {
    public TicketDisembarkViewModel() {
        super();
    }

    private final TicketEmbarkDao _ticketEmbarkDao = DBManager.getTicketEmbarkDao();

    List<TicketEmbarkData> getTicketDisembarkData(long ticket_class_id, List<String> route_ids) {
        List<TicketEmbarkData> data_all = new ArrayList<>();

        for (String route_id: route_ids) {
            List<TicketEmbarkData> data = _ticketEmbarkDao.getAllTicketEmbarkByRouteId(ticket_class_id, "disembark", route_id);
            if (data != null) {
                for (TicketEmbarkData dst: data) {
                    boolean bFind = false;
                    for (TicketEmbarkData src: data_all) {
                        if (src.stop_id.equals(dst.stop_id)) {
                            bFind = true;
                            break;
                        }
                    }
                    if (!bFind) {
                        data_all.add(dst);
                    }
                }
            }
        }
        return data_all;
    }

    public Observable<List<TicketEmbarkData>> initFetchData(long ticket_class_id, List<String> route_ids) {

        return Observable.fromCallable(() -> {
                    return getTicketDisembarkData(ticket_class_id, route_ids);
                })
                .subscribeOn(Schedulers.single())
                .observeOn(AndroidSchedulers.mainThread());
    }
    public Observable<List<String>> getRoutes(long ticket_class_id, String stop_id) {
        return Observable.fromCallable(() -> {
                    return _ticketEmbarkDao.getAllRouteEmbarkByStopId(ticket_class_id, "embark", stop_id);
                })
                .subscribeOn(Schedulers.single())
                .observeOn(AndroidSchedulers.mainThread());
    }
}
