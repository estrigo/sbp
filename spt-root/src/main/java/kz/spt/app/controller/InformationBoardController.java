package kz.spt.app.controller;

import kz.spt.app.repository.GateRepository;
import kz.spt.app.rest.RateRestController;
import kz.spt.app.service.GateService;
import kz.spt.lib.model.CarState;
import kz.spt.lib.model.Gate;
import kz.spt.lib.model.Parking;
import kz.spt.lib.model.dto.CarStateDto;
import kz.spt.lib.model.dto.GateDto;
import kz.spt.lib.model.dto.ParkingDto;
import kz.spt.lib.service.CarStateService;
import kz.spt.lib.service.InformationBoardService;
import kz.spt.lib.service.ParkingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/infoboard")
@RequiredArgsConstructor
public class InformationBoardController {
    private final InformationBoardService informationBoardService;
    private final GateService gateService;
    private final ParkingService parkingService;
    private final CarStateService carStateService;

    @GetMapping("/list")
    public String showBoard(Model model){
        model.addAttribute("parkings", parkingService.listAllParking());
        return "infoboard/list";
    }

    @GetMapping("/list/{parkingId}")
    @ResponseBody
    public List<GateDto> getGates(@PathVariable Long parkingId) {
        return gateService.getGateByParkingId(parkingId);
    }

    @GetMapping("/carstate")
    @ResponseBody
    public CarStateDto getCarState(Model model, @RequestParam Long gateId) throws Exception {
        CarStateDto carState = carStateService.getCarState(gateId);
//        model.addAttribute("carState", carState);
//        return "redirect:/infoboard/list";
        return carState;
    }

}
