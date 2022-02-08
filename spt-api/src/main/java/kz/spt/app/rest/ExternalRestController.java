package kz.spt.app.rest;

import kz.spt.lib.service.ArmService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.text.ParseException;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/rest/external")
public class ExternalRestController {
    private final ArmService armService;

    @PreAuthorize("hasAnyRole('ROLE_SUPERADMIN', 'ROLE_ADMIN', 'ROLE_OPERATOR')")
    @GetMapping(value = "/open/{cameraId}")
    public Boolean openGate(@PathVariable("cameraId") Long cameraId) throws IOException, ParseException, InterruptedException {
        return armService.openGate(cameraId);
    }

    @PreAuthorize("hasAnyRole('ROLE_SUPERADMIN', 'ROLE_ADMIN', 'ROLE_OPERATOR')")
    @GetMapping(value = "/close/{cameraId}")
    public Boolean closeGate(@PathVariable("cameraId") Long cameraId) throws IOException, ParseException, InterruptedException {
        return armService.closeGate(cameraId);
    }
}
