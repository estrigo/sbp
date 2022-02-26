package kz.spt.app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/balance")
public class BalanceController {

    @GetMapping("/operation")
    public String getOperations() {
        return "balance/operation";
    }
}
