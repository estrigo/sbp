package kz.spt.carmodelplugin.service.impl;

import kz.spt.carmodelplugin.CarmodelPlugin;
import kz.spt.carmodelplugin.service.RootServicesGetterService;
import kz.spt.lib.service.CarsService;
import kz.spt.lib.service.EventLogService;
import org.springframework.stereotype.Service;

@Service
public class RootServicesGetterServiceImpl implements RootServicesGetterService {

    private CarsService carsService;
    private EventLogService eventLogService;

    @Override
    public CarsService getCarsService() {
        if (this.carsService == null) {
            carsService = (CarsService) CarmodelPlugin.INSTANCE.getMainApplicationContext().getBean("carsServiceImpl");
        }
        return this.carsService;
    }

    @Override
    public EventLogService getEventLogService(){
        if(this.eventLogService == null) {
            eventLogService = (EventLogService) CarmodelPlugin.INSTANCE.getMainApplicationContext().getBean("eventLogServiceImpl");
        }
        return this.eventLogService;
    }
}
