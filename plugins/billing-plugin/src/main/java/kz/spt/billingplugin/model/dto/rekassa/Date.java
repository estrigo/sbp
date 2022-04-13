package kz.spt.billingplugin.model.dto.rekassa;

import java.util.Calendar;

public class Date{
    public int year;
    public int month;
    public int day;

    public Date() {
        Calendar calendar = Calendar.getInstance();
        this.day = calendar.get(Calendar.DAY_OF_MONTH);
        this.month = calendar.get(Calendar.MONTH);
        this.year = calendar.get(Calendar.YEAR);
    }
}
