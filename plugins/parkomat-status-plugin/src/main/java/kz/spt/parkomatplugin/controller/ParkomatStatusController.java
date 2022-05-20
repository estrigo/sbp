package kz.spt.parkomatplugin.controller;

import kz.spt.parkomatplugin.service.ParkomatService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Controller
@RequestMapping(value = "/parkomat-monitor")
public class ParkomatStatusController {

    private ParkomatService parkomatService;

    public ParkomatStatusController(ParkomatService parkomatService) {
        this.parkomatService = parkomatService;
    }

    @GetMapping("/status")
    public String showTestList(Model model) {

        List<?> parList =  parkomatService.getParkomatProviders();
        return "parkomat-status/index";
    }

    @PostMapping("/log")
    public String receiveLog() {



        return null;
    }
}
