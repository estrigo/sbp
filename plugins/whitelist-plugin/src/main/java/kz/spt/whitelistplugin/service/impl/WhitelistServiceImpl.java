package kz.spt.whitelistplugin.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.spt.lib.model.Cars;
import kz.spt.whitelistplugin.model.AbstractWhitelist;
import kz.spt.whitelistplugin.model.WhitelistCategory;
import kz.spt.whitelistplugin.model.WhitelistGroups;
import kz.spt.whitelistplugin.repository.WhitelistCategoryRepository;
import kz.spt.whitelistplugin.repository.WhitelistGroupsRepository;
import kz.spt.whitelistplugin.service.RootServicesGetterService;
import kz.spt.whitelistplugin.model.Whitelist;
import kz.spt.whitelistplugin.repository.WhitelistRepository;
import kz.spt.whitelistplugin.service.WhitelistService;
import lombok.extern.java.Log;
import org.pf4j.util.StringUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Log
@Service
public class WhitelistServiceImpl implements WhitelistService {

    private static final String datePrettyFormat = "dd.MM.yyyy HH:mm:ss";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final String dateformat = "yyyy-MM-dd'T'HH:mm";
    private WhitelistRepository whitelistRepository;
    private WhitelistGroupsRepository whitelistGroupsRepository;
    private WhitelistCategoryRepository whitelistCategoryRepository;
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

    public WhitelistServiceImpl(WhitelistRepository whitelistRepository, WhitelistGroupsRepository whitelistGroupsRepository,
                                RootServicesGetterService rootServicesGetterService, WhitelistCategoryRepository whitelistCategoryRepository) {
        this.whitelistRepository = whitelistRepository;
        this.whitelistGroupsRepository = whitelistGroupsRepository;
        this.rootServicesGetterService = rootServicesGetterService;
        this.whitelistCategoryRepository  = whitelistCategoryRepository;
    }

