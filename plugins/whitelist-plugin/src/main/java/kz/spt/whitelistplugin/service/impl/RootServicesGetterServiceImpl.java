package kz.spt.whitelistplugin.service.impl;

import kz.spt.lib.service.CarsService;
import kz.spt.whitelistplugin.WhitelistPlugin;
import kz.spt.whitelistplugin.service.RootServicesGetterService;
import org.springframework.stereotype.Service;

@Service
public class RootServicesGetterServiceImpl implements RootServicesGetterService {

    private CarsService carsService;

    @Override
    public CarsService getCarsService() {
        if (this.carsService == null) {
            carsService = (CarsService) WhitelistPlugin.INSTANCE.getMainApplicationContext().getBean("carsServiceImpl");
        }
        return carsService;
    }
}
