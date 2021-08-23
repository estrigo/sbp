package kz.spt.tariffplugin.service.impl;

import kz.spt.tariffplugin.service.TariffService;
import org.springframework.stereotype.Service;

import java.time.Period;
import java.util.Calendar;
import java.util.Date;

@Service
public class TariffServiceImpl implements TariffService {

    @Override
    public int calculatePayment(Long parkingId, Date inDate, Date outDate) {
        Calendar inCalendar = Calendar.getInstance();
        inCalendar.setTime(inDate);

        Calendar outCalendar = Calendar.getInstance();
        outCalendar.setTime(outDate);

        if(inCalendar.after(outCalendar)){
            return 0;
        }
        int hours = 0;
        while (inCalendar.before(outCalendar)){
            hours++;
            inCalendar.add(Calendar.HOUR, 1);
        }
        return hours*100;
    }
}
