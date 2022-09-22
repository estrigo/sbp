package kz.spt.megaplugin.service.impl;

import kz.spt.lib.service.CarStateService;
import kz.spt.lib.service.CarsService;
import kz.spt.megaplugin.MegaPlugin;
import kz.spt.megaplugin.service.RootServicesGetterService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RootServicesGetterServiceImpl implements RootServicesGetterService {

    private CarsService carsService;
    private CarStateService carStateService;

    @Override
    public CarsService getCarsService() {
        if (this.carsService == null) {
            carsService = (CarsService) MegaPlugin.INSTANCE.getMainApplicationContext()
                    .getBean("carsServiceImpl");
        }
        return this.carsService;
    }

    @Override
    public CarStateService getCarStateService() {
        if(this.carStateService == null) {
            carStateService = (CarStateService) MegaPlugin.INSTANCE.getMainApplicationContext()
                    .getBean("carStateServiceImpl");
        }
        return this.carStateService;
    }


}
