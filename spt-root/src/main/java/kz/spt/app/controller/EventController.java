package kz.spt.app.controller;

import kz.spt.app.service.GateService;
import kz.spt.lib.service.AdminService;
import kz.spt.lib.service.EventLogService;
import kz.spt.lib.model.dto.EventFilterDto;
import kz.spt.reportplugin.controller.ReportNameEnum;
import lombok.extern.java.Log;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/events")
@Log
public class EventController {

    private final String dateformat = "yyyy-MM-dd'T'HH:mm";
    private EventLogService eventLogService;
    private GateService gateService;
    private AdminService adminService;

    public EventController(EventLogService eventLogService, GateService gateService, AdminService adminService){
        this.eventLogService = eventLogService;
        this.gateService = gateService;
        this.adminService = adminService;
    }

    @GetMapping("/list")
    public String showAllEvents(Model model) throws ParseException {
        EventFilterDto eventFilterDto = null;
        if(!model.containsAttribute("eventFilterDto")){
            SimpleDateFormat format = new SimpleDateFormat(dateformat);
            eventFilterDto = new EventFilterDto();

            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MINUTE, 1);
            Date dateTo = calendar.getTime();
            eventFilterDto.dateToString = format.format(dateTo);

            calendar.add(Calendar.WEEK_OF_YEAR, -1);
            Date dateFrom = calendar.getTime();
            eventFilterDto.dateFromString = format.format(dateFrom);
            model.addAttribute("eventFilterDto", eventFilterDto);
        } else {
            eventFilterDto = (EventFilterDto) model.getAttribute("eventFilterDto");
            model.addAttribute("eventFilterDto", eventFilterDto);
        }
        model.addAttribute("allGates", gateService.listAllGates());
        return "events/list";
    }

    @PostMapping("/list")
    public String processRequestSearch(Model model, @Valid @ModelAttribute("eventFilterDto") EventFilterDto eventFilterDto,
                                       BindingResult bindingResult) throws Exception {
        if (!bindingResult.hasErrors()) {
            model.addAttribute("eventFilterDto", eventFilterDto);
        }
        model.addAttribute("allGates", gateService.listAllGates());
        return "events/list";
    }

    @PostMapping("/excel")
    public void downloadEventsFile(@Valid @ModelAttribute("eventFilterDto") EventFilterDto eventFilterDto,
                                   HttpServletResponse response) throws Exception {
        byte[] bytes;
        ReportNameEnum name = ReportNameEnum.EVENTS;
        String format = "XLSX";

        List<?> list = eventLogService.getEventExcel(eventFilterDto);

        bytes = adminService.report(list, name.name(), format);

        response.setContentType("application/octet-stream");
        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename = "
                .concat(UUID.randomUUID().toString()).concat(".").concat(format.toLowerCase());
        response.setHeader(headerKey, headerValue);
        ServletOutputStream outputStream = response.getOutputStream();
        outputStream.write(bytes);
        outputStream.close();
    }

}
