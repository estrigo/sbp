package kz.spt.reportplugin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SumReportDto {

    private String dateTime;
    private int count;
    private int freeCount;
    private int paymentsCount;
    private int whitelistsCount;
    private int abonementsCount;
    private BigDecimal kaspiSum;
    private BigDecimal yurtaSum;
    private BigDecimal totalSum;
}
