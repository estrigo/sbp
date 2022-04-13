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

    @RequestMapping(value = "/type/create", method = RequestMethod.POST, consumes = "multipart/form-data")
    public JsonNode createAbonomentType(@RequestParam("period") int period,
                                        @RequestParam("customJson") String customJson,
                                        @RequestParam("abonementTypes") String type,
                                    @RequestParam("price") int price) throws Exception {
        return abonomentService.createAbonomentType(period, customJson, type, price);
    }

    @RequestMapping(value = "/type/delete", method = RequestMethod.POST, consumes = "multipart/form-data")
    public JsonNode deleteAbonomentType(@RequestParam("id") Long id) throws Exception {
        return abonomentService.deleteAbonomentType(id);
    }

    @RequestMapping(value = "/create", method = RequestMethod.POST, consumes = "multipart/form-data")
    public JsonNode createAbonoment(@RequestParam("platenumber") String platenumber,
                                    @RequestParam("parkingId") Long parkingId,
                                    @RequestParam("typeId") Long typeId,
                                    @RequestParam("dateStart") String dateStart,
                                    @RequestParam("checked") Boolean checked) throws Exception {
        return abonomentService.createAbonoment(platenumber, parkingId, typeId, dateStart, checked);
    }

    @RequestMapping(value = "/delete", method = RequestMethod.POST, consumes = "multipart/form-data")
    public JsonNode deleteAbonoment(@RequestParam("id") Long id) throws Exception {
        return abonomentService.deleteAbonoment(id);
    }
}
