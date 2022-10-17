package kz.spt.app.controller;

import kz.spt.app.service.GateService;
import kz.spt.lib.model.Gate;
import kz.spt.lib.service.EventLogService;
import kz.spt.lib.model.dto.EventFilterDto;
import lombok.extern.java.Log;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.validation.Valid;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

@Controller
@RequestMapping("/events")
@Log
public class EventController {

    private final String dateformat = "yyyy-MM-dd'T'HH:mm";
    private EventLogService eventLogService;
    private GateService gateService;

    public EventController(EventLogService eventLogService, GateService gateService){
        this.eventLogService = eventLogService;
        this.gateService = gateService;
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
    public String processRequestSearch(Model model, @Valid @ModelAttribute("eventFilterDto") EventFilterDto eventFilterDto, BindingResult bindingResult) throws ParseException {
        if (!bindingResult.hasErrors()) {
            model.addAttribute("eventFilterDto", eventFilterDto);
        }
        model.addAttribute("allGates", gateService.listAllGates());
        return "events/list";
    }
}
