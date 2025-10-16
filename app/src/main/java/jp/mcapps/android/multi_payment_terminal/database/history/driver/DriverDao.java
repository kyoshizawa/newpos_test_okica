package jp.mcapps.android.multi_payment_terminal.database.history.driver;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface DriverDao {
    @Query("SELECT DISTINCT driver_code AS code, driver_name AS name FROM driver_history ORDER BY id DESC LIMIT 3")
    public List<Driver> getDrivers();

    @Query("DELETE FROM driver_history")
    void deleteAll();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    public void addDriverHistory(DriverData driverData);

    static class Driver {
        public String code;
        public String name;
    }
}
