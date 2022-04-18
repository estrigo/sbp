package kz.spt.billingplugin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilterPaymentDTO {
    private Date dateFrom;
    private Date dateTo;
    private Long paymentProvider;
    private String carNumber;
    private BigDecimal total;
    private String transaction;
}
