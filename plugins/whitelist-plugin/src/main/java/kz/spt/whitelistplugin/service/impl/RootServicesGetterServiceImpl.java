package kz.spt.whitelistplugin.service.impl;

import kz.spt.lib.service.*;
import kz.spt.whitelistplugin.WhitelistPlugin;
import kz.spt.whitelistplugin.service.RootServicesGetterService;
import org.springframework.stereotype.Service;

@Service
public class RootServicesGetterServiceImpl implements RootServicesGetterService {

    private CarsService carsService;
    private CarStateService carStateService;
    private ParkingService parkingService;

    private AdminService adminService;

    @Override
    public CarsService getCarsService() {
        if (this.carsService == null) {
            carsService = (CarsService) WhitelistPlugin.INSTANCE.getMainApplicationContext().getBean("carsServiceImpl");
        }
        return this.carsService;
    }

    @Override
    public CarStateService getCarStateService() {
        if (this.carStateService == null){
            carStateService = (CarStateService) WhitelistPlugin.INSTANCE.getMainApplicationContext().getBean("carStateServiceImpl");
        }
        return this.carStateService;
    }

    @Override
    public ParkingService getParkingService() {
        if (this.parkingService == null){
            parkingService = (ParkingService) WhitelistPlugin.INSTANCE.getMainApplicationContext().getBean("parkingServiceImpl");
        }
        return this.parkingService;
    }

    @Override
    public AdminService getAdminService() {
        if (this.adminService == null) {
            adminService = (AdminService) WhitelistPlugin.INSTANCE.getMainApplicationContext().getBean("adminServiceImpl");
        }
        return this.adminService;
    }

}
