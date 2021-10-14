package kz.spt.billingplugin.model.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.spt.billingplugin.model.Payment;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PaymentDto {

    public Long id;
    public Date created;
    public BigDecimal price;
    public String transaction;
    public String providerPaymentName;

    public static PaymentDto fromPayment(Payment  payment){
        PaymentDto dto = new PaymentDto();
        dto.id = payment.getId();
        dto.created = payment.getCreated();
        dto.providerPaymentName = payment.getProvider().getProvider();
        dto.price = payment.getPrice();
        dto.transaction = payment.getTransaction();
        return dto;
    }

    public static List<PaymentDto> fromPayments(List<Payment> payments){
        List<PaymentDto> list = new ArrayList<>();
        for(Payment payment:payments){
            list.add(fromPayment(payment));
        }
        return list;
    }

    public static ArrayNode arrayNodeFromPayments(List<Payment> payments){
        List<PaymentDto> paymentDtoList = fromPayments(payments);
        ObjectMapper objectMapper = new ObjectMapper();
        ArrayNode arrayNode = objectMapper.createArrayNode();
        SimpleDateFormat format =  new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        for (PaymentDto dto:paymentDtoList){
            ObjectNode objectNode = objectMapper.createObjectNode();
            objectNode.put("id", dto.id);
            objectNode.put("created", format.format(dto.created));
            objectNode.put("providerPaymentName", dto.providerPaymentName);
            objectNode.put("price", dto.price.setScale(2));
            objectNode.put("transaction", dto.transaction);
            arrayNode.add(objectNode);
        }
        return arrayNode;
    }
}
