package kz.spt.reportplugin.rest;

import kz.spt.reportplugin.dto.JournalReportDto;
import kz.spt.reportplugin.service.ReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/rest/report/journal")
public class JournalReportRestController extends BasicRestController<JournalReportDto> {

    public JournalReportRestController(ReportService<JournalReportDto> journalReportService) {
        super(journalReportService);
    }

    @GetMapping("/list")
    public List<JournalReportDto> list(){
        return this.reportService.list();
    }
}
