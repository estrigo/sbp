package kz.spt.app.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.intelligt.modbus.jlibmodbus.exception.ModbusIOException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusNumberException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusProtocolException;
import kz.spt.app.repository.EventLogRepository;
import kz.spt.app.service.GateService;
import kz.spt.app.utils.StringExtensions;
import kz.spt.app.job.StatusCheckJob;
import kz.spt.lib.model.dto.CameraStatusDto;
import kz.spt.app.model.dto.GateStatusDto;
import kz.spt.app.repository.EventLogRepository;
import kz.spt.lib.service.LanguagePropertiesService;
import kz.spt.app.utils.StringExtensions;
import kz.spt.lib.bootstrap.datatable.*;
import kz.spt.lib.model.EventLog;
import kz.spt.lib.model.EventLogSpecification;
import kz.spt.lib.model.Gate;
import kz.spt.lib.model.dto.EventFilterDto;
import kz.spt.lib.model.dto.EventLogExcelDto;
import kz.spt.lib.model.dto.EventsDto;
import kz.spt.lib.service.CarsService;
import kz.spt.lib.service.EventLogService;
import kz.spt.lib.utils.StaticValues;
import kz.spt.lib.utils.Utils;
import lombok.extern.java.Log;
import org.pf4j.PluginManager;
import org.pf4j.util.StringUtils;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Log
@Service
@Transactional(noRollbackFor = Exception.class)
public class EventLogServiceImpl implements EventLogService {

    @Autowired
    private Environment env;

    @Autowired
    private SimpMessageSendingOperations messagingTemplate;

    @Autowired
    private LanguagePropertiesService languagePropertiesService;

    private String dateFormat = "yyyy-MM-dd'T'HH:mm";

    private static final Comparator<EventsDto> EMPTY_COMPARATOR = (e1, e2) -> 0;

    private ObjectMapper objectMapper = new ObjectMapper();

    private EventLogRepository eventLogRepository;

    private CarsService carsService;

    private PluginManager pluginManager;

    @Value("${telegram.bot.external.enabled}")
    Boolean telegramBotExternalEnabled;

    public EventLogServiceImpl(EventLogRepository eventLogRepository, PluginManager pluginManager, CarsService carsService, LanguagePropertiesService languagePropertiesService) {
        this.eventLogRepository = eventLogRepository;
        this.pluginManager = pluginManager;
        this.carsService = carsService;
        this.languagePropertiesService = languagePropertiesService;
    }

    @Override
    public void createEventLog(String objectClass, Long objectId, Map<String, Object> properties, Map<String, Object> messageValues, String key) {
        EventLog eventLog = new EventLog();

        if(objectClass != null && objectClass.equals("Camera"))
            checkDuplicateAndSetGateName(objectId, properties);

        eventLog.setObjectClass(objectClass);

        eventLog.setPlateNumber((properties != null && properties.containsKey("carNumber")) ? (String) properties.get("carNumber") : "");
        if(StringUtils.isNotNullOrEmpty(eventLog.getPlateNumber())){
            eventLog.setCar(carsService.createCar(eventLog.getPlateNumber()));
        }

        Map<String, String> messages = languagePropertiesService.getWithDifferentLanguages(key, messageValues);

        eventLog.setObjectId(objectId);
        eventLog.setStatusType((properties != null && properties.containsKey("type")) ? EventLog.StatusType.valueOf(properties.get("type").toString()) : null);
        eventLog.setEventType((properties != null && properties.containsKey("event")) ? EventLog.EventType.valueOf(properties.get("event").toString()) : null);
        eventLog.setDescription(messages.get("ru"));
        eventLog.setDescriptionEn(messages.get("en"));
        eventLog.setDescriptionDe(messages.get("local"));
        eventLog.setCreated(new Date());
        eventLog.setProperties(properties != null ? properties : new HashMap<>());

        eventLogRepository.save(eventLog);
    }

    @Override
    public void createEventLog(String objectClass, Long objectId, Map<String, Object> properties, Map<String, Object> messageValues, String key, EventLog.EventType eventType) {
        EventLog eventLog = new EventLog();

        if(objectClass != null && objectClass.equals("Camera"))
            checkDuplicateAndSetGateName(objectId, properties);

        eventLog.setPlateNumber((properties != null && properties.containsKey("carNumber")) ? (String) properties.get("carNumber") : "");
        if(StringUtils.isNotNullOrEmpty(eventLog.getPlateNumber())){
            eventLog.setCar(carsService.createCar(eventLog.getPlateNumber()));
        }

        Map<String, String> messages = languagePropertiesService.getWithDifferentLanguages(key, messageValues);

        eventLog.setObjectClass(objectClass);
        eventLog.setObjectId(objectId);
        eventLog.setStatusType((properties != null && properties.containsKey("type")) ? EventLog.StatusType.valueOf(properties.get("type").toString()) : null);
        eventLog.setEventType((properties != null && properties.containsKey("event")) ? EventLog.EventType.valueOf(properties.get("event").toString()) : null);
        eventLog.setDescription(messages.get("ru"));
        eventLog.setDescriptionEn(messages.get("en"));
        eventLog.setDescriptionDe(messages.get("local"));
        eventLog.setCreated(new Date());
        eventLog.setProperties(properties != null ? properties : new HashMap<>());
        eventLog.setEventType(eventType);
        eventLogRepository.save(eventLog);
    }

