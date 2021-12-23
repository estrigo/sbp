package kz.spt.app.rest;

import kz.spt.lib.service.ArmService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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

    @PreAuthorize("hasAnyRole('ROLE_SUPERADMIN', 'ROLE_ADMIN')")
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
}
