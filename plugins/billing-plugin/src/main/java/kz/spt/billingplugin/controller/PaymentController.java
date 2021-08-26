package kz.spt.billingplugin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/payments")
public class PaymentController {

    @GetMapping("/list")
    public String showAllWhitelist(Model model) {

        return "billing/payments/list";
    }
}
