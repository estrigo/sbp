package kz.spt.app.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.spt.app.model.dto.Period;
import kz.spt.lib.service.LanguagePropertiesService;
import kz.spt.lib.utils.Language;
import kz.spt.lib.utils.MessageKey;
import kz.spt.app.service.WhitelistRootService;
import kz.spt.lib.extension.PluginRegister;
import kz.spt.lib.model.*;
import kz.spt.lib.model.dto.RateQueryDto;
import kz.spt.lib.model.dto.parkomat.ParkomatBillingInfoSuccessDto;
import kz.spt.lib.model.dto.parkomat.ParkomatCommandDTO;
import kz.spt.lib.model.dto.payment.BillingInfoErrorDto;
import kz.spt.lib.model.dto.payment.BillingInfoSuccessDto;
import kz.spt.lib.model.dto.payment.BillingPaymentSuccessDto;
import kz.spt.lib.model.dto.payment.CommandDto;
import kz.spt.lib.service.*;
import kz.spt.lib.utils.StaticValues;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

@Log
@Service
@Transactional(noRollbackFor = Exception.class)
public class PaymentServiceImpl implements PaymentService {

    private final PluginService pluginService;
    private final CarStateService carStateService;
    private final CarsService carService;
    private final ParkingService parkingService;
    private final CarModelService carModelService;
    private final EventLogService eventLogService;
    private final AbonomentService abonomentService;
    private final WhitelistRootService whitelistRootService;
    private final QrPanelService qrPanelService;
    private final LanguagePropertiesService languagePropertiesService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    SimpleDateFormat format = new SimpleDateFormat(StaticValues.dateFormatTZ);

    private final PaymentCheckLogService paymentCheckLogService;

    @Value("${fiscalization.mobile}")
    Boolean mobilePayFiscalization;

    public PaymentServiceImpl(CarStateService carStateService, PluginService pluginService, CarsService carService, ParkingService parkingService, EventLogService eventLogService,
                              CarModelService carModelService, AbonomentService abonomentService, WhitelistRootService whitelistRootService, QrPanelService qrPanelService,
                              LanguagePropertiesService languagePropertiesService, PaymentCheckLogService paymentCheckLogService) {
        this.pluginService = pluginService;
        this.carStateService = carStateService;
        this.carService = carService;
        this.parkingService = parkingService;
        this.eventLogService = eventLogService;
        this.carModelService = carModelService;
        this.abonomentService = abonomentService;
        this.whitelistRootService = whitelistRootService;
        this.qrPanelService = qrPanelService;
        this.languagePropertiesService = languagePropertiesService;
        this.paymentCheckLogService = paymentCheckLogService;
    }

