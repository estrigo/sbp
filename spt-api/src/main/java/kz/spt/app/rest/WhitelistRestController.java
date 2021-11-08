package kz.spt.app.rest;

import com.fasterxml.jackson.databind.node.ArrayNode;
import kz.spt.lib.model.dto.ParkingDto;
import kz.spt.lib.service.PluginService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/rest/whitelist")
public class WhitelistRestController {

    private PluginService pluginService;

    public WhitelistRestController(PluginService pluginService){
        this.pluginService = pluginService;
    }

    @GetMapping("/parking/id/platenumber/{platenumber}")
    public ArrayNode getByPlatenumber(@PathVariable("id") Long parkingId, @PathVariable("platenumber") String platenumber) throws Exception {
        return pluginService.getWhitelist(parkingId, platenumber);
    }
}
