package kz.spt.billingplugin.model.dto.rekassa;

import java.util.Calendar;

public class Time{
    public int hour;
    public int minute;
    public int second;

    public Time() {
        Calendar calendar = Calendar.getInstance();
        this.hour = calendar.get(Calendar.HOUR_OF_DAY);
        this.minute = calendar.get(Calendar.MINUTE);
        this.second = calendar.get(Calendar.SECOND);
    }
}
