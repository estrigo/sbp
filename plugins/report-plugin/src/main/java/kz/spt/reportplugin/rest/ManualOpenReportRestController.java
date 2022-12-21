package kz.spt.reportplugin.rest;

import kz.spt.lib.bootstrap.datatable.Page;
import kz.spt.lib.bootstrap.datatable.PagingRequest;
import kz.spt.lib.model.EventLog;
import kz.spt.lib.model.dto.EventFilterDto;
import kz.spt.lib.model.dto.EventsDto;
import kz.spt.reportplugin.dto.filter.FilterJournalReportDto;
import kz.spt.reportplugin.service.ReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping(value = "/rest/report/manualOpen")
public class ManualOpenReportRestController extends BasicRestController<EventsDto> {
    public ManualOpenReportRestController(ReportService<EventsDto> manualOpenReportService) {
        super(manualOpenReportService);
    }

    @PostMapping
    public Page<EventsDto> list(@RequestBody PagingRequest pagingRequest, @RequestParam String dateFromString,
                                @RequestParam String dateToString, @RequestParam EventLog.EventType eventType)
            throws ParseException {
        EventFilterDto eventFilterDto = new EventFilterDto();
        eventFilterDto.setDateFromString(dateFromString);
        eventFilterDto.setDateToString(dateToString);
        eventFilterDto.setEventType(eventType);
        return reportService.pageFilter(pagingRequest, eventFilterDto);
    }

    @PostMapping("/excel")
    public List<EventsDto> excel(@RequestBody FilterJournalReportDto filter){
        return reportService.list(filter);
    }
}
