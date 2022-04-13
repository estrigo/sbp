package kz.spt.whitelistplugin.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import kz.spt.lib.bootstrap.datatable.Page;
import kz.spt.lib.bootstrap.datatable.PagingRequest;
import kz.spt.lib.model.dto.CarStateFilterDto;
import kz.spt.whitelistplugin.model.Whitelist;
import kz.spt.whitelistplugin.service.WhitelistService;
import kz.spt.whitelistplugin.viewmodel.WhiteListDto;
import lombok.extern.java.Log;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

@Log
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

    @GetMapping("/groupList/excel")
    public List<WhiteListDto> groupList(@RequestParam("groupName") String groupName) throws JsonProcessingException {
        return whitelistService.groupWhitelistForExcel(groupName);
    }


}
