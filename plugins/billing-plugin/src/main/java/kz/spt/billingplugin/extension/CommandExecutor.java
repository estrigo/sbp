package kz.spt.billingplugin.extension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.spt.billingplugin.BillingPlugin;
import kz.spt.billingplugin.model.Payment;
import kz.spt.billingplugin.model.PaymentProvider;
import kz.spt.billingplugin.model.PaymentProviderAbstract;
import kz.spt.billingplugin.model.dto.OfdCheckData;
import kz.spt.billingplugin.model.dto.PaymentApiDto;
import kz.spt.billingplugin.model.dto.PaymentDto;
import kz.spt.billingplugin.model.dto.PaymentStatusApiDto;
import kz.spt.billingplugin.model.dto.rekassa.RekassaCheckRequest;
import kz.spt.billingplugin.model.dto.webkassa.AuthRequestDTO;
import kz.spt.billingplugin.model.dto.webkassa.Check;
import kz.spt.billingplugin.model.dto.webkassa.Position;
import kz.spt.billingplugin.model.dto.webkassa.ZReport;
import kz.spt.billingplugin.repository.PaymentProviderRepository;
import kz.spt.billingplugin.repository.PaymentRepository;
import kz.spt.billingplugin.service.*;
import kz.spt.billingplugin.service.impl.ReKassaServiceImpl;
import kz.spt.billingplugin.service.impl.WebKassaServiceImpl;
import kz.spt.lib.extension.PluginRegister;
import kz.spt.lib.model.CarState;
import kz.spt.lib.model.Customer;
import kz.spt.lib.model.Parking;
import kz.spt.lib.utils.StaticValues;
import lombok.extern.java.Log;
import org.pf4j.Extension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Log
@Extension
public class CommandExecutor implements PluginRegister {

    private PaymentProviderService paymentProviderService;
    private PaymentService paymentService;
    private RootServicesGetterService rootServicesGetterService;
    private BalanceService balanceService;
    private WebKassaService webKassaService;
    private WebKassaService reKassaService;
    private PaymentRepository paymentRepository;
    private PaymentProviderRepository paymentProviderRepository;

    private static final String TRANSACTION_ID = "transactionId";

    private final SimpleDateFormat simpleDateFormatWithoutZone = new SimpleDateFormat(StaticValues.dateOnly);

