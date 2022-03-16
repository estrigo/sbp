package kz.spt.billingplugin.service;


import kz.spt.lib.service.CarStateService;
import kz.spt.lib.service.CustomerService;
import kz.spt.lib.service.ParkingService;

public interface RootServicesGetterService {

    ParkingService getParkingService();
    CustomerService getCustomerService();
    CarStateService getCarStateService();
}
