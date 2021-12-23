package kz.spt.app.controller;

import kz.spt.app.repository.ParkingRepository;
import kz.spt.app.service.CameraService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/arm")
public class ArmController {

    private CameraService cameraService;

    public ArmController(CameraService cameraService){
        this.cameraService = cameraService;
    }

    @GetMapping("/realtime")
    public String getCamersForRealtime(Model model) {
        model.addAttribute("cameras", cameraService.cameraList());
        return "arm/realtime";
    }
}
