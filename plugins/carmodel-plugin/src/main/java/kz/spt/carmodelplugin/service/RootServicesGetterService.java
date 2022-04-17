package kz.spt.carmodelplugin.service;

import kz.spt.lib.service.CarsService;
import kz.spt.lib.service.EventLogService;

public interface RootServicesGetterService {

    CarsService getCarsService();

    EventLogService getEventLogService();

}
