package kz.spt.abonomentplugin.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.spt.abonomentplugin.bootstrap.datatable.AbonomentTypeDtoComparators;
import kz.spt.abonomentplugin.dto.AbonomentDTO;
import kz.spt.abonomentplugin.dto.AbonomentTypeDTO;
import kz.spt.abonomentplugin.model.Abonement;
import kz.spt.abonomentplugin.model.AbonementSpecification;
import kz.spt.abonomentplugin.model.AbonomentTypes;
import kz.spt.abonomentplugin.model.dto.AbonementFilterDto;
import kz.spt.abonomentplugin.repository.AbonomentRepository;
import kz.spt.abonomentplugin.repository.AbonomentTypesRepository;
import kz.spt.abonomentplugin.repository.CarsRepository;
import kz.spt.abonomentplugin.service.AbonomentPluginService;
import kz.spt.abonomentplugin.service.RootServicesGetterService;
import kz.spt.lib.bootstrap.datatable.*;
import kz.spt.lib.model.Cars;
import kz.spt.lib.model.Parking;
import lombok.extern.java.Log;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Log
@Service
@Transactional(noRollbackFor = Exception.class)
public class AbonomentPluginServiceImpl implements AbonomentPluginService {

    private final AbonomentTypesRepository abonomentTypesRepository;
    private final AbonomentRepository abonomentRepository;
    private final RootServicesGetterService rootServicesGetterService;
    private final CarsRepository carsRepository;

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

