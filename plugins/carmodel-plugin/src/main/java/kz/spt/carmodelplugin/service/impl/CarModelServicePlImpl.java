package kz.spt.carmodelplugin.service.impl;

import kz.spt.lib.service.LanguagePropertiesService;
import kz.spt.lib.utils.Dimension;
import kz.spt.lib.utils.MessageKey;
import kz.spt.carmodelplugin.bootstrap.datatable.CarmodelComparators;

import kz.spt.carmodelplugin.repository.CarmodelRepository;
import kz.spt.carmodelplugin.repository.CarmodelRepository2;
import kz.spt.carmodelplugin.service.CarModelServicePl;
import kz.spt.carmodelplugin.service.RootServicesGetterService;
import kz.spt.carmodelplugin.viewmodel.CarmodelDto;
import kz.spt.lib.bootstrap.datatable.Column;
import kz.spt.lib.bootstrap.datatable.Order;
import kz.spt.lib.bootstrap.datatable.Page;
import kz.spt.lib.bootstrap.datatable.PagingRequest;
import kz.spt.lib.model.*;
import lombok.extern.java.Log;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.util.StringUtils;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Predicate;

@Log
@Service
@Transactional(noRollbackFor = Exception.class)
public class CarModelServicePlImpl implements CarModelServicePl {

    private CarmodelRepository carmodelRepository;
    private RootServicesGetterService rootServicesGetterService;

    private CarmodelRepository2 carmodelRepository2;

    private LanguagePropertiesService languagePropertiesService;


    public CarModelServicePlImpl(CarmodelRepository carmodelRepository,
                                 RootServicesGetterService rootServicesGetterService,
                                 CarmodelRepository2 carmodelRepository2) {
        this.carmodelRepository = carmodelRepository;
        this.rootServicesGetterService = rootServicesGetterService;
        this.carmodelRepository2 = carmodelRepository2;
    }

    private String dateFormat = "yyyy-MM-dd'T'HH:mm";

    private static final Comparator<CarmodelDto> EMPTY_COMPARATOR = (e1, e2) -> 0;


