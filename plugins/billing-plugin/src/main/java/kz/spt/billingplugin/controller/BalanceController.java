package kz.spt.billingplugin.controller;


import kz.spt.billingplugin.service.BalanceService;
import kz.spt.lib.utils.StaticValues;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

@Controller
@RequestMapping("/billing/balance")
public class BalanceController {

    private BalanceService balanceService;

    public BalanceController(BalanceService balanceService) {
        this.balanceService = balanceService;
    }

    @GetMapping("/list")
    public String showAllBalances(Model model) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
        model.addAttribute("whitelist", balanceService.listAllBalances());
        Calendar calendar = Calendar.getInstance();
        model.addAttribute("currentDate", format.format(calendar.getTime()));
        calendar.add(Calendar.MINUTE, 1);
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        model.addAttribute("oneDayEarly", format.format(calendar.getTime()));
        return "billing/balance/list";
    }
}