    @Override
    public Object billingInteractions(CommandDto commandDto) throws Exception {
        if (commandDto.account != null) {
            commandDto.account = commandDto.account.toUpperCase();
            commandDto.account = commandDto.account.replaceAll("\\s", "");

            if ("check".equals(commandDto.command)) {
                if (commandDto.service_id != null && commandDto.service_id == 2) {
                    Parking parking = parkingService.findByType(Parking.ParkingType.PREPAID);
                    if (parking != null) {
                        BillingInfoSuccessDto successDto = fillPrepaid(commandDto, parking);
                        successDto.currency = getCurrency(parking.getParkingType());

                        savePaymentCheckLog(commandDto.account, null, successDto.sum, null, PaymentCheckLog.PaymentCheckType.PREPAID, successDto.current_balance, commandDto.getTxn_id(), commandDto.getClientId());
                        return successDto;
                    } else {
                        BillingInfoErrorDto dto = new BillingInfoErrorDto();
                        dto.message = languagePropertiesService.getMessageFromProperties(MessageKey.BILLING_PREPAID_NOT_FOUND);
                        dto.result = 1;
                        dto.sum = commandDto.sum;
                        dto.txn_id = commandDto.txn_id;
                        dto.currency = getCurrency();
                        savePaymentCheckLog(commandDto.account, null, dto.sum, null, PaymentCheckLog.PaymentCheckType.PREPAID, dto.current_balance, commandDto.getTxn_id(), commandDto.getClientId());
                        return dto;
                    }
//                } else if (commandDto.service_id!=null && commandDto.service_id==3) {
                } else {
                    CarState carState = carStateService.getLastNotLeft(commandDto.account);
                    JsonNode abonomentResultNode = getNotPaidAbonoment(commandDto);
                    if (abonomentResultNode != null && abonomentResultNode.has("price")) {
                        BillingInfoSuccessDto dto = fillAbonomentDetails(abonomentResultNode, commandDto);
                        dto.currency = getCurrency();
                        return dto;
                    }
                    if (carState == null) {
                        JsonNode currentBalanceResult = getCurrentBalance(commandDto.account);
                        if (currentBalanceResult.has("currentBalance") && BigDecimal.ZERO.compareTo(currentBalanceResult.get("currentBalance").decimalValue()) > 0) {
                            BillingInfoSuccessDto dto = fillDebtDetails(commandDto, currentBalanceResult.get("currentBalance").decimalValue());
                            dto.currency = getCurrency();
                            return dto;
                        }
                        BillingInfoErrorDto dto = new BillingInfoErrorDto();
                        dto.sum = BigDecimal.ZERO;
                        dto.txn_id = commandDto.txn_id;
                        dto.message = languagePropertiesService.getMessageFromProperties(MessageKey.BILLING_ERROR_INCORRECT_PLATENUMBER);
                        dto.result = 1;
                        dto.currency = getCurrency();
                        savePaymentCheckLog(commandDto.account, dto.message, dto.sum, null, PaymentCheckLog.PaymentCheckType.NOT_FOUND, dto.current_balance, commandDto.getTxn_id(), commandDto.getClientId());

                        return dto;
                    } else {
                        BillingInfoSuccessDto dto = new BillingInfoSuccessDto();
                        dto.txn_id = commandDto.txn_id;
                        dto.sum = BigDecimal.ZERO;
                        dto.current_balance = BigDecimal.ZERO;
                        dto.in_date = format.format(carState.getInTimestamp());
                        dto.result = 0;
                        dto.left_free_time_minutes = 15;
                        carState.setCashlessPayment(true);

                        JsonNode whiteLists = whitelistRootService.getValidWhiteListsInPeriod(carState.getParking().getId(), commandDto.account, carState.getInTimestamp(), new Date(), format);
                        if (whiteLists != null && whiteLists.isArray() && whiteLists.size() > 0) {
                            checkWhiteListExtraPayment(commandDto, carState, format, whiteLists, dto);
                            dto.currency = getCurrency(carState.getType());
                            return dto;
                        }

                        JsonNode abonements = abonomentService.getAbonomentsDetails(commandDto.account, carState, format);
                        if (abonements != null && abonements.isArray() && abonements.size() > 0) {
                            checkAbonomentExtraPayment(commandDto, carState, format, abonements, dto);
                            dto.currency = getCurrency(carState.getType());
                            return dto;
                        }

                        if (Parking.ParkingType.PAYMENT.equals(carState.getParking().getParkingType())) {
                            dto = fillPayment(carState, format, commandDto, carState.getPaymentJson());
                            dto.currency = getCurrency(carState.getType());
                            return dto;
                        } else if (Parking.ParkingType.WHITELIST_PAYMENT.equals(carState.getParking().getParkingType())) {
                            if (carState.getPaid()) {
                                dto = fillPayment(carState, format, commandDto, carState.getPaymentJson());
                                dto.currency = getCurrency(carState.getType());
                                return dto;
                            }
                        }
                        savePaymentCheckLog(commandDto.account, null, dto.sum, carState.getId(), PaymentCheckLog.PaymentCheckType.STANDARD, dto.current_balance, commandDto.getTxn_id(), commandDto.getClientId());
                        dto.currency = getCurrency(carState.getType());
                        return dto;
                    }
                }
            } else if ("pay".equals(commandDto.command)) {
                if (commandDto.txn_id == null || "".equals(commandDto.txn_id)) {
                    BillingInfoErrorDto dto = new BillingInfoErrorDto();
                    dto.message = languagePropertiesService.getMessageFromProperties(MessageKey.BILLING_ERROR_NULL_TXN);
                    dto.result = 4;
                    dto.sum = commandDto.sum;
                    dto.txn_id = commandDto.txn_id;
                    return dto;
                }
                if (commandDto.sum == null || BigDecimal.ZERO.compareTo(commandDto.sum) > -1) {
                    BillingInfoErrorDto dto = new BillingInfoErrorDto();
                    dto.message = languagePropertiesService.getMessageFromProperties(MessageKey.BILLING_ERROR_INCORRECT_SUM);
                    dto.result = 5;
                    dto.sum = commandDto.sum;
                    dto.txn_id = commandDto.txn_id;
                    return dto;
                }
                if (!checkProvider(commandDto.clientId)) {
                    BillingInfoErrorDto dto = new BillingInfoErrorDto();
                    dto.message = languagePropertiesService.getMessageFromProperties(MessageKey.BILLING_NOT_ALLOWED_PAYMENT);
                    dto.result = 6;
                    dto.sum = commandDto.sum;
                    dto.txn_id = commandDto.txn_id;
                    return dto;
                }
                CarState carState = carStateService.getLastNotLeft(commandDto.account);
                if (carState == null) {
                    if (commandDto.service_id != null && commandDto.service_id == 2) {
                        Object payment = savePayment(commandDto, null, Parking.ParkingType.PREPAID, false);
                        if (BillingInfoErrorDto.class.equals(payment.getClass())) {
                            return payment;
                        }

                        JsonNode result = (JsonNode) payment;
                        Long paymentId = result.get("paymentId").longValue();

                        return successPayment(commandDto, paymentId);
//                    } else if (commandDto.service_id!=null && commandDto.service_id==3) {
                    } else {
                        JsonNode abonomentResultNode = getNotPaidAbonoment(commandDto);
                        if (abonomentResultNode != null && abonomentResultNode.has("price")) {
                            return payAbonoment(commandDto, carState, abonomentResultNode);
                        }
                        CarState lastDebtCarState = carStateService.getLastCarState(commandDto.account); // Оплата долга
                        if (lastDebtCarState != null) {
                            Object payment = savePayment(commandDto, lastDebtCarState, null, false);
                            if (BillingInfoErrorDto.class.equals(payment.getClass())) {
                                return payment;
                            }
                            JsonNode result = (JsonNode) payment;
                            Long paymentId = result.get("paymentId").longValue();

                            lastDebtCarState.setAmount(lastDebtCarState.getAmount() != null ? lastDebtCarState.getAmount().add(commandDto.sum) : commandDto.sum); // if he paid early we should add this amount
                            lastDebtCarState.setPaymentId(paymentId);
                            lastDebtCarState.setPaymentJson(result.get("paymentArray").toString());
                            lastDebtCarState.setCashlessPayment(result.get("cashlessPayment").booleanValue());
                            carStateService.save(lastDebtCarState);
                            return successPayment(commandDto, paymentId);
                        }
                        BillingInfoErrorDto dto = new BillingInfoErrorDto();
                        dto.message = languagePropertiesService.getMessageFromProperties(MessageKey.BILLING_ERROR_INCORRECT_PLATENUMBER);
                        dto.result = 1;
                        dto.sum = commandDto.sum;
                        dto.txn_id = commandDto.txn_id;
                        return dto;
                    }
//                } else if (commandDto.service_id!=null && commandDto.service_id==3) {
                } else {
                    JsonNode abonomentResultNode = getNotPaidAbonoment(commandDto);
                    if (abonomentResultNode != null && abonomentResultNode.has("price")) {
                        return payAbonoment(commandDto, carState, abonomentResultNode);
                    }
                    Object payment = savePayment(commandDto, carState, null, false);
                    if (BillingInfoErrorDto.class.equals(payment.getClass())) {
                        return payment;
                    }

                    JsonNode result = (JsonNode) payment;
                    Long paymentId = result.get("paymentId").longValue();

                    carState.setAmount(carState.getAmount() != null ? carState.getAmount().add(commandDto.sum) : commandDto.sum); // if he paid early we should add this amount
                    carState.setPaymentId(paymentId);
                    carState.setPaymentJson(result.get("paymentArray").toString());
                    carState.setCashlessPayment(result.get("cashlessPayment").booleanValue());
                    carStateService.save(carState);

                    return successPayment(commandDto, paymentId);
                }
            }
        }

        BillingInfoErrorDto dto = new BillingInfoErrorDto();
        dto.sum = BigDecimal.ZERO;
        dto.txn_id = commandDto.txn_id;
        dto.message = languagePropertiesService.getMessageFromProperties(MessageKey.BILLING_ERROR_INCORRECT_PLATENUMBER);
        dto.result = 1;
        return dto;
    }

