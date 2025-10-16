package jp.mcapps.android.multi_payment_terminal.database.pos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public abstract class TaxCalcDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insertTaxCalcData(TaxCalcData taxCalcData);

    @Query("SELECT * FROM pos_tax_calc")
    public abstract TaxCalcData getData();

    @Query("SELECT count(*) FROM pos_tax_calc")
    public abstract Integer getCount();


    @Query("delete from pos_tax_calc")
    public abstract void deleteTaxCalc();

}
