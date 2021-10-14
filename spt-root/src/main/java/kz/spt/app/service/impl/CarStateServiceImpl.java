package kz.spt.app.service.impl;

import kz.spt.lib.bootstrap.datatable.*;
import kz.spt.lib.model.Camera;
import kz.spt.lib.model.CarState;
import kz.spt.lib.model.dto.CarStateDto;
import kz.spt.lib.service.CarStateService;
import kz.spt.app.repository.CarStateRepository;
import org.springframework.stereotype.Service;
import org.thymeleaf.util.StringUtils;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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

    @Override
    public Boolean checkIsLastEnteredNotLeft(String carNumber) {
        return getLastNotLeft(carNumber) != null;
    }

    @Override
    public CarState getLastNotLeft(String carNumber) {
        return carStateRepository.getCarStateNotLeft(carNumber);
    }

    @Override
    public Iterable<CarState> getAllNotLeft() {
        return carStateRepository.getAllCarStateNotLeft();
    }
    
    public List<CarState> listByFilters(String plateNumber){
        if(plateNumber != null && !"".equals(plateNumber)){
            return carStateRepository.getAllByPlateNumber(plateNumber);
        }
        return carStateRepository.findAll();
    }

    @Override
    public Page<CarStateDto> getAll(PagingRequest pagingRequest, String plateNumber, String dateFromString, String dateToString) {
        List<CarState> carStates = this.listByFilters(plateNumber);
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
