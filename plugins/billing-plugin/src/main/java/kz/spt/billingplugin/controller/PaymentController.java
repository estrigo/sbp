package kz.spt.billingplugin.controller;

import kz.spt.billingplugin.dto.FilterPaymentDTO;
import kz.spt.billingplugin.service.PaymentProviderService;
import kz.spt.billingplugin.service.PaymentService;
import kz.spt.lib.model.dto.CarStateFilterDto;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/billing/payments")
public class PaymentController {

    private PaymentService paymentService;
    private PaymentProviderService paymentProviderService;

    public PaymentController(PaymentService paymentService, PaymentProviderService paymentProviderService) {
        this.paymentService = paymentService;
        this.paymentProviderService = paymentProviderService;
    }

    @GetMapping("/list")
    public String showAllPayments(Model model) {
        model.addAttribute("paymentProviders", paymentProviderService.getSelectOption());
        return "/billing/payments/list";
    }
}