    public Page<CarmodelDto> listCarsBy(PagingRequest pagingRequest, CarmodelDto filter) {
        Long count = carmodelRepository.countCarsByFilter(filter);

        filter.setPage(pagingRequest.getStart());
        filter.setElements(pagingRequest.getLength());
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
                String replacedUrl = mp.get("in_photo_url").toString().replace("_resize_w_200_h_100.jpeg", ".jpeg");
                carmodelDto.setPhoto((String) mp.get("in_photo_url"));
                carmodelDto.setBigPhoto(replacedUrl);
            } else {
                carmodelDto.setPhoto("NO Photo!");
            }
            if (mp.get("car_model") != null) {
                carmodelDto.setCarModel((String) mp.get("car_model"));
            }
            String dimension=MessageKey.DIMENSION_NOT_RECOGNIZED;
            if (mp.get("dimension") != null) {
                Integer type = (Integer) mp.get("dimension");
                if (type==1) {
                    dimension = MessageKey.DIMENSION_PASSENGER_CAR;
                } else if (type==2) {
                    dimension=MessageKey.DIMENSION_GAZELLE;
                } else if (type==3){
                    dimension = MessageKey.DIMENSION_TRUCK;
                } else {
                    dimension = MessageKey.DIMENSION_NOT_RECOGNIZED;
                }
            }
            String dimensionMessage = rootServicesGetterService.getLanguagesService().getMessageFromProperties(dimension);
            carmodelDto.setDimension(dimensionMessage);
            resultList.add(carmodelDto);
        }
        return getPage(count, resultList, pagingRequest);
    }

    private Page<CarmodelDto> getPage(long count, List<CarmodelDto> cars, PagingRequest pagingRequest) {
//        List<CarmodelDto> filtered = cars.stream()
//                .sorted(sortCars(pagingRequest))
//                .filter(filterCars(pagingRequest))
//                .skip(pagingRequest.getStart())
//                .limit(pagingRequest.getLength())
//                .collect(Collectors.toList());
//        long count = cars.stream()
//                .filter(filterCars(pagingRequest))
//                .count();

//        Page<CarmodelDto> page = new Page<>(filtered);
        Page<CarmodelDto> page = new Page<>(cars);
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

        if (dimension != null){
            Dimension checkedDimension = checkForDimension(dimension);
            if (checkedDimension.equals(Dimension.PASSENGER_CAR)) {
                cars.setModel("Toyota_Camry");
            } else if (checkedDimension.equals(Dimension.GAZELLE)){
                cars.setModel("Hyundai_Bus");
            } else if (checkedDimension.equals(Dimension.TRUCK)){
                cars.setModel("Zil_Truck");
            } else {
                cars.setModel("Toyota_Camry");
            }
        }

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

        Map<String, Object> messageValues = new HashMap<>();
        messageValues.put("dimension", dimension);
        messageValues.put("newModel", cars.getModel());
        messageValues.put("oldModel", oldModel);
        messageValues.put("username", username);

        rootServicesGetterService.getEventLogService().createEventLog(Cars.class.getSimpleName(),
                null,
                properties,
                messageValues, MessageKey.MANUAL_EDIT_DIMENSION);


    }

    @Override
    public List<CarModel> findAll() {
        return carmodelRepository2.findAll();

    }
    @Override
    public CarModel getCarModelById(Integer id) {
        return carmodelRepository2.getById(id);
    }

    @Override
    public void deleteCarModel (Integer id) {
        CarModel carmodel = carmodelRepository2.getById(id);
        carmodelRepository2.delete(carmodel);
    }

    @Override
    public org.springframework.data.domain.Page<CarModel> findAllUsersPageable (Pageable pageable) {
        return carmodelRepository2.findAll(pageable);
    }

    @Override
    public CarModel findByModel (String model) {
        return carmodelRepository2.findByModel(model);
    }
    @Override
    public void saveCarModel (CarModel carModel, UserDetails currentUser) {
        LocalDateTime localDateTime = LocalDateTime.now();
        carModel.setUpdatedTime(localDateTime);
        carModel.setUpdatedBy(currentUser.getUsername());
        carModel.setType(Math.toIntExact(carModel.getDimensions().getId()));
        carmodelRepository2.save(carModel);
    }

    @Override
    public void updateCarModel(int id, CarModel updateCarModel, UserDetails currentUser) {
        CarModel carModel = getCarModelById(id);
        carModel.setModel(updateCarModel.getModel());
        carModel.setUpdatedBy(currentUser.getUsername());
        carModel.setDimensions(updateCarModel.getDimensions());
        carModel.setType(Math.toIntExact(updateCarModel.getDimensions().getId()));
        LocalDateTime localDateTime = LocalDateTime.now();
        carModel.setUpdatedTime(localDateTime);

        try {
            carmodelRepository2.save(carModel);
        } catch (Exception e) {
          log.warning("Update error CarModel: " + carModel.getModel());
        }
    }

    private Dimension checkForDimension(String dimension){
        Map<String, String> passengerCarDimensions = rootServicesGetterService.getLanguagesService()
                .getWithDifferentLanguages(MessageKey.CAR_MODEL_PASSENGER_CAR);
        Map<String, String> truckDimensions = rootServicesGetterService.getLanguagesService()
                .getWithDifferentLanguages(MessageKey.CAR_MODEL_TYPE_TRUCK);
        Map<String, String> gazelleDimensions = rootServicesGetterService.getLanguagesService()
                .getWithDifferentLanguages(MessageKey.CAR_MODEL_GAZELLE);

        dimension = dimension.trim().toLowerCase();
        if (clearString(passengerCarDimensions).containsValue(dimension)){
            return Dimension.PASSENGER_CAR;
        }
        else if (clearString(truckDimensions).containsValue(dimension)){
            return Dimension.TRUCK;
        }
        else if (clearString(gazelleDimensions).containsValue(dimension)){
            return Dimension.GAZELLE;
        }
        else return Dimension.NOT_RECOGNIZED;
    }

    private Map<String, String> clearString(Map<String, String> dimensions){
        dimensions.forEach((k, v) -> dimensions.put(k, v.trim().toLowerCase()));
        return dimensions;
    }


}
