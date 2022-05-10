package kz.spt.app.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.intelligt.modbus.jlibmodbus.exception.ModbusIOException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusNumberException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusProtocolException;
import com.intelligt.modbus.jlibmodbus.master.ModbusMaster;
import kz.spt.app.model.dto.EventLogExcelDto;
import kz.spt.app.service.BarrierService;
import kz.spt.app.service.GateService;
import kz.spt.lib.bootstrap.datatable.*;
import kz.spt.lib.extension.PluginRegister;
import kz.spt.lib.model.EventLog;
import kz.spt.lib.model.EventLogSpecification;
import kz.spt.lib.model.Gate;
import kz.spt.lib.model.dto.EventFilterDto;
import kz.spt.lib.model.dto.EventsDto;
import kz.spt.lib.service.EventLogService;
import kz.spt.app.repository.EventLogRepository;
import kz.spt.lib.utils.StaticValues;
import kz.spt.app.utils.StringExtensions;
import kz.spt.lib.utils.Utils;
import lombok.extern.java.Log;
import org.pf4j.PluginManager;
import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import org.thymeleaf.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Log
@Service
public class EventLogServiceImpl implements EventLogService {

    @Autowired
    private Environment env;

    @Autowired
    private SimpMessageSendingOperations messagingTemplate;

    private String dateFormat = "yyyy-MM-dd'T'HH:mm";

    private static final Comparator<EventsDto> EMPTY_COMPARATOR = (e1, e2) -> 0;

    private ObjectMapper objectMapper = new ObjectMapper();

    private EventLogRepository eventLogRepository;

    private PluginManager pluginManager;

    private GateService gateService;

    ResourceBundle bundle = ResourceBundle.getBundle("messages", Locale.forLanguageTag("ru".equals(LocaleContextHolder.getLocale().toString()) ? "ru-RU" : "en"));

    @Value("${telegram.bot.external.enabled}")
    Boolean telegramBotExternalEnabled;

    public EventLogServiceImpl(EventLogRepository eventLogRepository, PluginManager pluginManager, GateService gateService) {
        this.eventLogRepository = eventLogRepository;
        this.pluginManager = pluginManager;
        this.gateService = gateService;
    }

    @Override
    public void createEventLog(String objectClass, Long objectId, Map<String, Object> properties, String description, String descriptionEn) {
        EventLog eventLog = new EventLog();
        eventLog.setObjectClass(objectClass);
        eventLog.setObjectId(objectId);
        eventLog.setPlateNumber((properties != null && properties.containsKey("carNumber")) ? (String) properties.get("carNumber") : "");
        eventLog.setStatusType((properties != null && properties.containsKey("type")) ? EventLog.StatusType.valueOf(properties.get("type").toString()) : null);
        eventLog.setEventType((properties != null && properties.containsKey("event")) ? EventLog.EventType.valueOf(properties.get("event").toString()) : null);
        eventLog.setDescription(description);
        eventLog.setDescriptionEn(descriptionEn);
        eventLog.setCreated(new Date());
        eventLog.setProperties(properties != null ? properties : new HashMap<>());
        eventLogRepository.save(eventLog);
    }

    @Override
    public void createEventLog(String objectClass, Long objectId, Map<String, Object> properties, String description, String descriptionEn, EventLog.EventType eventType) {
        EventLog eventLog = new EventLog();
        eventLog.setObjectClass(objectClass);
        eventLog.setObjectId(objectId);
        eventLog.setPlateNumber((properties != null && properties.containsKey("carNumber")) ? (String) properties.get("carNumber") : "");
        eventLog.setStatusType((properties != null && properties.containsKey("type")) ? EventLog.StatusType.valueOf(properties.get("type").toString()) : null);
        eventLog.setEventType((properties != null && properties.containsKey("event")) ? EventLog.EventType.valueOf(properties.get("event").toString()) : null);
        eventLog.setDescription(description);
        eventLog.setDescriptionEn(descriptionEn);
        eventLog.setCreated(new Date());
        eventLog.setProperties(properties != null ? properties : new HashMap<>());
        eventLog.setEventType(eventType);
        eventLogRepository.save(eventLog);
    }

