package kz.spt.app.rest;

import kz.spt.lib.bootstrap.datatable.Page;
import kz.spt.lib.bootstrap.datatable.PagingRequest;
import kz.spt.lib.model.dto.BlacklistDto;
import kz.spt.lib.service.BlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/rest/blacklist")
public class BlacklistRestController {

    private BlacklistService blacklistService;

    public BlacklistRestController(BlacklistService blacklistService){
        this.blacklistService = blacklistService;
    }

    @PostMapping
    public Page<BlacklistDto> list(@RequestBody PagingRequest pagingRequest) {
        return blacklistService.getAll(pagingRequest);
    }
}
