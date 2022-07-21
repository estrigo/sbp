package kz.spt.billingplugin.service.impl;

import kz.spt.billingplugin.BillingPlugin;
import kz.spt.billingplugin.repository.PaymentProviderRepository;
import kz.spt.billingplugin.service.RootServicesGetterService;
import kz.spt.lib.service.*;
import org.springframework.stereotype.Service;

@Service
public class RootServicesGetterServiceImpl implements RootServicesGetterService {

    private ParkingService parkingService;
    private CustomerService customerService;
    private CarStateService carStateService;
    private PaymentProviderRepository paymentProviderRepository;

    private PaymentCheckLogService paymentCheckLogService;


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

    @Override
    public CarStateService getCarStateService() {
        if(this.carStateService == null){
            carStateService = (CarStateService) BillingPlugin.INSTANCE.getMainApplicationContext().getBean("carStateServiceImpl");
        }
        return carStateService;
    }

    @Override
    public PaymentProviderRepository getPaymentProviderRepository() {
        if(this.paymentProviderRepository == null) {
            paymentProviderRepository = (PaymentProviderRepository) BillingPlugin.INSTANCE.getApplicationContext()
                    .getBean("paymentProviderRepository");
        }
        return paymentProviderRepository;
    }

    @Override
    public PaymentCheckLogService getPaymentCheckLogService() {
        if (this.paymentCheckLogService == null) {
            paymentCheckLogService = (PaymentCheckLogService)
                    BillingPlugin.INSTANCE.getMainApplicationContext().getBean("paymentCheckLogServiceImpl");
        }
        return paymentCheckLogService;
    }
}
