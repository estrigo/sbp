package kz.spt.app.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.spt.app.repository.PosTerminalRepository;
import kz.spt.lib.extension.PluginRegister;
import kz.spt.lib.model.CarState;
import kz.spt.lib.model.PosTerminal;
import kz.spt.lib.service.CarStateService;
import kz.spt.lib.service.PluginService;
import kz.spt.lib.utils.StaticValues;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Iterator;
import java.util.List;

@Slf4j
@Component
public class ScheduleWorks {

    @Autowired
    private CarStateService carStateService;

    @Autowired
    private PluginService pluginService;

    @Value("${parking.remove.all.debts:false}")
    Boolean parkingRemoveAllDebts;

    @Value("${posterminal.reconsilation.enable:false}")
    Boolean terminalReconsilation;

    private PosTerminalRepository posTerminalRepository;

    public ScheduleWorks(PosTerminalRepository posTerminalRepository) {
        this.posTerminalRepository = posTerminalRepository;
    }

    @Scheduled(cron = "0 50 1 * * ?")
    public void terminalNightSchedule2() {
        List<PosTerminal> posTerminalList = posTerminalRepository.findPosTerminalsByReconsilatedIsFalse();
        for (PosTerminal posTerminal : posTerminalList) {
            posTerminal.setReconsilated(false);
        }
        posTerminalRepository.saveAll(posTerminalList);
    }

    @Scheduled(cron = "0 */15 2 * * ?")
    public void terminalNightSchedule() {
        log.info("Terminal reconsilation enabled");
        if (terminalReconsilation) {
            try {
                List<PosTerminal> posTerminalList = posTerminalRepository.findPosTerminalsByReconsilatedIsFalse();
                for (PosTerminal pt : posTerminalList) {
                    RestTemplate restTemplate = new RestTemplate();
                    String url = "http://" + pt.getIp() + ":8080/apibank/?key=" + pt.getApikey()
                            + "&message=reconciliation&bankSlot=1";
                    ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
                    if (response.getStatusCode().is2xxSuccessful()) {
                        pt.setReconsilated(true);
                        posTerminalRepository.save(pt);
                    } else {
                        log.info("Terminal reconsilation request failed, response status: " + response.getStatusCode());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void nightSchedule() {
        if (parkingRemoveAllDebts) { // Если включено удаление долга и платных парковок
            log.info("Remove debt enabled");
            Iterable<CarState> notLeftList = carStateService.getAllNotLeft();
            Iterator<CarState> iterator = notLeftList.iterator();
            while (iterator.hasNext()) {
                CarState carState = iterator.next();
                if (carState.getPaid()) {
                    log.info("Removing debt for car = " + carState.getCarNumber());
                    try {
                        carStateService.removeDebt(carState.getCarNumber(), true);
                    } catch (Exception e) {
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
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            log.info("Remove debt disabled");
        }

        // Автопродление и удаление не оплатившие абонементы
        PluginRegister abonementPluginRegister = pluginService.getPluginRegister(StaticValues.abonementPlugin);
        if (abonementPluginRegister != null) {
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode jsonNode = objectMapper.createObjectNode();
            try {
                jsonNode.put("command", "removeNotPaid");
                abonementPluginRegister.execute(jsonNode);

            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                jsonNode.put("command", "renewAbonement");
                abonementPluginRegister.execute(jsonNode);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
