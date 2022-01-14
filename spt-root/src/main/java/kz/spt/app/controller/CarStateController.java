package kz.spt.app.controller;

import kz.spt.app.service.GateService;
import kz.spt.lib.model.Camera;
import kz.spt.lib.model.CarState;
import kz.spt.lib.model.Gate;
import kz.spt.lib.model.dto.CarStateFilterDto;
import kz.spt.lib.service.CarStateService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Controller
@RequestMapping("/journal")
public class CarStateController {

    private GateService gateService;
    private CarStateService carStateService;

    public CarStateController(GateService gateService, CarStateService carStateService){
        this.gateService = gateService;
        this.carStateService = carStateService;
    }

    private final String dateformat = "yyyy-MM-dd'T'HH:mm";

    @GetMapping("/list")
    public String showAllCarStates(Model model) {
        CarStateFilterDto carStateFilterDto = null;
        if(!model.containsAttribute("carStateFilterDto")){
            SimpleDateFormat format = new SimpleDateFormat(dateformat);
            carStateFilterDto = new CarStateFilterDto();

            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MINUTE, 1);
            Date dateTo = calendar.getTime();
            carStateFilterDto.dateToString = format.format(dateTo);

            calendar.add(Calendar.MONTH, -1);
            Date dateFrom = calendar.getTime();
            carStateFilterDto.dateFromString = format.format(dateFrom);
            model.addAttribute("carStateFilterDto", carStateFilterDto);
        }

        List<Gate> allReverseGates = (List<Gate>) gateService.listGatesByType(Gate.GateType.REVERSE);
        List<Gate> allInGates = (List<Gate>) gateService.listGatesByType(Gate.GateType.IN);
        List<Gate> allOutGates = (List<Gate>) gateService.listGatesByType(Gate.GateType.OUT);
        if(allReverseGates.size() > 0){
            allInGates.addAll(allReverseGates);
            allOutGates.addAll(allReverseGates);
        }
        model.addAttribute("allInGates", allInGates);
        model.addAttribute("allOutGates", allOutGates);
        return "journal/list";
    }

    @PostMapping("/list")
    public String processRequestSearch(Model model, @Valid @ModelAttribute("carStateFilterDto") CarStateFilterDto carStateFilterDto, BindingResult bindingResult) throws ParseException {
        if(carStateFilterDto != null){
            model.addAttribute("carStateFilterDto", carStateFilterDto);
        }
        List<Gate> allReverseGates = (List<Gate>) gateService.listGatesByType(Gate.GateType.REVERSE);
        List<Gate> allInGates = (List<Gate>) gateService.listGatesByType(Gate.GateType.IN);
        List<Gate> allOutGates = (List<Gate>) gateService.listGatesByType(Gate.GateType.OUT);
        if(allReverseGates.size() > 0){
            allInGates.addAll(allReverseGates);
            allOutGates.addAll(allReverseGates);
        }
        model.addAttribute("allInGates", allInGates);
        model.addAttribute("allOutGates", allOutGates);
        return "journal/list";
    }

    @GetMapping("/remove/debt")
    public String getRemoveDebt() {
        return "/journal/remove/debt";
    }
}
