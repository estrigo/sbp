package kz.spt.carmodelplugin.service;

import kz.spt.lib.service.CarsService;
import kz.spt.lib.service.EventLogService;
import kz.spt.lib.service.LanguagePropertiesService;

public interface RootServicesGetterService {

    CarsService getCarsService();

    EventLogService getEventLogService();

    LanguagePropertiesService getLanguagesService();

}
