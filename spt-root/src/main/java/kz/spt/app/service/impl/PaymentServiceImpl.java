package kz.spt.app.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.spt.lib.model.dto.parkomat.ParkomatBillingInfoSuccessDto;
import kz.spt.lib.model.dto.parkomat.ParkomatCommandDTO;
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
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

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
        SimpleDateFormat format = new SimpleDateFormat(StaticValues.dateFormatTZ);

        if(commandDto.account != null) {
            commandDto.account = commandDto.account.toUpperCase();
            if ("check".equals(commandDto.command)) {
                CarState carState = carStateService.getLastNotLeft(commandDto.account);
                if (carState == null) {
                    BillingInfoErrorDto dto = new BillingInfoErrorDto();
                    dto.sum = BigDecimal.ZERO;
                    dto.txn_id  = commandDto.txn_id;
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
                        return fillPayment(carState, format, commandDto, carState.getPaymentJson());
                    } else if (Parking.ParkingType.WHITELIST_PAYMENT.equals(carState.getParking().getParkingType())) {
                        if(carState.getPaid()){
                            return fillPayment(carState, format, commandDto, carState.getPaymentJson());
                        }
                    }
                    return dto;
                }
            } else if ("pay".equals(commandDto.command)) {
                if(commandDto.txn_id == null || "".equals(commandDto.txn_id)){
                    BillingInfoErrorDto dto = new BillingInfoErrorDto();
                    dto.message = "Пустое значение для поля txn_id";
                    dto.result = 2;
                    dto.sum = commandDto.sum;
                    dto.txn_id = commandDto.txn_id;
                    return dto;
                }
                if(commandDto.sum == null || BigDecimal.ZERO.compareTo(commandDto.sum) > -1){
                    BillingInfoErrorDto dto = new BillingInfoErrorDto();
                    dto.message = "Некорректное значение для sum";
                    dto.result = 3;
                    dto.sum = commandDto.sum;
                    dto.txn_id = commandDto.txn_id;
                    return dto;
                }
                CarState carState = carStateService.getLastNotLeft(commandDto.account);
                if (carState == null) {
                    BillingInfoErrorDto dto = new BillingInfoErrorDto();
                    dto.message = "Некорректный номер авто свяжитесь с оператором.";
                    dto.result = 1;
                    dto.sum = commandDto.sum;
                    dto.txn_id = commandDto.txn_id;
                    return dto;
                } else {
                    PluginRegister billingPluginRegister = pluginService.getPluginRegister(StaticValues.billingPlugin);
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

                        PluginRegister ratePluginRegister = pluginService.getPluginRegister(StaticValues.ratePlugin);
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
                        if (result.has("paymentError")){
                            BillingInfoErrorDto dto = new BillingInfoErrorDto();
                            dto.result = result.get("paymentErrorCode").intValue();
                            dto.message = result.get("paymentError").textValue();
                            dto.txn_id = commandDto.txn_id;
                            dto.sum = commandDto.sum;
                            return dto;
                        }
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
    public Object billingInteractions(ParkomatCommandDTO commandDto) throws Exception {
        SimpleDateFormat format = new SimpleDateFormat(StaticValues.dateFormatTZ);

        if(commandDto.getAccount() != null) {
            if ("check".equals(commandDto.getCommand())) {
                CarState carState = carStateService.getLastNotLeft(commandDto.getAccount());
                if (carState == null) {
                    BillingInfoErrorDto dto = new BillingInfoErrorDto();
                    dto.sum = BigDecimal.ZERO;
                    dto.txn_id  = commandDto.getTxn_id();
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

                        CommandDto payCommandDto = new CommandDto();
                        payCommandDto.setTxn_id(commandDto.getTxn_id());
                        carState.setCashlessPayment(false);
                        BillingInfoSuccessDto paymentOfflineResult = fillPayment(carState, format, payCommandDto, carState.getPaymentJson());
                        carState.setCashlessPayment(true);
                        BillingInfoSuccessDto paymentOnlineResult = fillPayment(carState, format, payCommandDto, carState.getPaymentJson());
                        ParkomatBillingInfoSuccessDto parkomatBillingInfoSuccessDto = ParkomatBillingInfoSuccessDto.convert(paymentOfflineResult);
                        parkomatBillingInfoSuccessDto.setOnlineSum(paymentOnlineResult.sum);
                        SimpleDateFormat simpleFormat = new SimpleDateFormat(StaticValues.simpleDateTimeFormat);
                        Date inDate = carState.getInTimestamp();
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTime(inDate);
                        calendar.add(Calendar.HOUR_OF_DAY, paymentOfflineResult.hours);
                        parkomatBillingInfoSuccessDto.setPayed_till(simpleFormat.format(calendar.getTime()));
                        parkomatBillingInfoSuccessDto.setIn_date(simpleFormat.format(carState.getInTimestamp().getTime()));
                        parkomatBillingInfoSuccessDto.setHours(paymentOfflineResult.hours);

                        long timeDiff = new Date().getTime()- inDate.getTime();
                        long minutes = TimeUnit.MINUTES.convert(timeDiff, TimeUnit.MILLISECONDS);

                        long leftFreeMinutes = paymentOfflineResult.left_free_time_minutes - minutes;

                        parkomatBillingInfoSuccessDto.setLeft_free_time_minutes((leftFreeMinutes>0) ? (int)leftFreeMinutes : 0);

                        return parkomatBillingInfoSuccessDto;


                    } else if (Parking.ParkingType.WHITELIST_PAYMENT.equals(carState.getParking().getParkingType())) {
                        if(carState.getPaid()){
                            CommandDto payCommandDto = new CommandDto();
                            payCommandDto.setTxn_id(commandDto.getTxn_id());
                            return fillPayment(carState, format, payCommandDto, carState.getPaymentJson());
                        }
                    }
                    return dto;
                }
            } else if ("pay".equals(commandDto.getCommand())) {
                if(commandDto.getTxn_id() == null || "".equals(commandDto.getTxn_id().isEmpty())){
                    BillingInfoErrorDto dto = new BillingInfoErrorDto();
                    dto.message = "Пустое значение для поля txn_id";
                    dto.result = 2;
                    dto.sum = commandDto.getSum();
                    dto.txn_id = commandDto.getTxn_id();
                    return dto;
                }
                if(commandDto.getSum() == null || BigDecimal.ZERO.compareTo(commandDto.getSum()) > -1){
                    BillingInfoErrorDto dto = new BillingInfoErrorDto();
                    dto.message = "Некорректное значение для sum";
                    dto.result = 3;
                    dto.sum = commandDto.getSum();
                    dto.txn_id = commandDto.getTxn_id();
                    return dto;
                }
                CarState carState = carStateService.getLastNotLeft(commandDto.getAccount());
                if (carState == null) {
                    BillingInfoErrorDto dto = new BillingInfoErrorDto();
                    dto.message = "Некорректный номер авто свяжитесь с оператором.";
                    dto.result = 1;
                    dto.sum = commandDto.getSum();
                    dto.txn_id = commandDto.getTxn_id();
                    return dto;
                } else {
                    PluginRegister billingPluginRegister = pluginService.getPluginRegister(StaticValues.billingPlugin);
                    if (billingPluginRegister != null) {


                        ObjectNode node = this.objectMapper.createObjectNode();
                        node.put("command", "getParkomatClientId");
                        node.put("parkomatId", commandDto.getParkomat());
                        JsonNode clientIdResult = billingPluginRegister.execute(node);

                        String clientId = clientIdResult.get("clientId").textValue();

                        node = this.objectMapper.createObjectNode();
                        node.put("command", "savePayment");
                        node.put("carNumber", commandDto.getAccount());
                        node.put("sum", commandDto.getSum());
                        node.put("transaction", commandDto.getTxn_id());
                        node.put("parkingId", carState.getParking().getId());
                        node.put("carStateId", carState.getId());
                        node.put("inDate", format.format(carState.getInTimestamp()));
                        node.put("clientId", clientId);

                        Cars cars = carService.findByPlatenumberWithCustomer(commandDto.getAccount());
                        if(cars != null && cars.getCustomer() != null){
                            node.put("customerId", cars.getCustomer().getId());
                        }

                        PluginRegister ratePluginRegister = pluginService.getPluginRegister(StaticValues.ratePlugin);
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
                        if (result.has("paymentError")){
                            BillingInfoErrorDto dto = new BillingInfoErrorDto();
                            dto.result = result.get("paymentErrorCode").intValue();
                            dto.message = result.get("paymentError").textValue();
                            dto.txn_id = commandDto.getTxn_id();
                            dto.sum = commandDto.getSum();
                            return dto;
                        }
                        Long paymentId = result.get("paymentId").longValue();

                        carState.setAmount(carState.getAmount() != null ? carState.getAmount().add(commandDto.getSum()) : commandDto.getSum()); // if he paid early we should add this amount
                        carState.setPaymentId(paymentId);
                        carState.setPaymentJson(result.get("paymentArray").toString());
                        carState.setCashlessPayment(result.get("cashlessPayment").booleanValue());
                        carStateService.save(carState);

                        BillingPaymentSuccessDto dto = new BillingPaymentSuccessDto();
                        dto.result = 0;
                        dto.sum = commandDto.getSum();
                        dto.txn_id = commandDto.getTxn_id();
                        dto.payment_id = paymentId.toString();
                        return dto;
                    }
                }
            } else if ("getCheck".equals(commandDto.getCommand())) {

                PluginRegister billingPluginRegister = pluginService.getPluginRegister(StaticValues.billingPlugin);
                ObjectNode node = this.objectMapper.createObjectNode();
                node.put("command", "getCheck");
                node.put("parkomatId", commandDto.getParkomat());
                node.put("sum", commandDto.getSum());
                node.put("change", commandDto.getChange());
                node.put("change", commandDto.getChange());
                node.put("txn_id", commandDto.getTxn_id());
                node.put("operationName", "Оплата парковки");
                node.put("paymentType", 0);
                JsonNode checkResult = billingPluginRegister.execute(node);
                return checkResult;
            } else if ("zReport".equals(commandDto.getCommand())) {

                PluginRegister billingPluginRegister = pluginService.getPluginRegister(StaticValues.billingPlugin);
                ObjectNode node = this.objectMapper.createObjectNode();
                node.put("command", "zReport");
                node.put("parkomatId", commandDto.getParkomat());
                JsonNode checkResult = billingPluginRegister.execute(node);
                return checkResult;
            }
        }
        return null;
    }

    @Override
    public BigDecimal getRateValue(RateQueryDto rateQueryDto) throws Exception {
        SimpleDateFormat format = new SimpleDateFormat(StaticValues.dateFormatTZ);

        PluginRegister ratePluginRegister = pluginService.getPluginRegister(StaticValues.ratePlugin);
        if (ratePluginRegister != null) {
            ObjectNode node = this.objectMapper.createObjectNode();
            node.put("parkingId", rateQueryDto.parkingId);
            node.put("inDate", format.format(rateQueryDto.inDate));
            node.put("outDate", format.format(rateQueryDto.outDate));
            node.put("cashlessPayment", rateQueryDto.cashlessPayment);
            JsonNode result = ratePluginRegister.execute(node);
            return result.get("rateResult").decimalValue().setScale(2);
        }
        return null;
    }

    private BillingInfoSuccessDto fillPayment(CarState carState, SimpleDateFormat format, CommandDto commandDto, String paymentsJson) throws Exception {
        BillingInfoSuccessDto dto = null;
        PluginRegister ratePluginRegister = pluginService.getPluginRegister(StaticValues.ratePlugin);
        if (ratePluginRegister != null) {
            ObjectNode node = this.objectMapper.createObjectNode();
            node.put("parkingId", carState.getParking().getId());
            node.put("inDate", format.format(carState.getInTimestamp()));
            node.put("outDate", format.format(new Date()));
            node.put("cashlessPayment", carState.getCashlessPayment() != null ? carState.getCashlessPayment() : false);
            node.put("paymentsJson", paymentsJson);

            JsonNode result = ratePluginRegister.execute(node);
            BigDecimal rateResult = result.get("rateResult").decimalValue();

            dto = new BillingInfoSuccessDto();
            dto.sum = rateResult.setScale(2);
            dto.in_date = format.format(carState.getInTimestamp());
            dto.result = 0;
            dto.left_free_time_minutes = result.get("rateFreeMinutes").intValue();
            dto.tariff = result.get("rateName") != null ? result.get("rateName").textValue() : "";
            dto.txn_id = commandDto.txn_id;
            dto.hours = result.get("payed_till") != null ? (int) result.get("payed_till").longValue() : 0;
        }
        PluginRegister billingPluginRegister = pluginService.getPluginRegister(StaticValues.billingPlugin);
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