    @Override
    public void saveWhitelist(Whitelist whitelist, UserDetails currentUser) throws Exception {
        whitelist.setPlatenumber(whitelist.getPlatenumber().toUpperCase());

        if (whitelist.getGroupId() != null) {
            WhitelistGroups group = whitelistGroupsRepository.getOne(whitelist.getGroupId());
            whitelist.setGroup(group);
            whitelist.setCategory(null);
            whitelist.setType(null);
            whitelist.setCustomJson(null);
            whitelist.setAccess_start(null);
            whitelist.setAccess_end(null);
        } else if (whitelist.getGroupId() == null) {
            whitelist.setGroup(null);
        }
        if(whitelist.getCategoryId() != null){
            WhitelistCategory whitelistCategory = whitelistCategoryRepository.getOne(whitelist.getCategoryId());
            whitelist.setCategory(whitelistCategory);
        } else {
            whitelist.setCategory(null);
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
        whitelistRepository.save(whitelist);
    }

    @Override
    public Iterable<Whitelist> listAllWhitelist() throws JsonProcessingException {
        List<Whitelist> whitelistLists = whitelistRepository.findAll();
        for(Whitelist w: whitelistLists){
            w.setConditionDetail(formConditionDetails(w, w.getCar().getPlatenumber()));
        }
        return whitelistLists;
    }

    @Override
    public List<Whitelist> listByGroupId(Long groupId) {
        return whitelistRepository.findByGroupId(groupId);
    }

    @Override
    public ArrayNode hasAccess(String platenumber, Date date) throws JsonProcessingException {
        ArrayNode arrayNode = objectMapper.createArrayNode();

        Cars car = rootServicesGetterService.getCarsService().findByPlatenumber(platenumber);
        if (car != null) {
            List<Whitelist> whitelists = whitelistRepository.findValidWhiteListByCar(car, date);
            List<Whitelist> groupWhitelists = whitelistRepository.findValidWhiteListGroupByCar(car, date);
            SimpleDateFormat format =  new SimpleDateFormat(datePrettyFormat);

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            LocalDate localDate = LocalDate.of(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH));
            int day = localDate.getDayOfWeek().getValue()-1;
            int hour = calendar.get(Calendar.HOUR_OF_DAY);

            for(Whitelist whitelist : whitelists){
                boolean valid = isCustomValid(day, hour, whitelist);

                if(valid){
                    ObjectNode objectNode = objectMapper.createObjectNode();
                    objectNode.put("id", whitelist.getId());
                    objectNode.put("plateNumber", whitelist.getCar().getPlatenumber());
                    objectNode.put("type", whitelist.getType().toString());
                    if(whitelist.getCategory() != null){
                        objectNode.put("categoryName", whitelist.getCategory().getName());
                    }
                    if(AbstractWhitelist.Type.PERIOD.equals(whitelist.getType())){
                        if(whitelist.getAccess_start() != null){
                            objectNode.put("accessStart", format.format(whitelist.getAccess_start()));
                        }
                        if(whitelist.getAccess_end() != null){
                            objectNode.put("accessEnd", format.format(whitelist.getAccess_end()));
                        }
                    }
                    if(Whitelist.Type.CUSTOM.equals(whitelist.getType()) && whitelist.getCustomJson() != null){
                        objectNode.set("customJson", objectMapper.readTree(whitelist.getCustomJson()));
                    }
                    objectNode.put("conditionDetails", formConditionDetails(whitelist, whitelist.getCar().getPlatenumber()));
                    arrayNode.add(objectNode);
                }
            }
            for(Whitelist whitelist : groupWhitelists){
                Boolean valid = isCustomValid(day, hour, whitelist.getGroup());

                if(valid) {
                    ObjectNode objectNode = objectMapper.createObjectNode();
                    if(whitelist.getGroup().getSize() != null && whitelist.getGroup().getSize() >  0){
                        objectNode.put("exceedPlaceLimit", false);
                        int size = whitelist.getGroup().getSize();
                        List<String> groupPlateNumberList = whitelist.getGroup().getWhitelists().stream().map(Whitelist::getCar).map(Cars::getPlatenumber).collect(Collectors.toList());
                        List<String> enteredCarsFromGroup = rootServicesGetterService.getCarStateService().getInButNotPaidFromList(groupPlateNumberList);
                        if(enteredCarsFromGroup.size() >= size){
                            objectNode.put("exceedPlaceLimit", true);
                            ArrayNode list = objectMapper.createArrayNode();
                            for(String carNumber : enteredCarsFromGroup){
                                list.add(carNumber);
                            }
                            objectNode.set("placeOccupiedCars", list);
                        }
                    }

                    objectNode.put("id", whitelist.getGroup().getId());
                    objectNode.put("plateNumber", whitelist.getCar().getPlatenumber());
                    objectNode.put("groupId", whitelist.getGroup().getId());
                    objectNode.put("groupName", whitelist.getGroup().getName());
                    objectNode.put("type", whitelist.getGroup().getType().toString());
                    if(whitelist.getGroup().getCategory() != null){
                        objectNode.put("categoryName", whitelist.getGroup().getCategory().getName());
                    }
                    if (Whitelist.Type.PERIOD.equals(whitelist.getGroup().getType())) {
                        if (whitelist.getGroup().getAccess_start() != null) {
                            objectNode.put("accessStart", format.format(whitelist.getGroup().getAccess_start()));
                        }
                        if (whitelist.getGroup().getAccess_end() != null) {
                            objectNode.put("accessEnd", format.format(whitelist.getGroup().getAccess_end()));
                        }
                    }
                    if(Whitelist.Type.CUSTOM.equals(whitelist.getType()) && whitelist.getCustomJson() != null){
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
        Whitelist whitelist = whitelistRepository.getWithCarAndGroup(id);
        whitelist.setPlatenumber(whitelist.getCar().getPlatenumber());
        if (whitelist.getGroup() != null) {
            whitelist.setGroupId(whitelist.getGroup().getId());
        }
        if (whitelist.getCategory() != null) {
            whitelist.setCategoryId(whitelist.getCategory().getId());
        }
        if (AbstractWhitelist.Type.PERIOD.equals(whitelist.getType())) {
            if (whitelist.getAccess_start() != null) {
                whitelist.setAccessStartString(format.format(whitelist.getAccess_start()));
            }
            whitelist.setAccessEndString(format.format(whitelist.getAccess_end()));
        }
        return whitelist;
    }

    @Override
    public void deleteById(Long id) {
        whitelistRepository.deleteById(id);
    }

    @Override
    public void saveWhitelistFromGroup(String plateNumber, WhitelistGroups group, String currentUser) {
        Cars car = rootServicesGetterService.getCarsService().createCar(plateNumber);

        Whitelist whitelist = whitelistRepository.findWhiteListByCar(car);

        if (whitelist == null) {
            whitelist = new Whitelist();
            whitelist.setGroup(group);
            whitelist.setUpdatedUser(currentUser);
            whitelist.setCar(car);
        } else {
            if(!group.getId().equals(whitelist.getGroupId())) {
                // alert
                whitelist.setGroup(group);
            }
        }
        whitelistRepository.save(whitelist);
    }

    public static String formConditionDetails(AbstractWhitelist w, String name) throws JsonProcessingException {
        String result = "";
        SimpleDateFormat format = new SimpleDateFormat(datePrettyFormat);

        Whitelist.Type type;
        String customJson;
        String resultStart = "";

        if(w instanceof Whitelist){
            Whitelist wl = (Whitelist) w;
            if(wl.getGroup() != null){
                w = wl.getGroup();
            }
            resultStart = "Авто с гос. номером " + name + " может посещать объект";
        } if(w instanceof WhitelistGroups){
            resultStart = "Группа с названием " + name + " может посещать объект";
        }
        customJson = w.getCustomJson();
        type = w.getType();

        if(Whitelist.Type.CUSTOM.equals(type)){
            if(w.getCustomJson() != null){
                JsonNode values = objectMapper.readTree(customJson);
                StringBuilder details = new StringBuilder(resultStart + " в следующие дни:");
                for(int day = 0; day < 7; day++){
                    if(values.has("" + day)){
                        details.append(dayValues.get(day + "") + ":");

                        TreeSet<Integer> sortedHours = new TreeSet<>();
                        for (final JsonNode hour : values.get("" + day)) {
                            sortedHours.add(hour.intValue());
                        }
                        int prev = -1, count = 0, size = sortedHours.size();
                        for(int i : sortedHours){
                            if(count == 0){
                                details.append("С " + i + ":00 до ");
                            } else if(i-1 > prev){
                                details.append((prev + 1) + ":00. " + " С " + i + ":00 до ");
                            }
                            prev = i;
                            if(count == size-1){
                                details.append((i + 1) + ":00. ");
                            }
                            count++;
                        }
                    }
                }
                result = details.toString();
            }
        } else if(Whitelist.Type.PERIOD.equals(type)){
            result = resultStart + (w.getAccess_start() != null && w.getAccess_start().after(new Date()) ? " с даты " + format.format(w.getAccess_start()) : "в любое время") + ". Сроком до " + format.format(w.getAccess_end());
        } else if(Whitelist.Type.UNLIMITED.equals(type)){
            result = resultStart + " в любое время.";
        };

        return result;
    }

    private boolean isCustomValid(int day, int hour, AbstractWhitelist abstractWhitelist) throws JsonProcessingException {
        boolean valid = true;
        if(Whitelist.Type.CUSTOM.equals(abstractWhitelist.getType()) && abstractWhitelist.getCustomJson() != null){
            JsonNode node = objectMapper.readTree(abstractWhitelist.getCustomJson());
            if(node.has(day+"")){
                TreeSet<Integer> sortedHours = new TreeSet<>();
                for (final JsonNode h : node.get("" + day)) {
                    sortedHours.add(h.intValue());
                }
                if(!sortedHours.contains(hour)){
                    valid = false;
                }
            } else {
                valid = false;
            }
        }
        return valid;
    }
}