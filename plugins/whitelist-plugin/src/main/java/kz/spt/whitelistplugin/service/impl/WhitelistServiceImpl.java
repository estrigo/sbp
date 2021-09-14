package kz.spt.whitelistplugin.service.impl;

import kz.spt.lib.model.CarState;
import kz.spt.lib.model.Cars;
import kz.spt.whitelistplugin.model.WhitelistGroups;
import kz.spt.lib.model.Parking;
import kz.spt.lib.service.CarStateService;
import kz.spt.whitelistplugin.service.RootServicesGetterService;
import kz.spt.whitelistplugin.service.WhitelistGroupsService;
import kz.spt.lib.service.ParkingService;
import kz.spt.whitelistplugin.WhitelistPlugin;
import kz.spt.whitelistplugin.model.Whitelist;
import kz.spt.whitelistplugin.model.dto.ParkingCarsDTO;
import kz.spt.whitelistplugin.repository.WhitelistRepository;
import kz.spt.whitelistplugin.service.WhitelistService;
import org.pf4j.util.StringUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.swing.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Service
public class WhitelistServiceImpl implements WhitelistService {

    private final String dateformat = "yyyy-MM-dd'T'HH:mm";
    private CarStateService carStateService;
    private ParkingService parkingService;
    private WhitelistRepository whitelistRepository;
    private WhitelistGroupsService whitelistGroupsService;
    private RootServicesGetterService rootServicesGetterService;

    public WhitelistServiceImpl(WhitelistRepository whitelistRepository, WhitelistGroupsService whitelistGroupsService,
                                RootServicesGetterService rootServicesGetterService){
        this.whitelistRepository = whitelistRepository;
        this.whitelistGroupsService = whitelistGroupsService;
        this.rootServicesGetterService  = rootServicesGetterService;
    }

    @Override
    public void saveWhitelist(Whitelist whitelist, UserDetails currentUser) throws Exception {

        if(Whitelist.Kind.GROUP.equals(whitelist.getKind())){
            if(whitelist.getGroup() != null){
                WhitelistGroups group = whitelist.getGroup();
                whitelistGroupsService.updateGroup(group.getId(), whitelist.getGroupName(), whitelist.getCarsList(), currentUser.getUsername());
            } else {
                WhitelistGroups whitelistGroups = whitelistGroupsService.createGroup(whitelist.getGroupName(), whitelist.getCarsList(), currentUser.getUsername());
                whitelist.setGroup(whitelistGroups);
            }
            whitelist.setCar(null);
        }

        if(Whitelist.Kind.INDIVIDUAL.equals(whitelist.getKind())){
            Cars car = rootServicesGetterService.getCarsService().createCar(whitelist.getPlatenumber());
            whitelist.setCar(car);
            if(whitelist.getGroup() != null){
                WhitelistGroups group = whitelist.getGroup();
                whitelist.setGroup(null);
                group.setCars(null);
                whitelistGroupsService.deleteGroup(group);
            }
        }

        SimpleDateFormat format = new SimpleDateFormat(dateformat);
        if (Whitelist.Type.PERIOD.equals(whitelist.getType()) || Whitelist.Type.ONCE.equals(whitelist.getType()) || Whitelist.Type.MONTHLY.equals(whitelist.getType())) {
            if (StringUtils.isNotNullOrEmpty(whitelist.getAccessStartString())) {
                whitelist.setAccess_start(format.parse(whitelist.getAccessStartString()));
            }
            if (StringUtils.isNotNullOrEmpty(whitelist.getAccessEndString())) {
                whitelist.setAccess_end(format.parse(whitelist.getAccessEndString()));
            }
        }
        if (whitelist.getId() != null) {
            whitelist.setUpdatedUser(currentUser.getUsername());
        } else {
            whitelist.setCreatedUser(currentUser.getUsername());
        }
        whitelistRepository.save(whitelist);
    }

    @Override
    public Iterable<Whitelist> listAllWhitelist() {
        return whitelistRepository.findAll();
    }

    @Override
    public Boolean hasAccess(String platenumber, Date date) {

        Cars car = rootServicesGetterService.getCarsService().findByPlatenumber(platenumber);
        if (car != null) {
            List<Whitelist> whitelists = whitelistRepository.findValidWhiteListByCar(car, date);
            List<Whitelist> groupWhitelists = whitelistRepository.findValidGroupWhiteListByCar(car, date);
            return whitelists.size() > 0 || groupWhitelists.size() > 0;
        }

        return false;
    }

    @Override
    public Whitelist prepareById(Long id) {
        SimpleDateFormat format = new SimpleDateFormat(dateformat);
        Whitelist whitelist = whitelistRepository.getWithCarAndGroup(id);
        if(Whitelist.Kind.INDIVIDUAL.equals(whitelist.getKind())){
            whitelist.setPlatenumber(whitelist.getCar().getPlatenumber());
        }
        if(Whitelist.Kind.GROUP.equals(whitelist.getKind())){
            Set<Cars> cars = whitelist.getGroup().getCars();
            List<String> plateNumbers = new ArrayList<>();
            for (Cars car : cars){
                plateNumbers.add(car.getPlatenumber());
            }
            String[] arr = new String[plateNumbers.size()];
            plateNumbers.toArray(arr);
            whitelist.carsList = arr;
            whitelist.setGroupName(whitelist.getGroup().getName());
        }
        if (Whitelist.Type.PERIOD.equals(whitelist.getType())) {
            if (whitelist.getAccess_start() != null) {
                whitelist.setAccessStartString(format.format(whitelist.getAccess_start()));
            }
            whitelist.setAccessEndString(format.format(whitelist.getAccess_end()));
        }
        return whitelist;
    }

    @Override
    public List<ParkingCarsDTO> listAllCarsInParking() {
        Iterable<CarState> carStates = getCarStateService().getAllNotLeft();
        List<Parking> parkings = (List<Parking>) getParkingService().listAllParking();

        List<ParkingCarsDTO> carsInParkings = new ArrayList<>();

        List<Cars> resultCars = new ArrayList<>();
        for (Parking parking : parkings) {
            ParkingCarsDTO parkingCarsDTO = new ParkingCarsDTO();
            parkingCarsDTO.setParking(parking);
            for (CarState carState : carStates) {
                if (carState.getParking() != null && carState.getParking().getId().equals(parking.getId())) {
                    resultCars.add(rootServicesGetterService.getCarsService().findByPlatenumber(carState.getCarNumber()));
                }
            }
            parkingCarsDTO.setCarsList(resultCars);
            carsInParkings.add(parkingCarsDTO);
        }
        return carsInParkings;
    }

    @Override
    public ParkingCarsDTO carsInParking(Long parkingId){
        List<ParkingCarsDTO> listCarsInParkings = listAllCarsInParking();
        for(ParkingCarsDTO parkingCarsDTO : listCarsInParkings)
        {
            if(parkingCarsDTO.getParking().getId().equals(parkingId)){
                return parkingCarsDTO;
            }
        }
        return null;
    }

    private CarStateService getCarStateService() {
        if (this.carStateService == null) {
            carStateService = (CarStateService) WhitelistPlugin.INSTANCE.getMainApplicationContext().getBean("carStateServiceImpl");
        }
        return carStateService;
    }

    private ParkingService getParkingService() {
        if (this.parkingService == null) {
            parkingService = (ParkingService) WhitelistPlugin.INSTANCE.getMainApplicationContext().getBean("parkingServiceImpl");
        }
        return parkingService;
    }
}
