package kz.spt.app.rest;

import kz.spt.lib.bootstrap.datatable.Page;
import kz.spt.lib.bootstrap.datatable.PagingRequest;
import kz.spt.lib.model.EventLog;
import kz.spt.lib.model.dto.EventFilterDto;
import kz.spt.lib.model.dto.EventsDto;
import kz.spt.lib.service.EventLogService;
import org.springframework.web.bind.annotation.*;

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


}
