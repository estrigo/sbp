package kz.spt.reportplugin.dto.filter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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

    public String dateToString(Date date){
        if(date == null) return null;
        DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().format(format);
    }
}
