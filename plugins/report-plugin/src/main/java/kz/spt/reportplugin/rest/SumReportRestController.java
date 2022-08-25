package kz.spt.reportplugin.rest;

import kz.spt.reportplugin.dto.SumReportDto;
import kz.spt.reportplugin.dto.SumReportFinalDto;
import kz.spt.reportplugin.dto.filter.FilterSumReportDto;
import kz.spt.reportplugin.service.ReportService;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping(value = "/rest/report/sum")
public class SumReportRestController extends BasicRestController<SumReportDto> {

    public SumReportRestController(ReportService<SumReportDto> sumReportService) {
        super(sumReportService);
    }

    @PostMapping
    public List<SumReportDto> sum(@RequestBody FilterSumReportDto filter) {
        return reportService.list(filter);
    }

    @PostMapping("detail")
    public List<SumReportDto> detail(@RequestBody FilterSumReportDto filter) {
        return reportService.list(filter);
    }


    @PostMapping("/final")
    public SumReportFinalDto sumReportFinal(@RequestBody FilterSumReportDto filter) {
        List<String> fields =
                Arrays.asList("records", "paymentRecords", "whitelistRecords", "abonementRecords", "freeMinuteRecords",
                        "debtRecords", "fromBalanceRecords", "freeRecords", "autoClosedRecords");
        String pattern = " dd.MM.yyyy hh:mm";

        List<SumReportDto> basicResult = reportService.list(filter);
        if (!CollectionUtils.isEmpty(basicResult)) {
            SumReportFinalDto finalDto = new SumReportFinalDto();
            finalDto.setFieldsMap(basicResult.get(0).getResults());

            List<Map<String, String>> mapList = new ArrayList<>();
            for (String field : fields) {
                List<SumReportDto> detailResult = reportService.list(
                        FilterSumReportDto.builder().dateFrom(filter.getDateFrom()).dateTo(filter.getDateTo())
                                .eventType(field).build());
                String result = "";
                if (!CollectionUtils.isEmpty(detailResult)) {
                    result = detailResult.get(0).getResults().get("result");
                }
                Map<String, String> map = new HashMap<>();
                map.put(field, result);
                mapList.add(map);
            }
            DateFormat dateFormat = new SimpleDateFormat(pattern);
            Map<String, String> map = new HashMap<>();
            map.put("dateTime", dateFormat.format(filter.getDateFrom()) + " - " + dateFormat.format(filter.getDateTo()) );
            mapList.add(map);

            finalDto.setMapList(mapList);

            List<SumReportDto> paymentsResult = reportService.list(
                    FilterSumReportDto.builder().dateFrom(filter.getDateFrom()).dateTo(filter.getDateTo())
                            .eventType("payments").build());
            if (!CollectionUtils.isEmpty(paymentsResult)) {
                finalDto.setPayments(paymentsResult.get(0).getResults());
            }

            List<SumReportDto> detailResult = reportService.list(
                    FilterSumReportDto.builder()
                            .dateFrom(filter.getDateFrom())
                            .dateTo(filter.getDateTo())
                            .eventType("paymentRecords")
                            .type("detailed").build());
            if (!CollectionUtils.isEmpty(detailResult)) {
                finalDto.setListResult(detailResult.get(0).getListResult());
            }
            return finalDto;
        }
        return null;
    }

}