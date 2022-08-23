package kz.spt.reportplugin.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class SumReportFinalDto {
    private Map<String, String> fieldsMap;
    private List<Map<String, String>> mapList;
    private Map<String, String> payments;
    List<SumReportListDto> listResult = new ArrayList<>();
}
