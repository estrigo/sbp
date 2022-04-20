package kz.spt.app.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.spt.lib.bootstrap.datatable.*;
import kz.spt.lib.extension.PluginRegister;
import kz.spt.lib.model.*;
import kz.spt.lib.model.dto.CarStateDto;
import kz.spt.lib.model.dto.CarStateExcelDto;
import kz.spt.lib.model.dto.CarStateFilterDto;
import kz.spt.lib.service.*;
import kz.spt.app.repository.CarStateRepository;
import kz.spt.lib.utils.StaticValues;
import kz.spt.lib.utils.Utils;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.thymeleaf.util.StringUtils;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Log
@Service
public class CarStateServiceImpl implements CarStateService {

    private final CarStateRepository carStateRepository;
    private final EventLogService eventLogService;
    private final CarsService carsService;
    private final PluginService pluginService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Comparator<CarStateDto> EMPTY_COMPARATOR = (e1, e2) -> 0;
    private String dateFormat = "yyyy-MM-dd'T'HH:mm";

    public CarStateServiceImpl(CarStateRepository carStateRepository, EventLogService eventLogService,
                               CarsService carsService, PluginService pluginService) {
        this.carStateRepository = carStateRepository;
        this.eventLogService = eventLogService;
        this.carsService = carsService;
        this.pluginService = pluginService;
    }

    @Override
    public CarState findById(Long carStateId) {
        return carStateRepository.findById(carStateId).get();
    }

    @Override
    public void createINState(String carNumber, Date inTimestamp, Camera camera, Boolean paid, String whitelistJson, String inPhotoUrl) {
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
        carState.setInPhotoUrl(inPhotoUrl);
        carStateRepository.save(carState);
    }

    @Override
    public void createOUTState(String carNumber, Date outTimestamp, Camera camera, CarState carState, String outPhotoUrl) {
        if(carState != null){
            carState.setOutTimestamp(outTimestamp);
            carState.setOutChannelIp(camera.getIp());
            carState.setOutGate(camera.getGate());
            carState.setOutBarrier(camera.getGate().getBarrier());
            carState.setOutPhotoUrl(outPhotoUrl);
            carStateRepository.save(carState);
        } else {
            carState = new CarState();
            carState.setOutTimestamp(outTimestamp);
            carState.setOutChannelIp(camera.getIp());
            carState.setOutGate(camera.getGate());
            carState.setCarNumber(carNumber);
            carState.setParking(camera.getGate().getParking());
            carState.setOutBarrier(camera.getGate().getBarrier());
            carState.setOutPhotoUrl(outPhotoUrl);
            carStateRepository.save(carState);
        }
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
            properties.put("type", EventLog.StatusType.Success);
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

    @SneakyThrows
    @Override
    public Iterable<CarState> listByFilters(CarStateFilterDto filterDto) {
        Specification<CarState> specification = getCarStateSpecification(filterDto);
        Sort sort = Sort.by("id").descending();
        if(specification != null){
            return carStateRepository.findAll(specification, sort);
        }
        return carStateRepository.findAll(sort);
    }

    private Specification<CarState> getCarStateSpecification(CarStateFilterDto filterDto) throws ParseException {
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
        return specification;
    }

    private org.springframework.data.domain.Page<CarState> listByFilters(Specification<CarState> carStateSpecification, PagingRequest pagingRequest) {
        Order order = pagingRequest.getOrder().get(0);

        int columnIndex = order.getColumn();
        Column column = pagingRequest.getColumns().get(columnIndex);
        String columnName = column.getData();
        Direction dir = order.getDir();

        Sort sort = null;
        if("carNumber".equals(columnName)){
            if(Direction.desc.equals(dir)){
                sort = Sort.by("carNumber").descending();
            } else {
                sort = Sort.by("carNumber").ascending();
            }
        } else if("inTimestampString".equals(columnName)){
            if(Direction.desc.equals(dir)){
                sort = Sort.by("inTimestamp").descending();
            } else {
                sort = Sort.by("inTimestamp").ascending();
            }
        } else if("inTimestampString".equals(columnName)){
            if(Direction.desc.equals(dir)){
                sort = Sort.by("outTimestamp").descending();
            } else {
                sort = Sort.by("outTimestamp").ascending();
            }
        } else if("paid".equals(columnName)){
            if(Direction.desc.equals(dir)){
                sort = Sort.by("paid").descending();
            } else {
                sort = Sort.by("paid").ascending();
            }
        }

        Pageable rows = PageRequest.of(pagingRequest.getStart() / pagingRequest.getLength(), pagingRequest.getLength(), sort);
        if (carStateSpecification != null) {
            return carStateRepository.findAll(carStateSpecification, rows);
        } else {
            return carStateRepository.findAll(rows);
        }
    }

    @Override
    public Page<CarStateDto> getAll(PagingRequest pagingRequest,
                                    CarStateFilterDto carStateFilterDto) throws ParseException {

        Specification<CarState> specification = getCarStateSpecification(carStateFilterDto);
        org.springframework.data.domain.Page<CarState> filteredCarStates =  listByFilters(specification, pagingRequest);
        return getPage(filteredCarStates, pagingRequest);
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
    public Boolean removeDebt(String carNumber) throws Exception {

        Boolean result = false;

        PluginRegister billingPluginRegister = pluginService.getPluginRegister(StaticValues.billingPlugin);
        if(billingPluginRegister != null){
            ObjectNode billinNode = this.objectMapper.createObjectNode();
            billinNode.put("command", "getCurrentBalance");
            billinNode.put("plateNumber", carNumber);
            JsonNode billingResult = billingPluginRegister.execute(billinNode);
            BigDecimal balance = billingResult.get("currentBalance").decimalValue().setScale(2);
            if(balance.compareTo(BigDecimal.ZERO) == -1){
                ObjectNode billingSubtractNode = this.objectMapper.createObjectNode();
                billingSubtractNode.put("command", "increaseCurrentBalance");
                billingSubtractNode.put("plateNumber", carNumber);
                billingSubtractNode.put("amount", balance.multiply(BigDecimal.valueOf(-1L)));
                billingSubtractNode.put("reason", "Списание долга");
                billingSubtractNode.put("reasonEn", "Debt cancellation");
                billingPluginRegister.execute(billingSubtractNode).get("currentBalance").decimalValue();
                result = true;
            }
        }

        CarState carState = getLastNotLeft(carNumber);
        if (carState != null) {
            cancelPaid(carState);
            result = true;
        }

        return result;
    }

    @Override
    public Iterable<CarState> getCurrentNotPayed(String carNumber) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.HOUR, -24);
        Date date24back = cal.getTime();
        return carStateRepository.getCurrentNotPayed(carNumber+"%", date24back);
    }

