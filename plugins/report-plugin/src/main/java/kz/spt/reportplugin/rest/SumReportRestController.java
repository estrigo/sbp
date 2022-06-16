package kz.spt.reportplugin.rest;

import kz.spt.reportplugin.dto.SumReportDto;
import kz.spt.reportplugin.dto.filter.FilterSumReportDto;
import kz.spt.reportplugin.service.ReportService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/rest/report/sum")
public class SumReportRestController extends BasicRestController<SumReportDto> {

    public SumReportRestController(ReportService<SumReportDto> sumReportService) {
        super(sumReportService);
    }

    @PostMapping
    public List<SumReportDto> sum(@RequestBody FilterSumReportDto filter){
        return reportService.list(filter);
    }

    @PostMapping("detail")
    public List<SumReportDto> detail(@RequestBody FilterSumReportDto filter){
        return reportService.list(filter);
    }
}