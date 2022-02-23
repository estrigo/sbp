package kz.spt.reportplugin.service.impl;

import kz.spt.lib.service.CarStateService;
import kz.spt.reportplugin.ReportPlugin;
import kz.spt.reportplugin.service.RootGetterService;
import org.jvnet.hk2.annotations.Service;

@Service
public class RootGetterServiceImpl implements RootGetterService {
    private CarStateService carStateService;

    @Override
    public CarStateService getCarStateService() {
        if (this.carStateService == null) {
            carStateService = (CarStateService) ReportPlugin.INSTANCE.getMainApplicationContext().getBean("carsStateServiceImpl");
        }

        return this.carStateService;
    }
}