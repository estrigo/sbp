package kz.spt.reportplugin.dto;

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
public class JournalReportDto {
    private Long carStateId;
    private Long paymentId;
    private String carNumber;
    private Date inTimestamp;
    private Date outTimestamp;
    private String parkingTypeCode;
    private String parkingType;
    private String provider;
    private BigDecimal sum;
}
