package kz.spt.reportplugin;

import kz.spt.lib.model.dto.EventsDto;
import kz.spt.reportplugin.dto.JournalReportDto;
import kz.spt.reportplugin.dto.SumReportDto;
import kz.spt.reportplugin.service.ReportService;
import kz.spt.reportplugin.service.impl.JournalReportServiceImpl;
import kz.spt.reportplugin.service.impl.ManualOpenReportServiceImpl;
import kz.spt.reportplugin.service.impl.SumReportServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ReportConfiguration {
    @Bean("journalReportService")
    public ReportService<JournalReportDto> journalReportService() {
        return new JournalReportServiceImpl();
    }

    @Bean("manualOpenReportService")
    public ReportService<EventsDto> manualOpenReportService() {
        return new ManualOpenReportServiceImpl();
    }

    @Bean("sumReportService")
    public ReportService<SumReportDto> sumReportService() {
        return new SumReportServiceImpl();
    }
}
