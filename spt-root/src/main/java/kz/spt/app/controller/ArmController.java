package kz.spt.app.controller;

import kz.spt.app.service.CameraService;
import kz.spt.app.service.ReasonsService;
import kz.spt.lib.model.Camera;
import kz.spt.lib.model.Reasons;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Date;

@Log
@Controller
@RequestMapping("/arm")
public class ArmController {

    private CameraService cameraService;
    private ReasonsService reasonsService;

    public ArmController(CameraService cameraService, ReasonsService reasonsService) {
        this.cameraService = cameraService;
        this.reasonsService = reasonsService;
    }

    @GetMapping("/realtime")
    public String realtime(@Value("${parkomat.ip}") String ip, Model model, @AuthenticationPrincipal UserDetails currentUser) {
        model.addAttribute("cameras", cameraService.cameraListWithoutTab());
        model.addAttribute("ip", ip);
        model.addAttribute("canEdit", currentUser.getAuthorities().stream().anyMatch(m-> Arrays.asList("ROLE_ADMIN").contains(m.getAuthority())));
        model.addAttribute("reasons", reasonsService.findAllReasons());
        return "arm/realtime";
    }

    @PostMapping("/deleteReason")
    public String deleteGateOpenReason(@RequestParam("selectedReasonId") String selectedReasonId) {
        reasonsService.deleteReasonById(Long.valueOf(selectedReasonId));
        return "redirect:realtime/";
    }

    @PostMapping("/addReason")
    public String addNewGateOpenReason(@RequestParam("newReason") String newReason) {
        reasonsService.addNewReason(newReason);
        return "redirect:realtime/";
    }

    @GetMapping("/realtime/{cameraTabId}")
    public String getCamersForRealtime(@Value("${parkomat.ip}") String ip, Model model, @PathVariable Long cameraTabId, @AuthenticationPrincipal UserDetails currentUser) {
        model.addAttribute("cameras", cameraService.cameraListByTabId(cameraTabId));
        model.addAttribute("ip", ip);
        model.addAttribute("canEdit", currentUser.getAuthorities().stream().anyMatch(m-> Arrays.asList("ROLE_ADMIN").contains(m.getAuthority())));
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
    public String timeUnavailable(Model model, @PathVariable Long cameraId, @AuthenticationPrincipal UserDetails currentUser) {
        Camera camera = cameraService.getCameraById(cameraId);
        model.addAttribute("camera", camera);
        return "/arm/timeUnavailable";
    }

    @Transactional
    @PostMapping("/interval-add")
    public String editCameraTime(Model model, @ModelAttribute(value = "camera") Camera camera, @AuthenticationPrincipal UserDetails currentUser) {
        Camera updateCameraTime = cameraService.getCameraById(camera.getId());
        updateCameraTime.setStartTime(camera.getStartTime());
        updateCameraTime.setEndTime(camera.getEndTime());
        updateCameraTime.setUpdatedTime(new Date());
        updateCameraTime.setUpdatedTimeBy(currentUser.getUsername());
        cameraService.saveCamera(updateCameraTime, true);
        model.addAttribute("camera", updateCameraTime);
        return "/arm/timeUnavailable";
    }
}
