package kz.spt.app.service.impl;

import kz.spt.api.model.EventLog;
import kz.spt.api.service.EventLogService;
import kz.spt.app.repository.EventLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class EventLogServiceImpl implements EventLogService {

    @Autowired
    private EventLogRepository eventLogRepository;

    @Override
    public void createEventLog(String objectClass, Long objectId, Map<String, Object> properties, String description) {
        EventLog eventLog = new EventLog();
        eventLog.setObjectClass(objectClass);
        eventLog.setObjectId(objectId);
        eventLog.setDescription(description);
        eventLog.setCreated(new Date());
        eventLog.setProperties(properties != null ? properties : new HashMap<>());
        eventLogRepository.save(eventLog);
    }
}