    @Override
    public CarState getLastCarState(String carNumber) {
        Pageable first = PageRequest.of(0, 1);
        List<CarState> carStates = carStateRepository.getLastCarState(carNumber, first);
        if(carStates.size() > 0){
            return carStates.get(0);
        }
        return null;
    }

    @Override
    public void setAbonomentDetails(Long id, JsonNode details) {
        CarState carState = carStateRepository.getOne(id);
        carState.setAbonomentJson(details.toString());
        carStateRepository.save(carState);
    }

    private Page<CarStateDto> getPage(org.springframework.data.domain.Page<CarState> carStates, PagingRequest pagingRequest) {
        long count = carStates.getTotalElements();

        List<CarStateDto> carStateDtoList = new ArrayList<>(carStates.getSize());
        for(CarState carState:carStates){
            CarStateDto carStateDto = CarStateDto.fromCarState(carState);
            Cars car = carsService.findByPlatenumber(carStateDto.carNumber);
            if(car != null){
                if(car.getRegion() != null){
                    carStateDto.carNumber = Utils.convertRegion(car.getRegion()) + " " + carStateDto.carNumber;
                }
                if(car.getType() != null){
                    carStateDto.carNumber = carStateDto.carNumber + "["+ car.getType() +"]";
                }
            }

            StringBuilder durationBuilder = new StringBuilder("");
            if (carStateDto.inTimestamp != null) {
                Locale locale = LocaleContextHolder.getLocale();
                String language = locale.toString();

                long time_difference = (carStateDto.outTimestamp == null ? (new Date()).getTime() : carStateDto.outTimestamp.getTime()) - carStateDto.inTimestamp.getTime();
                long days_difference = TimeUnit.MILLISECONDS.toDays(time_difference) % 365;
                if (days_difference > 0) {
                    durationBuilder.append(days_difference + (language.equals("ru")?"д ":"d "));
                }

                long hours_difference = TimeUnit.MILLISECONDS.toHours(time_difference) % 24;
                if (hours_difference > 0 || durationBuilder.length() > 0) {
                    durationBuilder.append(hours_difference + (language.equals("ru")?"ч ":"h "));
                }

                long minutes_difference = TimeUnit.MILLISECONDS.toMinutes(time_difference) % 60;
                if (minutes_difference > 0 || durationBuilder.length() > 0) {
                    durationBuilder.append(minutes_difference + (language.equals("ru")?"м ":"m "));
                }

                long seconds_difference = TimeUnit.MILLISECONDS.toSeconds(time_difference) % 60;
                if (seconds_difference > 0 || durationBuilder.length() > 0) {
                    durationBuilder.append(seconds_difference + (language.equals("ru")?"с ":"s "));
                }

                if (carStateDto.outTimestamp == null &&
                        (days_difference > 0 || (days_difference <= 0 && hours_difference >= 16))) {
                    carStateDto.css = "table-danger";
                }
            }
            carStateDto.duration = durationBuilder.toString();
            carStateDtoList.add(carStateDto);
        }

        Page<CarStateDto> page = new Page<>(carStateDtoList);
        page.setRecordsFiltered((int) count);
        page.setRecordsTotal((int) count);
        page.setDraw(pagingRequest.getDraw());

        return page;
    }


    @Override
    public List<CarStateExcelDto> getExcelData(CarStateFilterDto carStateFilterDto) throws ParseException {
        Specification<CarState> specification = getCarStateSpecification(carStateFilterDto);
        List<CarState> filteredCarStates = listByFiltersForExcel(specification);

        List<CarStateExcelDto> carStateDtoList = new ArrayList<>(filteredCarStates.size());
        for(CarState carState: filteredCarStates){
            carStateDtoList.add(CarStateExcelDto.fromCarState(carState));
        }

        return carStateDtoList;
    }

    private List<CarState> listByFiltersForExcel(Specification<CarState> carStateSpecification) {
        if (carStateSpecification != null) {
            return carStateRepository.findAll(carStateSpecification);
        } else {
            return carStateRepository.findAll();
        }
    }

}
