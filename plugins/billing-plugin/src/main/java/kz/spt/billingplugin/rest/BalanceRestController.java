package kz.spt.billingplugin.rest;

import kz.spt.billingplugin.model.Balance;
import kz.spt.billingplugin.service.BalanceService;
import kz.spt.lib.bootstrap.datatable.Page;
import kz.spt.lib.bootstrap.datatable.PagingRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;

@RestController
@RequestMapping(value = "/rest/balances")
public class BalanceRestController {

    private BalanceService balanceService;

    public BalanceRestController(BalanceService balanceService)
    {
        this.balanceService = balanceService;

    }

    @PostMapping
    public Page<Balance> list(@RequestBody PagingRequest pagingRequest) throws ParseException {
        return balanceService.getBalanceList(pagingRequest);
    }
}
