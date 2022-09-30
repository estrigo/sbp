package kz.spt.reportplugin.dto;

import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class SumReportFinalDto {
    private Map<String, String> fieldsMap;
    private List<Map<String, String>> mapList;
    private Map<String, String> payments;
    private Map<String, List<SumReportListDto>> listResult = new HashMap<>();
}
