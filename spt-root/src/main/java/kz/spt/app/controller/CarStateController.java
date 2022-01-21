package kz.spt.app.controller;

import kz.spt.app.service.BlacklistService;
import kz.spt.app.service.GateService;
import kz.spt.lib.model.Camera;
import kz.spt.lib.model.CarState;
import kz.spt.lib.model.Gate;
import kz.spt.lib.model.dto.BlacklistDto;
import kz.spt.lib.model.dto.CarStateFilterDto;
import kz.spt.lib.service.CarStateService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Controller
@RequestMapping("/journal")
public class CarStateController {

    private GateService gateService;
    private CarStateService carStateService;
    private BlacklistService blacklistService;

    public CarStateController(GateService gateService, CarStateService carStateService, BlacklistService blacklistService) {
        this.gateService = gateService;
        this.carStateService = carStateService;
        this.blacklistService = blacklistService;
    }

    private final String dateformat = "yyyy-MM-dd'T'HH:mm";

    @GetMapping("/list")
    public String showAllCarStates(Model model, @AuthenticationPrincipal UserDetails currentUser) {
        CarStateFilterDto carStateFilterDto = null;
        if (!model.containsAttribute("carStateFilterDto")) {
            SimpleDateFormat format = new SimpleDateFormat(dateformat);

            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MINUTE, 1);
            Date dateTo = calendar.getTime();

            calendar.add(Calendar.MONTH, -1);
            Date dateFrom = calendar.getTime();

            model.addAttribute("carStateFilterDto", CarStateFilterDto.builder()
                    .dateFromString(format.format(dateFrom))
                    .dateToString(format.format(dateTo))
                    .build());
        }

        List<Gate> allReverseGates = (List<Gate>) gateService.listGatesByType(Gate.GateType.REVERSE);
        List<Gate> allInGates = (List<Gate>) gateService.listGatesByType(Gate.GateType.IN);
        List<Gate> allOutGates = (List<Gate>) gateService.listGatesByType(Gate.GateType.OUT);
        if (allReverseGates.size() > 0) {
            allInGates.addAll(allReverseGates);
            allOutGates.addAll(allReverseGates);
        }
        model.addAttribute("allInGates", allInGates);
        model.addAttribute("allOutGates", allOutGates);
        model.addAttribute("canEdit", currentUser.getAuthorities().stream().anyMatch(m-> Arrays.asList("ROLE_SUPERADMIN","ROLE_ADMIN","ROLE_MANAGER").contains(m.getAuthority())));
        model.addAttribute("canRemove", currentUser.getAuthorities().stream().anyMatch(m-> Arrays.asList("ROLE_OPERATOR_NO_REVENUE_SHARE").contains(m.getAuthority())));
        model.addAttribute("canKick", currentUser.getAuthorities().stream().anyMatch(m-> Arrays.asList("ROLE_MANAGER","ROLE_OPERATOR").contains(m.getAuthority())));

        return "journal/list";
    }

    @PostMapping("/list")
    public String processRequestSearch(Model model, @Valid @ModelAttribute("carStateFilterDto") CarStateFilterDto carStateFilterDto, @AuthenticationPrincipal UserDetails currentUser) throws ParseException {
        if (carStateFilterDto != null) {
            model.addAttribute("carStateFilterDto", carStateFilterDto);
        }
        List<Gate> allReverseGates = (List<Gate>) gateService.listGatesByType(Gate.GateType.REVERSE);
        List<Gate> allInGates = (List<Gate>) gateService.listGatesByType(Gate.GateType.IN);
        List<Gate> allOutGates = (List<Gate>) gateService.listGatesByType(Gate.GateType.OUT);
        if (allReverseGates.size() > 0) {
            allInGates.addAll(allReverseGates);
            allOutGates.addAll(allReverseGates);
        }
        model.addAttribute("allInGates", allInGates);
        model.addAttribute("allOutGates", allOutGates);
        model.addAttribute("canEdit", currentUser.getAuthorities().stream().anyMatch(m-> Arrays.asList("ROLE_SUPERADMIN","ROLE_ADMIN","ROLE_MANAGER").contains(m.getAuthority())));
        model.addAttribute("canRemove", currentUser.getAuthorities().stream().anyMatch(m-> Arrays.asList("ROLE_OPERATOR_NO_REVENUE_SHARE").contains(m.getAuthority())));
        model.addAttribute("canKick", currentUser.getAuthorities().stream().anyMatch(m-> Arrays.asList("ROLE_MANAGER","ROLE_OPERATOR").contains(m.getAuthority())));

        return "journal/list";
    }

    @GetMapping("/out/{carNumber}")
    public String manualOut(@PathVariable String carNumber){
        CarState carState = carStateService.getLastNotLeft(carNumber);
        carStateService.createOUTManual(carNumber, new Date(),carState);
        return "redirect:/journal/list";
    }

    @PostMapping("/blacklist")
    public String addBlacklist(@ModelAttribute BlacklistDto model){
        CarState carState = carStateService.getLastNotLeft(model.getPlateNumber());
        carStateService.createOUTManual(model.getPlateNumber(), new Date(),carState);
        blacklistService.save(model);
        return "redirect:/journal/list";
    }

    @PostMapping("/edit-plate")
    public String editPlateNumber(@ModelAttribute CarState carState){
        carStateService.editPlateNumber(carState);
        return "redirect:/journal/list";
    }
}
