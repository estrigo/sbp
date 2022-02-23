package kz.spt.reportplugin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/report")
@RequiredArgsConstructor
public class ReportController {
    @GetMapping("/journal")
    public String showAllBalances(Model model) {
        //model.addAttribute("whitelist", balanceService.listAllBalances());
        return "report/journal";
    }
}
