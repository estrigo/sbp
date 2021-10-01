package kz.spt.billingplugin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import kz.spt.billingplugin.model.Payment;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data @NoArgsConstructor
public class PaymentLogDTO {

    public Long id;
    @JsonFormat(pattern="dd.MM.yyyy HH:mm:ss")
    public Date created;
    public Date updated;

    private BigDecimal price;

    String transaction;


    private Payment.Status status;

    private String provider;

    String description;

    private Payment.Type type;
    String carNumber;

    public static PaymentLogDTO convertToDto(Payment payment) {
        PaymentLogDTO paymentLogDTO = new PaymentLogDTO();
        paymentLogDTO.setId(payment.getId());
        paymentLogDTO.setCarNumber(payment.getCarNumber());
        paymentLogDTO.setCreated(payment.getCreated());
        paymentLogDTO.setDescription(payment.getDescription());
        paymentLogDTO.setPrice(payment.getPrice());
        paymentLogDTO.setProvider(payment.getProvider().getName());
        paymentLogDTO.setTransaction(payment.getTransaction());
        return paymentLogDTO;
    }
}
