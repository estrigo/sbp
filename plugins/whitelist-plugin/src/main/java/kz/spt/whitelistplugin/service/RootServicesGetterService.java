package kz.spt.whitelistplugin.service;

import kz.spt.lib.service.CarStateService;
import kz.spt.lib.service.CarsService;
import kz.spt.lib.service.ParkingService;

public interface RootServicesGetterService {

    CarsService getCarsService();

    CarStateService getCarStateService();

    ParkingService getParkingService();
}
