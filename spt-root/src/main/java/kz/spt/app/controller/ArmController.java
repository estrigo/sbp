package kz.spt.app.controller;

import kz.spt.app.service.CameraService;
import org.springframework.beans.factory.annotation.Value;
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
    public String getCamersForRealtime(@Value("${parkomat.ip}") String ip, Model model) {
        model.addAttribute("cameras", cameraService.cameraList());
        model.addAttribute("ip", ip);
        return "arm/realtime";
    }
}
