package kz.spt.carmodelplugin.controller;

import kz.spt.carmodelplugin.service.CarDimensionsService;
import kz.spt.lib.model.Dimensions;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Arrays;

@Log
@Controller
@RequestMapping("/cardimensions")
@RequiredArgsConstructor
public class CarDimensionsController {

    private final CarDimensionsService carDimensionsService;

    @GetMapping("/configure/car/delete/{id}")
    public String deleteCarDimension(Model model, @PathVariable("id") long id, @AuthenticationPrincipal UserDetails currentUser) {
        carDimensionsService.deleteDimension(id);
        model.addAttribute("dimensions", carDimensionsService.listDimensions());
        model.addAttribute("canEdit", currentUser.getAuthorities().stream().anyMatch(m -> Arrays.asList("ROLE_ADMIN", "ROLE_OPERATOR").contains(m.getAuthority())));
        return "redirect:/carmodel/configure/carDimensions/add";
    }

    @GetMapping("/configure/car/edit/{id}")
    public String editCarDimension(Model model, @PathVariable("id") long id, @AuthenticationPrincipal UserDetails currentUser) {
        model.addAttribute("dimensions", carDimensionsService.getById(id));
        return "/carmodel/editDimension";
    }

    @PostMapping("/configure/car/update/{id}")
    public String updateCarModel(@PathVariable("id") long id,
                                 @ModelAttribute("carModel") @Valid Dimensions dimensions,
                                 Model model,
                                 @AuthenticationPrincipal UserDetails currentUser) {
        carDimensionsService.updateCarDimension(id, dimensions, currentUser);
        return "redirect:/carmodel/configure/carDimensions/add";

    }
}


