package kz.spt.rateplugin.controller;

import kz.spt.lib.model.Parking;
import kz.spt.rateplugin.model.ParkingRate;
import kz.spt.rateplugin.service.RateService;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.validation.Valid;
import java.util.Locale;
import java.util.ResourceBundle;

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

    @GetMapping("/edit/{parkingId}")
    public String getEditingRateId(Model model, @PathVariable Long parkingId) {
        ParkingRate parkingRate = rateService.getByParkingId(parkingId);
        if(parkingRate == null){
            parkingRate = new ParkingRate();
            Parking parking = rateService.getParkingById(parkingId);
            parkingRate.setParking(parking);
        }
        model.addAttribute("parkingRate", parkingRate);
        return "/rate/edit";
    }

    @PostMapping("/add/parking/{parkingId}")
    public String rateEdit(@PathVariable Long parkingId, @Valid ParkingRate rate, BindingResult bindingResult){
        Locale locale = LocaleContextHolder.getLocale();
        String language = "en";
        if (locale.toString().equals("ru")) {
            language = "ru-RU";
        }

        ResourceBundle bundle = ResourceBundle.getBundle("messages", Locale.forLanguageTag(language));
        if(ParkingRate.RateType.STANDARD.equals(rate.getRateType()) && (rate.getOnlinePaymentValue() == 0 && rate.getCashPaymentValue() == 0)){
            ObjectError error = new ObjectError("fillOnlineOrCash", bundle.getString("rate.fillOnlineOrCash"));
            bindingResult.addError(error);
        } else if(ParkingRate.RateType.PROGRESSIVE.equals(rate.getRateType())){
            if(rate.getProgressiveJson() == null || "".equals(rate.getProgressiveJson())){
                ObjectError error = new ObjectError("fillProgressiveValues", bundle.getString("rate.fillProgressiveValues"));
                bindingResult.addError(error);
            } else {
                
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

        if (!bindingResult.hasErrors()) {
            Parking parking = rateService.getParkingById(parkingId);
            rate.setParking(parking);
            rateService.saveRate(rate);
        }
        return "redirect:/rate/list";
    }
}