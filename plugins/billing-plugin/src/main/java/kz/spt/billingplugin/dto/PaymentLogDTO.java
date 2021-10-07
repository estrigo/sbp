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

    @JsonFormat(pattern="dd.MM.yyyy HH:mm:ss")
    public Date inDate;
    @JsonFormat(pattern="dd.MM.yyyy HH:mm:ss")
    public Date outDate;

    private BigDecimal price;

    String transaction;

    String rateDetails;

    private String provider;

    String description;

    String carNumber;

    private String customerDetail;

    private String parking;

    public static PaymentLogDTO convertToDto(Payment payment) {
        PaymentLogDTO paymentLogDTO = new PaymentLogDTO();
        paymentLogDTO.setId(payment.getId());
        paymentLogDTO.setCarNumber(payment.getCarNumber());
        paymentLogDTO.setCreated(payment.getCreated());
        paymentLogDTO.setInDate(payment.getInDate());
        paymentLogDTO.setOutDate(payment.getOutDate());
        paymentLogDTO.setDescription(payment.getDescription());
        paymentLogDTO.setPrice(payment.getPrice());
        paymentLogDTO.setProvider(payment.getProvider().getName());
        paymentLogDTO.setTransaction(payment.getTransaction());
        paymentLogDTO.setRateDetails(payment.getRateDetails());
        paymentLogDTO.setParking(payment.getParking() != null ? payment.getParking().getName() : "");
        paymentLogDTO.setCustomerDetail(payment.getCustomer() != null ? payment.getCustomer().getFirstName() + " " + payment.getCustomer().getLastName(): null);
        return paymentLogDTO;
    }
}
