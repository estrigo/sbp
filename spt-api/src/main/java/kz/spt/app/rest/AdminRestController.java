package kz.spt.app.rest;

import kz.spt.lib.service.GitInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping(value = "/rest/admin")
@RequiredArgsConstructor
public class AdminRestController {

    private final GitInfoService gitInfoService;

    @RequestMapping(value = "/gitinfo", method = RequestMethod.GET)
    @ResponseBody
    public Object gitInfo(){
        return gitInfoService.gitInfo();
    }
}