    @Override
    public JsonNode execute(JsonNode command) throws Exception {

        SimpleDateFormat format = new SimpleDateFormat(StaticValues.dateFormatTZ);
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode node = objectMapper.createObjectNode();

        if (command != null && command.has("command")) {
            String commandName = command.get("command").textValue();
            if ("getPasswordHash".equals(commandName)) {
                if (command.has("client_id") && command.get("client_id").isTextual()) {
                    PaymentProvider paymentProvider = getPaymentProviderService().getProviderByClientId(command.get("client_id").textValue());
                    if (paymentProvider != null && paymentProvider.getEnabled() && paymentProvider.getSecret() != null) {
                        node.put("passwordHash", paymentProvider.getSecret());
                    }
                } else {
                    throw new RuntimeException("Not all getPasswordHash parameters set");
                }
            } else if ("checkProvider".equals(commandName)) {
                PaymentProvider paymentProvider = getPaymentProviderService().getProviderByClientId(command.get("providerName").textValue());
                node.put("providerExists", paymentProvider != null ? true : false);
            } else if ("savePayment".equals(commandName)) {
                PaymentProvider provider = getPaymentProviderService().getProviderByClientId(command.get("clientId").textValue());
                List<Payment> oldPayments = getPaymentService().findByTransactionAndProvider(command.get("transaction").textValue(), provider);

                if (oldPayments.size() > 0) {
                    node.put("paymentError", "платеж с таким ID транзакции уже оплачен");
                    node.put("paymentErrorCode", 10);
                    return node;
                }

                Payment payment = new Payment();
                payment.setCarNumber(command.get("carNumber").textValue());
                payment.setPrice(command.get("sum").decimalValue());
                payment.setProvider(provider);
                payment.setTransaction(command.get("transaction").textValue());
                if (command.has("parkingId")) {
                    Parking parking = getRootServicesGetterService().getParkingService().findById(command.get("parkingId").longValue());
                    payment.setParking(parking);
                }
                if (command.has("customerId")) {
                    Customer customer = getRootServicesGetterService().getCustomerService().findById(command.get("customerId").longValue());
                    payment.setCustomer(customer);
                }
                payment.setInDate(command.get("inDate").textValue() != null ? format.parse(command.get("inDate").textValue()) : null);
                payment.setRateDetails(command.has("rateName") ? command.get("rateName").textValue() : "");
                payment.setCarStateId(command.has("carStateId") ? command.get("carStateId").longValue() : null);

                payment.setIkkm(command.has("paymentType") ? command.get("paymentType").intValue() == 1 : false);

                Payment savedPayment = getPaymentService().savePayment(payment);
                node.put("paymentId", savedPayment.getId());
                node.put("cashlessPayment", payment.getProvider().getCashlessPayment() != null ? payment.getProvider().getCashlessPayment() : false);

                String carNumber = command.get("carNumber").textValue();
                BigDecimal sum = command.get("sum").decimalValue();
                Long carStateId = command.has("carStateId") ? command.get("carStateId").longValue() : null;
                getBalanceService().addBalance(carNumber, sum, carStateId, "Received payment from " + payment.getProvider().getName(), "Получен платеж от " + payment.getProvider().getName(), payment.getProvider().getName());

                if (savedPayment.getCarStateId() != null) {
                    List<Payment> carStatePayments = getPaymentService().getPaymentsByCarStateId(savedPayment.getCarStateId());
                    ArrayNode paymentArray = PaymentDto.arrayNodeFromPayments(carStatePayments);
                    node.set("paymentArray", paymentArray);
                }

            } else if ("saveOnlyPayment".equals(commandName)) {
                PaymentProvider paymentProvider = getRootServicesGetterService().getPaymentProviderRepository().
                        findByClientId("ThirdParty");
                if (paymentProvider == null) {
                    paymentProvider = new PaymentProvider();
                    paymentProvider.setName("ThirdParty");
                    paymentProvider.setClientId("ThirdParty");
                    paymentProvider.setProvider("ThirdParty");
                    paymentProvider.setOfdProviderType(PaymentProviderAbstract.OFD_PROVIDER_TYPE.WebKassa);
                    paymentProvider.setEnabled(true);
                    paymentProvider.setCashlessPayment(true);
                    getPaymentProviderRepository().save(paymentProvider);
                }
                CarState carState = getRootServicesGetterService().getCarStateService().getLastCarState(command.get("carNumber").textValue());
                Payment payment = new Payment();
                payment.setCarNumber(command.get("carNumber").textValue());
                payment.setCarStateId(carState.getId());
                payment.setCreated(format.parse(command.get("entryDate").textValue()));
                payment.setInDate(format.parse(command.get("entryDate").textValue()));
                payment.setOutDate(format.parse(command.get("exitDate").textValue()));
                payment.setPrice(command.get("rateResult").decimalValue());
                payment.setProvider(paymentProvider);
                payment.setParking(carState.getParking());
                getPaymentRepository().save(payment);
            } else if ("getCurrentBalance".equals(commandName)) {
                if (command.has("plateNumber")) {
                    node.put("currentBalance", getBalanceService().getBalance(command.get("plateNumber").textValue()));
                } else {
                    throw new RuntimeException("Not all getCurrentBalance parameters set");
                }
            } else if ("decreaseCurrentBalance".equals(commandName)) {
                if (command.has("plateNumber") && command.has("amount") && command.has("reason")
                        && command.has("reasonEn") && command.has("provider")) {
                    String reason = command.get("reason").textValue();
                    String reasonEn = command.get("reasonEn").textValue();
                    String provider = command.get("provider").textValue();
                    node.put("currentBalance", getBalanceService().subtractBalance(command.get("plateNumber").textValue(), command.get("amount").decimalValue(), command.has("carStateId") ? command.get("carStateId").longValue() : null, reasonEn, reason, provider));
                } else {
                    throw new RuntimeException("Not all decreaseCurrentBalance parameters set");
                }
            } else if ("increaseCurrentBalance".equals(commandName)) {
                if (command.has("plateNumber") && command.has("amount") && command.has("reason")
                        && command.has("reasonEn") && command.has("provider")) {
                    String plateNumber = command.get("plateNumber").textValue();
                    BigDecimal amount = command.get("amount").decimalValue();
                    String reason = command.get("reason").textValue();
                    String reasonEn = command.get("reasonEn").textValue();
                    String provider = command.get("provider").textValue();
                    node.put("currentBalance", getBalanceService().addBalance(plateNumber, amount, null, reason, reasonEn, provider));
                } else {
                    throw new RuntimeException("Not all increaseCurrentBalance parameters set");
                }
            } else if ("addOutTimestampToPayments".equals(commandName)) {
                if (command.has("outTimestamp") && command.has("carStateId")) {
                    getPaymentService().updateOutTimestamp(command.get("carStateId").longValue(), format.parse(command.get("outTimestamp").textValue()));
                } else {
                    throw new RuntimeException("Not all addOutTimestampToPayments parameters set");
                }
            } else if ("getParkomatClientId".equals(commandName)) {
                PaymentProvider provider = getPaymentProviderService().getProviderByClientId(command.get("parkomatId").textValue());
                node.put("clientId", provider.getClientId());
            } else if ("getCheck".equals(commandName)) {

                PaymentProvider provider = getPaymentProviderService().getProviderByClientId(command.get("parkomatId").textValue());
                int paymentType = command.get("paymentType").intValue();
                String txn_id = command.get("txn_id").textValue();

                OfdCheckData ofdCheckData = registerCheck(provider, command);

                log.info("Check Response " + ofdCheckData.toString());

                if (ofdCheckData != null && ofdCheckData.getCheckNumber() != null) {
                    node.put("checkNumber", ofdCheckData.getCheckNumber());
                    node.put("ticketUrl", ofdCheckData.getCheckUrl());
                    List<Payment> paymentList = getPaymentService().findByTransactionAndProvider(txn_id, provider);
                    if (!paymentList.isEmpty()) {
                        Payment payment = paymentList.get(0);
                        payment.setCheckNumber(ofdCheckData.getCheckNumber());
                        payment.setCheckUrl(ofdCheckData.getCheckUrl());
                        payment.setIkkm(paymentType == 1);
                        getPaymentService().savePayment(payment);
                    }

                }


            } else if ("zReport".equals(commandName)) {

                PaymentProvider provider = getPaymentProviderService().getProviderByClientId(command.get("parkomatId").textValue());

                ZReport zReport = new ZReport();
                zReport.setCashboxUniqueNumber(provider.getWebKassaID());
                String checkResponse = getWebKassaService().closeOperationDay(zReport, provider);

                if (checkResponse != null) {
                    node.put("result", checkResponse);
                }
            } else if ("getProviderNames".equals(commandName)) {
                ArrayNode paymentProviders = objectMapper.createArrayNode();
                getPaymentProviderService().listAllPaymentProviders().forEach(paymentProvider -> {
                    paymentProviders.add(paymentProvider.getName());
                });
                node.set("providerNames", paymentProviders);
            } else if ("getPayments".equals(commandName)) {
                ArrayNode payments = objectMapper.createArrayNode();
                if (command.has("carStateIds")) {
                    ArrayNode carStateIds = (ArrayNode) command.get("carStateIds");
                    List<Long> ids = new ArrayList<>(carStateIds.size());
                    carStateIds.forEach(jsonNode -> ids.add(jsonNode.longValue()));
                    List<Payment> paymentList = getPaymentService().getPaymentsByCarStateId(ids);
                    if (!paymentList.isEmpty()) {
                        paymentList.stream()
                                .map(m -> objectMapper.createObjectNode()
                                        .put("paymentId", m.getId())
                                        .put("carStateId", m.getCarStateId())
                                        .put("sum", m.getPrice().toString())
                                        .put("provider", m.getProvider() != null ? m.getProvider().getName() : "")
                                        .put("cashlessPayment", m.getProvider() != null ? m.getProvider().getCashlessPayment() : false)
                                        .put("rate", m.getRateDetails()))
                                .forEach(m -> {
                                    payments.add(m);
                                });
                    }
                } else {
                    var result = (List<Payment>) getPaymentService().getPaymentsByCarStateId(command.get("carStateId").longValue());
                    if (!result.isEmpty()) {
                        result.stream()
                                .map(m -> objectMapper.createObjectNode()
                                        .put("paymentId", m.getId())
                                        .put("carStateId", m.getCarStateId())
                                        .put("sum", m.getPrice().toString())
                                        .put("provider", m.getProvider() != null ? m.getProvider().getName() : "")
                                        .put("cashlessPayment", m.getProvider() != null ? m.getProvider().getCashlessPayment() : false)
                                        .put("rate", m.getRateDetails()))
                                .forEach(m -> {
                                    payments.add(m);
                                });
                    }
                }
                node.set("payments", payments);
            } else if ("deleteAllDebts".equals(commandName)) {
                getBalanceService().deleteAllDebts();
            } else if ("getPaymentProvider".equals(commandName)) {
                PaymentProvider provider = getPaymentProviderService().getProviderByClientId(command.get("clientId").textValue());
                node.put("providerName", provider.getName());
                node.put("providerId", provider.getId());
            } else if ("listOfPaymentsBetween2Date".equalsIgnoreCase(commandName)) {

                List<PaymentApiDto> payments = getPaymentRepository()
                        .findAllByCreatedBetweenAndProviderName(
                                command.get("providerName").textValue(),
                                simpleDateFormatWithoutZone.format(
                                        simpleDateFormatWithoutZone.parse(
                                                command.get("dateFrom").textValue())),
                                simpleDateFormatWithoutZone.format(
                                        simpleDateFormatWithoutZone.parse(
                                                command.get("dateTo").textValue())))
                        .stream()
                        .map(payment -> {
                            if (command.get("onlyTransactionId").booleanValue()) {
                                return PaymentApiDto.builder()
                                        .providerTrnId(payment.getTransaction())
                                        .transactionId(payment.getTransaction())
                                        .build();
                            } else {
                                return PaymentApiDto.builder()
                                        .amount(payment.getPrice().toBigInteger())
                                        .providerTrnId(payment.getTransaction())
                                        .transactionId(payment.getTransaction())
                                        .transactionTime(payment.getCreated())
                                        .build();
                            }
                        })
                        .collect(Collectors.toList());
                node.set("payments", objectMapper.valueToTree(payments));
            } else if ("findFirstByTransactionAndProviderNameAndCreated".equalsIgnoreCase(commandName)) {
                Optional<Payment> paymentOptional = getPaymentRepository()
                        .findFirstByTransactionAndProviderNameAndCreated(
                                command.get(TRANSACTION_ID).textValue(),
                                command.get("providerName").textValue(),
                                simpleDateFormatWithoutZone.format(
                                        simpleDateFormatWithoutZone.parse(
                                                command.get("transactionTime").textValue())));

                if (paymentOptional.isPresent()) {
                    PaymentStatusApiDto paymentStatusApiDto = PaymentStatusApiDto.builder()
                            .status((byte) 0)
                            .providerTrnId(paymentOptional.get().getTransaction())
                            .transactionState((byte) (paymentOptional.get().isCanceled() ? 2 : 1))
                            .transactionStateErrorMsg("Ok")
                            .transactionStateErrorStatus((byte) 0)
                            .build();
                    node.set("payment", objectMapper.valueToTree(paymentStatusApiDto));
                } else {
                    throw new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Entity not found by : " +
                            TRANSACTION_ID + " " + command.get(TRANSACTION_ID).textValue());
                }
            } else if ("cancelPayment".equalsIgnoreCase(commandName)) {
                getPaymentService().cancelPaymentByTransactionId(
                        command.get(TRANSACTION_ID).textValue(),
                        command.get("reason").textValue());
            } else {
                throw new RuntimeException("Unknown command for billing operation");
            }
        } else {
            throw new RuntimeException("Unknown command for billing operation");
        }

