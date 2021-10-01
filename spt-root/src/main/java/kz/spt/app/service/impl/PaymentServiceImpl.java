package kz.spt.app.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.spt.app.utils.StaticValues;
import kz.spt.lib.extension.PluginRegister;
import kz.spt.lib.model.CarState;
import kz.spt.lib.model.Parking;
import kz.spt.lib.model.dto.payment.*;
import kz.spt.lib.service.CarStateService;
import kz.spt.lib.service.PaymentService;
import org.pf4j.PluginManager;
import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Service
public class PaymentServiceImpl implements PaymentService {

    private PluginManager pluginManager;
    private CarStateService carStateService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PaymentServiceImpl(PluginManager pluginManager, CarStateService carStateService){
        this.pluginManager = pluginManager;
        this.carStateService = carStateService;
    }

    @Override
    public Object billingInteractions(CommandDto commandDto) throws Exception {
        SimpleDateFormat format = new SimpleDateFormat(StaticValues.dateFormat);

        if("check".equals(commandDto.command)){
            if(commandDto.account != null){
                CarState carState = carStateService.getLastNotLeft(commandDto.account);
                if(carState == null){
                    BillingInfoErrorDto dto = new BillingInfoErrorDto();
                    dto.message = "Некорректный номер авто свяжитесь с оператором.";
                    dto.result = 1;
                    return dto;
                } else {
                    BillingInfoSuccessDto dto = new BillingInfoSuccessDto();
                    dto.sum = 0;
                    dto.in_date = format.format(carState.getInTimestamp());
                    dto.result = 0;
                    dto.left_free_time_minutes = 15;

                    if(Parking.ParkingType.PAYMENT.equals(carState.getParking().getParkingType())) {
                        PluginWrapper ratePlugin = pluginManager.getPlugin("rate-plugin");
                        if(ratePlugin!=null && ratePlugin.getPluginState().equals(PluginState.STARTED)){
                            List<PluginRegister> pluginRegisters = pluginManager.getExtensions(PluginRegister.class, ratePlugin.getPluginId());
                            if(pluginRegisters.size() > 0) {
                                ObjectNode node = this.objectMapper.createObjectNode();
                                node.put("parkingId", carState.getParking().getId());
                                node.put("inDate", format.format(carState.getInTimestamp()));
                                node.put("outDate", format.format(new Date()));

                                PluginRegister pluginRegister = pluginRegisters.get(0);
                                JsonNode result = pluginRegister.execute(node);
                                int rateResult = result.get("rateResult").intValue();

                                dto.sum = rateResult;
                                dto.in_date = format.format(carState.getInTimestamp());
                                dto.result = 0;
                                dto.left_free_time_minutes = result.get("rateFreeMinutes").intValue();
                            }
                        }
                    } else if(Parking.ParkingType.WHITELIST_PAYMENT.equals(carState.getParking().getParkingType())){
                        // TODO: Check in Whitelist if not in it then return payment amount
                    } else {

                    }
                    return dto;
                }
            } else {
                BillingInfoErrorDto dto = new BillingInfoErrorDto();
                dto.message = "Некорректный номер авто свяжитесь с оператором.";
                dto.result = 1;
                return dto;
            }
        } else if("pay".equals(commandDto.command)){
            CarState carState = carStateService.getLastNotLeft(commandDto.account);
            if(carState == null){
                BillingInfoErrorDto dto = new BillingInfoErrorDto();
                dto.message = "Некорректный номер авто свяжитесь с оператором.";
                dto.result = 1;
                dto.sum = commandDto.sum;
                dto.txn_id = commandDto.txn_id;
                return dto;
            } else {
                BillingPaymentSuccessDto dto = new BillingPaymentSuccessDto();
                dto.result = 0;
                dto.sum = commandDto.sum;
                dto.txn_id = commandDto.txn_id;
                dto.payment_id = ""; // TODO set real payment id
                return dto;
            }
        }
        return null;
    }

    
}
