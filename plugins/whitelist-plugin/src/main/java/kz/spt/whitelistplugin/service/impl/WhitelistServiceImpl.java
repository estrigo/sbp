package kz.spt.whitelistplugin.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.spt.lib.bootstrap.datatable.Column;
import kz.spt.lib.bootstrap.datatable.Order;
import kz.spt.lib.bootstrap.datatable.Page;
import kz.spt.lib.bootstrap.datatable.PagingRequest;
import kz.spt.lib.model.Cars;
import kz.spt.lib.model.Parking;
import kz.spt.whitelistplugin.bootstrap.datatable.WhiteListComparators;
import kz.spt.whitelistplugin.model.AbstractWhitelist;
import kz.spt.whitelistplugin.model.WhitelistGroups;
import kz.spt.whitelistplugin.repository.WhitelistGroupsRepository;
import kz.spt.whitelistplugin.service.RootServicesGetterService;
import kz.spt.whitelistplugin.model.Whitelist;
import kz.spt.whitelistplugin.repository.WhitelistRepository;
import kz.spt.whitelistplugin.service.WhitelistService;
import kz.spt.whitelistplugin.viewmodel.WhiteListDto;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.pf4j.util.StringUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Log
@Service
public class WhitelistServiceImpl implements WhitelistService {

    private static final String datePrettyFormat = "dd.MM.yyyy HH:mm:ss";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final String dateformat = "yyyy-MM-dd'T'HH:mm";
    private WhitelistRepository whitelistRepository;
    private WhitelistGroupsRepository whitelistGroupsRepository;
    private RootServicesGetterService rootServicesGetterService;
    private static final Map<String, String> dayValues = new HashMap<>() {{
        put("0", "Понедельник");
        put("1", "Вторник");
        put("2", "Среда");
        put("3", "Четверг");
        put("4", "Пятница");
        put("5", "Суббота");
        put("6", "Воскресенье");
    }};
    private static final Comparator<WhiteListDto> EMPTY_COMPARATOR = (e1, e2) -> 0;

    public WhitelistServiceImpl(WhitelistRepository whitelistRepository, WhitelistGroupsRepository whitelistGroupsRepository,
                                RootServicesGetterService rootServicesGetterService) {
        this.whitelistRepository = whitelistRepository;
        this.whitelistGroupsRepository = whitelistGroupsRepository;
        this.rootServicesGetterService = rootServicesGetterService;
    }

    @Override
    public void saveWhitelist(Whitelist whitelist, UserDetails currentUser) throws Exception {
        whitelist.setPlatenumber(whitelist.getPlatenumber().toUpperCase());

        if (whitelist.getGroupId() != null) {
            WhitelistGroups group = whitelistGroupsRepository.getOne(whitelist.getGroupId());
            whitelist.setGroup(group);
            whitelist.setType(null);
            whitelist.setCustomJson(null);
            whitelist.setAccess_start(null);
            whitelist.setAccess_end(null);
        } else if (whitelist.getGroupId() == null) {
            whitelist.setGroup(null);
        }
        Cars car = rootServicesGetterService.getCarsService().createCar(whitelist.getPlatenumber());
        whitelist.setCar(car);

        SimpleDateFormat format = new SimpleDateFormat(dateformat);
        if (AbstractWhitelist.Type.PERIOD.equals(whitelist.getType())) {
            if (StringUtils.isNotNullOrEmpty(whitelist.getAccessStartString())) {
                whitelist.setAccess_start(format.parse(whitelist.getAccessStartString()));
            }
            if (StringUtils.isNotNullOrEmpty(whitelist.getAccessEndString())) {
                whitelist.setAccess_end(format.parse(whitelist.getAccessEndString()));
            }
            whitelist.setCustomJson(null);
        } else {
            if (!AbstractWhitelist.Type.CUSTOM.equals(whitelist.getType())) {
                whitelist.setCustomJson(null);
            }
            whitelist.setAccess_start(null);
            whitelist.setAccess_end(null);
        }

        if (whitelist.getId() != null) {
            whitelist.setUpdatedUser(currentUser.getUsername());
        } else {
            whitelist.setCreatedUser(currentUser.getUsername());
        }
        if (whitelist.getParkingId() != null) {
            whitelist.setParking(rootServicesGetterService.getParkingService().findById(whitelist.getParkingId()));
        }
        whitelistRepository.save(whitelist);
    }

