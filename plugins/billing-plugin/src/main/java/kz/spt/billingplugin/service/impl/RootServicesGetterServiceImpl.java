package kz.spt.billingplugin.service.impl;

import kz.spt.billingplugin.BillingPlugin;
import kz.spt.billingplugin.repository.PaymentProviderRepository;
import kz.spt.billingplugin.service.RootServicesGetterService;
import kz.spt.lib.service.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(noRollbackFor = Exception.class)
public class RootServicesGetterServiceImpl implements RootServicesGetterService {

    private ParkingService parkingService;
    private CustomerService customerService;
    private CarStateService carStateService;
    private PaymentProviderRepository paymentProviderRepository;
    private PaymentCheckLogService paymentCheckLogService;
    private AdminService adminService;
    private MailService mailService;
    private PaymentRegistryJob paymentRegistryJob;
    private SyslogService syslogService;
    private LanguagePropertiesService languagePropertiesService;

    private CarsService carsService;

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

    @Override
    public AdminService getAdminService() {
        if (this.adminService == null) {
            adminService = (AdminService) BillingPlugin.INSTANCE.getMainApplicationContext().getBean("adminServiceImpl");
        }
        return this.adminService;
    }

    @Override
    public CarsService getCarService() {
        if(this.carsService == null){
            carsService = (CarsService) BillingPlugin.INSTANCE.getMainApplicationContext().getBean("carsServiceImpl");
        }
        return carsService;
    }

    @Override
    public MailService getMailService() {
        if (this.mailService == null) {
            mailService = (MailService) BillingPlugin.INSTANCE.getMainApplicationContext().getBean("mailServiceImpl");
        }
        return this.mailService;
    }

    @Override
    public PaymentRegistryJob getPaymentRegistryJob() {
        if (this.paymentRegistryJob == null) {
            paymentRegistryJob = (PaymentRegistryJob) BillingPlugin.INSTANCE.getMainApplicationContext().getBean("paymentRegistryJobImpl");
        }
        return this.paymentRegistryJob;
    }

    @Override
    public SyslogService getSyslogService() {
        if (this.syslogService == null) {
            syslogService = (SyslogService) BillingPlugin.INSTANCE.getMainApplicationContext().getBean("syslogServiceImpl");
        }
        return this.syslogService;
    }

    @Override
    public LanguagePropertiesService getLanguagePropertiesService() {
        if (languagePropertiesService == null) {
            languagePropertiesService = (LanguagePropertiesService) BillingPlugin.INSTANCE.getMainApplicationContext().getBean("languagePropertiesServiceImpl");
        }
        return languagePropertiesService;
    }

}
