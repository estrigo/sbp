package kz.spt.prkstatusplugin.controller;

import kz.spt.prkstatusplugin.model.ParkomatConfig;
import kz.spt.prkstatusplugin.model.PaymentProvider;
import kz.spt.prkstatusplugin.service.ParkomatService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@Controller
@RequestMapping(value = "/prkstatus/status")
public class ParkomatStatusController {

    private ParkomatService parkomatService;

    public ParkomatStatusController(ParkomatService parkomatService) {
        this.parkomatService = parkomatService;
    }

    @GetMapping("/list")
    public String showTestList(Model model) {
        List<PaymentProvider> paymentProviderList = (List<PaymentProvider>) parkomatService.getParkomatProviders();
        model.addAttribute("parkomatList", paymentProviderList);

        return "prkstatus/status/list";
    }

    @GetMapping("/config")
    public String getConfig(Model model, @RequestParam(value = "parkomatIP", required = false) String parkomatIP) {

        if (parkomatIP != null) {
            ParkomatConfig parkomatConfig = parkomatService.getParkomatConfig(parkomatIP);
            if (parkomatConfig != null) {
                model.addAttribute("parkomatConfig",  parkomatConfig);
            } else {
                parkomatConfig = new ParkomatConfig();
                parkomatConfig.setIp(parkomatIP);
                parkomatService.saveParkomatConfig(parkomatConfig);
                model.addAttribute("parkomatConfig",  parkomatConfig);
            }
        }

        return "prkstatus/status/config";
    }

    @PostMapping("/save")
    public String providerEdit(@Valid ParkomatConfig parkomatConfig, BindingResult bindingResult) {

        if (!bindingResult.hasErrors()) {

            parkomatService.saveParkomatConfig(parkomatConfig);
        }
        return "redirect:/prkstatus/status/list";
    }


}
