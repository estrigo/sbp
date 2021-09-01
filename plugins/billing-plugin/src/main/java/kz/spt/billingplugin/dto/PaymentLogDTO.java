package kz.spt.billingplugin.dto;

import kz.spt.billingplugin.model.Payment;
import kz.spt.billingplugin.model.PaymentProvider;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.modelmapper.ModelMapper;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

import static kz.spt.billingplugin.BillingPlugin.modelMapper;

@Data @NoArgsConstructor
public class PaymentLogDTO {



    public Long id;
    public Date created;
    public Date updated;

    private BigDecimal price;

    String transaction;


    private Payment.Status status;

    private PaymentProvider provider;

    String description;

    private Payment.Type type;
    String carNumber;

    public static PaymentLogDTO convertToDto(Payment payment) {

        PaymentLogDTO paymentLogDTO = new ModelMapper().map(payment, PaymentLogDTO.class);

        return paymentLogDTO;
    }
}
