package kz.spt.billingplugin.controller;

import kz.spt.billingplugin.dto.PaymentLogDTO;
import kz.spt.billingplugin.dto.TableDataDTO;
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
import java.util.stream.Collectors;

@Controller
@RequestMapping("/billing/payments")
public class PaymentController {

    PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/list")
    public String showAllWhitelist(Model model) {
        return "/billing/payments/list";
    }

    @PostMapping("/list")
    public ResponseEntity getData() {

        List<Payment> paymentList = (List<Payment>) paymentService.listAllPayments();
        TableDataDTO tableDataDTO = new TableDataDTO();
        tableDataDTO.setDraw(1);
        tableDataDTO.setRecordsTotal(10);
        tableDataDTO.setRecordsFiltered(1);
        tableDataDTO.setData(paymentList.stream().map(payment -> PaymentLogDTO.convertToDto(payment)).collect(Collectors.toList()));
        return new ResponseEntity(tableDataDTO, HttpStatus.OK);

    }
}