package kz.spt.app.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.spt.lib.bootstrap.datatable.*;
import kz.spt.lib.model.EventLog;
import kz.spt.lib.model.EventLogSpecification;
import kz.spt.lib.model.dto.CarEventDto;
import kz.spt.lib.model.dto.EventFilterDto;
import kz.spt.lib.service.EventLogService;
import kz.spt.app.repository.EventLogRepository;
import lombok.extern.java.Log;
import org.apache.poi.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.thymeleaf.util.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Log
@Service
public class EventLogServiceImpl implements EventLogService {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private EventLogRepository eventLogRepository;

    @Autowired
    private SimpMessageSendingOperations messagingTemplate;

    private static final Comparator<EventLog> EMPTY_COMPARATOR = (e1, e2) -> 0;

    @Override
    public void createEventLog(String objectClass, Long objectId, Map<String, Object> properties, String description) {
        EventLog eventLog = new EventLog();
        eventLog.setObjectClass(objectClass);
        eventLog.setObjectId(objectId);
        eventLog.setPlateNumber((properties != null && properties.containsKey("carNumber")) ? (String) properties.get("carNumber") : "");
        eventLog.setDescription(description);
        eventLog.setCreated(new Date());
        eventLog.setProperties(properties != null ? properties : new HashMap<>());
        eventLogRepository.save(eventLog);
    }

    public void sendSocketMessage(ArmEventType eventType, Long id, String plateNumber, String message) {

        SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
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

        if (eventFilterDto.dateToString != null && !"".equals(eventFilterDto.dateToString)) {
            specification = EventLogSpecification.lessDate(format.parse(eventFilterDto.dateToString));
        }
        if (eventFilterDto.dateFromString != null && !"".equals(eventFilterDto.dateFromString)) {
            specification = specification == null ? EventLogSpecification.greaterDate(format.parse(eventFilterDto.dateFromString)) : specification.and(EventLogSpecification.greaterDate(format.parse(eventFilterDto.dateFromString)));
        }
        if (eventFilterDto.plateNumber != null && !"".equals(eventFilterDto.plateNumber)) {
            specification = specification == null ? EventLogSpecification.likePlateNumber(eventFilterDto.plateNumber) : specification.and(EventLogSpecification.likePlateNumber(eventFilterDto.plateNumber));
        }
        if (eventFilterDto.description != null && !"".equals(eventFilterDto.description)) {
            specification = specification == null ? EventLogSpecification.likeDescription(eventFilterDto.description) : specification.and(EventLogSpecification.likeDescription(eventFilterDto.description));
        }
        if (specification != null) {
            specification = specification.and(EventLogSpecification.orderById());
            return eventLogRepository.findAll(specification);
        } else {
            return eventLogRepository.findAll();
        }
    }

    @Override
    public EventLog getById(Long id) {
        return eventLogRepository.getOne(id);
    }

    @Override
    public Page<EventLog> getEventLogs(PagingRequest pagingRequest, EventFilterDto eventFilterDto) throws ParseException {
        List<EventLog> events = (List<EventLog>) this.listByFilters(eventFilterDto);
        List<EventLog> filteredEvents  = new ArrayList<>();
        for(EventLog eventLog : events){
            Map<String, Object> properties = eventLog.getProperties();
            if(eventFilterDto.gateId != null){
                if(properties.containsKey("gateId") && properties.get("gateId") != null && ((Integer)properties.get("gateId")).equals(eventFilterDto.gateId.intValue())){
                    filteredEvents.add(eventLog);
                }
            } else {
                filteredEvents.add(eventLog);
            }
        }
        return getPage(filteredEvents, pagingRequest);
    }

    private Page<EventLog> getPage(List<EventLog> events, PagingRequest pagingRequest) {
        List<EventLog> filtered = events.stream()
                .sorted(sortEvents(pagingRequest))
                .filter(filterEvents(pagingRequest))
                .skip(pagingRequest.getStart())
                .limit(pagingRequest.getLength())
                .collect(Collectors.toList());

        long count = events.stream()
                .filter(filterEvents(pagingRequest))
                .count();

        Page<EventLog> page = new Page<>(filtered);
        page.setRecordsFiltered((int) count);
        page.setRecordsTotal((int) count);
        page.setDraw(pagingRequest.getDraw());

        return page;
    }

    private Predicate<EventLog> filterEvents(PagingRequest pagingRequest) {
        if (pagingRequest.getSearch() == null || StringUtils.isEmpty(pagingRequest.getSearch()
                .getValue())) {
            return events -> true;
        }
        String value = pagingRequest.getSearch().getValue();

        return events -> (events.getCreated() != null && events.getCreated().toString().toLowerCase().contains(value.toLowerCase())
                || (events.getDescription() != null && events.getDescription().toLowerCase().contains(value.toLowerCase()))
        );
    }

    private Comparator<EventLog> sortEvents(PagingRequest pagingRequest) {
        if (pagingRequest.getOrder() == null) {
            return EMPTY_COMPARATOR;
        }

        try {
            Order order = pagingRequest.getOrder()
                    .get(0);

            int columnIndex = order.getColumn();
            Column column = pagingRequest.getColumns()
                    .get(columnIndex);

            Comparator<EventLog> comparator = EventComparator.getComparator(column.getData(), order.getDir());
            return Objects.requireNonNullElse(comparator, EMPTY_COMPARATOR);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return EMPTY_COMPARATOR;
    }


}