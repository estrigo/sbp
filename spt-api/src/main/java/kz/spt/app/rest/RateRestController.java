package kz.spt.app.rest;

import kz.spt.lib.model.dto.RateQueryDto;
import kz.spt.lib.service.PaymentService;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.math.BigDecimal;

@RestController
@RequestMapping(value = "/rest/rate")
public class RateRestController {

    private PaymentService paymentService;

    public RateRestController(PaymentService paymentService){
        this.paymentService = paymentService;
    }

    @PostMapping(value = "/get/value")
    public BigDecimal getRateValue(@Valid @RequestBody RateQueryDto rateQueryDto) throws Exception {
        return paymentService.getRateValue(rateQueryDto);
    }
}
