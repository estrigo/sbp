package kz.spt.whitelistplugin.rest;

import kz.spt.lib.bootstrap.datatable.Page;
import kz.spt.lib.bootstrap.datatable.PagingRequest;
import kz.spt.whitelistplugin.service.WhitelistService;
import kz.spt.whitelistplugin.viewmodel.WhiteListDto;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;

@RestController
@RequestMapping(value = "/rest/whitelist")
public class WhiteListRestController {
    private WhitelistService whitelistService;

    public WhiteListRestController(WhitelistService whitelistService){
        this.whitelistService = whitelistService;
    }

    @PostMapping
    public Page<WhiteListDto> list(@RequestBody PagingRequest pagingRequest) throws ParseException {
        return whitelistService.listByPage(pagingRequest);
    }
}
