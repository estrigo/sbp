package kz.spt.app.controller;

import kz.spt.api.model.Parking;
import kz.spt.app.service.GateService;
import kz.spt.app.service.ParkingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/gate")
public class GateController {

    private ParkingService parkingService;
    private GateService gateService;

    public GateController(ParkingService parkingService){
        this.parkingService = parkingService;
    }

    @GetMapping("/list/parking/{parkingId}")
    public String showAllParking(Model model, @PathVariable Long parkingId) {
        Parking parking = parkingService.findById(parkingId);
        if(parking != null){
            model.addAttribute("parking", parking);
            return "gate/list";
        } else {
            model.addAttribute("error", "global.notFound");
            return "404";
        }
    }
}

