package kz.spt.app.controller;

import kz.spt.app.service.GateService;
import kz.spt.lib.model.Gate;
import kz.spt.lib.model.dto.CarStateFilterDto;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.validation.Valid;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

@Controller
@RequestMapping("/journal")
public class CarStateController {

    private GateService gateService;

    public CarStateController(GateService gateService){
        this.gateService = gateService;
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
        model.addAttribute("allInGates", gateService.listGatesByType(Gate.GateType.IN));
        model.addAttribute("allOutGates", gateService.listGatesByType(Gate.GateType.OUT));
        return "journal/list";
    }

    @PostMapping("/list")
    public String processRequestSearch(Model model, @Valid @ModelAttribute("carStateFilterDto") CarStateFilterDto carStateFilterDto, BindingResult bindingResult) throws ParseException {
        if(carStateFilterDto != null){
            model.addAttribute("carStateFilterDto", carStateFilterDto);
        }
        model.addAttribute("allInGates", gateService.listGatesByType(Gate.GateType.IN));
        model.addAttribute("allOutGates", gateService.listGatesByType(Gate.GateType.OUT));
        return "journal/list";
    }
}
