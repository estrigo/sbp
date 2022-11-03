package kz.spt.rateplugin.controller;

import kz.spt.lib.model.Dimensions;
import kz.spt.lib.model.Parking;
import kz.spt.rateplugin.model.IntervalRate;
import kz.spt.rateplugin.model.ParkingRate;
import kz.spt.rateplugin.model.RateCondition;
import kz.spt.rateplugin.service.DimensionsService;
import kz.spt.rateplugin.service.RateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashSet;
import java.util.Set;


@Slf4j
@Controller
@RequestMapping("/rate-dimensions")
@RequiredArgsConstructor
public class RateDimensionsController {

    private final RateService rateService;
    private final DimensionsService dimensionsService;

    /*@PostMapping("/interval-add")
    public String addIntervalRate(@ModelAttribute(value="intervalRate") IntervalRate intervalRate,
                                  BindingResult bindingResult,
                                  Model model) {
        IntervalRate newInterval = new IntervalRate();
        newInterval.setParkingRate(intervalRate.getParkingRate());
        if (newInterval.getRateConditions()!=null) {
            newInterval.getRateConditions().clear();
        }
        newInterval.setDatetimeTo(intervalRate.getDatetimeTo());
        newInterval.setDatetimeFrom(intervalRate.getDatetimeFrom());
        rateService.saveIntervalRate(newInterval);
        ParkingRate parkingRate = rateService.getById(newInterval.getParkingRate().getId());
        return "redirect:interval-dimensions-edit/"+parkingRate.getParking().getId();
    }*/

    @PostMapping("/interval-add")
    public String addIntervalRate(@ModelAttribute(value="intervalRate") IntervalRate intervalRate,
                                  BindingResult bindingResult,
                                  Model model) {
        IntervalRate newInterval = new IntervalRate();
        newInterval.setParkingRate(intervalRate.getParkingRate());
        if (newInterval.getRateConditions()!=null) {
            newInterval.getRateConditions().clear();
        }
        newInterval.setDatetimeTo(intervalRate.getDatetimeTo());
        newInterval.setDatetimeFrom(intervalRate.getDatetimeFrom());
        ObjectError objectError = bindingResult.getAllErrors().get(0);
        Object rejectedValue = ((FieldError) objectError).getRejectedValue();
        String s = rejectedValue.toString();
        String[] output = s.split(",");
        Set<Dimensions> dimensionsSet = new HashSet<>();
        for (String s1 : output) {
            Dimensions byId = dimensionsService.findById(s1);
            dimensionsSet.add(byId);
        }
        newInterval.setDimensionSet(dimensionsSet);
        rateService.saveIntervalRate(newInterval);
        ParkingRate parkingRate = rateService.getById(newInterval.getParkingRate().getId());
        return "redirect:interval-dimensions-edit/"+parkingRate.getParking().getId();
    }
    @GetMapping("/interval-dimensions-edit/{parkingId}")
    public String editingIntervalRate(Model model, @PathVariable Long parkingId) {
        ParkingRate parkingRate = rateService.getByParkingId(parkingId);
        if(parkingRate == null){
            parkingRate = new ParkingRate();
            Parking parking = rateService.getParkingById(parkingId);
            parkingRate.setParking(parking);
        }
        model.addAttribute("parkingRate", parkingRate);
        model.addAttribute("IntervalType", RateCondition.IntervalType.values());
        model.addAttribute("intervalRates", rateService.getIntervalRateByParkingRate(parkingRate));
        return "/rate/interval-edit";
    }

    /*@GetMapping("/interval-by-dimensions-edit/{parkingId}")
    public String editingDimensionsByIntervalRate(Model model, @PathVariable Long parkingId) {
        ParkingRate parkingRate = rateService.getByParkingId(parkingId);
        if(parkingRate == null){
            parkingRate = new ParkingRate();
            Parking parking = rateService.getParkingById(parkingId);
            parkingRate.setParking(parking);
        }
        model.addAttribute("parkingRate", parkingRate);
        m odel.addAttribute("intervalRate", new IntervalRate());
        model.addAttribute("IntervalType", RateCondition.IntervalType.values());
        model.addAttribute("dimensions", dimensionsService.findAll());
        model.addAttribute("intervalRates", rateService.getIntervalRateByParkingRate(parkingRate));
        return "/rate/interval-dimensions-edit";
    }*/

    @GetMapping("/interval-by-dimensions-edit/{parkingId}")
    public String editingDimensionsByIntervalRate(Model model, @PathVariable Long parkingId) {
        ParkingRate parkingRate = rateService.getByParkingId(parkingId);
        if(parkingRate == null){
            parkingRate = new ParkingRate();
            Parking parking = rateService.getParkingById(parkingId);
            parkingRate.setParking(parking);
        }
        model.addAttribute("parkingRate", parkingRate);
        model.addAttribute("intervalRate", new IntervalRate());
        model.addAttribute("IntervalType", RateCondition.IntervalType.values());
        model.addAttribute("dimensions", dimensionsService.findAll());
        model.addAttribute("intervalRates", rateService.getIntervalRateByParkingRate(parkingRate));
        return "/rate/interval-dimensions-editXXXX";
    }

    //#ans NACHAL TUT TESTIT
   /* @GetMapping("/interval-by-dimensions-edit/{parkingId}")
    public String editingDimensionsByIntervalRate(Model model, @PathVariable Long parkingId) {
        ParkingRate parkingRate = rateService.getByParkingId(parkingId);
        model.addAttribute("intervalRateDto", new IntervalRateDto());
        model.addAttribute("dimensions", dimensionsService.findAll());
        return "/rate/interval-dimensions-editDDDDDDDDD";
    }*/

   /* @PostMapping("/create")
    public String createCarModel(@ModelAttribute("intervalRateDto") @Valid IntervalRateDto intervalRateDto,
                                 Model model,
                                 BindingResult bindingResult,
                                 @AuthenticationPrincipal UserDetails currentUser) {
    return null;
    }*/


//TUT KONEC TESTA

    @PostMapping("/interval-delete")
    public String deleteIntervalRate(@ModelAttribute(value="intervalRate") IntervalRate intervalRate) {
        IntervalRate intervalRateById = rateService.getIntervalRateById(intervalRate.getId());
        Long id = intervalRateById.getParkingRate().getParking().getId();
        rateService.deleteIntervalRate(intervalRateById);
        return "redirect:interval-dimensions-edit/"+id;
    }
}
