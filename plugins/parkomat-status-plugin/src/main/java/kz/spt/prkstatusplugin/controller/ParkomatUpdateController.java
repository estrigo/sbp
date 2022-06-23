package kz.spt.prkstatusplugin.controller;

import kz.spt.prkstatusplugin.enums.SoftwareType;
import kz.spt.prkstatusplugin.model.ParkomatUpdate;
import kz.spt.prkstatusplugin.service.ParkomatService;
import kz.spt.prkstatusplugin.service.ParkomatUpdateFileService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static kz.spt.prkstatusplugin.enums.SoftwareType.PARKOMAT;
import static kz.spt.prkstatusplugin.enums.SoftwareType.SERVICE;

@Controller
@RequestMapping(value = "/prkstatus/update")
public class ParkomatUpdateController {


    private ParkomatService parkomatService;
    private ParkomatUpdateFileService parkomatUpdateFileService;

    public ParkomatUpdateController(ParkomatService parkomatService, ParkomatUpdateFileService parkomatUpdateFileService) {
        this.parkomatService = parkomatService;
        this.parkomatUpdateFileService = parkomatUpdateFileService;
    }

    @GetMapping("/list")
    public String showTestList(Model model) {
        Page<ParkomatUpdate> parkomatUpdatePage =  parkomatService.getUpdates();

        model.addAttribute("updateList", parkomatUpdatePage.toList());

        return "prkstatus/update/list";
    }

    @PostMapping("/save")
    public String saveUpdates(@RequestParam("file") MultipartFile file, @RequestParam("SoftwareType") String softwareType) throws IOException {


        ParkomatUpdate parkomatUpdate = new ParkomatUpdate();
        parkomatUpdate.setType("PARKOMAT".equals(softwareType) ? PARKOMAT : SERVICE);
        parkomatService.saveParkomatUpdate(parkomatUpdate);

        parkomatUpdateFileService.store(parkomatUpdate, file.getBytes());



        return "redirect:/prkstatus/update/list";
    }
}
