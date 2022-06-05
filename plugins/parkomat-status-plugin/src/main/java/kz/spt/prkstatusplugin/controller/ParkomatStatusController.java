package kz.spt.prkstatusplugin.controller;

import kz.spt.prkstatusplugin.service.ParkomatService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping(value = "/prkstatus/status")
public class ParkomatStatusController {

    private ParkomatService parkomatService;

    public ParkomatStatusController(ParkomatService parkomatService) {
        this.parkomatService = parkomatService;
    }

    @GetMapping("/list")
    public String showTestList(Model model) {

        model.addAttribute("whitelist", null);
        //List<?> parList =  parkomatService.getParkomatProviders();
        return "prkstatus/balance/list";
    }

    @PostMapping("/log")
    public String receiveLog() {



        return null;
    }
}
