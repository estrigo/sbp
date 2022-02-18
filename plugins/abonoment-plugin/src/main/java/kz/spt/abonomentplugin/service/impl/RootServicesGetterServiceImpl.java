package kz.spt.abonomentplugin.service.impl;

import kz.spt.abonomentplugin.AbonomentPlugin;
import kz.spt.abonomentplugin.service.RootServicesGetterService;
import kz.spt.lib.service.CarsService;
import kz.spt.lib.service.ParkingService;
import org.springframework.stereotype.Service;

@Service
public class RootServicesGetterServiceImpl implements RootServicesGetterService {

    private CarsService carsService;
    private ParkingService parkingService;

    @Override
    public CarsService getCarsService() {
        if (this.carsService == null) {
            carsService = (CarsService) AbonomentPlugin.INSTANCE.getMainApplicationContext().getBean("carsServiceImpl");
        }
        return this.carsService;
    }

    @Override
    public ParkingService getParkingService() {
        if (this.parkingService == null){
            parkingService = (ParkingService) AbonomentPlugin.INSTANCE.getMainApplicationContext().getBean("parkingServiceImpl");
        }
        return this.parkingService;
    }
}
