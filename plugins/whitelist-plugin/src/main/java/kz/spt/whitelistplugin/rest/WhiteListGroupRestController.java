package kz.spt.whitelistplugin.rest;

import kz.spt.lib.bootstrap.datatable.Page;
import kz.spt.lib.bootstrap.datatable.PagingRequest;
import kz.spt.whitelistplugin.service.WhitelistGroupsService;
import kz.spt.whitelistplugin.service.WhitelistService;
import kz.spt.whitelistplugin.viewmodel.WhiteListDto;
import kz.spt.whitelistplugin.viewmodel.WhiteListGroupDto;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.util.List;

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

    @PostMapping("/by/parking")
    public List<WhiteListGroupDto> list(@RequestParam("parkingId") Long parkingId) throws ParseException {
        return whitelistGroupsService.listByParkingId(parkingId);
    }
}
