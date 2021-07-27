package kz.spt.app.controller;

import kz.spt.api.model.Parking;
import kz.spt.app.service.ParkingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.validation.Valid;

@Controller
@RequestMapping("/parking")
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

    @PostMapping("/add")
    public String processRequestAddParking(Model model, @Valid Parking parking, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "parking/add";
        } else {
            parkingService.saveParking(parking);
            return "redirect:/parking/list";
        }
    }

    @GetMapping("/edit/{id}")
    public String showFormEditParrking(Model model, @PathVariable Long id) {
        model.addAttribute("parking", parkingService.findById(id));
        return "parking/edit";
    }

    @PostMapping("/edit/{id}")
    public String processRequestEditCar(@PathVariable Long id, @Valid Parking parking, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "redirect:/parking/edit/" + id;
        } else {
            parkingService.saveParking(parking);
            return "redirect:/parking/list";
        }
    }

    @GetMapping("/details/{parkingId}")
    public String showAllParking(Model model, @PathVariable Long parkingId) {
        Parking parking = parkingService.findById(parkingId);
        if(parking != null){
            model.addAttribute("parking", parking);
            return "parking/details";
        } else {
            model.addAttribute("error", "global.notFound");
            return "404";
        }
    }
}