package kz.spt.app.rest;

import kz.spt.lib.service.PluginService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping(value = "/rest/balance")
public class BalanceRestController {

    private PluginService pluginService;

    public BalanceRestController(PluginService pluginService){
        this.pluginService = pluginService;
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
