package kz.spt.reportplugin.controller;

import kz.spt.reportplugin.dto.filter.FilterJournalReportDto;
import kz.spt.reportplugin.service.ReportService;
import kz.spt.reportplugin.service.RootServicesGetterService;
import kz.spt.reportplugin.service.impl.JournalReportServiceImpl;
import kz.spt.reportplugin.service.impl.ManualOpenReportServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Controller
@RequestMapping("/report")
public class ReportController {

    @Autowired
    private RootServicesGetterService rootServicesGetterService;

    @GetMapping("/journal")
    public String journal() {
        return "report/journal";
    }

    @GetMapping("/manualOpen")
    public String manualOpen() {
        return "report/manualOpen";
    }

    @GetMapping("/sum")
    public String sum() {
        return "report/sum";
    }


    @PostMapping("/download/{name}/{format}")
    public void downloadFile(@PathVariable ReportNameEnum name, @PathVariable String format,
                             @RequestParam(required = false, name = "dateFrom") String dateFrom,
                             @RequestParam(required = false, name = "dateTo") String dateTo,

                             HttpServletResponse response) throws IOException, ParseException {
        FilterJournalReportDto filter = new FilterJournalReportDto();
        if (!ObjectUtils.isEmpty(dateFrom)) {
            filter.setDateFrom(new SimpleDateFormat("dd/MM/yyyy").parse(dateFrom));
        }
        if (!ObjectUtils.isEmpty(dateTo)) {
            filter.setDateTo(new SimpleDateFormat("dd/MM/yyyy").parse(dateTo));
        }
        List<?> list = Objects.requireNonNull(getReportService(name)).list(filter);

        byte[] bytes = rootServicesGetterService.getAdminService().report(list, name.name(), format);
        response.setContentType("application/octet-stream");
        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename = "
                .concat(UUID.randomUUID().toString()).concat(".").concat(format.toLowerCase());
        response.setHeader(headerKey, headerValue);
        ServletOutputStream outputStream = response.getOutputStream();
        outputStream.write(bytes);
        outputStream.close();
    }


    private ReportService<?> getReportService(ReportNameEnum name) {
        switch (name) {
            case JOURNAL:
                return new JournalReportServiceImpl();
            case MANUAL_OPEN:
                return new ManualOpenReportServiceImpl();
            case PAYMENTS:
            default:
                return null;
        }
    }
}
