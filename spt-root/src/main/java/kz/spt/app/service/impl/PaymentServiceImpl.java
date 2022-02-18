package kz.spt.app.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.spt.lib.model.*;
import kz.spt.lib.model.dto.parkomat.ParkomatBillingInfoSuccessDto;
import kz.spt.lib.model.dto.parkomat.ParkomatCommandDTO;
import kz.spt.lib.service.*;
import kz.spt.lib.model.Cars;
import kz.spt.lib.model.dto.RateQueryDto;
import kz.spt.lib.utils.StaticValues;
import kz.spt.lib.extension.PluginRegister;
import kz.spt.lib.model.CarState;
import kz.spt.lib.model.Parking;
import kz.spt.lib.model.dto.payment.*;
import lombok.extern.java.Log;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Log
@Service
public class PaymentServiceImpl implements PaymentService {

    private final PluginService pluginService;
    private final CarStateService carStateService;
    private final CarsService carService;
    private final ParkingService parkingService;
    private final EventLogService eventLogService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PaymentServiceImpl(CarStateService carStateService, PluginService pluginService, CarsService carService, ParkingService parkingService, EventLogService eventLogService){
        this.pluginService = pluginService;
        this.carStateService = carStateService;
        this.carService = carService;
        this.parkingService = parkingService;
        this.eventLogService = eventLogService;
    }

