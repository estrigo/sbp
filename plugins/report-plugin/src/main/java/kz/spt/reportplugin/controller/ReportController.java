package kz.spt.reportplugin.controller;

import kz.spt.reportplugin.dto.SumReportDto;
import kz.spt.reportplugin.dto.SumReportFinalDto;
import kz.spt.reportplugin.dto.SumReportListDto;
import kz.spt.reportplugin.dto.filter.FilterJournalReportDto;
import kz.spt.reportplugin.dto.filter.FilterSumReportDto;
import kz.spt.reportplugin.rest.BasicRestController;
import kz.spt.reportplugin.service.ReportService;
import kz.spt.reportplugin.service.RootServicesGetterService;
import kz.spt.reportplugin.service.impl.JournalReportServiceImpl;
import kz.spt.reportplugin.service.impl.ManualOpenReportServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

@Controller
@RequestMapping("/report")
public class ReportController extends BasicRestController<SumReportDto> {

    @Autowired
    private RootServicesGetterService rootServicesGetterService;

    public ReportController(ReportService<SumReportDto> reportService) {
        super(reportService);
    }

    @GetMapping("/journal")
    public String journal() {
        return "report/journal";
    }

    @GetMapping("/manualOpen")
    public String manualOpen() {
        return "report/manualOpen";
    }

    @GetMapping("/sum")
    public String sum() {
        return "report/sum";
    }


    @PostMapping("/download/{name}/{format}")
    public void downloadFile(@PathVariable ReportNameEnum name, @PathVariable String format,
                             @RequestParam(required = false, name = "dateFrom") String dateFrom,
                             @RequestParam(required = false, name = "dateTo") String dateTo,

                             HttpServletResponse response) throws Exception {
        FilterJournalReportDto filter = new FilterJournalReportDto();
        String pattern_1 = "yyyy-MM-dd hh:mm:ss";
        byte[] bytes;
        if (!ReportNameEnum.MANUAL_OPEN.equals(name)) {
            if (ReportNameEnum.BILLING.equals(name)) {
                dateTo = dateFrom;
            } else if ((ObjectUtils.isEmpty(dateFrom) || ObjectUtils.isEmpty(dateTo))) {
                throw new Exception("Is null dateFrom or dateTo");
            }
            dateFrom = dateFrom.concat(" 00:00:00");
            dateTo = dateTo.concat(" 23:59:00");
        }
        if (ReportNameEnum.BILLING.equals(name)) {
            bytes = rootServicesGetterService.getAdminService().report(billingData(
                    new SimpleDateFormat(pattern_1).parse(dateFrom),
                    new SimpleDateFormat(pattern_1).parse(dateTo)), name.name(), format);
        } else {
            if (!ObjectUtils.isEmpty(dateFrom)) {
                filter.setDateFrom(new SimpleDateFormat(pattern_1).parse(dateFrom));
            }
            if (!ObjectUtils.isEmpty(dateTo)) {
                filter.setDateTo(new SimpleDateFormat(pattern_1).parse(dateTo));
            }
            List<?> list = Objects.requireNonNull(getReportService(name)).list(filter);
            bytes = rootServicesGetterService.getAdminService().report(list, name.name(), format);
        }


        response.setContentType("application/octet-stream");
        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename = "
                .concat(UUID.randomUUID().toString()).concat(".").concat(format.toLowerCase());
        response.setHeader(headerKey, headerValue);
        ServletOutputStream outputStream = response.getOutputStream();
        outputStream.write(bytes);
        outputStream.close();
    }



    private SumReportFinalDto billingData(Date dateFrom, Date dateTo){
        List<String> fields =
                Arrays.asList("records", "paymentRecords", "whitelistRecords", "abonementRecords", "freeMinuteRecords",
                        "debtRecords", "fromBalanceRecords", "freeRecords", "autoClosedRecords");
        FilterSumReportDto filter = new FilterSumReportDto();
        filter.setDateFrom(dateFrom);
        filter.setDateTo(dateTo);
        filter.setEventType("fields");
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
            map.put("dateTime", dateFormat.format(dateFrom) + " - " + dateFormat.format(dateTo) );
            mapList.add(map);

            finalDto.setMapList(mapList);

            List<SumReportDto> paymentsResult = reportService.list(
                    FilterSumReportDto.builder().dateFrom(filter.getDateFrom()).dateTo(filter.getDateTo())
                            .eventType("payments").build());
            if (!CollectionUtils.isEmpty(paymentsResult)) {
                finalDto.setPayments(paymentsResult.get(0).getResults());
            }

            Map<String, List<SumReportListDto>> listResult = new HashMap<>();
            for (Map.Entry<String, String> entry : basicResult.get(0).getResults().entrySet()) {
                try {
                    List<SumReportDto> detailResult = reportService.list(
                            FilterSumReportDto.builder()
                                    .dateFrom(filter.getDateFrom())
                                    .dateTo(filter.getDateTo())
                                    .eventType(entry.getKey())
                                    .type("detailed").build());
                    if (!CollectionUtils.isEmpty(detailResult)
                            && !CollectionUtils.isEmpty(detailResult.get(0).getListResult())) {
                        listResult.put(entry.getKey(), detailResult.get(0).getListResult());
                    }
                }catch (Exception e) {
                    System.out.println("detail result by " + entry.getKey() + ". MSG: " + e);
                }
            }
            finalDto.setListResult(listResult);
            return finalDto;
        }
        return null;
    }

    private ReportService<?> getReportService(ReportNameEnum name) {
        switch (name) {
            case JOURNAL:
                return new JournalReportServiceImpl();
            case MANUAL_OPEN:
                return new ManualOpenReportServiceImpl();
            case BILLING:
            default:
                return null;
        }
    }
}
