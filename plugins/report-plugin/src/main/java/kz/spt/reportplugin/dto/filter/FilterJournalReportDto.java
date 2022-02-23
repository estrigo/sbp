package kz.spt.reportplugin.dto.filter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilterJournalReportDto extends FilterReportDto{
    private Date dateFrom;
    private Date dateTo;
    private String paymentProvider;
    private String carNumber;
}
