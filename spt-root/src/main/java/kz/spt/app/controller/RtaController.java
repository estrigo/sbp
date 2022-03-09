package kz.spt.app.controller;

import kz.spt.app.service.GateService;
import kz.spt.lib.model.Gate;
import kz.spt.lib.model.dto.CarStateFilterDto;
import kz.spt.lib.service.CarStateService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.validation.Valid;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Controller
@RequestMapping("/rta")
public class RtaController {

    private GateService gateService;
    private CarStateService carStateService;

    private final String dateformat = "yyyy-MM-dd'T'HH:mm";

    public RtaController(GateService gateService, CarStateService carStateService){
        this.gateService = gateService;
        this.carStateService = carStateService;
    }

    @GetMapping("/journal")
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
        model.addAttribute("canEdit", currentUser.getAuthorities().stream().anyMatch(m-> Arrays.asList("ROLE_SUPERADMIN","ROLE_ADMIN").contains(m.getAuthority())));
        model.addAttribute("canRemove", currentUser.getAuthorities().stream().anyMatch(m-> Arrays.asList("ROLE_OPERATOR_NO_REVENUE_SHARE").contains(m.getAuthority())));
        model.addAttribute("canKick", currentUser.getAuthorities().stream().anyMatch(m-> Arrays.asList("ROLE_OPERATOR").contains(m.getAuthority())));

        return "rta/journal";
    }

    @PostMapping("/journal")
    public String processRequestSearch(Model model, @Valid @ModelAttribute("carStateFilterDto") CarStateFilterDto carStateFilterDto, @AuthenticationPrincipal UserDetails currentUser) throws ParseException {
        if (carStateFilterDto != null) {
            model.addAttribute("carStateFilterDto", carStateFilterDto);
        }
        List<Gate> allReverseGates = (List<Gate>) gateService.listGatesByType(Gate.GateType.REVERSE);
        List<Gate> allInGates = (List<Gate>) gateService.listGatesByType(Gate.GateType.IN);
        if (allReverseGates.size() > 0) {
            allInGates.addAll(allReverseGates);
        }
        model.addAttribute("allInGates", allInGates);
        return "rta/journal";
    }

    @GetMapping("/entries")
    public String showAllEntriesCarStates(Model model, @AuthenticationPrincipal UserDetails currentUser) {
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
        if (allReverseGates.size() > 0) {
            allInGates.addAll(allReverseGates);
        }
        model.addAttribute("allInGates", allInGates);
        return "rta/entries";
    }

    @PostMapping("/entries")
    public String processRequestEntriesSearch(Model model, @Valid @ModelAttribute("carStateFilterDto") CarStateFilterDto carStateFilterDto, @AuthenticationPrincipal UserDetails currentUser) throws ParseException {
        if (carStateFilterDto != null) {
            model.addAttribute("carStateFilterDto", carStateFilterDto);
        }
        List<Gate> allReverseGates = (List<Gate>) gateService.listGatesByType(Gate.GateType.REVERSE);
        List<Gate> allInGates = (List<Gate>) gateService.listGatesByType(Gate.GateType.IN);
        if (allReverseGates.size() > 0) {
            allInGates.addAll(allReverseGates);
        }
        model.addAttribute("allInGates", allInGates);

        return "rta/entries";
    }

    @GetMapping("/exits")
    public String showAllExitsCarStates(Model model, @AuthenticationPrincipal UserDetails currentUser) {
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
        List<Gate> allOutGates = (List<Gate>) gateService.listGatesByType(Gate.GateType.OUT);
        if (allReverseGates.size() > 0) {
            allOutGates.addAll(allReverseGates);
        }
        model.addAttribute("allOutGates", allOutGates);
        return "rta/exits";
    }

    @PostMapping("/exits")
    public String processRequestExitsSearch(Model model, @Valid @ModelAttribute("carStateFilterDto") CarStateFilterDto carStateFilterDto, @AuthenticationPrincipal UserDetails currentUser) throws ParseException {
        if (carStateFilterDto != null) {
            model.addAttribute("carStateFilterDto", carStateFilterDto);
        }
        List<Gate> allReverseGates = (List<Gate>) gateService.listGatesByType(Gate.GateType.REVERSE);
        List<Gate> allOutGates = (List<Gate>) gateService.listGatesByType(Gate.GateType.OUT);
        if (allReverseGates.size() > 0) {
            allOutGates.addAll(allReverseGates);
        }
        model.addAttribute("allOutGates", allOutGates);

        return "rta/exits";
    }
}