    public void sendSocketMessage(ArmEventType eventType, EventLog.StatusType eventStatus, Long id, String plateNumber, String message, String messageEng) {

        if (ArmEventType.Photo.equals(eventType) && message == null && messageEng == null) {
            return;
        }
        SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        ObjectNode node = objectMapper.createObjectNode();
        node.put("datetime", format.format(new Date()));
        node.put("message", message);
        node.put("messageEng", messageEng);
        node.put("plateNumber", plateNumber);
        node.put("id", id);
        node.put("eventType", eventType.toString());
        node.put("eventStatus", eventStatus.toString());

        //////////////////////send notification to bot
        if(telegramBotExternalEnabled){
            sendEventBot(node);
        }
        /////////////////////

        messagingTemplate.convertAndSend("/topic", node.toString());
    }

    public void sendEventBot(ObjectNode node) {

        try{
            String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();

            URI uri = URI.create(baseUrl);
            uri = new URI(uri.getScheme(), uri.getHost(), uri.getPath(), uri.getFragment());
            String url = uri.toString() + ":8081/event";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<ObjectNode> requestEntity = new HttpEntity<>(node, headers);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
        }
        catch (URISyntaxException e){
            e.printStackTrace();
        }
        catch (Exception e){
            //e.printStackTrace();
        }
    }

    @Override
    public Iterable<EventLog> listByType(EventLog.EventType type) {
        return eventLogRepository.listByType(type);
    }

    public Long countByFilters(Specification<EventLog> eventLogSpecification) {
        if (eventLogSpecification != null) {
            return eventLogRepository.count(eventLogSpecification);
        } else {
            return eventLogRepository.count();
        }
    }

    public org.springframework.data.domain.Page<EventLog> listByFilters(Specification<EventLog> eventLogSpecification, PagingRequest pagingRequest) {
        Order order = pagingRequest.getOrder().get(0);

        int columnIndex = order.getColumn();
        Column column = pagingRequest.getColumns().get(columnIndex);
        String columnName = column.getData(); //created, plateNumber
        Direction dir = order.getDir();

        Sort sort = null;
        if("id".equals(columnName)){
            if(Direction.desc.equals(dir)){
                sort = Sort.by("id").descending();
            } else {
                sort = Sort.by("id").ascending();
            }
        } else if("created".equals(columnName)){
            if(Direction.desc.equals(dir)){
                sort = Sort.by("created").descending();
            } else {
                sort = Sort.by("created").ascending();
            }
        } else if("plateNumber".equals(columnName)){
            if(Direction.desc.equals(dir)){
                sort = Sort.by("plateNumber").descending();
            } else {
                sort = Sort.by("plateNumber").ascending();
            }
        }

        Pageable rows = PageRequest.of(pagingRequest.getStart() / pagingRequest.getLength(), pagingRequest.getLength(), sort);
        if (eventLogSpecification != null) {
            return eventLogRepository.findAll(eventLogSpecification, rows);
        } else {
            return eventLogRepository.findAll(rows);
        }
    }

    private Specification<EventLog> getEventLogFilterSpecification(EventFilterDto eventFilterDto) throws ParseException {
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
        if (eventFilterDto.gateId != null && !"".equals(eventFilterDto.gateId)) {
            specification = specification == null ? EventLogSpecification.equalGateId(eventFilterDto.gateId) : specification.and(EventLogSpecification.equalGateId(eventFilterDto.gateId));
        }
        if (eventFilterDto.eventType != null && !"".equals(eventFilterDto.eventType.toString())) {
            specification = specification == null ? EventLogSpecification.equalType(eventFilterDto.eventType) : specification.and(EventLogSpecification.equalType(eventFilterDto.eventType));
        }
        return specification;
    }

    @Override
    public EventLog getById(Long id) {
        return eventLogRepository.getOne(id);
    }

