package kz.spt.reportplugin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SumReportListDto {

    String plateNumber;
    String formattedInDate;
    String formattedOutDate;
    String inPlace;
    String outPlace;
}
