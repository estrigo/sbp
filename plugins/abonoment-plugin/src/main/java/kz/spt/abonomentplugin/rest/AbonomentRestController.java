package kz.spt.abonomentplugin.rest;

import kz.spt.abonomentplugin.dto.AbonomentDTO;
import kz.spt.abonomentplugin.dto.AbonomentTypeDTO;
import kz.spt.abonomentplugin.service.AbonomentPluginService;
import kz.spt.lib.bootstrap.datatable.Page;
import kz.spt.lib.bootstrap.datatable.PagingRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;

@RestController
@RequestMapping(value = "/rest/abonoment/internal")
public class AbonomentRestController {

    private final AbonomentPluginService abonomentPluginService;

    public AbonomentRestController(AbonomentPluginService abonomentPluginService){
        this.abonomentPluginService = abonomentPluginService;
    }

    @PostMapping("/type/list")
    public Page<AbonomentTypeDTO> typeList(@RequestBody PagingRequest pagingRequest) throws ParseException {
        return abonomentPluginService.abonomentTypeDtoList(pagingRequest);
    }

    @PostMapping("/list")
    public Page<AbonomentDTO> list(@RequestBody PagingRequest pagingRequest) throws ParseException {
        return abonomentPluginService.abonomentDtoList(pagingRequest);
    }
}
