package kz.spt.whitelistplugin.service.impl;

import kz.spt.lib.service.CarStateService;
import kz.spt.lib.service.CarsService;
import kz.spt.whitelistplugin.WhitelistPlugin;
import kz.spt.whitelistplugin.service.RootServicesGetterService;
import org.springframework.stereotype.Service;

@Service
public class RootServicesGetterServiceImpl implements RootServicesGetterService {

    private CarsService carsService;
    private CarStateService carStateService;

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
}
