package kz.spt.reportplugin.rest;

import kz.spt.lib.bootstrap.datatable.Page;
import kz.spt.lib.bootstrap.datatable.PagingRequest;
import kz.spt.lib.model.dto.EventsDto;
import kz.spt.reportplugin.dto.filter.FilterJournalReportDto;
import kz.spt.reportplugin.service.ReportService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/rest/report/manualOpen")
public class ManualOpenReportRestController extends BasicRestController<EventsDto> {
    public ManualOpenReportRestController(ReportService<EventsDto> manualOpenReportService) {
        super(manualOpenReportService);
    }

    @PostMapping
    public Page<EventsDto> list(@RequestBody PagingRequest pagingRequest){
        return reportService.page(pagingRequest);
    }

    @PostMapping("/excel")
    public List<EventsDto> excel(@RequestBody FilterJournalReportDto filter){
        return reportService.list(filter);
    }
}
