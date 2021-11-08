package kz.spt.app.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.spt.lib.service.PluginService;
import kz.spt.lib.model.Cars;
import kz.spt.lib.model.dto.RateQueryDto;
import kz.spt.lib.service.CarsService;
import kz.spt.lib.utils.StaticValues;
import kz.spt.lib.extension.PluginRegister;
import kz.spt.lib.model.CarState;
import kz.spt.lib.model.Parking;
import kz.spt.lib.model.dto.payment.*;
import kz.spt.lib.service.CarStateService;
import kz.spt.lib.service.PaymentService;
import lombok.extern.java.Log;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

@Log
@Service
public class PaymentServiceImpl implements PaymentService {

    private final PluginService pluginService;
    private final CarStateService carStateService;
    private final CarsService carService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PaymentServiceImpl(CarStateService carStateService, PluginService pluginService, CarsService carService){
        this.pluginService = pluginService;
        this.carStateService = carStateService;
        this.carService = carService;
    }

    @Override
    public Object billingInteractions(CommandDto commandDto) throws Exception {
        SimpleDateFormat format = new SimpleDateFormat(StaticValues.dateFormat);

        if(commandDto.account != null) {
            commandDto.account = commandDto.account.toUpperCase();
            if ("check".equals(commandDto.command)) {
                CarState carState = carStateService.getLastNotLeft(commandDto.account);
                if (carState == null) {
                    BillingInfoErrorDto dto = new BillingInfoErrorDto();
                    dto.message = "Некорректный номер авто свяжитесь с оператором.";
                    dto.result = 1;
                    return dto;
                } else {
                    BillingInfoSuccessDto dto = new BillingInfoSuccessDto();
                    dto.sum = BigDecimal.ZERO;
                    dto.in_date = format.format(carState.getInTimestamp());
                    dto.result = 0;
                    dto.left_free_time_minutes = 15;

                    if (Parking.ParkingType.PAYMENT.equals(carState.getParking().getParkingType())) {
                        return fillPayment(carState, format);
                    } else if (Parking.ParkingType.WHITELIST_PAYMENT.equals(carState.getParking().getParkingType())) {
                        if(carState.getPaid()){
                            return fillPayment(carState, format);
                        }
                    }
                    return dto;
                }
            } else if ("pay".equals(commandDto.command)) {
                CarState carState = carStateService.getLastNotLeft(commandDto.account);
                if (carState == null) {
                    BillingInfoErrorDto dto = new BillingInfoErrorDto();
                    dto.message = "Некорректный номер авто свяжитесь с оператором.";
                    dto.result = 1;
                    dto.sum = commandDto.sum;
                    dto.txn_id = commandDto.txn_id;
                    return dto;
                } else {
                    PluginRegister billingPluginRegister = pluginService.getPluginRegister("billing-plugin");
                    if (billingPluginRegister != null) {
                        ObjectNode node = this.objectMapper.createObjectNode();
                        node.put("command", "savePayment");
                        node.put("carNumber", commandDto.account);
                        node.put("sum", commandDto.sum);
                        node.put("transaction", commandDto.txn_id);
                        node.put("parkingId", carState.getParking().getId());
                        node.put("carStateId", carState.getId());
                        node.put("inDate", format.format(carState.getInTimestamp()));

                        Authentication a = SecurityContextHolder.getContext().getAuthentication();
                        String clientId = ((OAuth2Authentication) a).getOAuth2Request().getClientId();
                        node.put("clientId", clientId);

                        Cars cars = carService.findByPlatenumberWithCustomer(commandDto.account);
                        if(cars != null && cars.getCustomer() != null){
                            node.put("customerId", cars.getCustomer().getId());
                        }

                        PluginRegister ratePluginRegister = pluginService.getPluginRegister("rate-plugin");
                        if (ratePluginRegister != null) {
                            ObjectNode rateRequestNode = this.objectMapper.createObjectNode();
                            rateRequestNode.put("parkingId", carState.getParking().getId());
                            JsonNode result = ratePluginRegister.execute(rateRequestNode);
                            String rateName = result.get("rateName").textValue();
                            Long rateId = result.get("rateName").longValue();
                            node.put("rateName", rateName);
                            node.put("rateId", rateId);
                        }

                        JsonNode result = billingPluginRegister.execute(node);
                        Long paymentId = result.get("paymentId").longValue();

                        carState.setAmount(carState.getAmount() != null ? carState.getAmount().add(commandDto.sum) : commandDto.sum); // if he paid early we should add this amount
                        carState.setPaymentId(paymentId);
                        carState.setPaymentJson(result.get("paymentArray").toString());
                        carState.setCashlessPayment(result.get("cashlessPayment").booleanValue());
                        carStateService.save(carState);

                        BillingPaymentSuccessDto dto = new BillingPaymentSuccessDto();
                        dto.result = 0;
                        dto.sum = commandDto.sum;
                        dto.txn_id = commandDto.txn_id;
                        dto.payment_id = paymentId.toString();
                        return dto;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public BigDecimal getRateValue(RateQueryDto rateQueryDto) throws Exception {
        SimpleDateFormat format = new SimpleDateFormat(StaticValues.dateFormat);

        PluginRegister ratePluginRegister = pluginService.getPluginRegister("rate-plugin");
        if (ratePluginRegister != null) {
            ObjectNode node = this.objectMapper.createObjectNode();
            node.put("parkingId", rateQueryDto.parkingId);
            log.info("format.format(rateQueryDto.inDate): " + format.format(rateQueryDto.inDate));
            node.put("inDate", format.format(rateQueryDto.inDate));
            log.info("format.format(rateQueryDto.outDate): " + format.format(rateQueryDto.outDate));
            log.info("current time: " + (new Date()));
            node.put("outDate", format.format(rateQueryDto.outDate));
            node.put("cashlessPayment", rateQueryDto.cashlessPayment);
            JsonNode result = ratePluginRegister.execute(node);
            return result.get("rateResult").decimalValue().setScale(2);
        }
        return null;
    }

    private BillingInfoSuccessDto fillPayment(CarState carState, SimpleDateFormat format) throws Exception {
        BillingInfoSuccessDto dto = null;
        PluginRegister ratePluginRegister = pluginService.getPluginRegister("rate-plugin");
        if (ratePluginRegister != null) {
            ObjectNode node = this.objectMapper.createObjectNode();
            node.put("parkingId", carState.getParking().getId());
            node.put("inDate", format.format(carState.getInTimestamp()));
            node.put("outDate", format.format(new Date()));
            node.put("cashlessPayment", carState.getCashlessPayment() != null ? carState.getCashlessPayment() : false);

            JsonNode result = ratePluginRegister.execute(node);
            BigDecimal rateResult = result.get("rateResult").decimalValue();

            dto = new BillingInfoSuccessDto();
            dto.sum = rateResult.setScale(2);
            dto.in_date = format.format(carState.getInTimestamp());
            dto.result = 0;
            dto.left_free_time_minutes = result.get("rateFreeMinutes").intValue();
        }
        PluginRegister billingPluginRegister = pluginService.getPluginRegister("billing-plugin");
        if (billingPluginRegister != null) {
            ObjectNode node = this.objectMapper.createObjectNode();
            node.put("command", "getCurrentBalance");
            node.put("plateNumber", carState.getCarNumber());

            JsonNode result = billingPluginRegister.execute(node);
            dto.current_balance = result.get("currentBalance").decimalValue().setScale(2);
        }

        return dto;
    }
}
