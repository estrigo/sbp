package kz.spt.app.rest;

import kz.spt.lib.service.ArmService;
import lombok.SneakyThrows;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.text.ParseException;

@RestController
@RequestMapping(value = "/rest/arm")
public class ArmRestController {

    private final ArmService armService;

    public ArmRestController(ArmService armService){
        this.armService = armService;
    }

    @PreAuthorize("hasAnyRole('ROLE_SUPERADMIN', 'ROLE_ADMIN', 'ROLE_OPERATOR')")
    @GetMapping(value = "/open/{cameraId}")
    public Boolean openGate(@PathVariable("cameraId") Long cameraId) throws IOException, ParseException, InterruptedException {
        return armService.openGate(cameraId);
    }

    @GetMapping(value = "/close/{cameraId}")
    public Boolean closeGate(@PathVariable("cameraId") Long cameraId) throws IOException, ParseException, InterruptedException {
        return armService.closeGate(cameraId);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping(value = "/emergency/open/{value}")
    public Boolean emergencyOpen(@PathVariable("value") String value, @AuthenticationPrincipal UserDetails currentUser){
        return armService.setEmergencyOpen(Boolean.valueOf(value), currentUser);
    }

    @GetMapping(value = "/emergency/status")
    public Boolean emergencyOpen(){
        return armService.getEmergencyStatus();
    }

    @RequestMapping(value = "/pass", method = RequestMethod.POST, consumes = "multipart/form-data")
    public Boolean passCar(@RequestParam("cameraId") Long cameraId,
                           @RequestParam("platenumber") String platenumber,
                           @RequestParam("snapshot") String snapshot) throws Exception {
        return armService.passCar(cameraId, platenumber, snapshot);
    }

    @SneakyThrows
    @GetMapping(value = "/snapshot/{cameraId}")
    public byte[] snapshot(@PathVariable("cameraId") Long cameraId,@RequestParam("ver") String ver){
        return armService.snapshot(cameraId);
    }

    @SneakyThrows
    @GetMapping(value = "/enable/{cameraId}")
    public void enableSnapshot(@PathVariable("cameraId") Long cameraId,@RequestParam("ver") String ver){
        armService.enableSnapshot(cameraId);
    }

    @SneakyThrows
    @GetMapping(value = "/disable")
    public void disableSnapshot(){
        armService.disableSnapshot();
    }

    @GetMapping("/restart/{ip}")
    public Boolean restartParkomat(@PathVariable("ip") String ip){
        return armService.restartParkomat(ip);
    }
}
