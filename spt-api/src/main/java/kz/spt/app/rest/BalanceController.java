package kz.spt.app.rest;

import kz.spt.lib.service.PluginService;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping(value = "/rest/balance")
public class BalanceController {

    private PluginService pluginService;

    public BalanceController(PluginService pluginService){
        this.pluginService = pluginService;
    }

    @GetMapping("/check/{platenumber}")
    public BigDecimal checkBalancePlatenumber(@PathVariable String platenumber) throws Exception {
        return pluginService.checkBalance(platenumber);
    }
}
