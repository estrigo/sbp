package kz.spt.app.controller;

import kz.spt.api.service.EventLogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/events")
public class EventController {

    private EventLogService eventLogService;

    public EventController(EventLogService eventLogService){
        this.eventLogService = eventLogService;
    }

    @GetMapping("/list")
    public String showAllEvents(Model model) {
        model.addAttribute("events", eventLogService.listAllLogs());
        return "events/list";
    }
}
