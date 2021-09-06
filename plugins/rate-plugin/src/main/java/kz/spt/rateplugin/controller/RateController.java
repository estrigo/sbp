package kz.spt.rateplugin.controller;

import kz.spt.api.model.Parking;
import kz.spt.rateplugin.model.ParkingRate;
import kz.spt.rateplugin.service.RateService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.validation.Valid;

@Controller
@RequestMapping("/rate")
public class RateController {

    private RateService rateService;

    public RateController(RateService rateService){
        this.rateService = rateService;
    }

    @GetMapping("/list")
    public String showAllWhitelist(Model model) {
        model.addAttribute("parkings", rateService.listPaymentParkings());
        return "rate/list";
    }

    @GetMapping("{rateId}")
    public String getEditingRateId(Model model, @PathVariable Long rateId) {
        ParkingRate parkingRate = rateService.getById(rateId);
        model.addAttribute("parkingRate", parkingRate);
        return "/rate/edit";
    }

    @PostMapping("/add/parking/{parkingId}")
    public String rateEdit(@PathVariable Long parkingId, @Valid ParkingRate rate, BindingResult bindingResult){
        if (!bindingResult.hasErrors()) {
            Parking parking = rateService.getParkingById(parkingId);
            rate.setParking(parking);
            rateService.saveRate(rate);
        }
        return "redirect:/rate/list";
    }

    @GetMapping("/{parkingId}/new/rate")
    public String getEditingRateId(@PathVariable Long parkingId, Model model) {
        ParkingRate parkingRate = new ParkingRate();
        parkingRate.setParking(rateService.getParkingById(parkingId));
        model.addAttribute("parkingRate", parkingRate);
        return "/rate/edit";
    }
}