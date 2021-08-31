package kz.spt.app.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.spt.api.model.EventLog;
import kz.spt.api.model.EventLogSpecification;
import kz.spt.api.model.dto.EventFilterDto;
import kz.spt.api.service.EventLogService;
import kz.spt.app.repository.EventLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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

    @Override
    public Iterable<EventLog> listAllLogs() {
        return eventLogRepository.listAllEvents();
    }

    @Override
    public Iterable<EventLog> listByFilters(EventFilterDto eventFilterDto) throws ParseException {

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
        Specification<EventLog> specification = null;

        if(eventFilterDto.dateFromString != null && eventFilterDto.dateToString != null ){
            specification = EventLogSpecification.between(format.parse(eventFilterDto.dateFromString), format.parse(eventFilterDto.dateToString));
        }
        if(eventFilterDto.plateNumber != null){
            specification = specification==null ? EventLogSpecification.equalPlateNumber(eventFilterDto.plateNumber) : specification.and(EventLogSpecification.equalPlateNumber(eventFilterDto.plateNumber));
        }
        if(specification != null){
            specification =  specification.and(EventLogSpecification.orderById());
            return eventLogRepository.findAll(specification);
        } else {
            return eventLogRepository.findAll();
        }
    }

    @Override
    public EventLog getById(Long id) {
        return eventLogRepository.getOne(id);
    }
}