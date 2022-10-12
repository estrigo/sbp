package kz.spt.billingplugin.service;


import kz.spt.app.repository.PropertyRepository;
import kz.spt.billingplugin.repository.PaymentProviderRepository;
import kz.spt.lib.service.*;

public interface RootServicesGetterService {

    ParkingService getParkingService();
    CustomerService getCustomerService();
    CarStateService getCarStateService();
    PaymentProviderRepository getPaymentProviderRepository();
    PaymentCheckLogService getPaymentCheckLogService();
    AdminService getAdminService();
    CarsService getCarService();
    MailService getMailService();
    PropertyRepository getPropertyRepository();
}
