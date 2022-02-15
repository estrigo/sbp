package kz.spt.app.rest;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import kz.spt.lib.service.AbonomentService;

@RestController
@RequestMapping(value = "/rest/abonoment")
public class AbonomentRestController {

    private final AbonomentService abonomentService;

    public AbonomentRestController(AbonomentService abonomentService){
        this.abonomentService = abonomentService;
    }

    @RequestMapping(value = "/create", method = RequestMethod.POST, consumes = "multipart/form-data")
    public JsonNode createAbonoment(@RequestParam("period") int period,
                                    @RequestParam("price") int price) throws Exception {
        return abonomentService.createAbonomentType(period, price);
    }

    @RequestMapping(value = "/delete", method = RequestMethod.POST, consumes = "multipart/form-data")
    public JsonNode deleteAbonoment(@RequestParam("id") Long id) throws Exception {
        return abonomentService.deleteAbonomentType(id);
    }
}