    public void sendSocketMessage(ArmEventType eventType, EventLog.StatusType eventStatus, Long id, String plateNumber, Map<String, Object> messageValues, String key) {

        CameraStatusDto cameraStatusDto = StatusCheckJob.findCameraStatusDtoById(id);
        CameraStatusDto cameraStatusDtoByIp = StatusCheckJob.findCameraStatusDtoByIp(cameraStatusDto.ip);

        Map<String, String> messages = languagePropertiesService.getWithDifferentLanguages(key, messageValues);

        if (ArmEventType.Photo.equals(eventType) && messages.get("ru") == null && messages.get("en") == null && messages.get("local") == null) {
            return;
        }
        SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        ObjectNode node = objectMapper.createObjectNode();
        node.put("datetime", format.format(new Date()));
        node.put("message", messages.get("ru"));
        node.put("messageEng", messages.get("en"));
        node.put("messageDe", messages.get("local"));
        node.put("plateNumber", plateNumber);
        node.put("id", cameraStatusDtoByIp.id);
        node.put("eventType", eventType.toString());
        node.put("eventStatus", eventStatus.toString());

        //////////////////////send notification to bot
        if(telegramBotExternalEnabled){
            sendEventBot(node);
        }
        /////////////////////

        messagingTemplate.convertAndSend("/topic", node.toString());
    }
    public void sendSocketMessage(ArmEventType eventType, EventLog.StatusType eventStatus, Long id, String plateNumber, String snapshot) {

        CameraStatusDto cameraStatusDto = StatusCheckJob.findCameraStatusDtoById(id);
        CameraStatusDto cameraStatusDtoByIp = StatusCheckJob.findCameraStatusDtoByIp(cameraStatusDto.ip);

        if (ArmEventType.Photo.equals(eventType) && snapshot == null) {
            return;
        }
        SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        ObjectNode node = objectMapper.createObjectNode();
        node.put("datetime", format.format(new Date()));
        node.put("message", snapshot);
        node.put("plateNumber", plateNumber);
        node.put("id", cameraStatusDtoByIp.id);
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

    @Override
    public List<EventLog> listByType(List<EventLog.EventType> types) {
        return eventLogRepository.findByEventTypeIn(types);
    }

    @Override
    public List<EventLog> listByType(List<EventLog.EventType> types, Pageable pageable) {
        return eventLogRepository.findByEventTypeIn(types, pageable);
    }

    @Override
    public List<EventLog> listByTypeAndDate(List<EventLog.EventType> types, Pageable pageable,
                                            Date dateFrom, Date dateTo) {
        return eventLogRepository.findAllByCreatedBetweenAndEventTypeInOrderByIdDesc(dateFrom, dateTo, types, pageable);
    }

    @Override
    public Long countByTypeAndDate(List<EventLog.EventType> types, Date dateFrom, Date dateTo) {
        return eventLogRepository.countByCreatedBetweenAndEventTypeIn(dateFrom, dateTo,types);
    }

    @Override
    public Long countByType(List<EventLog.EventType> types) {
        return eventLogRepository.countByEventTypeIn(types);
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
            if(EventLog.EventType.PASS.equals(eventFilterDto.eventType)){
                specification = specification == null ?
                        EventLogSpecification.inEventType(eventFilterDto.eventType, EventLog.EventType.PAID_PASS, EventLog.EventType.ABONEMENT_PASS, EventLog.EventType.BOOKING_PASS, EventLog.EventType.FREE_PASS, EventLog.EventType.REGISTER_PASS)
                        : specification.and(EventLogSpecification.inEventType(eventFilterDto.eventType, EventLog.EventType.PAID_PASS, EventLog.EventType.ABONEMENT_PASS, EventLog.EventType.BOOKING_PASS, EventLog.EventType.FREE_PASS, EventLog.EventType.REGISTER_PASS));
            } else if(EventLog.EventType.DEBT.equals(eventFilterDto.eventType)){
                specification = specification == null ?
                        EventLogSpecification.inEventType(eventFilterDto.eventType, EventLog.EventType.DEBT_OUT)
                        : specification.and(EventLogSpecification.inEventType(eventFilterDto.eventType, EventLog.EventType.DEBT_OUT));
            } else {
                specification = specification == null ? EventLogSpecification.equalType(eventFilterDto.eventType) : specification.and(EventLogSpecification.equalType(eventFilterDto.eventType));
            }
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
                            .descriptionDe(m.getNullSafeDescriptionDe())
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
    public List<EventLogExcelDto> getEventExcel(EventFilterDto eventFilterDto) throws Exception {
        Specification<EventLog> specification = getEventLogFilterSpecification(eventFilterDto);
        List<EventLog> events = this.listByFiltersForExcel(specification);

        SimpleDateFormat format = new SimpleDateFormat(StaticValues.simpleDateTimeFormat);
        List<EventLogExcelDto> eventLogExcelDtos = new ArrayList<>(events.size());
        for (EventLog eventLog : events) {
            EventLogExcelDto eventLogExcelDto = new EventLogExcelDto();
            eventLogExcelDto.plateNumber = eventLog.getPlateNumber();
            eventLogExcelDto.created = format.format(eventLog.getCreated());
            eventLogExcelDto.description = eventLog.getDescriptionDe()!=null ? eventLog.getDescriptionDe(): eventLog.getDescriptionEn();
            eventLogExcelDto.status = eventLog.getProperties().containsKey("type") && eventLog.getProperties().get("type") != null ? StringExtensions.locale("events.".concat(eventLog.getProperties().get("type").toString().toLowerCase())) : "";
            eventLogExcelDto.gate = eventLog.getProperties().containsKey("gateName") ? eventLog.getProperties().get("gateName").toString() : "";
            eventLogExcelDtos.add(eventLogExcelDto);
        }

        return eventLogExcelDtos;
    }

    public List<EventLog> listByFiltersForExcel(Specification<EventLog> eventLogSpecification) {
        Sort sort = Sort.by("id").descending();
        Pageable rows = PageRequest.of(0, 1000000, sort);
        if (eventLogSpecification != null) {
            return eventLogRepository.findAll(eventLogSpecification, rows).toList();
        } else {
            return eventLogRepository.findAll(rows).toList();
        }
    }

    @Override
    public void save(EventLog eventLog) {
        eventLogRepository.save(eventLog);
    }

    @Override
    public String getApplicationPropertyValue(String propertyName) throws ModbusIOException, ModbusProtocolException, ModbusNumberException, InterruptedException {

        return env.getProperty(propertyName);
    }

    @Override
    public String findLastNotEnoughFunds(Long gateId) {

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, -2);

        List<EventLog> eventLogs = eventLogRepository.getEventsFromDate(calendar.getTime(), Gate.class.getSimpleName(), gateId);
        if(eventLogs != null && eventLogs.size()>0){
            EventLog eventLog = eventLogs.get(0);
            if(eventLog.getDescription().startsWith("?? ?????????????? ????????????????: ???? ???????????????????? ??????????????")){
                return eventLog.getPlateNumber();
            } else if(eventLog.getDescription().startsWith("???????????????????????? ?????????? ?????????? ????????")){
                if(eventLogs.size()>1 && eventLogs.get(1).getDescription().startsWith("?? ?????????????? ????????????????: ???? ???????????????????? ??????????????") && eventLog.getPlateNumber().equals(eventLogs.get(1).getPlateNumber())){
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
            if(eventLog.getDescription().startsWith("?? ?????????????? ????????????????: ????????") && eventLog.getDescription().contains("?????????? ??????????????????????????")){
                return eventLog.getPlateNumber();
            } else if(eventLog.getDescription().startsWith("???????????????????????? ?????????? ?????????? ????????")){
                if(eventLogs.size()>1 && eventLogs.get(1).getDescription().startsWith("?? ?????????????? ????????????????: ????????") && eventLogs.get(1).getDescription().contains("?????????? ??????????????????????????") && eventLog.getPlateNumber().equals(eventLogs.get(1).getPlateNumber())){
                    return eventLog.getPlateNumber();
                }
            }
        }
        return null;
    }

    @Override
    public void sendSocketMessage(String topic, String message) {
        messagingTemplate.convertAndSend("/"+topic, message);
    }

    @Override
    public String findLast(Long gateId) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, -3);

        List<EventLog> eventLogs = eventLogRepository.getEventsFromDate(calendar.getTime(), Gate.class.getSimpleName(), gateId);
        if(eventLogs != null && eventLogs.size()>0){
            EventLog eventLog = eventLogs.get(0);
            return eventLog.getPlateNumber();
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

    private void checkDuplicateAndSetGateName(Long cameraId, Map<String, Object> properties){
        CameraStatusDto cameraStatusDto = StatusCheckJob.findCameraStatusDtoById(cameraId);
        if(cameraStatusDto != null){
            GateStatusDto gateStatusDto = StatusCheckJob.findGateStatusDtoById(cameraStatusDto.gateId);

            properties.put("gateName", gateStatusDto.gateName);
        }
    }
}