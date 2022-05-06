package kz.spt.megaplugin.service;

import kz.spt.lib.service.CarStateService;
import kz.spt.lib.service.CarsService;

public interface RootServicesGetterService {

    CarsService getCarsService();

    CarStateService getCarStateService();
}
