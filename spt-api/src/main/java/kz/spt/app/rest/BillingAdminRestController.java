package kz.spt.app.rest;

import kz.spt.lib.model.dto.payment.CommandDto;
import kz.spt.lib.service.PaymentService;
import org.springframework.data.annotation.AccessType;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping(value = "/admin")
public class BillingAdminRestController {

    private PaymentService paymentService;

    public BillingAdminRestController(PaymentService paymentService){
        this.paymentService = paymentService;
    }

    @RequestMapping(value = "/billing", method = RequestMethod.POST, consumes = "application/json;charset=UTF-8", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public Object billingInteraction(@Valid @RequestBody CommandDto commandDto) throws Exception{
        return paymentService.billingInteractions(commandDto);
    }
}