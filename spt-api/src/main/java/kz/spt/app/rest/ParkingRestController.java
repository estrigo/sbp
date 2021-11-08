package kz.spt.app.rest;

import kz.spt.lib.model.Parking;
import kz.spt.lib.model.dto.ParkingDto;
import kz.spt.lib.service.ParkingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.stylesheets.LinkStyle;

import java.util.List;

@RestController
@RequestMapping(value = "/rest/parking")
public class ParkingRestController {

    private ParkingService parkingService;

    public ParkingRestController(ParkingService parkingService){
        this.parkingService = parkingService;
    }

    @GetMapping("/list")
    public List<ParkingDto> getParkingList(){
        return parkingService.getParkings();
    }
}
