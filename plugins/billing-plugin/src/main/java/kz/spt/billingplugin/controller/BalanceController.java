package kz.spt.billingplugin.controller;


import kz.spt.billingplugin.service.BalanceService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/billing/balance")
public class BalanceController {

    private BalanceService balanceService;

    public BalanceController(BalanceService balanceService) {
        this.balanceService = balanceService;
    }

    @GetMapping("/list")
    public String showAllBalances(Model model) {
        model.addAttribute("whitelist", balanceService.listAllBalances());
        return "billing/balance/list";
    }
}
