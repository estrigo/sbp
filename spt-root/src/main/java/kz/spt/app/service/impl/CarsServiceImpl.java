package kz.spt.app.service.impl;

import kz.spt.lib.bootstrap.datatable.*;
import kz.spt.lib.model.Cars;
import kz.spt.lib.model.EventLog;
import kz.spt.lib.service.EventLogService;
import kz.spt.app.repository.CarsRepository;
import kz.spt.lib.service.CarsService;
import kz.spt.lib.utils.Utils;
import lombok.extern.java.Log;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.util.StringUtils;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Log
@Service
@Transactional
public class CarsServiceImpl implements CarsService {

    private CarsRepository carsRepository;
    private EventLogService eventLogService;

    public CarsServiceImpl(CarsRepository carsRepository, EventLogService eventLogService){
        this.carsRepository = carsRepository;
        this.eventLogService = eventLogService;
    }

    private static final Comparator<Cars> EMPTY_COMPARATOR = (e1, e2) -> 0;

    @Override
    public Cars findByPlatenumber(String platenumber){
        return carsRepository.findCarsByPlatenumberIgnoreCase(platenumber);
    }

    @Override
    public List<Cars> findByPlatenumberContaining(String platenumber) {
        return carsRepository.findByPlatenumberContaining(platenumber);
    }

    @Override
    public Cars findByPlatenumberWithCustomer(String platenumber) {
        return carsRepository.findCarsByPlatenumberWithCustomers(platenumber);
    }

    public Cars findById(Long id){
        return carsRepository.getOne(id);
    }

    public Iterable<Cars> listAllCars(){
        return carsRepository.findAll();
    }

    public Cars saveCars(Cars cars){
        cars.setPlatenumber(cars.getPlatenumber().toUpperCase());
        return carsRepository.save(cars);
    }

    @Override
    public Cars createCar(String platenumber){
        return createCar(platenumber, null, null, null);
    }

    @Override
    public Cars createCar(String platenumber, String region, String type, String car_model){
        platenumber = platenumber.toUpperCase();

        Boolean contains = Pattern.matches(".*\\p{InCyrillic}.*", platenumber);
        if(contains){
            platenumber = Utils.changeCyrillicToLatin(platenumber);
        }

        Cars car = findByPlatenumber(platenumber);
        if(car == null){
            car = new Cars();
            platenumber = platenumber.trim();
            platenumber = platenumber.replaceAll("\\W", "");
            car.setPlatenumber(platenumber);

            Map<String, Object> properties = new HashMap<>();
            properties.put("carNumber", platenumber);

            properties.put("type", EventLog.StatusType.Success);
            eventLogService.createEventLog(Cars.class.getSimpleName(), car.getId(), properties, "Новый номер авто " + car.getPlatenumber() + " сохранен в системе ", "New car number " + car.getPlatenumber() + " added to the system ", EventLog.EventType.NEW_CAR_DETECTED);
        }
        if(region != null){
            car.setRegion(region);
            car.setType(type);
        }
        if(car_model != null){
            car.setModel(car_model);
        }
        car = saveCars(car);
        return car;
    }

    @Override
    public Page<Cars> getCars(PagingRequest pagingRequest) {
        List<Cars> cars = carsRepository.findAll();
        return getPage(cars, pagingRequest);
    }

    @Override
    public List<String> searchByPlateNumberContaining(String text) {
        Pageable twenty = PageRequest.of(0, 20);
        return carsRepository.searchPlateNumbersIgnoreCase(text, twenty);
    }

    private Page<Cars> getPage(List<Cars> cars, PagingRequest pagingRequest) {
        List<Cars> filtered = cars.stream()
                .sorted(sortCars(pagingRequest))
                .filter(filterCars(pagingRequest))
                .skip(pagingRequest.getStart())
                .limit(pagingRequest.getLength())
                .collect(Collectors.toList());

        long count = cars.stream()
                .filter(filterCars(pagingRequest))
                .count();

        Page<Cars> page = new Page<>(filtered);
        page.setRecordsFiltered((int) count);
        page.setRecordsTotal((int) count);
        page.setDraw(pagingRequest.getDraw());

        return page;
    }

    private Predicate<Cars> filterCars(PagingRequest pagingRequest) {
        if (pagingRequest.getSearch() == null || StringUtils.isEmpty(pagingRequest.getSearch()
                .getValue())) {
            return cars -> true;
        }
        String value = pagingRequest.getSearch().getValue();

        return cars -> (cars.getPlatenumber()!=null && cars.getPlatenumber().toLowerCase().contains(value))
                || (cars.getBrand()!=null && cars.getBrand().toLowerCase().contains(value))
                || (cars.getColor()!=null && cars.getColor().toLowerCase().contains(value));
    }

    private Comparator<Cars> sortCars(PagingRequest pagingRequest) {
        if (pagingRequest.getOrder() == null) {
            return EMPTY_COMPARATOR;
        }

        try {
            Order order = pagingRequest.getOrder()
                    .get(0);

            int columnIndex = order.getColumn();
            Column column = pagingRequest.getColumns()
                    .get(columnIndex);

            Comparator<Cars> comparator = CarsComparators.getComparator(column.getData(), order.getDir());
            return Objects.requireNonNullElse(comparator, EMPTY_COMPARATOR);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return EMPTY_COMPARATOR;
    }
}
