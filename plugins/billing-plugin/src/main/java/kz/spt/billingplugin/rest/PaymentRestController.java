package kz.spt.billingplugin.rest;

import kz.spt.billingplugin.dto.FilterPaymentDTO;
import kz.spt.billingplugin.dto.PaymentLogDTO;
import kz.spt.billingplugin.service.PaymentService;
import kz.spt.lib.bootstrap.datatable.Page;
import kz.spt.lib.bootstrap.datatable.PagingRequest;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.text.ParseException;
import java.time.LocalDateTime;

@RestController
@RequestMapping(value = "/rest/payments")
public class PaymentRestController {

    private PaymentService paymentService;

    public PaymentRestController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public Page<PaymentLogDTO> list(@RequestBody PagingRequest pagingRequest) throws ParseException {
        return paymentService.getPaymentDtoList(pagingRequest);
    }
}
