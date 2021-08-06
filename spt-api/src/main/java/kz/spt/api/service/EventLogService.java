package kz.spt.api.service;

import kz.spt.api.model.EventLog;

import java.util.Map;

public interface EventLogService {

    enum ArmEventType {
        Photo,
        CarEvent;
    }

    void createEventLog(String objectClass, Long objectId, Map<String, Object> properties, String description);

    void sendSocketMessage(ArmEventType eventType, Long gateId, String plateNumber, String message);

    Iterable<EventLog> listAllLogs();
}
