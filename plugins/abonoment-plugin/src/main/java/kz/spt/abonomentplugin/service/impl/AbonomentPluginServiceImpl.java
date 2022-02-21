package kz.spt.abonomentplugin.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import kz.spt.lib.utils.StaticValues;
import lombok.extern.java.Log;
import org.apache.commons.lang3.StringUtils;
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

    private static final Comparator<AbonomentTypeDTO> EMPTY_COMPARATOR = (e1, e2) -> 0;
    private static final Comparator<AbonomentDTO> ABONOMENT_EMPTY_COMPARATOR = (e1, e2) -> 0;

    public AbonomentPluginServiceImpl(AbonomentTypesRepository abonomentTypesRepository, AbonomentRepository abonomentRepository, RootServicesGetterService rootServicesGetterService){
        this.abonomentTypesRepository = abonomentTypesRepository;
        this.abonomentRepository = abonomentRepository;
        this.rootServicesGetterService = rootServicesGetterService;
    }

    @Override
    public AbonomentTypes createType(int period, int price) {
        AbonomentTypes abonomentTypes = new AbonomentTypes();
        abonomentTypes.setPeriod(period);
        abonomentTypes.setPrice(price);
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
        for(AbonomentTypes abonomentType: allAbonomentTypes){
            if (locale.toString().equals("ru")) {
                abonomentType.setDescription("На " + abonomentType.getPeriod() + (abonomentType.getPeriod() == 1 ? " месяц" : (abonomentType.getPeriod() < 5 ? " месяца" : " месяцев")) +  " (" + abonomentType.getPeriod()*30 + " дней, " + abonomentType.getPrice() + " в местной валюте)");
            } else {
                abonomentType.setDescription("For " + abonomentType.getPeriod() + (abonomentType.getPeriod() == 1 ? " month" : " months") +  " (" + abonomentType.getPeriod()*30 + " days, " + abonomentType.getPrice() + " in local currency)");
            }
        }
        return allAbonomentTypes;
    }

    @Override
    public Abonoment createAbonoment(String platenumber, Long parkingId, Long typeId, String dateStart) throws ParseException {

        final String dateformat = "yyyy-MM-dd'T'HH:mm";

        platenumber = platenumber.toUpperCase();
        Cars car = rootServicesGetterService.getCarsService().createCar(platenumber);
        Parking parking = rootServicesGetterService.getParkingService().findById(parkingId);
        AbonomentTypes type = abonomentTypesRepository.findById(typeId).get();
        SimpleDateFormat format = new SimpleDateFormat(dateformat);

        Abonoment abonoment = new Abonoment();
        abonoment.setCar(car);
        abonoment.setParking(parking);
        abonoment.setPrice(BigDecimal.valueOf(type.getPrice()));
        abonoment.setPaid(false);
        abonoment.setMonths(type.getPeriod());
        abonoment.setBegin(format.parse(dateStart));

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(abonoment.getBegin());
        calendar.add(Calendar.DATE, type.getPeriod() * 30);
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
            return node;
        }
        return null;
    }

    @Override
    public void setAbonomentPaid(Long id) {
        Abonoment abonoment  = abonomentRepository.getOne(id);
        abonoment.setPaid(true);
        abonomentRepository.save(abonoment);
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
