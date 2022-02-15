package kz.spt.billingplugin.extension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.spt.billingplugin.BillingPlugin;
import kz.spt.billingplugin.model.Payment;
import kz.spt.billingplugin.model.PaymentProvider;
import kz.spt.billingplugin.model.dto.PaymentDto;
import kz.spt.billingplugin.model.dto.webkassa.*;
import kz.spt.billingplugin.service.*;
import kz.spt.billingplugin.service.impl.WebKassaServiceImpl;
import kz.spt.lib.extension.PluginRegister;
import kz.spt.lib.model.Customer;
import kz.spt.lib.model.Parking;
import kz.spt.lib.utils.StaticValues;
import lombok.extern.java.Log;
import org.pf4j.Extension;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.List;

@Log
@Extension
public class CommandExecutor implements PluginRegister {

    private PaymentProviderService paymentProviderService;
    private PaymentService paymentService;
    private RootServicesGetterService rootServicesGetterService;
    private BalanceService balanceService;
    private WebKassaService webKassaService;

    @Override
    public JsonNode execute(JsonNode command) throws Exception {

        SimpleDateFormat format = new SimpleDateFormat(StaticValues.dateFormatTZ);
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode node = objectMapper.createObjectNode();

        if(command!=null && command.has("command")){
            String commandName = command.get("command").textValue();
            if("getPasswordHash".equals(commandName)){
                if(command.has("client_id") && command.get("client_id").isTextual()){
                    PaymentProvider paymentProvider = getPaymentProviderService().getProviderByClientId(command.get("client_id").textValue());
                    if(paymentProvider != null && paymentProvider.getEnabled() && paymentProvider.getSecret() != null){
                        node.put("passwordHash", paymentProvider.getSecret());
                    }
                } else {
                    throw new RuntimeException("Not all getPasswordHash parameters set");
                }
            } else if("savePayment".equals(commandName)){
                PaymentProvider provider = getPaymentProviderService().getProviderByClientId(command.get("clientId").textValue());
                List<Payment> oldPayments = getPaymentService().findByTransactionAndProvider(command.get("transaction").textValue(), provider);

                if(oldPayments.size() > 0){
                    node.put("paymentError", "txn_id уже зарегистрирован");
                    node.put("paymentErrorCode", 4);
                    return node;
                }

                Payment payment = new Payment();
                payment.setCarNumber(command.get("carNumber").textValue());
                payment.setPrice(command.get("sum").decimalValue());
                payment.setProvider(getPaymentProviderService().getProviderByClientId(command.get("clientId").textValue()));
                payment.setTransaction(command.get("transaction").textValue());
                if(command.has("parkingId")){
                    Parking parking = getRootServicesGetterService().getParkingService().findById(command.get("parkingId").longValue());
                    payment.setParking(parking);
                }
                if(command.has("customerId")){
                    Customer customer = getRootServicesGetterService().getCustomerService().findById(command.get("customerId").longValue());
                    payment.setCustomer(customer);
                }
                payment.setInDate(format.parse(command.get("inDate").textValue()));
                payment.setRateDetails(command.get("rateName").textValue());
                payment.setCarStateId(command.get("carStateId").longValue());

                Payment savedPayment = getPaymentService().savePayment(payment);
                node.put("paymentId", savedPayment.getId());
                node.put("cashlessPayment", payment.getProvider().getCashlessPayment() != null ? payment.getProvider().getCashlessPayment() : false);

                getBalanceService().addBalance(command.get("carNumber").textValue(), command.get("sum").decimalValue(), command.get("carStateId").longValue(), "Received payment from " + payment.getProvider().getName(),  "Получен платеж от " + payment.getProvider().getName());

                List<Payment> carStatePayments = getPaymentService().getPaymentsByCarStateId(savedPayment.getCarStateId());
                ArrayNode paymentArray = PaymentDto.arrayNodeFromPayments(carStatePayments);
                node.set("paymentArray", paymentArray);

            } else if("getCurrentBalance".equals(commandName)){
                if(command.has("plateNumber")){
                    node.put("currentBalance", getBalanceService().getBalance(command.get("plateNumber").textValue()));
                } else {
                    throw new RuntimeException("Not all getCurrentBalance parameters set");
                }
            } else if("decreaseCurrentBalance".equals(commandName)){
                if(command.has("plateNumber") && command.has("carStateId") && command.has("amount") && command.has("parkingName")){
                    node.put("currentBalance", getBalanceService().subtractBalance(command.get("plateNumber").textValue(), command.get("amount").decimalValue(), command.get("carStateId").longValue(),  "Payment for parking " + command.get("parkingName").textValue(),  "Оплата паркинга " + command.get("parkingName").textValue()));
                } else {
                    throw new RuntimeException("Not all decreaseCurrentBalance parameters set");
                }
            } else if("increaseCurrentBalance".equals(commandName)){
                if(command.has("plateNumber") && command.has("amount") && command.has("reason") && command.has("reasonEn")){
                    String plateNumber = command.get("plateNumber").textValue();
                    BigDecimal amount = command.get("amount").decimalValue();
                    String reason = command.get("reason").textValue();
                    String reasonEn = command.get("reasonEn").textValue();
                    node.put("currentBalance", getBalanceService().addBalance(plateNumber, amount, null, reason,  reasonEn));
                } else {
                    throw new RuntimeException("Not all increaseCurrentBalance parameters set");
                }
            } else if("addOutTimestampToPayments".equals(commandName)){
                if(command.has("outTimestamp") && command.has("carStateId")){
                    getPaymentService().updateOutTimestamp(command.get("carStateId").longValue(), format.parse(command.get("outTimestamp").textValue()));
                } else {
                    throw new RuntimeException("Not all addOutTimestampToPayments parameters set");
                }
            } else if ("getParkomatClientId".equals(commandName)) {
                PaymentProvider provider = getPaymentProviderService().getProviderByClientId(command.get("parkomatId").textValue());
                node.put("clientId" , provider.getClientId());
            } else if ("getCheck".equals(commandName)) {

                PaymentProvider provider = getPaymentProviderService().getProviderByClientId(command.get("parkomatId").textValue());
                String cashboxNumber = provider.getWebKassaID();
                int sum = command.get("sum").intValue();
                int change = command.get("change").intValue();
                String operationName = command.get("operationName").textValue();
                int paymentType = command.get("paymentType").intValue();
                String txn_id = command.get("txn_id").textValue();
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
                check.setExternalCheckNumber(txn_id+"-"+provider.getId());

                AuthRequestDTO authRequestDTO = new AuthRequestDTO();
                authRequestDTO.setPassword(provider.getWebKassaPassword());
                authRequestDTO.setLogin(provider.getWebKassaLogin());

                CheckResponse checkResponse = getWebKassaService().registerCheck(check, authRequestDTO);

                if (checkResponse!=null) {
                    node.put("checkNumber" , checkResponse.data.checkNumber);
                    node.put("ticketUrl" , checkResponse.data.ticketUrl);

                    List<Payment> paymentList = getPaymentService().findByTransactionAndProvider(txn_id, provider);
                    if (!paymentList.isEmpty()) {
                        paymentList.get(0).setCheckNumber(checkResponse.data.checkNumber);
                        getPaymentService().savePayment( paymentList.get(0));
                    }

                }


            }else if ("zReport".equals(commandName)) {

                PaymentProvider provider = getPaymentProviderService().getProviderByClientId(command.get("parkomatId").textValue());

                ZReport zReport = new ZReport();
                zReport.setCashboxUniqueNumber(provider.getWebKassaID());

                AuthRequestDTO authRequestDTO = new AuthRequestDTO();
                authRequestDTO.setPassword(provider.getWebKassaPassword());
                authRequestDTO.setLogin(provider.getWebKassaLogin());

                String checkResponse = getWebKassaService().closeOperationDay(zReport, authRequestDTO);

                if (checkResponse != null) {
                    node.put("result", checkResponse);
                }
            } else if("getProviderNames".equals(commandName)){
                ArrayNode paymentProviders = objectMapper.createArrayNode();
                getPaymentProviderService().listAllPaymentProviders().forEach(paymentProvider -> {
                    paymentProviders.add(paymentProvider.getName());
                });
                node.set("providerNames", paymentProviders);
            }
        }

        return node;
    }

