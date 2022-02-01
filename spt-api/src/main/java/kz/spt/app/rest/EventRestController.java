package kz.spt.app.rest;

import kz.spt.lib.bootstrap.datatable.Page;
import kz.spt.lib.bootstrap.datatable.PagingRequest;
import kz.spt.lib.model.EventLog;
import kz.spt.lib.model.dto.EventFilterDto;
import kz.spt.lib.model.dto.EventsDto;
import kz.spt.lib.service.EventLogService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.text.ParseException;


@RestController
@RequestMapping(value = "/rest/events")
public class EventRestController {

    private EventLogService eventLogService;

    public EventRestController(EventLogService eventLogService) {
        this.eventLogService = eventLogService;
    }

    @PostMapping
    public Page<EventsDto> list(@RequestBody PagingRequest pagingRequest, @RequestParam String dateFromString, @RequestParam String dateToString,
                                @RequestParam String plateNumber, @RequestParam Long gateId) throws ParseException {
        EventFilterDto eventFilterDto = new EventFilterDto();
        eventFilterDto.setDateFromString(dateFromString);
        eventFilterDto.setDateToString(dateToString);
        eventFilterDto.setPlateNumber(plateNumber);
        eventFilterDto.setGateId(gateId);
        return eventLogService.getEventLogs(pagingRequest,eventFilterDto);
    }

    @PreAuthorize("hasAnyRole('ROLE_SUPERADMIN', 'ROLE_ADMIN')")
    @GetMapping(value = "/check/{propertyName}")
    public String openGate(@PathVariable("propertyName") String propertyName) throws IOException, ParseException, InterruptedException {
        return eventLogService.getApplicationPropertyValue(propertyName);
    }

    @GetMapping(value = "/excel")
    public String eventExcel(@RequestBody PagingRequest pagingRequest, @RequestParam String dateFromString, @RequestParam String dateToString,
                             @RequestParam String plateNumber, @RequestParam Long gateId) throws Exception {
        EventFilterDto eventFilterDto = new EventFilterDto();
        eventFilterDto.setDateFromString(dateFromString);
        eventFilterDto.setDateToString(dateToString);
        eventFilterDto.setPlateNumber(plateNumber);
        eventFilterDto.setGateId(gateId);
        return eventLogService.getEventExcel(eventFilterDto);
    }
}
