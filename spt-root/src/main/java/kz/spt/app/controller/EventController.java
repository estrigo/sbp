package kz.spt.app.controller;

import kz.spt.api.service.EventLogService;
import kz.spt.app.entity.dto.EventFilterDto;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.validation.Valid;
import java.text.SimpleDateFormat;

@Controller
@RequestMapping("/events")
public class EventController {

    private final String dateformat = "yyyy-MM-dd'T'HH:mm";
    private EventLogService eventLogService;

    public EventController(EventLogService eventLogService){
        this.eventLogService = eventLogService;
    }

    @GetMapping("/list")
    public String showAllEvents(Model model) {
        if(!model.containsAttribute("eventFilter")){
            SimpleDateFormat format = new SimpleDateFormat(dateformat);
            EventFilterDto eventFilter = new EventFilterDto();
            //            eventFilter.dateFromString =
        }

        model.addAttribute("events", eventLogService.listAllLogs());
        return "events/list";
    }

    @PostMapping("/list")
    public String processRequestAddCar(Model model, @Valid @ModelAttribute("eventFilter") EventFilterDto eventFilter, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "cars/list";
        } else {
            return "cars/list";
        }
    }
}
