package kz.spt.app.rest;

import kz.spt.lib.service.ArmService;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.text.ParseException;

@RestController
@RequestMapping(value = "/rest/arm")
public class ArmRestController {

    private ArmService armService;

    public ArmRestController(ArmService armService){
        this.armService = armService;
    }

    @GetMapping(value = "/open/{cameraId}")
    public Boolean openGate(@PathVariable("cameraId") Long cameraId) throws IOException, ParseException, InterruptedException {
        return armService.openGate(cameraId);
    }

    @GetMapping(value = "/close/{cameraId}")
    public Boolean closeGate(@PathVariable("cameraId") Long cameraId) throws IOException, ParseException, InterruptedException {
        return armService.closeGate(cameraId);
    }
}