    private BillingInfoSuccessDto fillDebtDetails(CommandDto commandDto, BigDecimal currentBalance) {
        BillingInfoSuccessDto dto = new BillingInfoSuccessDto();
        dto.current_balance = BigDecimal.ZERO;
        dto.sum = currentBalance.abs().setScale(2);
        dto.tariff = "Оплата долга";
        dto.in_date = "";
        dto.result = 0;
        dto.left_free_time_minutes = 0;
        dto.hours = 0;
        dto.txn_id = commandDto.txn_id;

        savePaymentCheckLog(commandDto.account, null, dto.sum, null, PaymentCheckLog.PaymentCheckType.DEBT, dto.current_balance, commandDto.getTxn_id(), commandDto.getClientId());

        return dto;
    }

    private BillingInfoSuccessDto fillDebtDetails(ParkomatCommandDTO commandDto, BigDecimal currentBalance, String clientId) {
        BillingInfoSuccessDto dto = new BillingInfoSuccessDto();
        dto.current_balance = BigDecimal.ZERO;
        dto.sum = currentBalance.abs().setScale(2);
        dto.tariff = "Оплата долга";
        dto.in_date = "";
        dto.result = 0;
        dto.left_free_time_minutes = 0;
        dto.hours = 0;
        dto.txn_id = commandDto.getTxn_id();

        savePaymentCheckLog(commandDto.getAccount(), null, dto.sum, null, PaymentCheckLog.PaymentCheckType.DEBT, dto.current_balance, commandDto.getTxn_id(), clientId);

        return dto;
    }

    private Object payAbonoment(CommandDto commandDto, CarState carState, JsonNode abonomentResultNode) throws Exception {
        Object payment = savePayment(commandDto, carState, null, true);
        if (BillingInfoErrorDto.class.equals(payment.getClass())) {
            return payment;
        }

        BigDecimal current_balance = BigDecimal.ZERO;
        JsonNode currentBalanceResult = getCurrentBalance(commandDto.account);
        if (currentBalanceResult.has("currentBalance")) {
            current_balance = currentBalanceResult.get("currentBalance").decimalValue().setScale(2);
        }

        JsonNode result = (JsonNode) payment;
        Long paymentId = result.get("paymentId").longValue();

        if (current_balance.compareTo(abonomentResultNode.get("price").decimalValue()) >= 0) {
            PluginRegister billingPluginRegister = pluginService.getPluginRegister(StaticValues.billingPlugin);
            if (billingPluginRegister != null) {
                ObjectNode billingSubtractNode = this.objectMapper.createObjectNode();

                Map<String, Object> messageValues = new HashMap<>();
                messageValues.put("parking", abonomentResultNode.get("parkingName").textValue());
                Map<String, String> messages = languagePropertiesService.getWithDifferentLanguages(MessageKey.BILLING_REASON_PAYMENT_PAID_PERMIT, messageValues);

                billingSubtractNode.put("command", "decreaseCurrentBalance");
                billingSubtractNode.put("amount", abonomentResultNode.get("price").decimalValue());
                billingSubtractNode.put("plateNumber", commandDto.account);
                billingSubtractNode.put("reason", messages.get(Language.RU));
                billingSubtractNode.put("reasonEn", messages.get(Language.EN));
                billingSubtractNode.put("reasonLocal", messages.get(Language.LOCAL));
                billingSubtractNode.put("provider", commandDto.clientId);
                billingPluginRegister.execute(billingSubtractNode).get("currentBalance").decimalValue();
            }
            setAbonomentPaid(abonomentResultNode.get("id").longValue());
        }

        return successPayment(commandDto, paymentId);
    }
    private String getCurrency() throws Exception {
        return getCurrency(null);
    }

    private String getCurrency(Parking.ParkingType parkingType) throws Exception {
        String currency = "";
        PluginRegister ratePluginRegister = pluginService.getPluginRegister(StaticValues.ratePlugin);
        Long parkingId;

        if (!ObjectUtils.isEmpty(parkingType)) {
            Parking parking = parkingService.findByType(parkingType);
            parkingId = parking.getId();
            currency = "";
            if (ratePluginRegister != null) {
                ObjectNode node = this.objectMapper.createObjectNode();
                node.put("command", "getCurrency");
                node.put("parkingId", parkingId);
                JsonNode result = ratePluginRegister.execute(node);
                currency = result.get("currency").textValue();
            }

        } else {
            if (ratePluginRegister != null) {
                ObjectNode node = this.objectMapper.createObjectNode();
                node.put("command", "getCurrency");
                JsonNode result = ratePluginRegister.execute(node);
                currency = result.get("currency").textValue();
            }

        }
        return currency;
    }

