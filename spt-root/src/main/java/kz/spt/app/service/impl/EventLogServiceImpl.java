package kz.spt.app.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.spt.api.model.EventLog;
import kz.spt.api.service.EventLogService;
import kz.spt.app.repository.EventLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class EventLogServiceImpl implements EventLogService {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private EventLogRepository eventLogRepository;

    @Autowired
    private SimpMessageSendingOperations messagingTemplate;

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

    public void sendSocketMessage(ArmEventType eventType, Long id, String plateNumber, String message){

        SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy hh:mm");
        ObjectNode node = objectMapper.createObjectNode();
        node.put("datetime", format.format(new Date()));
        node.put("message", message);
        node.put("plateNumber", plateNumber);
        node.put("id", id);
        node.put("eventType", eventType.toString());

        messagingTemplate.convertAndSend("/topic", node.toString());
    }
}