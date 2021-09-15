package kz.spt.api.service;

import jdk.jfr.Event;
import kz.spt.api.bootstrap.datatable.Page;
import kz.spt.api.bootstrap.datatable.PagingRequest;
import kz.spt.api.model.EventLog;
import kz.spt.api.model.dto.EventFilterDto;

import java.text.ParseException;
import java.util.Map;

public interface EventLogService {

    enum ArmEventType {
        Photo,
        CarEvent;
    }

    void createEventLog(String objectClass, Long objectId, Map<String, Object> properties, String description);

    void sendSocketMessage(ArmEventType eventType, Long gateId, String plateNumber, String message);

    Iterable<EventLog> listAllLogs();

    Iterable<EventLog> listByFilters(EventFilterDto eventFilterDo) throws ParseException;

    EventLog getById(Long id);

    Page<EventLog> getEventLogs(PagingRequest pagingRequest, EventFilterDto eventFilterDto) throws ParseException;

}