    @Override
    public Object billingInteractions(CommandDto commandDto) throws Exception {
        SimpleDateFormat format = new SimpleDateFormat(StaticValues.dateFormatTZ);

        if(commandDto.account != null) {
            commandDto.account = commandDto.account.toUpperCase();
            if ("check".equals(commandDto.command)) {

                if(commandDto.prepaid != null && commandDto.prepaid){
                    Parking parking = parkingService.findByType(Parking.ParkingType.PREPAID);
                    if(parking != null){
                        return fillPrepaid(commandDto,parking);
                    } else {
                        BillingInfoErrorDto dto = new BillingInfoErrorDto();
                        dto.message = "Паркинг по предоплате не найден";
                        dto.result = 4;
                        dto.sum = commandDto.sum;
                        dto.txn_id = commandDto.txn_id;
                        return dto;
                    }
                } else {
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
                        dto.current_balance = BigDecimal.ZERO;
                        dto.in_date = format.format(carState.getInTimestamp());
                        dto.result = 0;
                        dto.left_free_time_minutes = 15;
                        carState.setCashlessPayment(true);

                        if (Parking.ParkingType.PAYMENT.equals(carState.getParking().getParkingType())) {
                            return fillPayment(carState, format, commandDto, carState.getPaymentJson());
                        } else if (Parking.ParkingType.WHITELIST_PAYMENT.equals(carState.getParking().getParkingType())) {
                            if(carState.getPaid()){
                                return fillPayment(carState, format, commandDto, carState.getPaymentJson());
                            }
                        }
                        return dto;
                    }
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
        BillingInfoErrorDto dto = new BillingInfoErrorDto();
        dto.sum = BigDecimal.ZERO;
        dto.txn_id  = commandDto.txn_id;
        dto.message = "Некорректный номер авто свяжитесь с оператором.";
        dto.result = 1;
        return dto;
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

                    if (Parking.ParkingType.PAYMENT.equals(carState.getParking().getParkingType())
                            || (Parking.ParkingType.WHITELIST_PAYMENT.equals(carState.getParking().getParkingType()) && carState.getPaid())) {
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

    @Override
    public void createDebtAndOUTState(String carNumber, Camera camera, Map<String, Object> properties) throws Exception {

        CarState carState = carStateService.getLastNotLeft(carNumber);
        if(carState != null && carState.getPaid()){
            PluginRegister ratePluginRegister = pluginService.getPluginRegister(StaticValues.ratePlugin);
            BigDecimal rateResult = BigDecimal.ZERO;
            if (ratePluginRegister != null) {
                log.info("ratePluginRegister: " + ratePluginRegister);
                SimpleDateFormat format = new SimpleDateFormat(StaticValues.dateFormatTZ);

                ObjectNode ratePluginNode = this.objectMapper.createObjectNode();
                ratePluginNode.put("parkingId", camera.getGate().getParking().getId());
                ratePluginNode.put("inDate", format.format(carState.getInTimestamp()));
                ratePluginNode.put("outDate", format.format(new Date()));
                ratePluginNode.put("cashlessPayment", carState.getCashlessPayment() != null ? carState.getCashlessPayment() : true);
                ratePluginNode.put("isCheck", false);
                ratePluginNode.put("paymentsJson", carState.getPaymentJson());

                JsonNode ratePluginResult = ratePluginRegister.execute(ratePluginNode);
                rateResult = ratePluginResult.get("rateResult").decimalValue().setScale(2);
                carState.setRateAmount(rateResult);

                PluginRegister billingPluginRegister = pluginService.getPluginRegister(StaticValues.billingPlugin);
                if (billingPluginRegister != null && BigDecimal.ZERO.compareTo(rateResult) != 0) {
                    log.info("billingPluginRegister: " + billingPluginRegister);
                    ObjectNode billingSubtractNode = this.objectMapper.createObjectNode();
                    billingSubtractNode.put("command", "decreaseCurrentBalance");
                    billingSubtractNode.put("amount", rateResult);
                    billingSubtractNode.put("plateNumber", carState.getCarNumber());
                    billingSubtractNode.put("parkingName", carState.getParking().getName());
                    billingSubtractNode.put("carStateId", carState.getId());
                    billingPluginRegister.execute(billingSubtractNode).get("currentBalance").decimalValue();
                }
            }
            carStateService.createOUTState(carNumber, new Date(), camera, carState);

            String descriptionRu = "Выпускаем авто: Авто с гос. номером " + carNumber + " с долгом -" + rateResult;
            String descriptionEn = "Releasing: Car with license plate " + carNumber + " with debt -" + rateResult;
            eventLogService.sendSocketMessage(EventLogService.ArmEventType.CarEvent, EventLogService.EventType.Allow, camera.getId(), carNumber, descriptionRu, descriptionEn);
            eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), properties, descriptionRu, descriptionEn);
        }
    }

    private BillingInfoSuccessDto fillPayment(CarState carState, SimpleDateFormat format, CommandDto commandDto, String paymentsJson) throws Exception {
        BillingInfoSuccessDto dto = null;

        PluginRegister ratePluginRegister = pluginService.getPluginRegister(StaticValues.ratePlugin);
        if (ratePluginRegister != null) {
            ObjectNode node = this.objectMapper.createObjectNode();
            node.put("parkingId", carState.getParking().getId());
            node.put("inDate", format.format(carState.getInTimestamp()));
            node.put("outDate", format.format(new Date()));
            node.put("isCheck", true);
            node.put("cashlessPayment", carState.getCashlessPayment() != null ? carState.getCashlessPayment() : true);
            node.put("paymentsJson", paymentsJson);

            JsonNode result = ratePluginRegister.execute(node);
            BigDecimal rateResult = result.get("rateResult").decimalValue();

            dto = new BillingInfoSuccessDto();
            dto.sum = rateResult.setScale(2);
            dto.in_date = format.format(carState.getInTimestamp());
            dto.result = 0;
            dto.left_free_time_minutes = result.get("rateFreeMinutes").intValue();

            log.info("dto.left_free_time_minutes: " + dto.left_free_time_minutes);

            dto.tariff = result.get("rateName") != null ? result.get("rateName").textValue() : "";
            dto.txn_id = commandDto.txn_id;
            dto.hours = result.get("payed_till") != null ? (int) result.get("payed_till").longValue() : 0;
        }

        JsonNode currentBalanceResult = getCurrentBalance(carState.getCarNumber());
        if(currentBalanceResult.has("currentBalance")){
            dto.current_balance = currentBalanceResult.get("currentBalance").decimalValue().setScale(2);
        }

        return dto;
    }

    private BillingInfoSuccessDto fillPrepaid(CommandDto commandDto, Parking parking) throws Exception{
        BillingInfoSuccessDto dto = new BillingInfoSuccessDto();
        dto.txn_id = commandDto.txn_id;
        dto.result = 0;
        dto.left_free_time_minutes = 0;
        dto.in_date = "";
        dto.hours = 0;

        JsonNode rateValue = getRateByParking(parking.getId());
        dto.tariff = rateValue.get("rateName").textValue();

        JsonNode prepaidValue = getPrepaidValue(parking.getId());
        dto.sum = new BigDecimal(prepaidValue.get("prepaidValue").intValue());

        JsonNode currentBalanceResult = getCurrentBalance(commandDto.account);
        dto.current_balance = currentBalanceResult.get("currentBalance").decimalValue().setScale(2);

        return dto;
    }

    private JsonNode getPrepaidValue(Long parkingId) throws Exception {
        ObjectNode node = this.objectMapper.createObjectNode();
        JsonNode prepaidValueResult = null;
        node.put("command", "getPrepaidValue");
        node.put("parkingId", parkingId);

        PluginRegister ratePluginRegister = pluginService.getPluginRegister(StaticValues.ratePlugin);
        if (ratePluginRegister != null) {
            prepaidValueResult = ratePluginRegister.execute(node);
        }
        return prepaidValueResult;
    }

    private JsonNode getRateByParking(Long parkingId) throws Exception {
        ObjectNode node = this.objectMapper.createObjectNode();
        JsonNode rateResult = null;
        node.put("command", "getRateByParking");
        node.put("parkingId", parkingId);

        PluginRegister ratePluginRegister = pluginService.getPluginRegister(StaticValues.ratePlugin);
        if (ratePluginRegister != null) {
            rateResult = ratePluginRegister.execute(node);
        }
        return rateResult;
    }

    private JsonNode getCurrentBalance(String car_number) throws Exception {
        ObjectNode node = this.objectMapper.createObjectNode();
        JsonNode currentBalanceResult = null;
        node.put("command", "getCurrentBalance");
        node.put("plateNumber", car_number);

        PluginRegister billingPluginRegister = pluginService.getPluginRegister(StaticValues.billingPlugin);
        if (billingPluginRegister != null) {
            currentBalanceResult = billingPluginRegister.execute(node);
        }
        return currentBalanceResult;
    }
}
