package kz.spt.tgbotplugin.service.impl;

import kz.spt.lib.service.CarStateService;
import kz.spt.lib.service.CarsService;
import kz.spt.lib.service.CustomerService;
import kz.spt.lib.service.ParkingService;
import kz.spt.tgbotplugin.TgBotPlugin;
import kz.spt.tgbotplugin.service.RootServicesGetterService;
import org.springframework.stereotype.Service;

@Service
public class RootServicesGetterServiceImpl implements RootServicesGetterService {

    private CarsService carsService;
    private CustomerService customerService;
    private ParkingService parkingService;

    @Override
    public CarsService getCarsService() {
        if (this.carsService == null) {
            carsService = (CarsService) TgBotPlugin.INSTANCE.getMainApplicationContext().getBean("carsServiceImpl");
        }
        return this.carsService;
    }

    @Override
    public CustomerService getCustomerService() {
        if (this.customerService == null){
            customerService = (CustomerService) TgBotPlugin.INSTANCE.getMainApplicationContext().getBean("customerServiceImpl");
        }
        return this.customerService;
    }

    @Override
    public ParkingService getParkingService() {
        if (this.parkingService == null){
            parkingService = (ParkingService) TgBotPlugin.INSTANCE.getMainApplicationContext().getBean("parkingServiceImpl");
        }
        return this.parkingService;
    }
}
