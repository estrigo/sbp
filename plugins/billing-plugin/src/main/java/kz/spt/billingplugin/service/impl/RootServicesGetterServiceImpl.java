package kz.spt.billingplugin.service.impl;

import kz.spt.billingplugin.BillingPlugin;
import kz.spt.billingplugin.service.RootServicesGetterService;
import kz.spt.lib.service.CarsService;
import kz.spt.lib.service.CustomerService;
import kz.spt.lib.service.ParkingService;
import org.springframework.stereotype.Service;

@Service
public class RootServicesGetterServiceImpl implements RootServicesGetterService {

    private ParkingService parkingService;
    private CustomerService customerService;

    @Override
    public ParkingService getParkingService() {
        if(this.parkingService == null){
            parkingService = (ParkingService) BillingPlugin.INSTANCE.getMainApplicationContext().getBean("parkingServiceImpl");
        }
        return parkingService;
    }

    @Override
    public CustomerService getCustomerService() {
        if(this.customerService == null){
            customerService = (CustomerService) BillingPlugin.INSTANCE.getMainApplicationContext().getBean("customerServiceImpl");
        }
        return customerService;
    }
}
