package kz.spt.rateplugin.controller;

import kz.spt.lib.model.Parking;
import kz.spt.rateplugin.model.IntervalRate;
import kz.spt.rateplugin.model.ParkingRate;
import kz.spt.rateplugin.model.RateCondition;
import kz.spt.rateplugin.service.DimensionsService;
import kz.spt.rateplugin.service.RateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Locale;
import java.util.ResourceBundle;

@Slf4j
@Controller
@RequestMapping("/rate")
@RequiredArgsConstructor
public class RateController {

    private final RateService rateService;
    private final DimensionsService dimensionsService;


    @GetMapping("/list")
    public String showAllWhitelist(Model model) {
        model.addAttribute("parkings", rateService.listPaymentParkings());
        return "rate/list";
    }


    @GetMapping("/edit/{parkingId}")
    public String getEditingRateId(Model model, @PathVariable Long parkingId) {
        ParkingRate parkingRate = rateService.getByParkingId(parkingId);
        if(parkingRate == null){
            parkingRate = new ParkingRate();
            Parking parking = rateService.getParkingById(parkingId);
            parkingRate.setParking(parking);
        }
        model.addAttribute("parkingRate", parkingRate);
        model.addAttribute("currencyTypes", ParkingRate.CurrencyType.values());
        model.addAttribute("dimensionsList", dimensionsService.findAll());
        model.addAttribute("intervalRate", new IntervalRate());
        model.addAttribute("intervalRates", rateService.getIntervalRateByParkingRate(parkingRate));
        return "/rate/edit";
    }

    @PostMapping("/add/parking/{parkingId}")
    public String rateEdit(Model model, @PathVariable Long parkingId, @Valid ParkingRate rate, BindingResult bindingResult){
        Locale locale = LocaleContextHolder.getLocale();
        String language = "en";
        if (locale.toString().equals("ru")) {
            language = "ru-RU";
        } else if (locale.toString().equals("de")) {
            language = "de";
        }

        ResourceBundle bundle = ResourceBundle.getBundle("messages", Locale.forLanguageTag(language));
        if(ParkingRate.RateType.STANDARD.equals(rate.getRateType()) && (rate.getOnlinePaymentValue() < 0 && rate.getCashPaymentValue() < 0)){
            ObjectError error = new ObjectError("fillOnlineOrCash", bundle.getString("rate.fillOnlineOrCash"));
            bindingResult.addError(error);
        } else if(ParkingRate.RateType.PROGRESSIVE.equals(rate.getRateType())){
            if(rate.getProgressiveJson() == null || "".equals(rate.getProgressiveJson())){
                ObjectError error = new ObjectError("fillProgressiveValues", bundle.getString("rate.fillProgressiveValues"));
                bindingResult.addError(error);
            }
        } else if(ParkingRate.RateType.INTERVAL.equals(rate.getRateType())){
            if(rate.getIntervalJson() == null || "".equals(rate.getIntervalJson())){
                ObjectError error = new ObjectError("fillIntervalValues", bundle.getString("rate.fillProgressiveValues"));
                bindingResult.addError(error);
            }
        }
        if(rate.getName() == null || "".equals(rate.getName())){
            ObjectError error = new ObjectError("fillName", bundle.getString("rate.fillName"));
            bindingResult.addError(error);
        }

        if(ParkingRate.RateType.FREE.equals(rate.getRateType())) {
            rate.setCashPaymentValue(0);
            rate.setDayPaymentValue(0);
            rate.setOnlinePaymentValue(0);
        }

        if (!bindingResult.hasErrors()) {
            Parking parking = rateService.getParkingById(parkingId);
            rate.setParking(parking);
            rateService.saveRate(rate);
            return "redirect:/rate/list";
        }
        ParkingRate parkingRate = rateService.getByParkingId(parkingId);
        if(parkingRate == null){
            parkingRate = new ParkingRate();
            Parking parking = rateService.getParkingById(parkingId);
            parkingRate.setParking(parking);
        }
        model.addAttribute("parkingRate", parkingRate);
        return "/rate/edit";

    }

    @GetMapping("/interval-edit/{parkingId}")
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


    @PostMapping("/interval-delete")
    public String deleteIntervalRate(@ModelAttribute(value="intervalRate") IntervalRate intervalRate) {
        IntervalRate intervalRateById = rateService.getIntervalRateById(intervalRate.getId());
        Long id = intervalRateById.getParkingRate().getParking().getId();
        rateService.deleteIntervalRate(intervalRateById);
        return "redirect:interval-edit/"+id;
    }

    @PostMapping("/interval-add")
    public String addIntervalRate(@ModelAttribute(value="intervalRate") IntervalRate intervalRate) {
        IntervalRate newInterval = new IntervalRate();
        newInterval.setParkingRate(intervalRate.getParkingRate());
        if (newInterval.getRateConditions()!=null) {
            newInterval.getRateConditions().clear();
        }
        newInterval.setDatetimeTo(intervalRate.getDatetimeTo());
        newInterval.setDatetimeFrom(intervalRate.getDatetimeFrom());
        rateService.saveIntervalRate(newInterval);
        ParkingRate parkingRate = rateService.getById(newInterval.getParkingRate().getId());
        return "redirect:interval-edit/"+parkingRate.getParking().getId();
    }

    @PostMapping("/rateCon-delete")
    public String delRateCondition(@ModelAttribute(value="rateCondition") RateCondition rateCondition) {
        ParkingRate parkingRate = rateService.getById(rateCondition.getIntervalRate().getParkingRate().getId());
        rateService.deleteRateConditionById(rateCondition.getId());
        return "redirect:interval-edit/"+parkingRate.getParking().getId();
    }

    @PostMapping("/rateCon-add")
    public String addRateCondition(@ModelAttribute(value="rateCondition") RateCondition rateCondition) {
        IntervalRate intervalRate = rateService.getIntervalRateById(rateCondition.getIntervalRate().getId());
        rateCondition.setIntervalRate(intervalRate);
        ParkingRate parkingRate = rateService.getById(intervalRate.getParkingRate().getId());
        rateService.saveRateCondition(rateCondition);
        return "redirect:interval-edit/"+parkingRate.getParking().getId();
    }


}