package kz.spt.app.controller;

import kz.spt.app.model.dto.BillingReportFilterDto;
import kz.spt.app.service.ReportService;
import kz.spt.lib.model.dto.EventFilterDto;
import lombok.extern.java.Log;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.text.SimpleDateFormat;
import java.util.Calendar;

@Log
@Controller
@RequestMapping("/report")
public class ReportController {

    private ReportService reportService;
    private final String dateformat = "yyyy-MM-dd'T'HH:mm";

    public ReportController(ReportService reportService){
        this.reportService = reportService;
    }

    @GetMapping("/billingReport")
    public String showAllBilling(Model model) throws Exception {
        BillingReportFilterDto billingReportFilterDto = null;
        if(!model.containsAttribute("billingReportFilterDto")){
            SimpleDateFormat format = new SimpleDateFormat(dateformat);
            billingReportFilterDto = new BillingReportFilterDto();

            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR, 23);
            calendar.set(Calendar.MINUTE, 59);
            calendar.set(Calendar.SECOND, 59);
            billingReportFilterDto.dateToString = format.format(calendar.getTime());

            calendar.set(Calendar.DATE, 1);
            calendar.set(Calendar.HOUR, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            billingReportFilterDto.dateFromString = format.format(calendar.getTime());

            model.addAttribute("billingReportFilterDto", billingReportFilterDto);
        } else {
            log.info("model.containsAttribute(\"billingReportFilterDto\")");
            billingReportFilterDto = (BillingReportFilterDto) model.getAttribute("billingReportFilterDto");
            model.addAttribute("billingReportFilterDto", billingReportFilterDto);
        }
        model.addAttribute("providers", reportService.getPaymentProviders());
        return "report/billingReport";
    }

}
