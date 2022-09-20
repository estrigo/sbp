package kz.spt.app.controller;

import kz.spt.app.service.CameraService;
import kz.spt.lib.model.Camera;
import kz.spt.lib.model.Parking;
import kz.spt.lib.model.User;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Locale;

@Log
@Controller
@RequestMapping("/arm")
public class ArmController {

    private CameraService cameraService;

    public ArmController(CameraService cameraService) {
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
    public String enable(@Value("${parkomat.ip}") String ip, @PathVariable Long cameraId, Model model) {
        cameraService.enableSnapshot(cameraId);
        model.addAttribute("cameras", cameraService.cameraListWithoutTab());
        model.addAttribute("ip", ip);
        return "arm/realtime";
    }

    @PostMapping(value = "/enable2/{cameraId}")
    public String enableSnapshotOnCalibration(@PathVariable("cameraId") Long cameraId, @RequestParam("isStreamOn") Boolean isStreamOn, Model model) {
        cameraService.enableSnapshot(cameraId, isStreamOn);
        model.addAttribute("camera", cameraService.getCameraById(cameraId));
        return "parking/camera/calibration";
    }

    @GetMapping("/edit/timeUnavailable/{cameraId}")
    public String ddd(Model model, @PathVariable Long cameraId) {
        Camera camera = cameraService.getCameraById(cameraId);
        model.addAttribute("camera", camera);
        return "/arm/timeUnavailable";
    }

    @Transactional
    @PostMapping("/interval-add")
    public String editCamera(Model model, @ModelAttribute(value = "camera") Camera camera) {
        System.out.println("ss");
        Camera updateCameraTime = cameraService.getCameraById(camera.getId());
        updateCameraTime.setStartTime(camera.getStartTime());
        updateCameraTime.setEndTime(camera.getEndTime());
        cameraService.saveCamera(updateCameraTime, true);
        model.addAttribute("camera", updateCameraTime);
        return "/arm/realtime";
    }
}
