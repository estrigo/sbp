package kz.spt.app.controller;

import kz.spt.app.service.GateService;
import kz.spt.lib.model.dto.GateDto;
import kz.spt.lib.model.dto.temp.CarStateCurrencyDto;
import kz.spt.lib.service.CarStateService;
import kz.spt.lib.service.ParkingService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/infoboard")
@RequiredArgsConstructor
public class InformationBoardController {
    private final GateService gateService;
    private final ParkingService parkingService;
    private final CarStateService carStateService;

    @SneakyThrows
    @GetMapping("/list")
    public String showBoard(Model model){
        model.addAttribute("parkings", parkingService.getParkings());
        return "infoboard/list";
    }

    @GetMapping("/list/{parkingId}")
    @ResponseBody
    public List<GateDto> getGates(@PathVariable Long parkingId) {
        return gateService.getGateByParkingId(parkingId);
    }

    @GetMapping("/carstate")
    @ResponseBody
    public CarStateCurrencyDto getCarState(@RequestParam Long gateId) throws Exception {
        return carStateService.getCarState(gateId);
    }

}