    private PaymentProviderService getPaymentProviderService(){
        if(paymentProviderService == null) {
            paymentProviderService = (PaymentProviderService) BillingPlugin.INSTANCE.getApplicationContext().getBean("paymentProviderServiceImpl");
        }
        return paymentProviderService;
    }

    private PaymentService getPaymentService(){
        if(paymentService == null) {
            paymentService = (PaymentService) BillingPlugin.INSTANCE.getApplicationContext().getBean("paymentServiceImpl");
        }
        return paymentService;
    }

    private RootServicesGetterService getRootServicesGetterService(){
        if(rootServicesGetterService == null) {
            rootServicesGetterService = (RootServicesGetterService) BillingPlugin.INSTANCE.getApplicationContext().getBean("rootServicesGetterServiceImpl");
        }
        return rootServicesGetterService;
    }

    private BalanceService getBalanceService(){
        if(balanceService == null) {
            balanceService = (BalanceService) BillingPlugin.INSTANCE.getApplicationContext().getBean("balanceServiceImpl");
        }
        return balanceService;
    }

    private WebKassaService getWebKassaService() {
        if(webKassaService == null) {
            webKassaService = (WebKassaServiceImpl) BillingPlugin.INSTANCE.getApplicationContext().getBean("webKassaServiceImpl");
        }
        return webKassaService;
    }
}
