package kz.spt.whitelistplugin.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import kz.spt.lib.bootstrap.datatable.Page;
import kz.spt.lib.bootstrap.datatable.PagingRequest;
import kz.spt.whitelistplugin.service.WhitelistService;
import kz.spt.whitelistplugin.viewmodel.WhiteListDto;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.util.List;

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

    @GetMapping("/list/excel")
    public List<WhiteListDto> listAll() throws ParseException, JsonProcessingException {
        return whitelistService.listAllWhitelistForExcel();
    }
}