    @Override
    public Page<EventsDto> getEventLogs(PagingRequest pagingRequest, EventFilterDto eventFilterDto) throws ParseException {
        Specification<EventLog> specification = getEventLogFilterSpecification(eventFilterDto);
        Long eventLogFilterCount = countByFilters(specification);
        org.springframework.data.domain.Page<EventLog> filteredEvents =  listByFilters(specification, pagingRequest);

        var eventDtos = filteredEvents.stream()
                .map(m -> {
                    String type = m.getProperties().containsKey("type") && m.getProperties().get("type") != null ? StringExtensions.locale("events.".concat(m.getProperties().get("type").toString().toLowerCase())) : "";
                    String gate = m.getProperties().containsKey("gateName") ? m.getProperties().get("gateName").toString() : "";
//                    String eventType = m.getProperties().containsKey("EventType")
                    return EventsDto.builder()
                            .id(m.getId())
                            .created(m.getCreated())
                            .plateNumber((m.getProperties().containsKey("region") ? Utils.convertRegion(m.getProperties().get("region").toString()) + " " : "") + m.getNullSafePlateNumber() + (m.getProperties().containsKey("vecihleType") ? " [" + m.getProperties().get("vecihleType").toString() + "]" : ""))
                            .description(m.getNullSafeDescription())
                            .descriptionEn(m.getNullSafeDescriptionEn())
                            .eventType(type)
                            .gate(gate)
                            .smallImgUrl(m.getProperties().containsKey("carSmallImageUrl") && m.getProperties().get("carSmallImageUrl") != null ? (String) m.getProperties().get("carSmallImageUrl") : "")
                            .bigImgUrl(m.getProperties().containsKey("carImageUrl") && m.getProperties().get("carImageUrl") != null ? (String) m.getProperties().get("carImageUrl") : "")
                            .build();
                })
                .collect(Collectors.toList());

        return getPage(eventLogFilterCount, eventDtos, pagingRequest);
    }

    @Override
    public String getEventExcel(EventFilterDto eventFilterDto) throws Exception {
        Specification<EventLog> specification = getEventLogFilterSpecification(eventFilterDto);

        List<EventLog> events = (List<EventLog>) this.listByFilters(specification, new PagingRequest());
        Map<String, EventLogExcelDto> eventLogExcelDtoMap = new HashMap<>();

        Long parkingId = null;

        for (EventLog eventLog : events) {
            EventLogExcelDto eventLogExcelDto =
                    new EventLogExcelDto();
            if (eventLogExcelDtoMap.containsKey(eventLog.getPlateNumber())) {
                eventLogExcelDto = eventLogExcelDtoMap.get(eventLog.getPlateNumber());
            }
            eventLogExcelDto.plateNumber = eventLog.getPlateNumber();

            EventLog.StatusType type = EventLog.StatusType.valueOf((String) eventLog.getProperties().get("type"));
            if (EventLog.StatusType.Allow.equals(type)) {
                eventLogExcelDto.allow = eventLogExcelDto.allow + 1;
            } else if (EventLog.StatusType.Deny.equals(type)) {
                eventLogExcelDto.deny = eventLogExcelDto.deny + 1;
            }
            eventLogExcelDtoMap.put(eventLog.getPlateNumber(), eventLogExcelDto);

            if (parkingId == null) {
                Long gateId = Long.valueOf((Integer) eventLog.getProperties().get("gateId"));
                Gate gate = gateService.getById(gateId);
                parkingId = gate.getParking().getId();
            }
        }

        PluginRegister whitelistPluginRegister = getPluginRegister(StaticValues.whitelistPlugin);
        SimpleDateFormat format = new SimpleDateFormat(dateFormat);

        ArrayNode arrayNode = objectMapper.createArrayNode();

        for (Map.Entry<String, EventLogExcelDto> entry : eventLogExcelDtoMap.entrySet()) {
            String platenumber = entry.getKey();

            ObjectNode objectNode = objectMapper.createObjectNode();
            EventLogExcelDto eventLogExcelDto = entry.getValue();

            objectNode.put("platenumber", platenumber);
            objectNode.put("deny", eventLogExcelDto.deny);
            objectNode.put("allow", eventLogExcelDto.allow);
            objectNode.put("all", eventLogExcelDto.deny + eventLogExcelDto.allow);
            objectNode.put("isWhitelist", bundle.getString("crm.no"));

            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode command = objectMapper.createObjectNode();
            command.put("parkingId", parkingId);
            command.put("car_number", platenumber);
            command.put("event_time", format.format(new Date()));

            JsonNode result = whitelistPluginRegister.execute(command);

            if (result != null) {
                JsonNode whitelistCheckResult = result.get("whitelistCheckResult");
                if (whitelistCheckResult != null) {
                    objectNode.put("isWhitelist", bundle.getString("crm.yes"));
                }
            }
            arrayNode.add(objectNode);
        }
        return arrayNode.toString();
    }

