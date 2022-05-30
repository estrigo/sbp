package kz.spt.app.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.spt.lib.extension.PluginRegister;
import kz.spt.lib.model.CarState;
import kz.spt.lib.service.CarStateService;
import kz.spt.lib.service.PluginService;
import kz.spt.lib.utils.StaticValues;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Iterator;

@Log
@Component
public class ScheduleWorks {

    @Autowired
    private CarStateService carStateService;

    @Autowired
    private PluginService pluginService;

    @Value("${parking.remove.all.debts:false}")
    Boolean parkingRemoveAllDebts;

    @Scheduled(cron = "0 0 3 * * ?")
    public void nightSchedule() {
        if(parkingRemoveAllDebts){ // Если включено удаление долга и платных парковок
            log.info("Remove debt enabled");
            Iterable<CarState> notLeftList = carStateService.getAllNotLeft();
            Iterator<CarState> iterator = notLeftList.iterator();
            while (iterator.hasNext()){
                CarState carState = iterator.next();
                if(carState.getPaid()){
                    log.info("Removing debt for car = " + carState.getCarNumber());
                    try {
                        carStateService.removeDebt(carState.getCarNumber(), false);
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
            PluginRegister billingPluginRegister = pluginService.getPluginRegister(StaticValues.billingPlugin);
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode jsonNode = objectMapper.createObjectNode();
            try {
                jsonNode.put("command", "deleteAllDebts");
                billingPluginRegister.execute(jsonNode);
            } catch (Exception e){
                e.printStackTrace();
            }
        } else {
            log.info("Remove debt disabled");
        }

        // Автопродление и удаление не оплатившие абонементы
        PluginRegister abonementPluginRegister = pluginService.getPluginRegister(StaticValues.abonementPlugin);
        if(abonementPluginRegister != null){
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode jsonNode = objectMapper.createObjectNode();
            try {
                jsonNode.put("command", "removeNotPaid");
                abonementPluginRegister.execute(jsonNode);

            } catch (Exception e){
                e.printStackTrace();
            }
            try {
                jsonNode.put("command", "renewAbonement");
                abonementPluginRegister.execute(jsonNode);
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
