package kz.spt.whitelistplugin.service.impl;

import kz.spt.lib.model.CarState;
import kz.spt.lib.model.Cars;
import kz.spt.lib.model.Parking;
import kz.spt.lib.service.CarStateService;
import kz.spt.lib.service.CarsService;
import kz.spt.lib.service.ParkingService;
import kz.spt.whitelistplugin.WhitelistPlugin;
import kz.spt.whitelistplugin.model.Whitelist;
import kz.spt.whitelistplugin.model.dto.ParkingCarsDTO;
import kz.spt.whitelistplugin.repository.WhitelistRepository;
import kz.spt.whitelistplugin.service.WhitelistService;
import org.pf4j.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
@Service
public class WhitelistServiceImpl implements WhitelistService {

    private final String dateformat = "yyyy-MM-dd'T'HH:mm";
    private CarsService carsService;
    private CarStateService carStateService;
    private ParkingService parkingService;


    @Autowired
    private WhitelistRepository whitelistRepository;

    @Override
    public void saveWhitelist(Whitelist whitelist, UserDetails currentUser) throws Exception {
        getCarsService().createCar(whitelist.getPlatenumber());
        Cars car = getCarsService().findByPlatenumber(whitelist.getPlatenumber());
        whitelist.setCar(car);

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
        return whitelistRepository.findAllByCarIsNotNull();
    }

    @Override
    public Boolean hasAccess(String platenumber, Date date) {

        Cars car = getCarsService().findByPlatenumber(platenumber);
        if (car != null) {
            List<Whitelist> whitelists = whitelistRepository.findValidWhiteListByCar(car, date);
            if (whitelists.size() > 0) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Whitelist findById(Long id) {
        SimpleDateFormat format = new SimpleDateFormat(dateformat);
        Whitelist whitelist = whitelistRepository.getWithCar(id);
        whitelist.setPlatenumber(whitelist.getCar().getPlatenumber());
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
                if (carState.getParking() != null && carState.getParking().getId() == parking.getId()) {
                    resultCars.add(getCarsService().findByPlatenumber(carState.getCarNumber()));
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
            if(parkingCarsDTO.getParking().getId() == parkingId){
                return parkingCarsDTO;
            }
        }
        return null;
    }


    private CarsService getCarsService() {
        if (this.carsService == null) {
            carsService = (CarsService) WhitelistPlugin.INSTANCE.getMainApplicationContext().getBean("carsServiceImpl");
        }
        return carsService;
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
