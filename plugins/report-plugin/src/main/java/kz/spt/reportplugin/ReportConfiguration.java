package kz.spt.reportplugin;

import kz.spt.billingplugin.service.PaymentService;
import kz.spt.lib.service.CarStateService;
import kz.spt.reportplugin.dto.JournalReportDto;
import kz.spt.reportplugin.service.ReportService;
import kz.spt.reportplugin.service.RootGetterService;
import kz.spt.reportplugin.service.impl.JournalReportServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ReportConfiguration {
    @Autowired
    private PaymentService paymentService;
    private CarStateService carStateService;

    public ReportConfiguration(){
        if(this.carStateService == null){
            carStateService = (CarStateService) ReportPlugin.INSTANCE.getMainApplicationContext().getBean("carStateServiceImpl");
        }
    }

    @Bean("journalReportService")
    public ReportService<JournalReportDto> journalReportService() {
        return new JournalReportServiceImpl(carStateService, paymentService);
    }
}
