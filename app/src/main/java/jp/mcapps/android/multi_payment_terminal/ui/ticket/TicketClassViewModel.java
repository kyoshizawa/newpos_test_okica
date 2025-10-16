package jp.mcapps.android.multi_payment_terminal.ui.ticket;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jp.mcapps.android.multi_payment_terminal.database.DBManager;
import jp.mcapps.android.multi_payment_terminal.database.pos.ProductDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.ProductData;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketClassDao;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketClassData;
import jp.mcapps.android.multi_payment_terminal.ui.pos.ProductCategorySelectModel;
import timber.log.Timber;

public class TicketClassViewModel extends ViewModel {

    public TicketClassViewModel() {
        super();
    }

    private final TicketClassDao _ticketClassDao = DBManager.getTicketClassDao();

    List<TicketClassData> getTicketClassData() {
        return _ticketClassDao.getAllTicketClasses();
    }

    public Observable<List<TicketClassData>> initFetchData() {

        return Observable.fromCallable(() -> {
                    return getTicketClassData();
                })
                .subscribeOn(Schedulers.single())
                .observeOn(AndroidSchedulers.mainThread());
     }
}
