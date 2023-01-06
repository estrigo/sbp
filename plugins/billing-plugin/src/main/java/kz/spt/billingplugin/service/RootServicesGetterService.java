package kz.spt.billingplugin.service;


import kz.spt.billingplugin.repository.PaymentProviderRepository;
import kz.spt.lib.service.*;
import kz.spt.lib.service.PaymentService;

public interface RootServicesGetterService {

    ParkingService getParkingService();
    CustomerService getCustomerService();
    CarStateService getCarStateService();
    PaymentProviderRepository getPaymentProviderRepository();
    PaymentCheckLogService getPaymentCheckLogService();
    AdminService getAdminService();
    CarsService getCarService();
    MailService getMailService();
    PaymentRegistryJob getPaymentRegistryJob();
    SyslogService getSyslogService();
    LanguagePropertiesService getLanguageService();
    PaymentService getRootPaymentService();
}
