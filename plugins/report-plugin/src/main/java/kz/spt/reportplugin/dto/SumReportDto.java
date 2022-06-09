package kz.spt.reportplugin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SumReportDto {

    Map<String,String> fields = new HashMap<>();
    Map<String, Object> results = new HashMap<>();
    SumReportListDto listResult;
}
