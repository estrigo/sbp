package kz.spt.app.service.impl;

import kz.spt.lib.bootstrap.datatable.*;
import kz.spt.lib.model.*;
import kz.spt.lib.model.dto.CarStateDto;
import kz.spt.lib.model.dto.CarStateFilterDto;
import kz.spt.lib.service.CarStateService;
import kz.spt.app.repository.CarStateRepository;
import lombok.extern.java.Log;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.thymeleaf.util.StringUtils;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Log
@Service
public class CarStateServiceImpl implements CarStateService {

    private CarStateRepository carStateRepository;
    private static final Comparator<CarStateDto> EMPTY_COMPARATOR = (e1, e2) -> 0;

    public CarStateServiceImpl(CarStateRepository carStateRepository){
        this.carStateRepository = carStateRepository;
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

    public void createOUTState(String carNumber, Date outTimestamp, CarState carState) {
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
        if(carStates.size() > 0){
            return carStates.get(0);
        }
        return null;
    }

    @Override
    public Iterable<CarState> getAllNotLeft() {
        return carStateRepository.getAllCarStateNotLeft();
    }

    public Iterable<CarState> listByFilters(CarStateFilterDto filterDto) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
        Specification<CarState> specification = null;

        if(!StringUtils.isEmpty(filterDto.plateNumber)){
            specification = CarStateSpecification.likePlateNumber(filterDto.plateNumber);
        }
        if(!StringUtils.isEmpty(filterDto.dateFromString)){
            specification = specification != null? specification.and(CarStateSpecification.greaterDate(format.parse(filterDto.dateFromString))) : CarStateSpecification.greaterDate(format.parse(filterDto.dateFromString));
        }
        if(!StringUtils.isEmpty(filterDto.dateToString)){
            specification = specification != null? specification.and(CarStateSpecification.lessDate(format.parse(filterDto.dateToString))) : CarStateSpecification.lessDate(format.parse(filterDto.dateToString));
        }
        if(filterDto.amount != null){
            specification = specification != null? specification.and(CarStateSpecification.equalAmount(BigDecimal.valueOf(filterDto.amount))) : CarStateSpecification.equalAmount(BigDecimal.valueOf(filterDto.amount));
        }
        if(filterDto.inGateId != null){
            specification = specification != null? specification.and(CarStateSpecification.equalInGateId(filterDto.inGateId)) : CarStateSpecification.equalInGateId(filterDto.inGateId);
        }
        if(filterDto.outGateId != null){
            specification = specification != null? specification.and(CarStateSpecification.equalOutGateId(filterDto.outGateId)) : CarStateSpecification.equalOutGateId(filterDto.outGateId);
        }
        specification = specification != null? specification.and(CarStateSpecification.orderById()) : CarStateSpecification.orderById();
        return carStateRepository.findAll(specification);
    }

    @Override
    public Page<CarStateDto> getAll(PagingRequest pagingRequest, String plateNumber, String dateFromString, String dateToString, Long inGateId, Long outGateId, Integer amount) throws ParseException {

        CarStateFilterDto filterDto = new CarStateFilterDto();
        filterDto.dateToString = dateToString;
        filterDto.dateFromString = dateFromString;
        filterDto.plateNumber = plateNumber;
        filterDto.amount = amount;
        filterDto.inGateId = inGateId;
        filterDto.outGateId = outGateId;

        List<CarState> carStates = (List<CarState>) this.listByFilters(filterDto);
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
    public Boolean checkIsLastLeft(String carNumber, String cameraIp) {
        Pageable first = PageRequest.of(0, 1);
        List<CarState> carStates = carStateRepository.getCarStateLastLeft(cameraIp, first);
        return carStates.size() > 0 && carNumber.equals(carStates.get(0).getCarNumber());
    }

    @Override
    public Boolean removeDebt(String carNumber) {
        CarState carState = getLastNotLeft(carNumber);

        if(carState == null){
            return false;
        } else {
            createOUTState(carNumber, new Date(), carState);
            return true;
        }
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

        return  carState -> ((carState.getCarNumber() != null && carState.getCarNumber().contains(value))
                || carState.getInTimestampString().contains(value)
                || carState.getOutTimestampString().contains(value)
                || carState.getDuration().contains(value)
                || (carState.getPayment()!=null && carState.getPayment().toString().contains(value))
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
