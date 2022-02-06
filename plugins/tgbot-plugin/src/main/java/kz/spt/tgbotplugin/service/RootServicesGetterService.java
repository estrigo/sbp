package kz.spt.tgbotplugin.service;

import kz.spt.lib.service.CarStateService;
import kz.spt.lib.service.CarsService;
import kz.spt.lib.service.CustomerService;
import kz.spt.lib.service.ParkingService;

public interface RootServicesGetterService {

    CarsService getCarsService();

    CustomerService getCustomerService();

    ParkingService getParkingService();
}
