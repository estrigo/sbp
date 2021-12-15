package kz.spt.lib.service;

import kz.spt.lib.bootstrap.datatable.Page;
import kz.spt.lib.bootstrap.datatable.PagingRequest;
import kz.spt.lib.model.EventLog;
import kz.spt.lib.model.dto.CarEventDto;
import kz.spt.lib.model.dto.EventFilterDto;
import kz.spt.lib.model.dto.EventsDto;

import java.text.ParseException;
import java.util.Map;

public interface EventLogService {

    enum ArmEventType {
        Photo,
        CarEvent;
    }

    enum EventType{
        Allow,
        Deny,
        Error,
        Success
    }

    void createEventLog(String objectClass, Long objectId, Map<String, Object> properties, String description);

    void sendSocketMessage(ArmEventType eventType, Long gateId, String plateNumber, String message);

    Iterable<EventLog> listAllLogs();

    Iterable<EventLog> listAllLogsDesc();

    Iterable<EventLog> listByFilters(EventFilterDto eventFilterDo) throws ParseException;

    EventLog getById(Long id);

    Page<EventsDto> getEventLogs(PagingRequest pagingRequest, EventFilterDto eventFilterDto) throws ParseException;

    void save(EventLog eventLog);
}
