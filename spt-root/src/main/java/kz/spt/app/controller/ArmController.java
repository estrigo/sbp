package kz.spt.app.controller;

import kz.spt.app.service.CameraService;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Log
@Controller
@RequestMapping("/arm")
public class ArmController {

    private CameraService cameraService;

    public ArmController(CameraService cameraService){
        this.cameraService = cameraService;
    }

    @GetMapping("/realtime")
    public String realtime(@Value("${parkomat.ip}") String ip, Model model) {
        model.addAttribute("cameras", cameraService.cameraListWithoutTab());
        model.addAttribute("ip", ip);
        return "arm/realtime";
    }

    @GetMapping("/realtime/{cameraTabId}")
    public String getCamersForRealtime(@Value("${parkomat.ip}") String ip, Model model, @PathVariable Long cameraTabId) {
        model.addAttribute("cameras", cameraService.cameraListByTabId(cameraTabId));
        model.addAttribute("ip", ip);
        return "arm/realtime";
    }

    @GetMapping("/enable/{cameraId}")
    public String enable(@Value("${parkomat.ip}") String ip, @PathVariable Long cameraId, Model model){
        cameraService.enableSnapshot(cameraId);
        model.addAttribute("cameras", cameraService.cameraListWithoutTab());
        model.addAttribute("ip", ip);
        return "arm/realtime";
    }
}
