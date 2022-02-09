package kz.spt.app.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import kz.spt.lib.bootstrap.datatable.*;
import kz.spt.lib.model.*;
import kz.spt.lib.model.dto.CarStateDto;
import kz.spt.lib.model.dto.CarStateFilterDto;
import kz.spt.lib.service.*;
import kz.spt.app.repository.CarStateRepository;
import lombok.extern.java.Log;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.thymeleaf.util.StringUtils;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Log
@Service
public class CarStateServiceImpl implements CarStateService {

    private final CarStateRepository carStateRepository;
    private final EventLogService eventLogService;
    private final CarsService carsService;

    private static final Comparator<CarStateDto> EMPTY_COMPARATOR = (e1, e2) -> 0;
    private String dateFormat = "yyyy-MM-dd'T'HH:mm";

    public CarStateServiceImpl(CarStateRepository carStateRepository, EventLogService eventLogService,
                               CarsService carsService) {
        this.carStateRepository = carStateRepository;
        this.eventLogService = eventLogService;
        this.carsService = carsService;
    }

    @Override
    public void createINState(String carNumber, Date inTimestamp, Camera camera, Boolean paid, String whitelistJson) {
        CarState carState = new CarState();
        carState.setCarNumber(carNumber);
        carState.setInTimestamp(inTimestamp);
        carState.setType(camera.getGate().getParking().getParkingType());
        carState.setInChannelIp(camera.getIp());
        carState.setParking(camera.getGate().getParking());
        carState.setInGate(camera.getGate());
        carState.setInBarrier(camera.getGate().getBarrier());
        carState.setWhitelistJson(whitelistJson);
        carState.setPaid(paid);
        carStateRepository.save(carState);
    }

    @Override
    public void createOUTState(String carNumber, Date outTimestamp, Camera camera, CarState carState) {
        carState.setOutTimestamp(outTimestamp);
        carState.setOutChannelIp(camera.getIp());
        carState.setOutGate(camera.getGate());
        carState.setOutBarrier(camera.getGate().getBarrier());
        carStateRepository.save(carState);
    }

    public void cancelPaid(CarState carState) {
        carState.setPaid(false);
        carStateRepository.save(carState);
    }

    @Override
    public void createOUTManual(String carNumber, Date outTimestamp, CarState carState) {
        carState.setOutTimestamp(outTimestamp);
        carStateRepository.save(carState);
    }

    @Override
    public Boolean checkIsLastEnteredNotLeft(String carNumber) {
        return getLastNotLeft(carNumber) != null;
    }

    @Override
    public CarState getLastNotLeft(String carNumber) {
        Pageable first = PageRequest.of(0, 1);
        List<CarState> carStates = carStateRepository.getCarStateNotLeft(carNumber, first);
        if (carStates.size() > 0) {
            return carStates.get(0);
        }
        return null;
    }

    @Override
    public Iterable<CarState> getAllNotLeft() {
        return carStateRepository.getAllCarStateNotLeft();
    }

    @Override
    public void editPlateNumber(CarState carState) {
        carStateRepository.findById(carState.getId()).ifPresent(m -> {
            String username = "";
            if (SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof CurrentUser) {
                CurrentUser currentUser = (CurrentUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                if (currentUser != null) {
                    username = currentUser.getUsername();
                }
            }

            SimpleDateFormat format = new SimpleDateFormat(dateFormat);

            Map<String, Object> properties = new HashMap<>();
            properties.put("carNumber", carState.getCarNumber());
            properties.put("eventTime", format.format(new Date()));
            properties.put("type", EventLogService.EventType.Success);
            eventLogService.createEventLog(CarState.class.getSimpleName(),
                    null,
                    properties,
                    "Журнал.Ручное изменение номера, новое значение:" + carState.getCarNumber() + ", старое значение:" + m.getCarNumber() + ", пользователь:" + username,
                    "Journal.Manual edit number, new value:" + carState.getCarNumber() + ", old value:" + m.getCarNumber() + ", user:" + username);

            m.setCarNumber(carState.getCarNumber());
            carStateRepository.save(m);

            if(carsService.findByPlatenumber(carState.getCarNumber()) == null){
                carsService.createCar(carState.getCarNumber());
            }
        });
    }

    public Iterable<CarState> listByFilters(CarStateFilterDto filterDto) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
        Specification<CarState> specification = null;

        if (!StringUtils.isEmpty(filterDto.getPlateNumber())) {
            specification = CarStateSpecification.likePlateNumber(filterDto.getPlateNumber());
        }
        if (!StringUtils.isEmpty(filterDto.getDateFromString())) {
            specification = specification != null ? specification.and(CarStateSpecification.greaterDate(format.parse(filterDto.getDateFromString()))) : CarStateSpecification.greaterDate(format.parse(filterDto.getDateFromString()));
        }
        if (!StringUtils.isEmpty(filterDto.getDateToString())) {
            specification = specification != null ? specification.and(CarStateSpecification.lessDate(format.parse(filterDto.getDateToString()))) : CarStateSpecification.lessDate(format.parse(filterDto.getDateToString()));
        }
        if (filterDto.getAmount() != null) {
            specification = specification != null ? specification.and(CarStateSpecification.equalAmount(BigDecimal.valueOf(filterDto.getAmount()))) : CarStateSpecification.equalAmount(BigDecimal.valueOf(filterDto.getAmount()));
        }
        if (filterDto.getInGateId() != null) {
            specification = specification != null ? specification.and(CarStateSpecification.equalInGateId(filterDto.getInGateId())) : CarStateSpecification.equalInGateId(filterDto.getInGateId());
        }
        if (filterDto.getOutGateId() != null) {
            specification = specification != null ? specification.and(CarStateSpecification.equalOutGateId(filterDto.getOutGateId())) : CarStateSpecification.equalOutGateId(filterDto.getOutGateId());
        }
        if (filterDto.isInParking()) {
            specification = specification != null ? specification.and(CarStateSpecification.emptyOutGateTime()) : CarStateSpecification.emptyOutGateTime();
        }

        specification = specification != null ? specification.and(CarStateSpecification.orderById()) : CarStateSpecification.orderById();
        return carStateRepository.findAll(specification);
    }

