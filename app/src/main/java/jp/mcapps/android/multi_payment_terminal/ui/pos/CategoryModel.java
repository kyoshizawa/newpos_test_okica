package jp.mcapps.android.multi_payment_terminal.ui.pos;

import androidx.room.Entity;

import jp.mcapps.android.multi_payment_terminal.database.pos.CategoryData;

@Entity
public class CategoryModel {
    public long category_id;
    public String category_name;

    public static CategoryModel newInstance(CategoryData item) {
        CategoryModel it = new CategoryModel();

        it.category_id = item.category_id;
        it.category_name = item.getDisplayName();

        return it;
    }
}
