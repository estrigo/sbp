package kz.spt.app.controller;

import kz.spt.api.model.Parking;
import kz.spt.app.service.ParkingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

public class ParkingController {

    @Autowired
    private ParkingService parkingService;

    @GetMapping("/list")
    public String showAllParking(Model model) {
        model.addAttribute("parkings", parkingService.listAllParking());
        return "parking/list";
    }

    @GetMapping("/add")
    public String showFormAddParking(Model model) {
        model.addAttribute("parking", new Parking());
        return "parking/add";
    }
}