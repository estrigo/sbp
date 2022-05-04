package kz.spt.reportplugin.rest;

import kz.spt.lib.bootstrap.datatable.Page;
import kz.spt.lib.bootstrap.datatable.PagingRequest;
import kz.spt.reportplugin.dto.SumReportDto;
import kz.spt.reportplugin.dto.filter.FilterJournalReportDto;
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
    public Page<SumReportDto> list(@RequestBody PagingRequest pagingRequest){
        return reportService.page(pagingRequest);
    }

    @PostMapping("/excel")
    public List<SumReportDto> excel(@RequestBody FilterSumReportDto filter){
        return reportService.list(filter);
    }
}
