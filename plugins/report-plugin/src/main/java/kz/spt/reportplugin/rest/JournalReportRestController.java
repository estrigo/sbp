package kz.spt.reportplugin.rest;

import kz.spt.lib.bootstrap.datatable.Page;
import kz.spt.lib.bootstrap.datatable.PagingRequest;
import kz.spt.reportplugin.dto.JournalReportDto;
import kz.spt.reportplugin.service.ReportService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/rest/report/journal")
public class JournalReportRestController extends BasicRestController<JournalReportDto> {

    public JournalReportRestController(ReportService<JournalReportDto> journalReportService) {
        super(journalReportService);
    }

    @PostMapping
    public Page<JournalReportDto> list(@RequestBody PagingRequest pagingRequest){
        return this.reportService.page(pagingRequest);
    }
}
