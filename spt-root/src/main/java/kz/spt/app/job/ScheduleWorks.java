package kz.spt.app.job;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.spt.app.repository.PosTerminalRepository;
import kz.spt.lib.extension.PluginRegister;
import kz.spt.lib.model.CarState;
import kz.spt.lib.model.PosTerminal;
import kz.spt.lib.model.dto.parkomat.ParkomatCommandDTO;
import kz.spt.lib.service.CarStateService;
import kz.spt.lib.service.PaymentService;
import kz.spt.lib.service.PluginService;
import kz.spt.lib.utils.StaticValues;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
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

    @Value("${shiftClosing.terminal:false}")
    Boolean terminalShiftClosing;

    @Value("${shiftClosing.parkomat:false}")
    Boolean parkomatShiftClosing;

    private PosTerminalRepository posTerminalRepository;

    private PaymentService paymentService;

    public ScheduleWorks(PosTerminalRepository posTerminalRepository, PaymentService paymentService) {
        this.posTerminalRepository = posTerminalRepository;
        this.paymentService = paymentService;
    }

    @Scheduled(cron = "${cron.scheduler.expr_reset}")
    public void resetKassa() {
        List<PosTerminal> posTerminalList = posTerminalRepository.findPosTerminalsByReconsilatedIsTrue();
        for (PosTerminal posTerminal : posTerminalList) {
            log.info("posTerminal id : " + posTerminal.getId());
            posTerminal.setReconsilated(false);
        }
        posTerminalRepository.saveAll(posTerminalList);
    }

    @Scheduled(cron = "${cron.scheduler.expr_parkomat}")
    public void webKassaCloseSchedule() throws Exception {
        if (parkomatShiftClosing) {
            List<PosTerminal> posTerminals =
                    posTerminalRepository.findPosTerminalsByReconsilatedIsFalseAndType(PosTerminal.terminalType.PARKOMAT);
            for (PosTerminal pt : posTerminals) {
                ObjectMapper mapper = new ObjectMapper();
                ParkomatCommandDTO parkomatCommandDto = new ParkomatCommandDTO();
                parkomatCommandDto.setParkomat(pt.getIp());
                parkomatCommandDto.setCommand("zReport");
                ObjectNode result = (ObjectNode) paymentService.billingInteractions(parkomatCommandDto);
                if (result != null && result.has("result")) {
                    JsonNode responseJson = mapper.readTree(result.get("result").textValue());
                    if (responseJson.has("Data")) {
                        pt.setReconsilated(true);
                        posTerminalRepository.save(pt);
                        log.info("[WebKassa] " + pt.getIp() + " parkomat shift closing succeed");
                    } else if (responseJson.has("Errors")) {
                        log.error("[WebKassa] " + pt.getIp() + ' ' + responseJson.get("Errors"));
                    }
                }
            }
        }
    }

    @Scheduled(cron = "${cron.scheduler.expr_terminal}")
    public void terminalCloseSchedule() {
        if (terminalShiftClosing) {
            List<PosTerminal> posTerminalList =
                    posTerminalRepository.findPosTerminalsByReconsilatedIsFalseAndType(PosTerminal.terminalType.TERMINAL);
            for (PosTerminal pt : posTerminalList) {
                try {
                    RestTemplate restTemplate = new RestTemplate();
                    String url = "http://" + pt.getIp() + ":8080/apibank/?key=" + pt.getApikey()
                            + "&message=reconciliation&bankSlot=1";
                    ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
                    if (response.getStatusCode().is2xxSuccessful()) {
                        Thread.sleep(15000);
                        checkTerminalsReconciliationStatus(pt);
                    } else {
                        log.error("[Terminal] " + pt.getIp() + " reconciliation request failed, response status code: "
                                + response.getStatusCode());
                    }
                } catch (Exception e) {
                    log.error("[Terminal] " + " reconciliation request failed: " + e.getMessage());
                }
            }
        }
    }

    public void checkTerminalsReconciliationStatus(PosTerminal posTerminal) {
        try {
            String url = "http://" + posTerminal.getIp() + ":8080/dump/bank/batches?key=" + posTerminal.getApikey();
            String authStr = "1:1";
            String base64Creds = Base64.getEncoder().encodeToString(authStr.getBytes());
            HttpHeaders headers = new HttpHeaders();
            headers.add("Authorization", "Basic " + base64Creds);
            HttpEntity<String> request = new HttpEntity<>(headers);
            ResponseEntity<String> response = new RestTemplate().exchange(url, HttpMethod.GET, request, String.class);
            JSONObject obj = new JSONObject(response.getBody());
            JSONArray arr = obj.getJSONArray("batches");
            String status = arr.getJSONObject(0).getString("status");
            if (status.equals("closed")) {
                log.info("[Terminal] " + posTerminal.getIp() + " reconciliation succeed, status: " + status);
                posTerminal.setReconsilated(true);
                posTerminalRepository.save(posTerminal);
            } else {
                log.warn("[Terminal] status is: {} ", status);
            }
        } catch (Exception e) {
            log.error("[Terminal] get status request failed.");
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
