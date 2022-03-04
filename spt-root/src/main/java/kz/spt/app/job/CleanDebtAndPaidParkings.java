package kz.spt.app.job;

import kz.spt.lib.model.CarState;
import kz.spt.lib.service.CarStateService;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Iterator;

@Log
@Component
public class CleanDebtAndPaidParkings {

    @Autowired
    private CarStateService carStateService;

    @Value("${parking.remove.all.debts:false}")
    Boolean parkingRemoveAllDebts;

    @Scheduled(cron = "0 0 3 * * ?")
    public void scheduleDebtClean() {

        if(parkingRemoveAllDebts){
            log.info("Remove debt enabled");
            Iterable<CarState> notLeftList = carStateService.getAllNotLeft();
            Iterator<CarState> iterator = notLeftList.iterator();
            while (iterator.hasNext()){
                CarState carState = iterator.next();
                if(carState.getPaid()){
                    log.info("Removing debt for car = " + carState.getCarNumber());
                    try {
                        carStateService.removeDebt(carState.getCarNumber());
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        } else {
            log.info("Remove debt disabled");
        }
    }
}
