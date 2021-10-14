package kz.spt.whitelistplugin.service;

import kz.spt.lib.service.CarStateService;
import kz.spt.lib.service.CarsService;

public interface RootServicesGetterService {

    CarsService getCarsService();

    CarStateService getCarStateService();
}