    @Override
    public List<Whitelist> listAllWhitelist() throws JsonProcessingException{
        List<Whitelist> whitelistLists = whitelistRepository.findAll();
        for (Whitelist w : whitelistLists) {
            w.setConditionDetail(formConditionDetails(w, w.getGroup() != null ? w.getGroup().getName() : w.getCar().getPlatenumber()));
        }
        return whitelistLists;
    }

    @SneakyThrows
    @Override
    public Page<WhiteListDto> listByPage(PagingRequest pagingRequest) {
        var list = listAllWhitelist().stream()
                .map(m -> WhiteListDto.builder()
                        .id(m.getId())
                        .plateNumber(m.getCar().getPlatenumber())
                        .parkingName(m.getParking().getName())
                        .groupName(m.getGroup() != null ? m.getGroup().getName() : "")
                        .conditionDetail(m.getConditionDetail())
                        .build())
                .collect(Collectors.toList());
        return getPage(list, pagingRequest);
    }

    @Override
    public List<Whitelist> listByGroupId(Long groupId) {
        return whitelistRepository.findByGroupId(groupId);
    }

    @Override
    public ArrayNode hasAccess(Long parkingId, String platenumber, Date date) throws JsonProcessingException {

        ArrayNode arrayNode = objectMapper.createArrayNode();

        Cars car = rootServicesGetterService.getCarsService().findByPlatenumber(platenumber);
        if (car != null) {
            List<Whitelist> whitelists = whitelistRepository.findValidWhiteListByCar(car, date, parkingId);
            List<Whitelist> groupWhitelists = whitelistRepository.findValidWhiteListGroupByCar(car, date, parkingId);
            SimpleDateFormat format = new SimpleDateFormat(datePrettyFormat);

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            LocalDate localDate = LocalDate.of(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH));
            int day = localDate.getDayOfWeek().getValue() - 1;
            int hour = calendar.get(Calendar.HOUR_OF_DAY);

            for (Whitelist whitelist : whitelists) {
                boolean valid = isCustomValid(day, hour, whitelist);

                if (valid) {
                    ObjectNode objectNode = objectMapper.createObjectNode();
                    objectNode.put("id", whitelist.getId());
                    objectNode.put("plateNumber", whitelist.getCar().getPlatenumber());
                    objectNode.put("type", whitelist.getType().toString());
                    if (AbstractWhitelist.Type.PERIOD.equals(whitelist.getType())) {
                        if (whitelist.getAccess_start() != null) {
                            objectNode.put("accessStart", format.format(whitelist.getAccess_start()));
                        }
                        if (whitelist.getAccess_end() != null) {
                            objectNode.put("accessEnd", format.format(whitelist.getAccess_end()));
                        }
                    }
                    if (Whitelist.Type.CUSTOM.equals(whitelist.getType()) && whitelist.getCustomJson() != null) {
                        objectNode.set("customJson", objectMapper.readTree(whitelist.getCustomJson()));
                    }
                    objectNode.put("conditionDetails", formConditionDetails(whitelist, whitelist.getCar().getPlatenumber()));
                    arrayNode.add(objectNode);
                }
            }
            for (Whitelist whitelist : groupWhitelists) {
                Boolean valid = isCustomValid(day, hour, whitelist.getGroup());

                if (valid) {
                    ObjectNode objectNode = objectMapper.createObjectNode();
                    if (whitelist.getGroup().getSize() != null && whitelist.getGroup().getSize() > 0) {
                        objectNode.put("exceedPlaceLimit", false);
                        WhitelistGroups group = whitelist.getGroup();
                        int size = group.getSize();
                        List<String> groupPlateNumberList = whitelistRepository.findByGroupId(group.getId()).stream().map(Whitelist::getCar).map(Cars::getPlatenumber).collect(Collectors.toList());
                        List<String> enteredCarsFromGroup = rootServicesGetterService.getCarStateService().getInButNotPaidFromList(groupPlateNumberList);
                        ArrayNode placeOccupiedCars = objectMapper.createArrayNode();

                        if(group.getPlaceCarsJson() == null || "".equals(group.getPlaceCarsJson())){
                            if (enteredCarsFromGroup.size() >= size) {
                                objectNode.put("exceedPlaceLimit", true);
                                for (String carNumber : enteredCarsFromGroup) {
                                    placeOccupiedCars.add(carNumber);
                                }
                                objectNode.set("placeOccupiedCars", placeOccupiedCars);
                            }
                        } else {
                            JsonNode placeCarsJson = objectMapper.readTree(group.getPlaceCarsJson());
                            boolean contains = false;
                            for(int i=0; i<size && !contains; i++){
                                if(placeCarsJson.has("placeCars" + i) && placeCarsJson.get("placeCars" + i).has("platenumbers")){
                                    String placeOccupiedCar = null;
                                    ArrayNode platenumbers = (ArrayNode) placeCarsJson.get("placeCars" + i).get("platenumbers");
                                    Iterator<JsonNode> iterator = platenumbers.iterator();
                                    while (iterator.hasNext()){
                                        String checkingNumber = iterator.next().textValue();
                                        if(platenumber.equals(checkingNumber)){
                                            contains = true;
                                        } else if(enteredCarsFromGroup.contains(checkingNumber)) {
                                            placeOccupiedCar = checkingNumber;
                                        }
                                    }
                                    if (contains){
                                        if(placeOccupiedCar != null){
                                            objectNode.put("exceedPlaceLimit", true);
                                            placeOccupiedCars.add(placeOccupiedCar);
                                            objectNode.set("placeOccupiedCars", placeOccupiedCars);
                                            if(placeCarsJson.get("placeCars" + i).has("name")){
                                                objectNode.put("placeName", placeCarsJson.get("placeCars" + i).get("name").textValue());
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    objectNode.put("id", whitelist.getGroup().getId());
                    objectNode.put("plateNumber", whitelist.getCar().getPlatenumber());
                    objectNode.put("groupId", whitelist.getGroup().getId());
                    objectNode.put("groupName", whitelist.getGroup().getName());
                    objectNode.put("type", whitelist.getGroup().getType().toString());
                    if (Whitelist.Type.PERIOD.equals(whitelist.getGroup().getType())) {
                        if (whitelist.getGroup().getAccess_start() != null) {
                            objectNode.put("accessStart", format.format(whitelist.getGroup().getAccess_start()));
                        }
                        if (whitelist.getGroup().getAccess_end() != null) {
                            objectNode.put("accessEnd", format.format(whitelist.getGroup().getAccess_end()));
                        }
                    }
                    if (Whitelist.Type.CUSTOM.equals(whitelist.getType()) && whitelist.getCustomJson() != null) {
                        objectNode.set("customJson", objectMapper.readTree(whitelist.getCustomJson()));
                    }
                    objectNode.put("conditionDetails", formConditionDetails(whitelist.getGroup(), whitelist.getGroup().getName()));
                    arrayNode.add(objectNode);
                }
            }
        }

        return arrayNode.isEmpty() ? null : arrayNode;
    }

    @Override
    public Whitelist prepareById(Long id) {
        SimpleDateFormat format = new SimpleDateFormat(dateformat);
        Whitelist whitelist = whitelistRepository.getWithCarAndGroupAndParking(id);
        whitelist.setPlatenumber(whitelist.getCar().getPlatenumber());
        if (whitelist.getGroup() != null) {
            whitelist.setGroupId(whitelist.getGroup().getId());
        }
        if (AbstractWhitelist.Type.PERIOD.equals(whitelist.getType())) {
            if (whitelist.getAccess_start() != null) {
                whitelist.setAccessStartString(format.format(whitelist.getAccess_start()));
            }
            whitelist.setAccessEndString(format.format(whitelist.getAccess_end()));
        }
        if (whitelist.getParking() != null) {
            whitelist.setParkingId(whitelist.getParking().getId());
        }
        return whitelist;
    }

    @Override
    public void deleteById(Long id) {
        whitelistRepository.deleteById(id);
    }

    @Override
    public void saveWhitelistFromGroup(String plateNumber, WhitelistGroups group, String currentUser, Parking parking) {
        Cars car = rootServicesGetterService.getCarsService().createCar(plateNumber);

        Whitelist whitelist = whitelistRepository.findWhiteListByCar(car, parking.getId());

        if (whitelist == null) {
            whitelist = new Whitelist();
            whitelist.setGroup(group);
            whitelist.setUpdatedUser(currentUser);
            whitelist.setCar(car);
        } else {
            if (!group.getId().equals(whitelist.getGroupId())) {
                // alert
                whitelist.setGroup(group);
            }
        }
        whitelist.setParking(parking);
        whitelistRepository.save(whitelist);
    }

    @Override
    public ArrayNode getList(Long parkingId, String plateNumber) throws JsonProcessingException {
        ArrayNode arrayNode = objectMapper.createArrayNode();

        Cars car = rootServicesGetterService.getCarsService().findByPlatenumber(plateNumber);
        if (car != null) {
            Whitelist whitelist = whitelistRepository.findWhiteListByCar(car, parkingId);
            SimpleDateFormat format = new SimpleDateFormat(datePrettyFormat);
            ObjectNode objectNode = objectMapper.createObjectNode();

            if (whitelist.getGroup() == null) {
                objectNode.put("id", whitelist.getId());
                objectNode.put("plateNumber", whitelist.getCar().getPlatenumber());
                objectNode.put("type", whitelist.getType().toString());
                if (AbstractWhitelist.Type.PERIOD.equals(whitelist.getType())) {
                    if (whitelist.getAccess_start() != null) {
                        objectNode.put("accessStart", format.format(whitelist.getAccess_start()));
                    }
                    if (whitelist.getAccess_end() != null) {
                        objectNode.put("accessEnd", format.format(whitelist.getAccess_end()));
                    }
                }
                if (Whitelist.Type.CUSTOM.equals(whitelist.getType()) && whitelist.getCustomJson() != null) {
                    objectNode.set("customJson", objectMapper.readTree(whitelist.getCustomJson()));
                }
                arrayNode.add(objectNode);
            } else {
                objectNode.put("id", whitelist.getGroup().getId());
                objectNode.put("plateNumber", whitelist.getCar().getPlatenumber());
                objectNode.put("groupId", whitelist.getGroup().getId());
                objectNode.put("groupName", whitelist.getGroup().getName());
                objectNode.put("type", whitelist.getGroup().getType().toString());
                if (Whitelist.Type.PERIOD.equals(whitelist.getGroup().getType())) {
                    if (whitelist.getGroup().getAccess_start() != null) {
                        objectNode.put("accessStart", format.format(whitelist.getGroup().getAccess_start()));
                    }
                    if (whitelist.getGroup().getAccess_end() != null) {
                        objectNode.put("accessEnd", format.format(whitelist.getGroup().getAccess_end()));
                    }
                }
                if (Whitelist.Type.CUSTOM.equals(whitelist.getType()) && whitelist.getCustomJson() != null) {
                    objectNode.set("customJson", objectMapper.readTree(whitelist.getCustomJson()));
                }
                arrayNode.add(objectNode);
            }
        }

        return arrayNode.isEmpty() ? null : arrayNode;
    }

    @Override
    public Whitelist findByPlatenumber(String platenumber, Long parkingId) {
        return whitelistRepository.findByPlatenumber(platenumber, parkingId);
    }

    @Override
    public List<String> getExistingPlatenumbers(List<String> platenumbers, Long parkingId) {
        return whitelistRepository.getExistingPlatenumbers(platenumbers, parkingId);
    }

    @Override
    public List<String> getExistingPlatenumbers(List<String> platenumbers, Long parkingId, Long groupId) {
        return whitelistRepository.getExistingPlatenumbers(platenumbers, parkingId, groupId);
    }

    public static String formConditionDetails(AbstractWhitelist w, String name) throws JsonProcessingException {
        String result = "";
        SimpleDateFormat format = new SimpleDateFormat(datePrettyFormat);

        Whitelist.Type type;
        String customJson;
        String resultStart = "";

        if (w instanceof Whitelist) {
            Whitelist wl = (Whitelist) w;
            if (wl.getGroup() != null) {
                w = wl.getGroup();
            }
            resultStart = "Авто с гос. номером " + name + " может посещать объект";
        }
        if (w instanceof WhitelistGroups) {
            resultStart = "Группа с названием " + name + " может посещать объект";
        }
        customJson = w.getCustomJson();
        type = w.getType();

        if (Whitelist.Type.CUSTOM.equals(type)) {
            if (w.getCustomJson() != null) {
                JsonNode values = objectMapper.readTree(customJson);
                StringBuilder details = new StringBuilder(resultStart + " в следующие дни:");
                for (int day = 0; day < 7; day++) {
                    if (values.has("" + day)) {
                        details.append(dayValues.get(day + "") + ":");

                        TreeSet<Integer> sortedHours = new TreeSet<>();
                        for (final JsonNode hour : values.get("" + day)) {
                            sortedHours.add(hour.intValue());
                        }
                        int prev = -1, count = 0, size = sortedHours.size();
                        for (int i : sortedHours) {
                            if (count == 0) {
                                details.append("С " + i + ":00 до ");
                            } else if (i - 1 > prev) {
                                details.append((prev + 1) + ":00. " + " С " + i + ":00 до ");
                            }
                            prev = i;
                            if (count == size - 1) {
                                details.append((i + 1) + ":00. ");
                            }
                            count++;
                        }
                    }
                }
                result = details.toString();
            }
        } else if (Whitelist.Type.PERIOD.equals(type)) {
            result = resultStart + (w.getAccess_start() != null && w.getAccess_start().after(new Date()) ? " с даты " + format.format(w.getAccess_start()) : "в любое время") + ". Сроком до " + format.format(w.getAccess_end());
        } else if (Whitelist.Type.UNLIMITED.equals(type)) {
            result = resultStart + " в любое время.";
        }
        ;

        return result;
    }

    private boolean isCustomValid(int day, int hour, AbstractWhitelist abstractWhitelist) throws JsonProcessingException {
        boolean valid = true;
        if (Whitelist.Type.CUSTOM.equals(abstractWhitelist.getType())) {
            if(abstractWhitelist.getCustomJson() != null){
                JsonNode node = objectMapper.readTree(abstractWhitelist.getCustomJson());
                if (node.has(day + "")) {
                    TreeSet<Integer> sortedHours = new TreeSet<>();
                    for (final JsonNode h : node.get("" + day)) {
                        sortedHours.add(h.intValue());
                    }
                    if (!sortedHours.contains(hour)) {
                        valid = false;
                    }
                } else {
                    valid = false;
                }
            } else {
                valid = false;
            }
        }
        return valid;
    }

    private Page<WhiteListDto> getPage(List<WhiteListDto> list, PagingRequest pagingRequest) {
        var filtered = list.stream()
                .sorted(sort(pagingRequest))
                .filter(filter(pagingRequest))
                .skip(pagingRequest.getStart())
                .limit(pagingRequest.getLength())
                .collect(Collectors.toList());
        var count = list.stream()
                .filter(filter(pagingRequest))
                .count();

        var page = new Page<>(filtered);
        page.setRecordsFiltered((int) count);
        page.setRecordsTotal((int) count);
        page.setDraw(pagingRequest.getDraw());
        return page;
    }

    private Predicate<WhiteListDto> filter(PagingRequest pagingRequest) {
        if (pagingRequest.getSearch() == null || StringUtils.isNullOrEmpty(pagingRequest.getSearch()
                .getValue())) {
            return model -> true;
        }

        String value = pagingRequest.getSearch().getValue();

        return model -> (model.getPlateNumber() !=  null && model.getPlateNumber().toLowerCase().contains(value.toLowerCase())
                || (model.getParkingName() != null && model.getParkingName().toLowerCase().contains(value.toLowerCase()))
                || (model.getGroupName() != null && model.getGroupName().toLowerCase().contains(value.toLowerCase())));
    }

    private Comparator<WhiteListDto> sort(PagingRequest pagingRequest){
        if (pagingRequest.getOrder() == null) {
            return EMPTY_COMPARATOR;
        }

        try {
            Order order = pagingRequest.getOrder().get(0);

            int columnIndex = order.getColumn();
            Column column = pagingRequest.getColumns().get(columnIndex);

            Comparator<WhiteListDto> comparator = WhiteListComparators.getComparator(column.getData(), order.getDir());
            return Objects.requireNonNullElse(comparator, EMPTY_COMPARATOR);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return EMPTY_COMPARATOR;
    }
}