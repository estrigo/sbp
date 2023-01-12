package kz.spt.app.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.intelligt.modbus.jlibmodbus.exception.ModbusIOException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusNumberException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusProtocolException;
import kz.spt.lib.model.Parking;
import kz.spt.lib.model.dto.EmergencySignalConfigDto;
import kz.spt.lib.model.dto.ParkingDto;
import kz.spt.lib.service.EmergencySignalService;
import kz.spt.lib.service.ParkingService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.w3c.dom.stylesheets.LinkStyle;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

@RestController
@RequestMapping(value = "/rest/parking")
public class ParkingRestController {

    private ParkingService parkingService;

    private EmergencySignalService emergencySignalService;

    public ParkingRestController(ParkingService parkingService, EmergencySignalService emergencySignalService){
        this.parkingService = parkingService;
        this.emergencySignalService = emergencySignalService;
    }

    @GetMapping("/list")
    public List<ParkingDto> getParkingList(){
        return parkingService.getParkings();
    }

    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_OPERATOR')")
    @PostMapping(value = "/emergencyConfig/add")
    public void setPermanentClose(@RequestParam("ip") String ip, @RequestParam("modbusRegister") Integer modbusRegister, @RequestParam("defaultActiveSignal") Integer defaultActiveSignal) {
        emergencySignalService.save(ip, modbusRegister, defaultActiveSignal);
    }

    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_OPERATOR')")
    @PostMapping(value = "/emergencyConfig/delete")
    public void setPermanentClose(@RequestParam("ip") String ip) {
        emergencySignalService.remove(ip);
    }

    @GetMapping("/emergencyConfig")
    public EmergencySignalConfigDto getConfigured(){
        return emergencySignalService.getConfigured();
    }
}
