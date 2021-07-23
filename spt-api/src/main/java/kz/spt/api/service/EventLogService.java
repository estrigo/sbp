package kz.spt.api.service;

import java.util.Map;

public interface EventLogService {

    void createEventLog(String objectClass, Long objectId, Map<String, Object> properties, String description);
}
