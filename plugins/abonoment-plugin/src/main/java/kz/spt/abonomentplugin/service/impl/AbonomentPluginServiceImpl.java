package kz.spt.abonomentplugin.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.spt.abonomentplugin.bootstrap.datatable.AbonomentDtoComparators;
import kz.spt.abonomentplugin.bootstrap.datatable.AbonomentTypeDtoComparators;
import kz.spt.abonomentplugin.dto.AbonomentDTO;
import kz.spt.abonomentplugin.dto.AbonomentTypeDTO;
import kz.spt.abonomentplugin.model.Abonoment;
import kz.spt.abonomentplugin.model.AbonomentTypes;
import kz.spt.abonomentplugin.repository.AbonomentRepository;
import kz.spt.abonomentplugin.repository.AbonomentTypesRepository;
import kz.spt.abonomentplugin.service.AbonomentPluginService;
import kz.spt.abonomentplugin.service.RootServicesGetterService;
import kz.spt.lib.bootstrap.datatable.Column;
import kz.spt.lib.bootstrap.datatable.Order;
import kz.spt.lib.bootstrap.datatable.Page;
import kz.spt.lib.bootstrap.datatable.PagingRequest;
import kz.spt.lib.model.Cars;
import kz.spt.lib.model.Parking;
import lombok.extern.java.Log;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Log
@Service
public class AbonomentPluginServiceImpl implements AbonomentPluginService {

    private final AbonomentTypesRepository abonomentTypesRepository;
    private final AbonomentRepository abonomentRepository;
    private final RootServicesGetterService rootServicesGetterService;
    private ObjectMapper objectMapper = new ObjectMapper();
    private static final Map<String, String> dayValues = new HashMap<>() {{
        put("0", "Понедельник");
        put("1", "Вторник");
        put("2", "Среда");
        put("3", "Четверг");
        put("4", "Пятница");
        put("5", "Суббота");
        put("6", "Воскресенье");
    }};


    private static final Comparator<AbonomentTypeDTO> EMPTY_COMPARATOR = (e1, e2) -> 0;
    private static final Comparator<AbonomentDTO> ABONOMENT_EMPTY_COMPARATOR = (e1, e2) -> 0;

    public AbonomentPluginServiceImpl(AbonomentTypesRepository abonomentTypesRepository, AbonomentRepository abonomentRepository, RootServicesGetterService rootServicesGetterService){
        this.abonomentTypesRepository = abonomentTypesRepository;
        this.abonomentRepository = abonomentRepository;
        this.rootServicesGetterService = rootServicesGetterService;
    }

