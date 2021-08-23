package kz.spt.rateplugin.controller;

import kz.spt.rateplugin.service.RateService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/rate")
public class RateController {

    private RateService rateService;

    public RateController(RateService rateService){
        this.rateService = rateService;
    }

    @GetMapping("/list")
    public String showAllWhitelist(Model model) {
//        model.addAttribute("tar", rateService.listAllRates());
        return "rate/list";
    }
}
