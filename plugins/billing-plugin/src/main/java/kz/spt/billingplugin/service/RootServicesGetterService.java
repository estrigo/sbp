package kz.spt.billingplugin.service;


import kz.spt.billingplugin.repository.PaymentProviderRepository;
import kz.spt.lib.service.CarStateService;
import kz.spt.lib.service.CustomerService;
import kz.spt.lib.service.ParkingService;
import kz.spt.lib.service.PaymentCheckLogService;

public interface RootServicesGetterService {

    ParkingService getParkingService();
    CustomerService getCustomerService();
    CarStateService getCarStateService();
    PaymentProviderRepository getPaymentProviderRepository();
    PaymentCheckLogService getPaymentCheckLogService();
}