    @Override
    public AbonomentTypes createType(int period,String customJson,String type, int price) throws JsonProcessingException {
        AbonomentTypes abonomentTypes = new AbonomentTypes();

        String custom = customJson;
        Locale locale = LocaleContextHolder.getLocale();
        String language = "en";
        if (locale.toString().equals("ru")) {
            language = "ru-RU";
        } else {
            dayValues.put("0", "Monday");
            dayValues.put("1", "Tuesday");
            dayValues.put("2", "Wednesday");
            dayValues.put("3", "Thursday");
            dayValues.put("4", "Friday");
            dayValues.put("5", "Saturday");
            dayValues.put("6", "Sunday");
        }
        ResourceBundle bundle = ResourceBundle.getBundle("messages", Locale.forLanguageTag(language));
        if (type.equals("CUSTOM")) {
            if (custom != null) {
                JsonNode values = objectMapper.readTree(customJson);
                StringBuilder details = new StringBuilder();
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
                                details.append(bundle.getString("abonoment.from") + i + bundle.getString("abonoment.until"));
                            } else if (i - 1 > prev) {
                                details.append((prev + 1) + ":00. " + bundle.getString("abonoment.from") + i + bundle.getString("abonoment.until"));
                            }
                            prev = i;
                            if (count == size - 1) {
                                details.append((i + 1) + ":00. ");
                            }
                            count++;
                        }
                    }
                }
                custom = details.toString();
            }
            abonomentTypes.setCustomJson(custom);
        }
        else {
            if (locale.toString().equals("ru")) {
                abonomentTypes.setCustomJson("Все дни недели");
            }else {
                abonomentTypes.setCustomJson("All days of the week");
            }
        }

        abonomentTypes.setPeriod(period);
        abonomentTypes.setType(type);
        abonomentTypes.setPrice(price);
        abonomentTypes.setCustomNumbers(customJson);
        AbonomentTypes savedAbonomentTypes = abonomentTypesRepository.save(abonomentTypes);

        return savedAbonomentTypes;
    }

    @Override
    public void deleteType(Long id){
        abonomentTypesRepository.deleteById(id);
    }

    @Override
    public Page<AbonomentTypeDTO> abonomentTypeDtoList(PagingRequest pagingRequest) {
        List<AbonomentTypes> allAbonomentTypes = listByFilters();
        return getPage(AbonomentTypeDTO.convertToDto(allAbonomentTypes), pagingRequest);
    }

    @Override
    public List<AbonomentTypes> getAllAbonomentTypes() {
        Locale locale = LocaleContextHolder.getLocale();

        List<AbonomentTypes> allAbonomentTypes = abonomentTypesRepository.findAll();
        for(AbonomentTypes abonomentType: allAbonomentTypes) {
            if (abonomentType.getType()!= null) {
                if(abonomentType.getType().equals("CUSTOM")) {
                    if (locale.toString().equals("ru")) {
                        abonomentType.setDescription("На " + abonomentType.getPeriod() + " дней, " + abonomentType.getPrice() + " в местной валюте" + "(выборочные дни)");
                    } else {
                        abonomentType.setDescription("For " + abonomentType.getPeriod() + " days, " + abonomentType.getPrice() + " in local currency" + "(custom days)");
                    }
                }
                else {
                    if (locale.toString().equals("ru")) {
                        abonomentType.setDescription("На " + abonomentType.getPeriod() + " дней, " + abonomentType.getPrice() + " в местной валюте");
                    } else {
                        abonomentType.setDescription("For " + abonomentType.getPeriod() + " days, " + abonomentType.getPrice() + " in local currency");
                    }
                }
            }
            else {
                if (locale.toString().equals("ru")) {
                    abonomentType.setDescription("На " + abonomentType.getPeriod() + " дней, " + abonomentType.getPrice() + " в местной валюте");
                } else {
                    abonomentType.setDescription("For " + abonomentType.getPeriod() + " days, " + abonomentType.getPrice() + " in local currency");
                }
            }
        }
        return allAbonomentTypes;
    }

    @Override
    public Abonoment createAbonoment(String platenumber, Long parkingId, Long typeId, String dateStart, Boolean checked) throws ParseException {

        final String dateTimeFormat = "yyyy-MM-dd'T'HH:mm";

        platenumber = platenumber.toUpperCase();
        Cars car = rootServicesGetterService.getCarsService().createCar(platenumber);
        Parking parking = rootServicesGetterService.getParkingService().findById(parkingId);
        AbonomentTypes type = abonomentTypesRepository.findById(typeId).get();
        SimpleDateFormat format = new SimpleDateFormat(dateTimeFormat);

        Abonoment abonoment = new Abonoment();
        abonoment.setCar(car);
        abonoment.setParking(parking);
        abonoment.setPrice(BigDecimal.valueOf(type.getPrice()));
        abonoment.setPaid(false);
        abonoment.setMonths(type.getPeriod());
        abonoment.setChecked(checked);
        Locale locale = LocaleContextHolder.getLocale();
        ResourceBundle bundle = ResourceBundle.getBundle("messages", Locale.forLanguageTag(locale.toString()));
        if (type.getType().equals("UNLIMITED")){
            abonoment.setType(bundle.getString("abonoment.allDaysinWeek"));
        }
        else {
            abonoment.setType(type.getCustomJson());
        }


        Calendar calendar = Calendar.getInstance();
        if (dateStart.equals("")){
            calendar.setTime(new Date());
        }
        else {
            calendar.setTime(format.parse(dateStart));
        }
        abonoment.setBegin(calendar.getTime());


        calendar.add(Calendar.DATE, type.getPeriod());
        abonoment.setEnd(calendar.getTime());
        Abonoment savedAbonoment = abonomentRepository.save(abonoment);

        return savedAbonoment;
    }

    @Override
    public void deleteAbonoment(Long id) {
        abonomentRepository.deleteById(id);
    }

    @Override
    public Page<AbonomentDTO> abonomentDtoList(PagingRequest pagingRequest) {
        List<Abonoment> allAbonoments = listAbonomentsByFilters();
        return getAbonomentPage(AbonomentDTO.convertToDto(allAbonoments), pagingRequest);
    }

    @Override
    public JsonNode getUnpaidNotExpiredAbonoment(String plateNumber) {
        Pageable first = PageRequest.of(0, 1);
        List<Abonoment> abonoments = abonomentRepository.findNotPaidAbonoment(plateNumber, new Date(), first);
        if(abonoments.size() > 0){
            Abonoment abonoment = abonoments.get(0);
            ObjectNode node = objectMapper.createObjectNode();
            node.put("price", abonoment.getPrice());
            node.put("parkingId", abonoment.getParking().getId());
            node.put("parkingName", abonoment.getParking().getName());
            node.put("id", abonoment.getId());
            return node;
        }
        return null;
    }

    @Override
    public void setAbonomentPaid(Long id) {
        Abonoment abonoment  = abonomentRepository.findById(id).get();
        abonoment.setPaid(true);
        abonomentRepository.save(abonoment);
    }

    @Override
    public JsonNode getPaidNotExpiredAbonoment(String plateNumber, Long parkingId, Date carInDate) {
        List<Abonoment> abonoments = abonomentRepository.findPaidNotExpiredAbonoment(plateNumber, parkingId, carInDate, new Date());

        ArrayNode abonements = objectMapper.createArrayNode();

        if(abonoments.size() > 0){
            final String dateFormat = "dd.MM.yyyy HH:mm";
            SimpleDateFormat format = new SimpleDateFormat(dateFormat);

            for(Abonoment abonoment:abonoments){
                ObjectNode result = objectMapper.createObjectNode();
                result.put("begin", format.format(abonoment.getBegin()));
                result.put("end", format.format(abonoment.getEnd()));
                abonements.add(result);
            }
        }
        return abonements.size() > 0? abonements : null;
    }

    @Override
    public Boolean checkAbonomentIntersection(String platenumber, Long parkingId, Long typeId, String dateStart, Boolean checked) throws ParseException {
        final String dateTimeFormat = "yyyy-MM-dd'T'HH:mm";

        platenumber = platenumber.toUpperCase();
        AbonomentTypes type = abonomentTypesRepository.findById(typeId).get();
        SimpleDateFormat format = new SimpleDateFormat(dateTimeFormat);

        Calendar calendar = Calendar.getInstance();

        if (dateStart.equals("")){
            calendar.setTime(new Date());
        }
        else {
            calendar.setTime(format.parse(dateStart));
        }
        Date begin = calendar.getTime();;

        calendar.add(Calendar.DATE, type.getPeriod());
        Date end = calendar.getTime();

        Long count = abonomentRepository.findIntersectionAbonoment(platenumber, parkingId, begin, end);
        return count > 0;
    }

    private List<AbonomentTypes> listByFilters() {
        return abonomentTypesRepository.findAll();
    }

    private Page<AbonomentTypeDTO> getPage(List<AbonomentTypeDTO> abonomentTypeDTOList, PagingRequest pagingRequest) {
        List<AbonomentTypeDTO> filtered = abonomentTypeDTOList.stream()
                .sorted(sortAbonomentTypeDTO(pagingRequest))
                .filter(filterAbonomentTypeDTOs(pagingRequest))
                .skip(pagingRequest.getStart())
                .limit(pagingRequest.getLength())
                .collect(Collectors.toList());

        long count = abonomentTypeDTOList.stream()
                .filter(filterAbonomentTypeDTOs(pagingRequest))
                .count();

        Page<AbonomentTypeDTO> page = new Page<>(filtered);
        page.setRecordsFiltered((int) count);
        page.setRecordsTotal((int) count);
        page.setDraw(pagingRequest.getDraw());

        return page;
    }

    private Predicate<AbonomentTypeDTO> filterAbonomentTypeDTOs(PagingRequest pagingRequest) {
        if (pagingRequest.getSearch() == null || StringUtils.isEmpty(pagingRequest.getSearch().getValue())) {
            return abonomentTypeDTOs -> true;
        }
        String value = pagingRequest.getSearch().getValue();

        return abonomentTypeDTOs -> (String.valueOf(abonomentTypeDTOs.period).contains(value) || String.valueOf(abonomentTypeDTOs.price).contains(value));
    }

    private Comparator<AbonomentTypeDTO> sortAbonomentTypeDTO(PagingRequest pagingRequest) {
        if (pagingRequest.getOrder() == null) {
            return EMPTY_COMPARATOR;
        }

        try {
            Order order = pagingRequest.getOrder().get(0);

            int columnIndex = order.getColumn();
            Column column = pagingRequest.getColumns().get(columnIndex);

            Comparator<AbonomentTypeDTO> comparator = AbonomentTypeDtoComparators.getComparator(column.getData(), order.getDir());
            return Objects.requireNonNullElse(comparator, EMPTY_COMPARATOR);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return EMPTY_COMPARATOR;
    }

    private List<Abonoment> listAbonomentsByFilters() {
        return abonomentRepository.findAll();
    }

    private Page<AbonomentDTO> getAbonomentPage(List<AbonomentDTO> abonomentDTOList, PagingRequest pagingRequest) {
        List<AbonomentDTO> filtered = abonomentDTOList.stream()
                .sorted(sortAbonomentDTO(pagingRequest))
                .filter(filterAbonomentDTOs(pagingRequest))
                .skip(pagingRequest.getStart())
                .limit(pagingRequest.getLength())
                .collect(Collectors.toList());

        long count = abonomentDTOList.stream()
                .filter(filterAbonomentDTOs(pagingRequest))
                .count();

        Page<AbonomentDTO> page = new Page<>(filtered);
        page.setRecordsFiltered((int) count);
        page.setRecordsTotal((int) count);
        page.setDraw(pagingRequest.getDraw());

        return page;
    }

    private Predicate<AbonomentDTO> filterAbonomentDTOs(PagingRequest pagingRequest) {
        if (pagingRequest.getSearch() == null || StringUtils.isEmpty(pagingRequest.getSearch().getValue())) {
            return abonomentDTOs -> true;
        }
        String value = pagingRequest.getSearch().getValue();

        return abonomentDTOs -> (String.valueOf(abonomentDTOs.platenumber).contains(value)
                || String.valueOf(abonomentDTOs.begin).contains(value)
                || String.valueOf(abonomentDTOs.end).contains(value)
                || String.valueOf(abonomentDTOs.months).contains(value)
                || String.valueOf(abonomentDTOs.price).contains(value)
                || String.valueOf(abonomentDTOs.parking).contains(value));
    }

    private Comparator<AbonomentDTO> sortAbonomentDTO(PagingRequest pagingRequest) {
        if (pagingRequest.getOrder() == null) {
            return ABONOMENT_EMPTY_COMPARATOR;
        }

        try {
            Order order = pagingRequest.getOrder().get(0);

            int columnIndex = order.getColumn();
            Column column = pagingRequest.getColumns().get(columnIndex);

            Comparator<AbonomentDTO> comparator = AbonomentDtoComparators.getComparator(column.getData(), order.getDir());
            return Objects.requireNonNullElse(comparator, ABONOMENT_EMPTY_COMPARATOR);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return ABONOMENT_EMPTY_COMPARATOR;
    }
}