    @Override
    public void save(EventLog eventLog) {
        eventLogRepository.save(eventLog);
    }

    @Override
    public String getApplicationPropertyValue(String propertyName) throws ModbusIOException, ModbusProtocolException, ModbusNumberException, InterruptedException {
/*
        ModbusMaster m = BarrierServiceImpl.modbusMasterMap.get("10.66.100.57");
        if(!m.isConnected()){
            m.connect();
        }
        int slaveId = 1;
        if("camera".equals(propertyName)){
            Thread.sleep(1000);
            m.writeSingleCoil(slaveId, 18, true);
            Thread.sleep(1000);
            m.writeSingleCoil(slaveId, 18, false);
        } else if("loop".equals(propertyName)){
            m.writeSingleCoil(slaveId, 18, true);
            Thread.sleep(1000);
            m.writeSingleCoil(slaveId, 19, true);
            Thread.sleep(1000);
            m.writeSingleCoil(slaveId, 18, false);
            Thread.sleep(1000);
            m.writeSingleCoil(slaveId, 19, false);
        }
*/
        return env.getProperty(propertyName);
    }

    @Override
    public String findLastNotEnoughFunds(Long gateId) {

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, -2);

        List<EventLog> eventLogs = eventLogRepository.getEventsFromDate(calendar.getTime(), Gate.class.getSimpleName(), gateId);
        if(eventLogs != null && eventLogs.size()>0){
            EventLog eventLog = eventLogs.get(0);
            if(eventLog.getDescription().startsWith("В проезде отказано: Не достаточно средств")){
                return eventLog.getPlateNumber();
            } else if(eventLog.getDescription().startsWith("Зафиксирован новый номер авто")){
                if(eventLogs.size()>1 && eventLogs.get(1).getDescription().startsWith("В проезде отказано: Не достаточно средств") && eventLog.getPlateNumber().equals(eventLogs.get(1).getPlateNumber())){
                    return eventLog.getPlateNumber();
                }
            }
        }
        return null;
    }

    @Override
    public String findLastWithDebts(Long gateId) {

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, -2);

        List<EventLog> eventLogs = eventLogRepository.getEventsFromDate(calendar.getTime(), Gate.class.getSimpleName(), gateId);
        if(eventLogs != null && eventLogs.size()>0){
            EventLog eventLog = eventLogs.get(0);
            if(eventLog.getDescription().startsWith("В проезде отказано: Авто") && eventLog.getDescription().contains("имеет задолженность")){
                return eventLog.getPlateNumber();
            } else if(eventLog.getDescription().startsWith("Зафиксирован новый номер авто")){
                if(eventLogs.size()>1 && eventLogs.get(1).getDescription().startsWith("В проезде отказано: Авто") && eventLogs.get(1).getDescription().contains("имеет задолженность") && eventLog.getPlateNumber().equals(eventLogs.get(1).getPlateNumber())){
                    return eventLog.getPlateNumber();
                }
            }
        }
        return null;
    }

    private Page<EventsDto> getPage(long count, List<EventsDto> events, PagingRequest pagingRequest) {

        Page<EventsDto> page = new Page<>(events);
        page.setRecordsFiltered((int) count);
        page.setRecordsTotal((int) count);
        page.setDraw(pagingRequest.getDraw());

        return page;
    }

    private PluginRegister getPluginRegister(String pluginId) {
        PluginWrapper pluginWrapper = pluginManager.getPlugin(pluginId);
        if (pluginWrapper != null && pluginWrapper.getPluginState().equals(PluginState.STARTED)) {
            List<PluginRegister> pluginRegisters = pluginManager.getExtensions(PluginRegister.class, pluginWrapper.getPluginId());
            if (pluginRegisters.size() > 0) {
                return pluginRegisters.get(0);
            }
        }
        return null;
    }
}