    private Boolean checkProvider(String providerName) throws Exception {
        PluginRegister billingPluginRegister = pluginService.getPluginRegister(StaticValues.billingPlugin);
        Boolean providerExists = false;
        if (billingPluginRegister != null) {
            ObjectNode node = this.objectMapper.createObjectNode();
            node.put("command", "checkProvider");
            node.put("providerName", providerName);
            JsonNode result = billingPluginRegister.execute(node);
            providerExists = result.get("providerExists").booleanValue();
        }
        return providerExists;
    }

    @Override
    public Object billingInteractions(ParkomatCommandDTO commandDto) throws Exception {
        SimpleDateFormat format = new SimpleDateFormat(StaticValues.dateFormatTZ);

        if (commandDto.getAccount() != null) {
            if ("check".equals(commandDto.getCommand())) {

                String clientId = null;
                PluginRegister billingPluginRegister = pluginService.getPluginRegister(StaticValues.billingPlugin);
                if (commandDto.getUsername() != null && billingPluginRegister != null) {
                    ObjectNode node = this.objectMapper.createObjectNode();
                    node.put("command", "getParkomatClientId");
                    node.put("parkomatId", commandDto.getUsername());
                    JsonNode clientIdResult = billingPluginRegister.execute(node);

                    clientId = clientIdResult.get("clientId").textValue();
                }

                CarState carState = carStateService.getLastNotLeft(commandDto.getAccount());
                if (carState == null) {

                    JsonNode currentBalanceResult = getCurrentBalance(commandDto.getAccount());
                    if (currentBalanceResult.has("currentBalance") && BigDecimal.ZERO.compareTo(currentBalanceResult.get("currentBalance").decimalValue()) > 0) {
                        return fillDebtDetails(commandDto, currentBalanceResult.get("currentBalance").decimalValue(), clientId);
                    }

                    BillingInfoErrorDto dto = new BillingInfoErrorDto();
                    dto.sum = BigDecimal.ZERO;
                    dto.txn_id = commandDto.getTxn_id();
                    dto.message = languagePropertiesService.getMessageFromProperties(MessageKey.BILLING_ERROR_INCORRECT_PLATENUMBER);
                    dto.result = 1;
                    savePaymentCheckLog(commandDto.getAccount(), null, dto.sum, null, PaymentCheckLog.PaymentCheckType.NOT_FOUND, dto.current_balance, commandDto.getTxn_id(), clientId);
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

                        long leftFreeMinutes = paymentOfflineResult.left_free_time_minutes;

                        parkomatBillingInfoSuccessDto.setLeft_free_time_minutes((leftFreeMinutes > 0) ? (int) leftFreeMinutes : 0);
                        if (qrPanelService != null) {
                            parkomatBillingInfoSuccessDto.setKaspiQr(qrPanelService.generateUrl(null, carState.getCarNumber()));
                        }
                        savePaymentCheckLog(commandDto.getAccount(), null, dto.sum, carState.getId(), PaymentCheckLog.PaymentCheckType.STANDARD, dto.current_balance, commandDto.getTxn_id(), clientId);
                        return parkomatBillingInfoSuccessDto;
                    }

                    savePaymentCheckLog(commandDto.getAccount(), null, dto.sum, carState.getId(), PaymentCheckLog.PaymentCheckType.STANDARD, dto.current_balance, commandDto.getTxn_id(), clientId);
                    return dto;
                }
            } else if ("pay".equals(commandDto.getCommand())) {
                if (commandDto.getTxn_id() == null || commandDto.getTxn_id().isEmpty()) {
                    BillingInfoErrorDto dto = new BillingInfoErrorDto();
                    dto.message = languagePropertiesService.getMessageFromProperties(MessageKey.BILLING_ERROR_NULL_TXN);
                    dto.result = 4;
                    dto.sum = commandDto.getSum();
                    dto.txn_id = commandDto.getTxn_id();
                    return dto;
                }
                if (commandDto.getSum() == null || BigDecimal.ZERO.compareTo(commandDto.getSum()) > -1) {
                    BillingInfoErrorDto dto = new BillingInfoErrorDto();
                    dto.message = languagePropertiesService.getMessageFromProperties(MessageKey.BILLING_ERROR_INCORRECT_SUM);
                    dto.result = 5;
                    dto.sum = commandDto.getSum();
                    dto.txn_id = commandDto.getTxn_id();
                    return dto;
                }
                CarState carState = carStateService.getLastNotLeft(commandDto.getAccount());
                CarState lastDebtCarState = carStateService.getLastCarState(commandDto.getAccount());
                if (carState == null && lastDebtCarState != null)
                    carState = lastDebtCarState;

                if (carState == null ) {
                    BillingInfoErrorDto dto = new BillingInfoErrorDto();
                    dto.message = languagePropertiesService.getMessageFromProperties(MessageKey.BILLING_ERROR_INCORRECT_PLATENUMBER);
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
                        node.put("paymentType", "ikkm_payment".equals(commandDto.getType()) ? 1 : 0);

                        Cars cars = carService.findByPlatenumberWithCustomer(commandDto.getAccount());
                        if (cars != null && cars.getCustomer() != null) {
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
                        if (result.has("paymentError")) {
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

                        if (commandDto.getIkkmOnline())
                            carState.setCashlessPayment(true);
                        else
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
                log.info("Webkassa check request " + commandDto);
                PluginRegister billingPluginRegister = pluginService.getPluginRegister(StaticValues.billingPlugin);
                ObjectNode node = this.objectMapper.createObjectNode();
                node.put("command", "getCheck");
                node.put("parkomatId", commandDto.getParkomat());
                node.put("sum", commandDto.getSum());
                node.put("change", commandDto.getChange());
                node.put("txn_id", commandDto.getTxn_id());
                node.put("operationName", "Оплата парковки, ГРНЗ: " + commandDto.getAccount());
                node.put("paymentType", "ikkm_payment".equals(commandDto.getType()) ? 1 : 0);
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
            node.put("carType", rateQueryDto.dimensionId);
            JsonNode result = ratePluginRegister.execute(node);
            return result.get("rateResult").decimalValue().setScale(2);
        }
        return null;
    }

    @Override
    public void createDebtAndOUTState(String carNumber, Camera camera, Map<String, Object> properties) throws Exception {

        CarState carState = carStateService.getLastNotLeft(carNumber);
        if (carState != null && carState.getPaid()) {
            PluginRegister ratePluginRegister = pluginService.getPluginRegister(StaticValues.ratePlugin);
            BigDecimal rateResult = BigDecimal.ZERO;
            if (ratePluginRegister != null) {
                SimpleDateFormat format = new SimpleDateFormat(StaticValues.dateFormatTZ);

                ObjectNode ratePluginNode = this.objectMapper.createObjectNode();
                ratePluginNode.put("parkingId", camera.getGate().getParking().getId());
                ratePluginNode.put("inDate", format.format(carState.getInTimestamp()));
                ratePluginNode.put("outDate", format.format(new Date()));
                ratePluginNode.put("plateNumber", carNumber);
                ratePluginNode.put("cashlessPayment", carState.getCashlessPayment() != null ? carState.getCashlessPayment() : true);
                ratePluginNode.put("isCheck", false);
                ratePluginNode.put("paymentsJson", carState.getPaymentJson());

                Cars cars = carService.findByPlatenumber(carState.getCarNumber());
                if (cars.getModel() != null && !cars.getModel().equals("")) {
                    ratePluginNode.put("carModel", cars.getModel());
                    if (carModelService.getByModel(cars.getModel()) != null) {
                        CarModel carModel = carModelService.getByModel(cars.getModel());
                        ratePluginNode.put("carType", carModel.getDimensions().getId());
                    } else {
                        log.info("This model doesn't exist in db - " + cars.getModel());
                    }
                } else {
                    log.info("Car record doesn't exist in database");
                }

                JsonNode ratePluginResult = ratePluginRegister.execute(ratePluginNode);
                rateResult = ratePluginResult.get("rateResult").decimalValue().setScale(2);
                carState.setRateAmount(rateResult);

                PluginRegister billingPluginRegister = pluginService.getPluginRegister(StaticValues.billingPlugin);
                if (billingPluginRegister != null && BigDecimal.ZERO.compareTo(rateResult) != 0) {
                    ObjectNode billingSubtractNode = this.objectMapper.createObjectNode();

                    Map<String, Object> messageValues =new HashMap<>();
                    messageValues.put("parking", carState.getParking().getName());
                    Map<String, String> messages = languagePropertiesService.getWithDifferentLanguages(MessageKey.BILLING_REASON_PAYMENT_PARKING, messageValues);

                    billingSubtractNode.put("command", "decreaseCurrentBalance");
                    billingSubtractNode.put("amount", rateResult);
                    billingSubtractNode.put("plateNumber", carState.getCarNumber());
                    billingSubtractNode.put("parkingName", carState.getParking().getName());
                    billingSubtractNode.put("reason", messages.get(Language.RU));
                    billingSubtractNode.put("reasonEn", messages.get(Language.EN));
                    billingSubtractNode.put("reasonLocal", messages.get(Language.LOCAL));
                    billingSubtractNode.put("carStateId", carState.getId());
                    billingSubtractNode.put("provider", "Parking fee");
                    billingPluginRegister.execute(billingSubtractNode).get("currentBalance").decimalValue();
                }
            }
            carState.setCarOutType(CarState.CarOutType.DEBT_OUT);
            carStateService.createOUTState(carNumber, new Date(), camera, carState, properties.containsKey(StaticValues.carSmallImagePropertyName) ? properties.get(StaticValues.carSmallImagePropertyName).toString() : null);

            Map<String, Object> messageValues = new HashMap<>();
            messageValues.put("platenumber", carNumber);
            messageValues.put("rateResult", rateResult);

            eventLogService.sendSocketMessage(EventLogService.ArmEventType.CarEvent, EventLog.StatusType.Allow, camera.getId(), carNumber, messageValues, MessageKey.ALLOWED_WITH_DEBT);
            eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), properties, messageValues, MessageKey.ALLOWED_WITH_DEBT, EventLog.EventType.DEBT_OUT);
        }
    }

    @Override
    public JsonNode findAllByCreatedBetweenAndProviderName(
            LocalDateTime dateFrom,
            LocalDateTime dateTo,
            Boolean onlyTransactionId,
            String providerName) throws Exception {

        ObjectNode node = this.objectMapper.createObjectNode();

        node.put("command", "listOfPaymentsBetween2Date");
        node.put("dateFrom", String.valueOf(dateFrom));
        node.put("dateTo", String.valueOf(dateTo));
        node.put("onlyTransactionId", onlyTransactionId);
        node.put("providerName", providerName);

        PluginRegister billingPluginRegister = pluginService.getPluginRegister(StaticValues.billingPlugin);

        if (billingPluginRegister != null) {
            return billingPluginRegister.execute(node);
        } else {
            throw new RuntimeException("listOfPaymentsBetween2Date billingPluginRegister FAILED");
        }
    }


    @Override
    public JsonNode findFirstByTransactionAndProviderNameAndCreated(
            String transactionId,
            String providerName,
            LocalDateTime transactionTime) throws Exception {
        ObjectNode node = this.objectMapper.createObjectNode();

        node.put("command", "findFirstByTransactionAndProviderNameAndCreated");
        node.put("transactionId", transactionId);
        node.put("providerName", providerName);
        node.put("transactionTime", String.valueOf(transactionTime));

        PluginRegister billingPluginRegister = pluginService.getPluginRegister(StaticValues.billingPlugin);

        if (billingPluginRegister != null) {
            return billingPluginRegister.execute(node);
        } else {
            throw new RuntimeException("findFirstByTransactionAndProviderNameAndCreated billingPluginRegister FAILED");
        }
    }

    @Override
    public void cancelTransactionByTrxId(String transactionId, String reason) throws Exception {
        ObjectNode node = this.objectMapper.createObjectNode();

        node.put("command", "cancelPayment");
        node.put("transactionId", transactionId);
        node.put("reason", reason);

        PluginRegister billingPluginRegister = pluginService.getPluginRegister(StaticValues.billingPlugin);

        if (billingPluginRegister != null) {
            billingPluginRegister.execute(node);
        } else {
            throw new RuntimeException("cancelPayment billingPluginRegister FAILED");
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
            node.put("plateNumber", carState.getCarNumber());
            node.put("isCheck", true);
            node.put("cashlessPayment", carState.getCashlessPayment() != null ? carState.getCashlessPayment() : true);
            node.put("paymentsJson", paymentsJson);

            Cars cars = carService.findByPlatenumber(carState.getCarNumber());
            if (cars.getModel() != null && !cars.getModel().equals("")) {
                node.put("carModel", cars.getModel());
                if (carModelService.getByModel(cars.getModel()) != null) {
                    CarModel carModel = carModelService.getByModel(cars.getModel());
                    node.put("carType", carModel.getDimensions().getId());
                } else {
                    log.info("This model doesn't exist in db - " + cars.getModel());
                }
            } else {
                log.info("Car record doesn't exist in database");
            }
            log.info("it checks in payment service imple " + node.get("carModel"));
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
        if (currentBalanceResult.has("currentBalance")) {
            dto.current_balance = currentBalanceResult.get("currentBalance").decimalValue().setScale(2);
        }
        savePaymentCheckLog(commandDto.account, null, dto != null ? dto.sum : null, carState.getId(), PaymentCheckLog.PaymentCheckType.STANDARD, dto != null ? dto.current_balance : null
                , commandDto.getTxn_id(), commandDto.getClientId());
        return dto;
    }

    private BillingInfoSuccessDto fillPrepaid(CommandDto commandDto, Parking parking) throws Exception {
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

    private BillingPaymentSuccessDto successPayment(CommandDto commandDto, Long paymentId) {
        BillingPaymentSuccessDto dto = new BillingPaymentSuccessDto();
        dto.result = 0;
        dto.sum = commandDto.sum;
        dto.txn_id = commandDto.txn_id;
        dto.payment_id = paymentId.toString();
        return dto;
    }

    private BillingInfoErrorDto fillError(CommandDto commandDto, String message, Integer result) {
        BillingInfoErrorDto dto = new BillingInfoErrorDto();
        dto.sum = commandDto.sum;
        dto.txn_id = commandDto.txn_id;
        dto.message = message;
        dto.result = result;
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

    private Object savePayment(CommandDto commandDto, CarState carState, Parking.ParkingType parkingType, Boolean isAbonomentPayment) throws Exception {
        ObjectNode node = this.objectMapper.createObjectNode();
        node.put("command", "savePayment");
        node.put("carNumber", commandDto.account);
        node.put("sum", commandDto.sum);
        node.put("transaction", commandDto.txn_id);
        node.put("discount", commandDto.discount);
        node.put("discountedSum", commandDto.discountedSum);

        Long parkingId = null;
        if (carState != null) {
            node.put("carStateId", carState.getId());
            node.put("inDate", format.format(carState.getInTimestamp()));

            parkingId = carState.getParking().getId();
        } else if (isAbonomentPayment) {
            node.put("inDate", format.format(new Date()));
            parkingId = getNotPaidAbonoment(commandDto).get("parkingId").longValue();
        } else {
            node.put("inDate", format.format(new Date()));

            if (parkingType != null) {
                Parking parking = parkingService.findByType(Parking.ParkingType.PREPAID);
                if (parking != null) {
                    parkingId = parking.getId();
                }
            }
        }

        if (parkingId != null) {
            node.put("parkingId", parkingId);

            JsonNode rateValue = getRateByParking(parkingId);
            if (rateValue.has("rateName")) {
                node.put("rateName", rateValue.get("rateName").textValue());
            }
            if (rateValue.has("rateId")) {
                node.put("rateId", rateValue.get("rateId").longValue());
//            if (commandDto.service_id != null && commandDto.service_id!=3) {
//                JsonNode rateValue = getRateByParking(parkingId);
//                if (rateValue.has("rateName")) {
//                    node.put("rateName", rateValue.get("rateName").textValue());
//                }
//                if (rateValue.has("rateId")) {
//                    node.put("rateId", rateValue.get("rateId").longValue());
//                }
//            } else {
//                log.info("No Parking Rate !");
            }
        } else {
            if (parkingType.equals(Parking.ParkingType.PREPAID)) {
                return fillError(commandDto, languagePropertiesService.getMessageFromProperties(MessageKey.BILLING_PREPAID_NOT_FOUND), 4);
            }
            return fillError(commandDto, languagePropertiesService.getMessageFromProperties(MessageKey.BILLING_NOT_FOUND_PARKING), 7);
        }

        node.put("clientId", getClientId(commandDto.clientId));

        Cars cars = carService.findByPlatenumberWithCustomer(commandDto.account);
        if (cars != null && cars.getCustomer() != null) {
            node.put("customerId", cars.getCustomer().getId());
        }

        JsonNode paymentResult = null;
        PluginRegister billingPluginRegister = pluginService.getPluginRegister(StaticValues.billingPlugin);
        if (billingPluginRegister != null) {
            paymentResult = billingPluginRegister.execute(node);
        }

        if (paymentResult != null && paymentResult.has("paymentError")) {
            return fillError(commandDto, paymentResult.get("paymentError").textValue(), paymentResult.get("paymentErrorCode").intValue());
        } else {
//          Request check number of succeeded payments from WebKassa | Enable only for airport of Astana
            if (mobilePayFiscalization) {
                log.info("Webkassa check request " + commandDto);
                ObjectNode objectNode = this.objectMapper.createObjectNode();
                objectNode.put("command", "getCheck");
                objectNode.put("parkomatId", commandDto.getClientId());
                objectNode.put("sum", commandDto.getSum());
                objectNode.put("change", 0);
                objectNode.put("txn_id", commandDto.getTxn_id());
                objectNode.put("operationName", "Оплата парковки, ГРНЗ: " + commandDto.getAccount());
                objectNode.put("paymentType", 4);
                if (billingPluginRegister != null) {
                    billingPluginRegister.execute(objectNode);
                }
            }
        }
        return paymentResult;
    }

    private Object saveDebtPayment(CommandDto commandDto) throws Exception {
        ObjectNode node = this.objectMapper.createObjectNode();
        node.put("command", "savePayment");
        node.put("carNumber", commandDto.account);
        node.put("sum", commandDto.sum);
        node.put("transaction", commandDto.txn_id);
        node.put("rateName", "Оплата долга");

        node.put("clientId", getClientId (commandDto.clientId));

        Cars cars = carService.findByPlatenumberWithCustomer(commandDto.account);
        if (cars != null && cars.getCustomer() != null) {
            node.put("customerId", cars.getCustomer().getId());
        }

        JsonNode paymentResult = null;
        PluginRegister billingPluginRegister = pluginService.getPluginRegister(StaticValues.billingPlugin);
        if (billingPluginRegister != null) {
            paymentResult = billingPluginRegister.execute(node);
        }

        if (paymentResult.has("paymentError")) {
            return fillError(commandDto, paymentResult.get("paymentError").textValue(), paymentResult.get("paymentErrorCode").intValue());
        }
        return paymentResult;
    }

    private JsonNode getNotPaidAbonoment(CommandDto commandDto) throws Exception {
        PluginRegister abonomentPluginRegister = pluginService.getPluginRegister(StaticValues.abonementPlugin);
        if (abonomentPluginRegister != null) {
            ObjectNode node = this.objectMapper.createObjectNode();
            node.put("command", "hasUnpaidNotExpiredAbonoment");
            node.put("plateNumber", commandDto.account);

            JsonNode result = abonomentPluginRegister.execute(node);
            if (result.has("unPaidNotExpiredAbonoment")) {
                return result.get("unPaidNotExpiredAbonoment");
            }
        }
        return null;
    }

    private BillingInfoSuccessDto fillAbonomentDetails(JsonNode abonomentJsonNode, CommandDto commandDto) throws Exception {
        BillingInfoSuccessDto billingInfoSuccessDto = new BillingInfoSuccessDto();
        billingInfoSuccessDto.hours = 0;
        billingInfoSuccessDto.tariff = "Оплата за абономент";
        billingInfoSuccessDto.txn_id = commandDto.txn_id;
        billingInfoSuccessDto.sum = abonomentJsonNode.get("price").decimalValue().setScale(2);
        billingInfoSuccessDto.left_free_time_minutes = 0;
        billingInfoSuccessDto.in_date = "";
        billingInfoSuccessDto.result = 0;
        JsonNode currentBalanceResult = getCurrentBalance(commandDto.account);
        if (currentBalanceResult.has("currentBalance")) {
            billingInfoSuccessDto.current_balance = currentBalanceResult.get("currentBalance").decimalValue().setScale(2);
        }

        savePaymentCheckLog(commandDto.account, null, billingInfoSuccessDto.sum, null, PaymentCheckLog.PaymentCheckType.ABONEMENT, billingInfoSuccessDto.current_balance
                , commandDto.getTxn_id(), commandDto.getClientId());

        return billingInfoSuccessDto;
    }

    private void setAbonomentPaid(Long id) throws Exception {
        PluginRegister abonomentPluginRegister = pluginService.getPluginRegister(StaticValues.abonementPlugin);
        if (abonomentPluginRegister != null) {
            ObjectNode node = this.objectMapper.createObjectNode();
            node.put("command", "setAbonomentPaid");
            node.put("id", id);

            abonomentPluginRegister.execute(node);
        }
    }

    private BillingInfoSuccessDto checkAbonomentExtraPayment(CommandDto commandDto, CarState carState, SimpleDateFormat format, JsonNode abonementJson, BillingInfoSuccessDto dto) throws Exception {

        JsonNode currentBalanceResult = getCurrentBalance(commandDto.account);
        dto.current_balance = currentBalanceResult.get("currentBalance").decimalValue();
        dto.left_free_time_minutes = 0;

        Date inDate = carState.getInTimestamp();
        Date outDate = new Date();

        List<Period> periods = abonomentService.calculatePaymentPeriods(abonementJson, inDate, outDate);

        if (periods.size() == 0) {
            return dto;
        } else {
            BigDecimal totalRate = BigDecimal.ZERO;
            for (Period p : periods) {
                PluginRegister ratePluginRegister = pluginService.getPluginRegister(StaticValues.ratePlugin);
                if (ratePluginRegister != null) {
                    ObjectNode ratePluginNode = this.objectMapper.createObjectNode();
                    ratePluginNode.put("parkingId", carState.getParking().getId());
                    ratePluginNode.put("inDate", format.format(p.getStart()));
                    ratePluginNode.put("outDate", format.format(p.getEnd()));
                    ratePluginNode.put("cashlessPayment", carState.getCashlessPayment() != null ? carState.getCashlessPayment() : true);
                    ratePluginNode.put("isCheck", false);
                    ratePluginNode.put("paymentsJson", carState.getPaymentJson());

                    JsonNode ratePluginResult = ratePluginRegister.execute(ratePluginNode);
                    BigDecimal rate = ratePluginResult.get("rateResult").decimalValue().setScale(2);
                    dto.tariff = ratePluginResult.get("rateName") != null ? ratePluginResult.get("rateName").textValue() : "";
                    dto.hours = ratePluginResult.get("payed_till") != null ? (int) ratePluginResult.get("payed_till").longValue() : 0;
                    log.info("payment service abonements calculated rate = " + rate + " for period: begin: " + p.getStart() + " end: " + p.getEnd());
                    totalRate = totalRate.add(rate);
                }
            }

            if (totalRate.equals(BigDecimal.ZERO)) {
                return dto;
            } else {
                dto.sum = totalRate;
            }
        }
        savePaymentCheckLog(commandDto.account, null, dto.sum, carState.getId(), PaymentCheckLog.PaymentCheckType.STANDARD,  dto.current_balance, commandDto.getTxn_id(), commandDto.getClientId());
        return dto;
    }

    private BillingInfoSuccessDto checkWhiteListExtraPayment(CommandDto commandDto, CarState carState, SimpleDateFormat format, JsonNode whiteListJson, BillingInfoSuccessDto dto) throws Exception {

        JsonNode currentBalanceResult = getCurrentBalance(commandDto.account);
        dto.current_balance = currentBalanceResult.get("currentBalance").decimalValue();
        dto.left_free_time_minutes = 0;

        Date inDate = carState.getInTimestamp();
        Date outDate = new Date();

        List<Period> periods = whitelistRootService.calculatePaymentPeriods(whiteListJson, inDate, outDate);

        if (periods.size() == 0) {
            return dto;
        } else {
            BigDecimal totalRate = BigDecimal.ZERO;
            for (Period p : periods) {
                PluginRegister ratePluginRegister = pluginService.getPluginRegister(StaticValues.ratePlugin);
                if (ratePluginRegister != null) {
                    ObjectNode ratePluginNode = this.objectMapper.createObjectNode();
                    ratePluginNode.put("parkingId", carState.getParking().getId());
                    ratePluginNode.put("inDate", format.format(p.getStart()));
                    ratePluginNode.put("outDate", format.format(p.getEnd()));
                    ratePluginNode.put("cashlessPayment", carState.getCashlessPayment() != null ? carState.getCashlessPayment() : true);
                    ratePluginNode.put("isCheck", true);
                    ratePluginNode.put("paymentsJson", carState.getPaymentJson());

                    JsonNode ratePluginResult = ratePluginRegister.execute(ratePluginNode);
                    BigDecimal rate = ratePluginResult.get("rateResult").decimalValue().setScale(2);
                    dto.tariff = ratePluginResult.get("rateName") != null ? ratePluginResult.get("rateName").textValue() : "";
                    dto.hours = ratePluginResult.get("payed_till") != null ? (int) ratePluginResult.get("payed_till").longValue() : 0;

                    log.info("payment service whitelist calculated rate = " + rate + "  period: begin: " + p.getStart() + " end: " + p.getEnd());
                    totalRate = totalRate.add(rate);
                }
            }

            if (totalRate.equals(BigDecimal.ZERO)) {
                return dto;
            } else {
                dto.sum = totalRate;
            }
        }
        savePaymentCheckLog(commandDto.account, null, dto.sum, carState.getId(), PaymentCheckLog.PaymentCheckType.STANDARD, dto.current_balance, commandDto.getTxn_id(), commandDto.getClientId());
        return dto;
    }

//    private void savePaymentCheckLog(String plateNumber, String message, BigDecimal summ, Long carStateId, PaymentCheckLog.PaymentCheckType paymentCheckType, BigDecimal currentBalance){
//        PaymentCheckLog log = new PaymentCheckLog(plateNumber, message, summ, carStateId, paymentCheckType, currentBalance);
//        paymentCheckLogService.save(log);
//    };


    private void savePaymentCheckLog(String plateNumber,
                                     String message,
                                     BigDecimal summ,
                                     Long carStateId,
                                     PaymentCheckLog.PaymentCheckType paymentCheckType,
                                     BigDecimal currentBalance,
                                     String transaction,
                                     String clientId) {
        try {
            JsonNode result = null;
            try {
                clientId = getClientId(clientId);

                ObjectNode node = this.objectMapper.createObjectNode();
                node.put("command", "getPaymentProvider");
                node.put("clientId", clientId);
                PluginRegister billingPluginRegister = pluginService.getPluginRegister(StaticValues.billingPlugin);
                result = billingPluginRegister.execute(node);
            } catch (Exception e){
                log.warning("Error getting client id for payment check log " +
                        "clientId: " + clientId + ", carStateId: " + carStateId + ", plateNumber: " + plateNumber + ". error msg +" + e.getMessage());
            }

            Long providerId = null;
            String providerName = null;
            if (result != null && !ObjectUtils.isEmpty(result)) {
                providerId = result.get("providerId").longValue();
                providerName = result.get("providerName").textValue();
            }
            PaymentCheckLog log = new PaymentCheckLog(plateNumber, message, summ, carStateId, paymentCheckType, currentBalance, transaction, providerName, providerId);
            paymentCheckLogService.save(log);
        } catch (Exception e) {
            log.warning("Error save paymentCheckLog " +
                    "carStateId: " + carStateId + ", plateNumber: " + plateNumber + ". error msg +" + e.getMessage());
        }

    }


    private String getClientId(String commandDtoClientId) {
        try {
            Authentication a = SecurityContextHolder.getContext().getAuthentication();
            String clientId = ((OAuth2Authentication) a).getOAuth2Request().getClientId();
            if (!ObjectUtils.isEmpty(clientId) && clientId.equals("gateway")) {
                clientId = commandDtoClientId;
            }
            return clientId;
        } catch (Exception e) {
            return commandDtoClientId;
        }
    }
}
