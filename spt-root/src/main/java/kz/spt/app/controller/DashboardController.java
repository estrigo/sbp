package kz.spt.app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    @GetMapping("/count")
    public String count() {
        return "dashboard/count";
    }

    @GetMapping("/payment")
    public String payment() {
        return "dashboard/payment";
    }
}
