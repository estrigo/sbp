package kz.spt.abonomentplugin.rest;

import kz.spt.abonomentplugin.dto.AbonomentDTO;
import kz.spt.abonomentplugin.dto.AbonomentTypeDTO;
import kz.spt.abonomentplugin.model.AbonomentTypes;
import kz.spt.abonomentplugin.model.dto.AbonementFilterDto;
import kz.spt.abonomentplugin.service.AbonomentPluginService;
import kz.spt.lib.bootstrap.datatable.Page;
import kz.spt.lib.bootstrap.datatable.PagingRequest;
import kz.spt.lib.model.dto.CarStateFilterDto;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;
import java.util.List;

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

    @PostMapping("/types")
    public List<AbonomentTypes> types() throws ParseException {
        return abonomentPluginService.getAllAbonomentTypes();
    }

    @PostMapping("/list")
    public Page<AbonomentDTO> list(@RequestBody PagingRequest pagingRequest) throws ParseException {
        AbonementFilterDto filter = pagingRequest.convertTo(AbonementFilterDto.builder().build());
        return abonomentPluginService.abonomentDtoList(pagingRequest, filter);
    }
}
