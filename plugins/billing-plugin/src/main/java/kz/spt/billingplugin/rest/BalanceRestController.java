package kz.spt.billingplugin.rest;

import kz.spt.billingplugin.dto.TransactionDto;
import kz.spt.billingplugin.dto.TransactionFilterDto;
import kz.spt.billingplugin.model.Balance;
import kz.spt.billingplugin.service.BalanceService;
import kz.spt.lib.bootstrap.datatable.Page;
import kz.spt.lib.bootstrap.datatable.PagingRequest;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/transactions")
    public Page<TransactionDto> transactions(@RequestBody PagingRequest pagingRequest,
                                             @RequestParam String dateFromString,
                                             @RequestParam String dateToString,
                                             @RequestParam String plateNumber,
                                             @RequestParam Integer amount) throws ParseException {
        TransactionFilterDto dto = new TransactionFilterDto();
        dto.fromDate = dateFromString;
        dto.toDate = dateToString;
        dto.amount = amount;
        dto.plateNumber = plateNumber;
        return balanceService.getTransactionList(pagingRequest, dto);
    }
}
