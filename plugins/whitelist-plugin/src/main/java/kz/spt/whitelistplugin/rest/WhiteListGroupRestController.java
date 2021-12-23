package kz.spt.whitelistplugin.rest;

import kz.spt.lib.bootstrap.datatable.Page;
import kz.spt.lib.bootstrap.datatable.PagingRequest;
import kz.spt.whitelistplugin.service.WhitelistGroupsService;
import kz.spt.whitelistplugin.service.WhitelistService;
import kz.spt.whitelistplugin.viewmodel.WhiteListDto;
import kz.spt.whitelistplugin.viewmodel.WhiteListGroupDto;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;

@RestController
@RequestMapping(value = "/rest/whitelist/group")
public class WhiteListGroupRestController {
    private WhitelistGroupsService whitelistGroupsService;

    public WhiteListGroupRestController(WhitelistGroupsService whitelistGroupsService){
        this.whitelistGroupsService = whitelistGroupsService;
    }

    @PostMapping
    public Page<WhiteListGroupDto> list(@RequestBody PagingRequest pagingRequest) throws ParseException {
        return whitelistGroupsService.listByPage(pagingRequest);
    }
}
