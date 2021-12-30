package kz.spt.billingplugin.extension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.spt.billingplugin.BillingPlugin;
import kz.spt.billingplugin.model.Payment;
import kz.spt.billingplugin.model.PaymentProvider;
import kz.spt.billingplugin.model.dto.PaymentDto;
import kz.spt.billingplugin.service.BalanceService;
import kz.spt.billingplugin.service.PaymentProviderService;
import kz.spt.billingplugin.service.PaymentService;
import kz.spt.billingplugin.service.RootServicesGetterService;
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

    @Override
    public JsonNode execute(JsonNode command) throws Exception {

        SimpleDateFormat format = new SimpleDateFormat(StaticValues.dateFormat);
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode node = objectMapper.createObjectNode();

        if(command!=null && command.has("command")){
            if("getPasswordHash".equals(command.get("command").textValue())){
                if(command.has("client_id") && command.get("client_id").isTextual()){
                    PaymentProvider paymentProvider = getPaymentProviderService().getProviderByClientId(command.get("client_id").textValue());
                    if(paymentProvider != null && paymentProvider.getEnabled() && paymentProvider.getSecret() != null){
                        node.put("passwordHash", paymentProvider.getSecret());
                    }
                }
            } else if("savePayment".equals(command.get("command").textValue())){
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

                getBalanceService().addBalance(command.get("carNumber").textValue(), command.get("sum").decimalValue());

                List<Payment> carStatePayments = getPaymentService().getPaymentsByCarStateId(savedPayment.getCarStateId());
                ArrayNode paymentArray = PaymentDto.arrayNodeFromPayments(carStatePayments);
                node.set("paymentArray", paymentArray);

            } else if("getCurrentBalance".equals(command.get("command").textValue())){
                if(command.has("plateNumber")){
                    node.put("currentBalance", getBalanceService().getBalance(command.get("plateNumber").textValue()));
                } else {
                    node.put("currentBalance", new BigDecimal(0));
                }
            } else if("decreaseCurrentBalance".equals(command.get("command").textValue())){
                if(command.has("plateNumber") && command.has("amount")){
                    node.put("currentBalance", getBalanceService().subtractBalance(command.get("plateNumber").textValue(), command.get("amount").decimalValue()));
                } else {
                    node.put("currentBalance", new BigDecimal(0));
                }
            } else if("addOutTimestampToPayments".equals(command.get("command").textValue())){
                if(command.has("outTimestamp") && command.has("carStateId")){
                    getPaymentService().updateOutTimestamp(command.get("carStateId").longValue(), format.parse(command.get("outTimestamp").textValue()));
                }
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
}
