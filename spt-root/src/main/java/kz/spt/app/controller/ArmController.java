package kz.spt.app.controller;

import kz.spt.app.service.CameraService;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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



//    @RequestMapping(value = "/enable2/{cameraId}", method = RequestMethod.POST, produces = {"application/json;charset=UTF-8"})
//    public String enable2(@ModelAttribute(value = "camera") Camera camera){
//        cameraService.enableSnapshot(Long.valueOf(camera.getId()));
//        return "parking/camera/calibration";
//    }

//    @RequestMapping(value = "/enable2/{cameraId}", method = RequestMethod.POST, produces = {"application/json;charset=UTF-8"})
//    public String enable2(@PathVariable Long cameraId, Model model) {
//        cameraService.enableSnapshot(cameraId);
//        model.addAttribute("camera" ,cameraService.getCameraById(cameraId));
//        return "parking/camera/calibration";
//    }
@PostMapping(value = "/enable2/{cameraId}")
public String enable2(@PathVariable("cameraId") Long cameraId, @RequestParam("isStreamOn") Boolean isStreamOn, Model model) {
    cameraService.enableSnapshot(cameraId, isStreamOn);
    model.addAttribute("camera" ,cameraService.getCameraById(cameraId));
    return "parking/camera/calibration";
}
}
