package kz.spt.billingplugin.controller;

import kz.spt.billingplugin.model.Payment;
import kz.spt.billingplugin.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/payments")
public class PaymentController {

    PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }


    @GetMapping("/list")
    public String showAllWhitelist(Model model) {

        return "billing/payments/list";
    }

    @PostMapping("/list")
    public ResponseEntity getData() {

        Iterable<Payment> paymentList = paymentService.listAllPayments();
        return new ResponseEntity(paymentList, HttpStatus.OK);

    }
}
