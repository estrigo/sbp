package kz.spt.carmodelplugin.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import kz.spt.carmodelplugin.bootstrap.datatable.CarmodelComparators;
import kz.spt.carmodelplugin.repository.CarmodelRepository;
import kz.spt.carmodelplugin.service.CarmodelService;
import kz.spt.carmodelplugin.service.RootServicesGetterService;
import kz.spt.carmodelplugin.viewmodel.CarmodelDto;
import kz.spt.lib.bootstrap.datatable.*;
import kz.spt.lib.model.CarState;
import kz.spt.lib.model.Cars;
import kz.spt.lib.model.CurrentUser;
import kz.spt.lib.model.EventLog;
import lombok.extern.java.Log;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.thymeleaf.util.StringUtils;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Log
@Service
public class CarmodelServiceImpl implements CarmodelService {

    private CarmodelRepository carmodelRepository;
    private RootServicesGetterService rootServicesGetterService;

    public CarmodelServiceImpl(CarmodelRepository carmodelRepository, RootServicesGetterService rootServicesGetterService) {
        this.carmodelRepository = carmodelRepository;
        this.rootServicesGetterService = rootServicesGetterService;
    }

    private String dateFormat = "yyyy-MM-dd'T'HH:mm";

    private static final Comparator<CarmodelDto> EMPTY_COMPARATOR = (e1, e2) -> 0;

    private static final ObjectMapper objectMapper = new ObjectMapper();


    public Page<CarmodelDto> listCarsBy(PagingRequest pagingRequest, CarmodelDto filter){

        List<Map<String, Object>> queryResult = carmodelRepository.getAllCarsByFilter(filter);
        List<CarmodelDto> resultList = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (Map<String, Object> mp:queryResult) {
            CarmodelDto carmodelDto = new CarmodelDto();
            if (mp.get("platenumber") != null) {
                carmodelDto.setPlateNumber((String) mp.get("platenumber"));
            }
            if (mp.get("in_timestamp") != null) {
                String entryDate = sdf.format(mp.get("in_timestamp"));
                carmodelDto.setEntryDate(entryDate);
            }
            if (mp.get("in_gate") != null) {
                BigInteger ss = (BigInteger) mp.get("in_gate");
                carmodelDto.setInGateId(ss.longValue());
            }
            if (mp.get("in_photo_url") != null) {
                carmodelDto.setPhoto((String) mp.get("in_photo_url"));
            } else {
                carmodelDto.setPhoto("NO Photo!");
            }
            if (mp.get("car_model") != null) {
                carmodelDto.setCarModel((String) mp.get("car_model"));
            }
            if (mp.get("dimension") != null) {
                Integer type = (Integer) mp.get("dimension");
                String dimension;
                if (type==1) {
                    dimension="Легковая";
                } else if (type==2) {
                    dimension="Газель";
                } else if (type==3){
                    dimension="Грузовик";
                } else {
                    dimension="Нераспознанный";
                }
                carmodelDto.setDimension(dimension);
            }
            resultList.add(carmodelDto);
        }
        return getPage(resultList, pagingRequest);
    }

    private Page<CarmodelDto> getPage(List<CarmodelDto> cars, PagingRequest pagingRequest) {
        List<CarmodelDto> filtered = cars.stream()
                .sorted(sortCars(pagingRequest))
                .filter(filterCars(pagingRequest))
                .skip(pagingRequest.getStart())
                .limit(pagingRequest.getLength())
                .collect(Collectors.toList());
        long count = cars.stream()
                .filter(filterCars(pagingRequest))
                .count();

        Page<CarmodelDto> page = new Page<>(filtered);
        page.setRecordsFiltered((int) count);
        page.setRecordsTotal((int) count);
        page.setDraw(pagingRequest.getDraw());

        return page;
    }

    private Comparator<CarmodelDto> sortCars(PagingRequest pagingRequest) {
        if (pagingRequest.getOrder() == null) {
            return EMPTY_COMPARATOR;
        }
        try {
            Order order = pagingRequest.getOrder()
                    .get(0);
            int columnIndex = order.getColumn();
            Column column = pagingRequest.getColumns()
                    .get(columnIndex);
            Comparator<CarmodelDto> comparator = CarmodelComparators.getComparator(column.getData(), order.getDir());
            return Objects.requireNonNullElse(comparator, EMPTY_COMPARATOR);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return EMPTY_COMPARATOR;
    }

    private Predicate<CarmodelDto> filterCars(PagingRequest pagingRequest) {
        if (pagingRequest.getSearch() == null || StringUtils.isEmpty(pagingRequest.getSearch()
                .getValue())) {
            return cars -> true;
        }
        String value = pagingRequest.getSearch().getValue();
        return cars -> (cars.getPlateNumber()!=null && cars.getPlateNumber().toLowerCase().contains(value))
                || (cars.getEntryDate()!=null && cars.getEntryDate().toLowerCase().contains(value))
                || (cars.getDimension()!=null && cars.getDimension().toLowerCase().contains(value));
    }

    public void editDimensionOfCar(String plateNumber, String dimension) {
        Cars cars = rootServicesGetterService.getCarsService().findByPlatenumber(plateNumber);
        String oldModel = cars.getModel();
        if (dimension != null && (dimension.equals("carmodel.passengerCar") || dimension.equals("Легковая"))) {
            cars.setModel("Camry");
        } else if (dimension != null && (dimension.equals("carmodel.gazelle") || dimension.equals("Газель"))){
            cars.setModel("Gazelle");
        } else if (dimension != null && (dimension.equals("carmodel.truck") || dimension.equals("Грузовик"))){
            cars.setModel("Truck");
        } else {
            cars.setModel("Passenger");
        }
        log.info("carmodel: " + cars.getModel() + ", and dimension: " + dimension);
        rootServicesGetterService.getCarsService().saveCars(cars);


        String username = "";
        if (SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof CurrentUser) {
            CurrentUser currentUser = (CurrentUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (currentUser != null) {
                username = currentUser.getUsername();
            }
        }
        SimpleDateFormat format = new SimpleDateFormat(dateFormat);
        Map<String, Object> properties = new HashMap<>();
        properties.put("carNumber", plateNumber);
        properties.put("eventTime", format.format(new Date()));
        properties.put("type", EventLog.StatusType.Success);

        rootServicesGetterService.getEventLogService().createEventLog(Cars.class.getSimpleName(),
                null,
                properties,
                "Ручное изменение габарита автомибиля, новое значение:" + dimension + ", новая модель: " + cars.getModel() + ", старая модель:" + oldModel + ", пользователь:" + username,
                "Manual edit dimension of car, new value:" + dimension + ", new model: " + cars.getModel() + ", old model:" + oldModel + ", user:" + username);


    }



}
