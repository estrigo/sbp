package kz.spt.abonomentplugin.controller;

import kz.spt.abonomentplugin.dto.AbonomentTypeDTO;
import kz.spt.abonomentplugin.service.AbonomentPluginService;
import kz.spt.lib.bootstrap.datatable.Page;
import kz.spt.lib.bootstrap.datatable.PagingRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.text.ParseException;

@Controller
@RequestMapping("/abonoment")
public class AbonomentController {

    private final AbonomentPluginService abonomentPluginService;

    public AbonomentController(AbonomentPluginService abonomentPluginService){
        this.abonomentPluginService = abonomentPluginService;
    }

    @GetMapping("/list")
    public String showList(Model model) {
        return "list";
    }

    @PostMapping("/type/list")
    public Page<AbonomentTypeDTO> list(@RequestBody PagingRequest pagingRequest) throws ParseException {
        return abonomentPluginService.abonomentTypeDtoList(pagingRequest);
    }
}
