package kz.spt.app.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.intelligt.modbus.jlibmodbus.exception.ModbusIOException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusNumberException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusProtocolException;
import kz.spt.lib.service.ArmService;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.text.ParseException;

@Log
@RestController
@RequestMapping(value = "/rest/arm")
public class ArmRestController {

    private final ArmService armService;

    public ArmRestController(ArmService armService){
        this.armService = armService;
    }

    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_OPERATOR')")
    @RequestMapping(value = "/open/barrier", method = RequestMethod.POST, consumes = "multipart/form-data")
    public Boolean openGateBarrier(@RequestParam("cameraId") Long cameraId,
                                   @RequestParam("reason") String reason,
                                   @RequestParam("snapshot") String snapshot) throws Exception {
        return armService.openGate(cameraId, snapshot, reason);
    }

    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_OPERATOR')")
    @GetMapping(value = "/open/{cameraId}")
    public Boolean openGate(@PathVariable("cameraId") Long cameraId) throws IOException, ParseException, InterruptedException, ModbusProtocolException, ModbusNumberException, ModbusIOException {
        return armService.openGate(cameraId);
    }

    @GetMapping(value = "/close/{cameraId}")
    public Boolean closeGate(@PathVariable("cameraId") Long cameraId) throws IOException, ParseException, InterruptedException, ModbusProtocolException, ModbusNumberException, ModbusIOException {
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

    @GetMapping("/restart/{ip}")
    public Boolean restartParkomat(@PathVariable("ip") String ip){
        return armService.restartParkomat(ip);
    }

    @GetMapping("/tab/camera")
    public JsonNode getTabsWithCameraList(){
        return armService.getTabsWithCameraList();
    }

    @RequestMapping(value = "/save/tab/camera", method = RequestMethod.POST, consumes = "multipart/form-data")
    public Boolean saveTabs(@RequestParam("json") String json) throws Exception {
        log.info(json);
        return armService.configureArm(json);
    }
}