    public AbonomentPluginServiceImpl(CarsRepository carsRepository,
                                      AbonomentTypesRepository abonomentTypesRepository,
                                      AbonomentRepository abonomentRepository,
                                      RootServicesGetterService rootServicesGetterService){
        this.abonomentTypesRepository = abonomentTypesRepository;
        this.abonomentRepository = abonomentRepository;
        this.rootServicesGetterService = rootServicesGetterService;
        this.carsRepository = carsRepository;
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
    public void deleteAbonomentByParkingID(Long parkingId) {
        List<Abonement> abonementList = abonomentRepository.findAbonomentByParking(parkingId);
        for (int i = 0; i< abonementList.size(); i++) {
            abonomentRepository.deleteById(abonementList.get(i).getId());
        }
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
    public Abonement createAbonoment(String platenumber, Long parkingId, Long typeId, String dateStart, Boolean checked) throws ParseException {

        final String dateTimeFormat = "yyyy-MM-dd'T'HH:mm";

        platenumber = platenumber.toUpperCase();
        Cars car = rootServicesGetterService.getCarsService().createCarObject(platenumber, null, null, null);
        carsRepository.save(car);
        Parking parking = rootServicesGetterService.getParkingService().findById(parkingId);
        AbonomentTypes type = abonomentTypesRepository.findById(typeId).get();
        SimpleDateFormat format = new SimpleDateFormat(dateTimeFormat);

        Abonement abonement = new Abonement();
        abonement.setCar(car);
        abonement.setParking(parking);
        abonement.setPrice(BigDecimal.valueOf(type.getPrice()));
        abonement.setPaid(false);
        abonement.setMonths(type.getPeriod());
        abonement.setChecked(checked);
        Locale locale = LocaleContextHolder.getLocale();
        ResourceBundle bundle = ResourceBundle.getBundle("messages", Locale.forLanguageTag(locale.toString()));
        if (type.getType().equals("UNLIMITED")){
            abonement.setType(bundle.getString("abonoment.allDaysinWeek"));
        }
        else {
            abonement.setType(type.getCustomJson());
        }
        abonement.setCustomNumbers(type.getCustomNumbers());

        Calendar calendar = Calendar.getInstance();
        if (dateStart.equals("")){
            calendar.setTime(new Date());
        }
        else {
            calendar.setTime(format.parse(dateStart));
        }
        abonement.setBegin(calendar.getTime());

        abonement.setPaidType(type.getType());
        calendar.add(Calendar.DATE, type.getPeriod());
        abonement.setEnd(calendar.getTime());
        return abonomentRepository.save(abonement);
    }

    @Override
    public void deleteAbonoment(Long id) {
        abonomentRepository.deleteById(id);
    }

    @Override
    public Page<AbonomentDTO> abonomentDtoList(PagingRequest pagingRequest, AbonementFilterDto filter) throws ParseException {
        Specification<Abonement> abonementSpecification = getAbonementSpecification(filter);
        org.springframework.data.domain.Page<Abonement> filteredAbonements = listAbonomentsByFilters(abonementSpecification, pagingRequest);
        return getAbonomentPage(filteredAbonements.getTotalElements(), AbonomentDTO.convertToDto(filteredAbonements.toList()), pagingRequest);
    }

    @Override
    public JsonNode getUnpaidNotExpiredAbonoment(String plateNumber) {
        Pageable first = PageRequest.of(0, 1);
        List<Abonement> abonements = abonomentRepository.findNotPaidAbonoment(plateNumber, new Date(), first);
        if(abonements.size() > 0){
            Abonement abonement = abonements.get(0);
            ObjectNode node = objectMapper.createObjectNode();
            node.put("price", abonement.getPrice());
            node.put("parkingId", abonement.getParking().getId());
            node.put("parkingName", abonement.getParking().getName());
            node.put("id", abonement.getId());
            return node;
        }
        return null;
    }

    @Override
    public void setAbonomentPaid(Long id) {
        Abonement abonement = abonomentRepository.findById(id).get();
        abonement.setPaid(true);
        if (abonement.getChecked()){
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date());
            abonement.setBegin(calendar.getTime());
            calendar.add(Calendar.DATE, abonement.getMonths());
            abonement.setEnd(calendar.getTime());
        }
        abonomentRepository.save(abonement);
    }

    @Override
    public JsonNode getPaidNotExpiredAbonoment(String plateNumber, Long parkingId, Date carInDate) throws JsonProcessingException {

        Date newDate = new Date();
        List<Abonement> abonoments = abonomentRepository.findPaidNotExpiredAbonoment(plateNumber, parkingId, carInDate, newDate);

        ArrayNode abonements = objectMapper.createArrayNode();

        if(abonoments.size() > 0){
            final String dateFormat = "dd.MM.yyyy HH:mm";
            SimpleDateFormat format = new SimpleDateFormat(dateFormat);

            for(Abonement abonement:abonoments){
                ObjectNode result = objectMapper.createObjectNode();
                result.put("begin", format.format(abonement.getBegin()));
                result.put("end", format.format(abonement.getEnd()));
                result.put("type", abonement.getPaidType());
                result.put("custom_numbers", abonement.getCustomNumbers());

                if("CUSTOM".equals(abonement.getPaidType())){
                    Boolean hasValueInsidePeriod = false;
                    JsonNode custom_numbersJson = objectMapper.readTree(abonement.getCustomNumbers());

                    Calendar startCalendar = Calendar.getInstance();
                    startCalendar.setTime(carInDate);

                    while (startCalendar.getTime().before(newDate)){
                        LocalDate localDate = LocalDate.of(startCalendar.get(Calendar.YEAR), startCalendar.get(Calendar.MONTH) + 1, startCalendar.get(Calendar.DAY_OF_MONTH));
                        int day = localDate.getDayOfWeek().getValue() - 1;
                        int hour = startCalendar.get(Calendar.HOUR_OF_DAY);

                        if (custom_numbersJson.has(day + "")) {
                            TreeSet<Integer> sortedHours = new TreeSet<>();
                            for (final JsonNode h : custom_numbersJson.get("" + day)) {
                                sortedHours.add(h.intValue());
                            }
                            if (sortedHours.contains(hour)) {
                                hasValueInsidePeriod = true;
                                break;
                            }
                        }
                        startCalendar.add(Calendar.HOUR_OF_DAY, 1);
                    }

                    if(hasValueInsidePeriod){
                        abonements.add(result);
                    }
                } else {
                    abonements.add(result);
                }
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

    @Override
    public void deleteNotPaidExpired() {
        LocalDateTime current = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
        current = current.minusDays(31); // Если больше 31 дней не оплачивал то удалять
        Date checkDate = Date.from(current.atZone(ZoneId.systemDefault()).toInstant());

        List<Abonement> abonements = abonomentRepository.findExpiredNotPaid(checkDate);
        abonomentRepository.deleteAll(abonements);
    }

    @Override
    public void creteNewForOld() {
        LocalDateTime current = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS); // Если до окончания срока осталась 2 дня то продлевать
        Date currentDate = java.util.Date.from(current.atZone(ZoneId.systemDefault()).toInstant());
        current = current.plusDays(2);
        Date checkDate = Date.from(current.atZone(ZoneId.systemDefault()).toInstant());

        List<Abonement> expiringAbonements = abonomentRepository.findPaidExpiresInFewDays(currentDate, checkDate);
        for(Abonement expiring : expiringAbonements){
            Calendar begin = Calendar.getInstance();
            begin.setTime(expiring.getEnd());

            Abonement newAbonement = new Abonement();
            newAbonement.setParking(expiring.getParking());
            newAbonement.setType(expiring.getType());
            newAbonement.setCar(expiring.getCar());
            newAbonement.setChecked(expiring.getChecked());
            newAbonement.setCustomNumbers(expiring.getCustomNumbers());
            newAbonement.setMonths(expiring.getMonths());
            newAbonement.setPaidType(expiring.getPaidType());
            newAbonement.setPrice(expiring.getPrice());

            try {
                BigDecimal balance = rootServicesGetterService.getBalance(expiring.getCar().getPlatenumber());
                if(balance.compareTo(expiring.getPrice()) >= 0){
                    rootServicesGetterService.decreaseBalance(expiring.getCar().getPlatenumber(), expiring.getPrice(), expiring.getParking().getName());
                    newAbonement.setPaid(true);
                } else {
                    newAbonement.setPaid(false);
                }
            } catch (Exception e){
                log.warning("Ошибка работы с балансом: " + expiring.getCar().getPlatenumber() + " ошибка: " + e.getMessage());
                newAbonement.setPaid(false);
            }



            newAbonement.setBegin(begin.getTime());
            begin.add(Calendar.DATE, expiring.getMonths());
            newAbonement.setEnd(begin.getTime());
            if(abonomentRepository.findIntersectionAbonoment(expiring.getCar().getPlatenumber(),expiring.getParking().getId(), newAbonement.getBegin(), newAbonement.getEnd()) == 0L){
                abonomentRepository.save(newAbonement);
            }
        }
    }

    @Override
    public String checkExpiration(String plateNumber, Long parkingId) {

        LocalDateTime now = LocalDateTime.now();
        Date currentDate = java.util.Date.from(now.atZone(ZoneId.systemDefault()).toInstant());
        now = now.plusDays(1);
        Date expireCheckDate = java.util.Date.from(now.atZone(ZoneId.systemDefault()).toInstant());

        List<Abonement> closeToExpireAbonement = abonomentRepository.findExpiresInFewDays(plateNumber, parkingId, currentDate, expireCheckDate);
        if(closeToExpireAbonement.size() > 0){
            return "closeToExire";
        }

        now = now.minusDays(1).minusDays(31);
        Date expiredCheckDate = java.util.Date.from(now.atZone(ZoneId.systemDefault()).toInstant());

        List<Abonement> expiredAbonements = abonomentRepository.findExpiresInFewDays(plateNumber, parkingId, expiredCheckDate, currentDate);
        if(expiredAbonements.size() > 0){
            List<Abonement> currentValidAbonements = abonomentRepository.findValidByPlatenumber(plateNumber, currentDate);
            if(currentValidAbonements.size() > 0){
                return "unknown";
            }
            return "expired";
        }
        return "unknown";
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

    private org.springframework.data.domain.Page<Abonement> listAbonomentsByFilters(Specification<Abonement> abonementSpecification, PagingRequest pagingRequest) {
        Order order = pagingRequest.getOrder().get(0);

        int columnIndex = order.getColumn();
        Column column = pagingRequest.getColumns().get(columnIndex);
        String columnName = column.getData();
        Direction dir = order.getDir();

        Sort sort = null;
        if("platenumber".equals(columnName)){
            if(Direction.desc.equals(dir)){
                sort = Sort.by("car.platenumber").descending();
            } else {
                sort = Sort.by("car.platenumber").ascending();
            }
        } else if("begin".equals(columnName)){
            if(Direction.desc.equals(dir)){
                sort = Sort.by("begin").descending();
            } else {
                sort = Sort.by("begin").ascending();
            }
        } else if("end".equals(columnName)){
            if(Direction.desc.equals(dir)){
                sort = Sort.by("end").descending();
            } else {
                sort = Sort.by("end").ascending();
            }
        } else if("months".equals(columnName)){
            if(Direction.desc.equals(dir)){
                sort = Sort.by("months").descending();
            } else {
                sort = Sort.by("months").ascending();
            }
        } else if("price".equals(columnName)){
            if(Direction.desc.equals(dir)){
                sort = Sort.by("price").descending();
            } else {
                sort = Sort.by("price").ascending();
            }
        } else if("created".equals(columnName)){
            if(Direction.desc.equals(dir)){
                sort = Sort.by("created").descending();
            } else {
                sort = Sort.by("created").ascending();
            }
        }

        Pageable rows = PageRequest.of(pagingRequest.getStart() / pagingRequest.getLength(), pagingRequest.getLength(), sort);
        if(abonementSpecification != null){
            return abonomentRepository.findAll(abonementSpecification, rows);
        }
        return abonomentRepository.findAll(rows);
    }

    private Page<AbonomentDTO> getAbonomentPage(long count, List<AbonomentDTO> abonementDTOList, PagingRequest pagingRequest) {
        Page<AbonomentDTO> page = new Page<>(abonementDTOList);
        page.setRecordsFiltered((int) count);
        page.setRecordsTotal((int) count);
        page.setDraw(pagingRequest.getDraw());

        return page;
    }

    private Specification<Abonement> getAbonementSpecification(AbonementFilterDto filterDto) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
        Specification<Abonement> specification = null;

        if (!StringUtils.isEmpty(filterDto.getCarNumber())) {
            specification = AbonementSpecification.likePlateNumber(filterDto.getCarNumber());
        }
        if (!StringUtils.isEmpty(filterDto.getDateFromString())) {
            specification = specification != null ? specification.and(AbonementSpecification.greaterCreateDate(format.parse(filterDto.getDateFromString()))) : AbonementSpecification.greaterCreateDate(format.parse(filterDto.getDateFromString()));
        }
        if (!StringUtils.isEmpty(filterDto.getDateToString())) {
            specification = specification != null ? specification.and(AbonementSpecification.lessCreateDate(format.parse(filterDto.getDateToString()))) : AbonementSpecification.lessCreateDate(format.parse(filterDto.getDateToString()));
        }
        if (filterDto.getSearchAbonementTypes() != null) {
            specification = specification != null ? specification.and(AbonementSpecification.equalAbonementType(filterDto.getSearchAbonementTypes())) : AbonementSpecification.equalAbonementType(filterDto.getSearchAbonementTypes());
        }
        return specification;
    }

    @Override
    public List<AbonomentDTO> getAbonementsByPlateNumber(String plateNumber){
        List<Abonement> abonomentDTOS = abonomentRepository.findAllByPlatenumber(plateNumber);
        return abonomentDTOS.stream().map(a->AbonomentDTO.convertToDtoWithCustomNumbers(a)).collect(Collectors.toList());
    }
}
