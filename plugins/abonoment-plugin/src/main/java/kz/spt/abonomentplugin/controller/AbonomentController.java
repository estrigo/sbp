package kz.spt.abonomentplugin.controller;

import kz.spt.abonomentplugin.dto.AbonomentTypeDTO;
import kz.spt.abonomentplugin.service.AbonomentPluginService;
import kz.spt.abonomentplugin.service.RootServicesGetterService;
import kz.spt.lib.bootstrap.datatable.Page;
import kz.spt.lib.bootstrap.datatable.PagingRequest;
import kz.spt.lib.service.ParkingService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.text.ParseException;
import java.util.Arrays;

@Controller
@RequestMapping("/abonoment")
public class AbonomentController {

    private RootServicesGetterService rootServicesGetterService;
    private ParkingService parkingService;
    private AbonomentPluginService abonomentPluginService;

    public AbonomentController(RootServicesGetterService rootServicesGetterService,  AbonomentPluginService abonomentPluginService){
        this.rootServicesGetterService = rootServicesGetterService;
        this.abonomentPluginService = abonomentPluginService;
    }

    @GetMapping("/list")
    public String showList(Model model, @AuthenticationPrincipal UserDetails currentUser) {
        parkingService = rootServicesGetterService.getParkingService();
        model.addAttribute("parkingList", parkingService.listPaymentParkings());
        model.addAttribute("typeList", abonomentPluginService.getAllAbonomentTypes());
        model.addAttribute("canEdit", currentUser.getAuthorities().stream().anyMatch(m-> Arrays.asList("ROLE_SUPERADMIN","ROLE_ADMIN","ROLE_MANAGER").contains(m.getAuthority())));
        return "list";
    }
}
