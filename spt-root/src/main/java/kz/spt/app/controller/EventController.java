package kz.spt.app.controller;

import kz.spt.api.service.EventLogService;
import kz.spt.api.model.dto.EventFilterDto;
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

    public EventController(EventLogService eventLogService){
        this.eventLogService = eventLogService;
    }

    @GetMapping("/list")
    public String showAllEvents(Model model) throws ParseException {
        EventFilterDto eventFilter = null;
        if(!model.containsAttribute("eventFilter")){
            SimpleDateFormat format = new SimpleDateFormat(dateformat);
            eventFilter = new EventFilterDto();

            Calendar calendar = Calendar.getInstance();
            Date dateTo = calendar.getTime();
            eventFilter.dateToString = format.format(dateTo);

            calendar.add(Calendar.MONTH, -1);
            Date dateFrom = calendar.getTime();
            eventFilter.dateFromString = format.format(dateFrom);
            model.addAttribute("eventFilter", eventFilter);
        } else {
            eventFilter = (EventFilterDto) model.getAttribute("eventFilter");
        }
        model.addAttribute("events", eventLogService.listByFilters(eventFilter));
        return "events/list";
    }

    @PostMapping("/list")
    public String processRequestAddCar(Model model, @Valid @ModelAttribute("eventFilter") EventFilterDto eventFilter, BindingResult bindingResult) throws ParseException {
        if (bindingResult.hasErrors()) {
            return "events/list";
        } else {
            model.addAttribute("events", eventLogService.listByFilters(eventFilter));
            return "events/list";
        }
    }
}
