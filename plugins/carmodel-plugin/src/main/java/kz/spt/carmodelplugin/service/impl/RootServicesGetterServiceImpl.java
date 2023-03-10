package kz.spt.carmodelplugin.service.impl;

import kz.spt.carmodelplugin.CarmodelPlugin;
import kz.spt.carmodelplugin.service.RootServicesGetterService;
import kz.spt.lib.service.CarsService;
import kz.spt.lib.service.EventLogService;
import kz.spt.lib.service.LanguagePropertiesService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(noRollbackFor = Exception.class)
public class RootServicesGetterServiceImpl implements RootServicesGetterService {

    private CarsService carsService;
    private EventLogService eventLogService;

    private LanguagePropertiesService languagePropertiesService;

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

    @Override
    public LanguagePropertiesService getLanguagesService() {
        if(this.languagePropertiesService == null) {
            languagePropertiesService = (LanguagePropertiesService) CarmodelPlugin.INSTANCE.getMainApplicationContext().getBean("languagePropertiesServiceImpl");
        }
        return this.languagePropertiesService;
    }
}
