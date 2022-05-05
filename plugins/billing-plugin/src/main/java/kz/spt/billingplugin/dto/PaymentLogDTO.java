package kz.spt.billingplugin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import kz.spt.billingplugin.model.Payment;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

    public BigDecimal price;

    public String transaction;

    public String rateDetails;

    public String provider;

    public String description;

    public String carNumber;

    public String customerDetail;

    public String parking;

    public Date getNullSafeInDate(){
        return (getInDate() == null ? new Date() : getInDate());
    }

    public Date getNullSafeOutDate(){
        return (getOutDate() == null ? new Date() : getOutDate());
    }

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

    public static List<PaymentLogDTO> convertToDto(List<Payment> payments){
        List<PaymentLogDTO> paymentLogDTOS = new ArrayList<>(payments.size());
        for (Payment payment:payments){
            paymentLogDTOS.add(convertToDto(payment));
        }
        return paymentLogDTOS;
    }
}