        return node;
    }

    private PaymentProviderService getPaymentProviderService() {
        if (paymentProviderService == null) {
            paymentProviderService = (PaymentProviderService) BillingPlugin.INSTANCE.getApplicationContext().getBean("paymentProviderServiceImpl");
        }
        return paymentProviderService;
    }

    private PaymentProviderRepository getPaymentProviderRepository() {
        if (paymentProviderRepository == null) {
            paymentProviderRepository = (PaymentProviderRepository) BillingPlugin.INSTANCE.getApplicationContext()
                    .getBean("paymentProviderRepository");
        }
        return paymentProviderRepository;
    }

    private PaymentRepository getPaymentRepository() {
        if (paymentRepository == null) {
            paymentRepository = (PaymentRepository) BillingPlugin.INSTANCE.getApplicationContext()
                    .getBean("paymentRepository");
        }
        return paymentRepository;
    }

    private PaymentService getPaymentService() {
        if (paymentService == null) {
            paymentService = (PaymentService) BillingPlugin.INSTANCE.getApplicationContext().getBean("paymentService");
        }
        return paymentService;
    }

    private RootServicesGetterService getRootServicesGetterService() {
        if (rootServicesGetterService == null) {
            rootServicesGetterService = (RootServicesGetterService) BillingPlugin.INSTANCE.getApplicationContext().getBean("rootServicesGetterServiceImpl");
        }
        return rootServicesGetterService;
    }

    private BalanceService getBalanceService() {
        if (balanceService == null) {
            balanceService = (BalanceService) BillingPlugin.INSTANCE.getApplicationContext().getBean("balanceServiceImpl");
        }
        return balanceService;
    }

    private WebKassaService getWebKassaService() {
        if (webKassaService == null) {
            webKassaService = (WebKassaServiceImpl) BillingPlugin.INSTANCE.getApplicationContext().getBean("webKassaServiceImpl");
        }
        return webKassaService;
    }

    private WebKassaService getReKassaService() {
        if (reKassaService == null) {
            reKassaService = (ReKassaServiceImpl) BillingPlugin.INSTANCE.getApplicationContext().getBean("reKassaServiceImpl");
        }
        return reKassaService;
    }

    private OfdCheckData registerCheck(PaymentProvider provider, JsonNode command) {

        int sum = command.get("sum").intValue();
        int change = command.get("change").intValue();
        String operationName = command.get("operationName").textValue();
        int paymentType = command.get("paymentType").intValue();
        String txn_id = command.get("txn_id").textValue();
        OfdCheckData ofdCheckData = null;
        if (provider.getOfdProviderType().equals(PaymentProvider.OFD_PROVIDER_TYPE.WebKassa)) {
            String cashboxNumber = provider.getWebKassaID();
            Check check = new Check();
            check.setCashboxUniqueNumber(cashboxNumber);
            Position position = new Position();
            position.price = sum - change;
            position.positionName = operationName;
            check.getPositions().add(position);

            kz.spt.billingplugin.model.dto.webkassa.Payment payment = new kz.spt.billingplugin.model.dto.webkassa.Payment();
            payment.paymentType = paymentType;
            payment.sum = String.valueOf(sum);
            check.getPayments().add(payment);
            check.setChange(String.valueOf(change));
            check.setExternalCheckNumber(txn_id + "-" + provider.getId());
            AuthRequestDTO authRequestDTO = new AuthRequestDTO();
            authRequestDTO.setPassword(provider.getWebKassaPassword());
            authRequestDTO.setLogin(provider.getWebKassaLogin());
            log.info("[WebKassa] Request for check number for txn " + txn_id);
            ofdCheckData = getWebKassaService().registerCheck(check, provider);
            log.info("[WebKassa] Result " + ofdCheckData.getCheckNumber());
        } else if (provider.getOfdProviderType().equals(PaymentProvider.OFD_PROVIDER_TYPE.ReKassa)) {
            RekassaCheckRequest checkRequest = new RekassaCheckRequest();
            checkRequest.fillPayment(sum, change, paymentType == 1);
            log.info("[ReKassa] Request for check number for txn " + txn_id);
            ofdCheckData = getReKassaService().registerCheck(checkRequest, provider);
            log.info("[ReKassa] Result " + ofdCheckData.getCheckNumber());


        }
        return ofdCheckData;
    }
}
