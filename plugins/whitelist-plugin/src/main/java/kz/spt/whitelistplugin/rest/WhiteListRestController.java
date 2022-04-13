package kz.spt.whitelistplugin.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import kz.spt.lib.bootstrap.datatable.Page;
import kz.spt.lib.bootstrap.datatable.PagingRequest;
import kz.spt.whitelistplugin.service.WhitelistService;
import kz.spt.whitelistplugin.viewmodel.WhiteListDto;
import lombok.extern.java.Log;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.util.List;

@RestController
@Log
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

    @GetMapping("/list/excel")
    public List<WhiteListDto> listAll() throws ParseException, JsonProcessingException {
        return whitelistService.listAllWhitelistForExcel();
    }

    @GetMapping("/group/excel")
    public List<WhiteListDto> listByGroup(@RequestParam String groupName) throws JsonProcessingException {
        log.info("GroupName param: " + groupName);
        return whitelistService.listByGroupName(groupName);
    }
}
