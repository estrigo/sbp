package kz.spt.rateplugin.extension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.spt.lib.extension.PluginRegister;
import kz.spt.lib.model.CarModel;
import kz.spt.lib.model.Parking;

import kz.spt.lib.service.CarModelService;
import kz.spt.rateplugin.RatePlugin;
import kz.spt.rateplugin.model.ParkingRate;
import kz.spt.rateplugin.service.RateService;
import lombok.extern.java.Log;
import org.pf4j.Extension;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Extension
public class CommandExecutor implements PluginRegister {

    private RateService rateService;


    @Override
    public JsonNode execute(JsonNode command) throws Exception {

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode node = objectMapper.createObjectNode();

        if(command!=null){
            String commandName = command.has("command") ? command.get("command").textValue() : "";
            if("getPrepaidValue".equals(commandName)){
                node.put("prepaidValue",getRateService().getByParkingId(command.get("parkingId").longValue()).getPrepaidValue());
            }else if("getRateByParking".equals(commandName)){
                if(command.get("parkingId")!=null) {
                    Long parkingId = command.get("parkingId").longValue();
                    ParkingRate parkingRate = getRateService().getByParkingId(parkingId);
                    node.put("rateId", parkingRate.getId());
                    node.put("rateName", parkingRate.getName());
                }
            }else{
                node.put("rateResult", BigDecimal.ZERO);
                node.put("rateFreeMinutes", 0);

                if(command.get("parkingId")!=null){
                    Long parkingId = command.get("parkingId").longValue();

                    if(command.get("inDate")!=null && command.get("outDate")!=null){
                        Date inDate = format.parse(command.get("inDate").textValue());
                        Date outDate = format.parse(command.get("outDate").textValue());
                        Boolean cashlessPayment = command.has("cashlessPayment") ?  command.get("cashlessPayment").booleanValue() : false;
                        Boolean isCheck = command.has("isCheck") ? command.get("isCheck").booleanValue() : false;
                        String paymentsJson = command.has("paymentsJson") && command.get("paymentsJson")!=null ? command.get("paymentsJson").textValue() : null;

                        String carType = "";
                        if (command.has("carType")) { //for dimensions tariffs
                            carType = command.get("carType").asText();
                        }
                        node.put("rateResult", getRateService().calculatePayment(parkingId, inDate, outDate, cashlessPayment, isCheck, paymentsJson, carType));
                        node.put("rateFreeMinutes", getRateService().calculateFreeMinutes(parkingId, inDate, outDate, paymentsJson));
                        long timeDiff = Math.abs(outDate.getTime() - inDate.getTime());
                        long hours = TimeUnit.HOURS.convert(timeDiff, TimeUnit.MILLISECONDS);
                        node.put("payed_till", hours);
                    }
                    ParkingRate parkingRate = getRateService().getByParkingId(parkingId);
                    if (parkingRate != null) {
                        node.put("rateId", parkingRate.getId());
                        node.put("rateName", parkingRate.getName());
                    }
                }
            }
        }
        return node;
    }

    private RateService getRateService(){
        if(rateService == null) {
            rateService = (RateService) RatePlugin.INSTANCE.getApplicationContext().getBean("rateServiceImpl");
        }
        return rateService;
    }
}
