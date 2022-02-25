package kz.spt.reportplugin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
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

    @JsonFormat(shape=JsonFormat.Shape.STRING, pattern="dd:MM:yyyy HH:mm")
    private Date inTimestamp;

    @JsonFormat(shape=JsonFormat.Shape.STRING, pattern="dd:MM:yyyy HH:mm")
    private Date outTimestamp;

    private String parkingTypeCode;
    private String parkingType;
    private String provider;
    private BigDecimal sum;
    private boolean cashlessPayment = false;
}