    @Override
    public Page<CarStateDto> getAll(PagingRequest pagingRequest,
                                    CarStateFilterDto carStateFilterDto) throws ParseException {

        List<CarState> carStates = (List<CarState>) this.listByFilters(carStateFilterDto);
        List<CarStateDto> carStateDtos = CarStateDto.fromCarStates(carStates);
        return getPage(carStateDtos, pagingRequest);
    }

    @Override
    public CarState save(CarState carState) {
        return carStateRepository.save(carState);
    }

    @Override
    public List<String> getInButNotPaidFromList(List<String> checkList) {
        return carStateRepository.getInButNotPaidFromList(checkList);
    }

    @Override
    public CarState getIfLastLeft(String carNumber, String cameraIp) {
        Pageable first = PageRequest.of(0, 1);
        List<CarState> carStates = carStateRepository.getCarStateLastLeft(cameraIp, first);
        if(carStates.size() > 0 && carNumber.equals(carStates.get(0).getCarNumber())){
            return carStates.get(0);
        }
        return null;
    }

    @Override
    public Boolean getIfHasLastFromOtherCamera(String carNumber, String cameraIp, Date secondsBefore) {
        Pageable first = PageRequest.of(0, 1);
        List<CarState> carStates = carStateRepository.getCarStateLastEnterFromOther(cameraIp, carNumber, secondsBefore, first);
        if(carStates.size() > 0){
            return true;
        }
        carStates = carStateRepository.getCarStateLastLeftFromOther(cameraIp, carNumber, secondsBefore, first);
        if(carStates.size() > 0){
            return true;
        }
        return false;
    }

    @Override
    public Boolean getIfHasLastFromThisCamera(String carNumber, String cameraIp, Date secondsBefore) {
        Pageable first = PageRequest.of(0, 1);
        List<CarState> carStates = carStateRepository.getCarStateLastLeftFromThis(cameraIp, carNumber, secondsBefore, first);
        return carStates.size() > 0;
    }

    @Override
    public Boolean removeDebt(String carNumber) {
        CarState carState = getLastNotLeft(carNumber);

        if (carState == null) {
            return false;
        } else {
            cancelPaid(carState);
            return true;
        }
    }

    @Override
    public Iterable<CarState> getCurrentNotPayed(String carNumber) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.HOUR, -24);
        Date date24back = cal.getTime();
        return carStateRepository.getCurrentNotPayed(carNumber+"%", date24back);
    }

    private Page<CarStateDto> getPage(List<CarStateDto> carStates, PagingRequest pagingRequest) {
        List<CarStateDto> filtered = carStates.stream()
                .sorted(sortCarStates(pagingRequest))
                .filter(filterCarStates(pagingRequest))
                .skip(pagingRequest.getStart())
                .limit(pagingRequest.getLength())
                .collect(Collectors.toList());

        long count = carStates.stream()
                .filter(filterCarStates(pagingRequest))
                .count();

        Page<CarStateDto> page = new Page<>(filtered);
        page.setRecordsFiltered((int) count);
        page.setRecordsTotal((int) count);
        page.setDraw(pagingRequest.getDraw());

        return page;
    }

    private Predicate<CarStateDto> filterCarStates(PagingRequest pagingRequest) {
        if (pagingRequest.getSearch() == null || StringUtils.isEmpty(pagingRequest.getSearch().getValue())) {
            return CarState -> true;
        }
        String value = pagingRequest.getSearch().getValue();

        return carState -> ((carState.getCarNumber() != null && carState.getCarNumber().contains(value))
                || carState.getInTimestampString().contains(value)
                || carState.getOutTimestampString().contains(value)
                || carState.getDuration().contains(value)
                || (carState.getPayment() != null && carState.getPayment().toString().contains(value))
        );
    }

    private Comparator<CarStateDto> sortCarStates(PagingRequest pagingRequest) {
        if (pagingRequest.getOrder() == null) {
            return EMPTY_COMPARATOR;
        }

        try {
            Order order = pagingRequest.getOrder().get(0);

            int columnIndex = order.getColumn();
            Column column = pagingRequest.getColumns()
                    .get(columnIndex);

            Comparator<CarStateDto> comparator = CarStateDtoComparators.getComparator(column.getData(), order.getDir());
            return Objects.requireNonNullElse(comparator, EMPTY_COMPARATOR);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return EMPTY_COMPARATOR;
    }
}
