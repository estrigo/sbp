package kz.spt.abonomentplugin.service;

import kz.spt.lib.service.CarsService;
import kz.spt.lib.service.ParkingService;

public interface RootServicesGetterService {

    CarsService getCarsService();

    ParkingService getParkingService();
}
