package kz.spt.app.rest;

import kz.spt.lib.model.dto.payment.CommandDto;
import kz.spt.lib.service.PaymentService;
import kz.spt.lib.service.PluginService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.math.BigDecimal;

@RestController
@RequestMapping(value = "/rest/balance")
public class BalanceRestController {

    private PluginService pluginService;

    private PaymentService paymentService;

    public BalanceRestController(PluginService pluginService, PaymentService paymentService){
        this.pluginService = pluginService;
        this.paymentService = paymentService;
    }

    @RequestMapping(value = "/billing", method = RequestMethod.POST, consumes = "application/json;charset=UTF-8", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public Object billingInteraction(@Valid @RequestBody CommandDto commandDto) throws Exception{
        return paymentService.billingInteractions(commandDto);
    }

    @GetMapping("/check/{platenumber}")
    public BigDecimal checkBalancePlatenumber(@PathVariable String platenumber) throws Exception {
        return pluginService.checkBalance(platenumber);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value = "/change", method = RequestMethod.POST, consumes = "multipart/form-data")
    public BigDecimal openGateBarrier(@RequestParam("plateNumber") String plateNumber,
                                   @RequestParam("value") BigDecimal value) throws Exception {
        return pluginService.changeBalance(plateNumber, value);
    }

}
