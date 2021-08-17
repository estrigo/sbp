package kz.spt.app.controller;

import kz.spt.api.model.Barrier;
import kz.spt.api.model.Camera;
import kz.spt.api.model.Gate;
import kz.spt.api.model.Parking;
import kz.spt.app.service.BarrierService;
import kz.spt.app.service.CameraService;
import kz.spt.app.service.GateService;
import kz.spt.app.service.ParkingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.validation.Valid;

@Controller
@RequestMapping("/parking")
public class ParkingController {

    private ParkingService parkingService;
    private CameraService cameraService;
    private GateService gateService;
    private BarrierService barrierService;

    public ParkingController(ParkingService parkingService, CameraService cameraService, GateService gateService, BarrierService barrierService){
        this.parkingService = parkingService;
        this.cameraService = cameraService;
        this.gateService = gateService;
        this.barrierService = barrierService;
    }

    @GetMapping("/list")
    public String showAllParking(Model model) {
        model.addAttribute("parkings", parkingService.listAllParking());
        return "parking/list";
    }

    @GetMapping("/add")
    public String showFormAddParking(Model model) {
        model.addAttribute("parking", new Parking());
        return "parking/add";
    }

    @PostMapping("/add")
    public String processRequestAddParking(Model model, @Valid Parking parking, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "parking/add";
        } else {
            parkingService.saveParking(parking);
            return "redirect:/parking/list";
        }
    }

    @GetMapping("/edit/{id}")
    public String showFormEditParrking(Model model, @PathVariable Long id) {
        model.addAttribute("parking", parkingService.findById(id));
        return "parking/edit";
    }

    @PostMapping("/edit/{id}")
    public String processRequestEditCar(@PathVariable Long id, @Valid Parking parking, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "redirect:/parking/edit/" + id;
        } else {
            parkingService.saveParking(parking);
            return "redirect:/parking/list";
        }
    }

    @GetMapping("/details/{parkingId}")
    public String showAllParking(Model model, @PathVariable Long parkingId) {
        Parking parking = parkingService.findById(parkingId);
        if(parking != null){
            model.addAttribute("parking", parking);
            return "parking/details";
        } else {
            model.addAttribute("error", "global.notFound");
            return "404";
        }
    }

    @GetMapping("/camera/{cameraId}")
    public String getEditingCameraId(Model model, @PathVariable Long cameraId) {
        model.addAttribute("camera", cameraService.getCameraById(cameraId));
        return "parking/camera/edit";
    }

    @PostMapping("/camera/{gateId}")
    public String cameraEdit(@PathVariable Long gateId, @Valid Camera camera, BindingResult bindingResult){

        if (!bindingResult.hasErrors()) {
            Gate gate = gateService.getById(gateId);
            camera.setGate(gate);
            cameraService.saveCamera(camera);
        }
        return "redirect:/parking/details/" + camera.getGate().getParking().getId();
    }

    @GetMapping("/{gateId}/new/camera")
    public String getEditingCameraId(@PathVariable Long gateId, Model model) {
        Camera camera = new Camera();
        camera.setGate(gateService.getById(gateId));
        model.addAttribute("camera", camera);
        return "parking/camera/edit";
    }

    @GetMapping("/barrier/{barrierId}")
    public String getEditingBarrierId(Model model, @PathVariable Long barrierId) {
        model.addAttribute("barrier", barrierService.getBarrierById(barrierId));
        return "parking/barrier/edit";
    }

    @PostMapping("/barrier/{gateId}")
    public String barrierEdit(@PathVariable Long gateId, @Valid Barrier barrier, BindingResult bindingResult){

        if (!bindingResult.hasErrors()) {
            Gate gate = gateService.getById(gateId);
            barrier.setGate(gate);
            barrierService.saveBarrier(barrier);
        }
        return "redirect:/parking/details/" + barrier.getGate().getParking().getId();
    }

    @GetMapping("/{gateId}/new/barrier")
    public String getEditingBarrierId(@PathVariable Long gateId, Model model) {
        Barrier barrier = new Barrier();
        barrier.setGate(gateService.getById(gateId));
        model.addAttribute("barrier", barrier);
        return "parking/barrier/edit";
    }
    
    @GetMapping("/gate/{gateId}")
    public String getEditingGateId(Model model, @PathVariable Long gateId) {
        model.addAttribute("gate", gateService.getById(gateId));
        return "parking/gate/edit";
    }

    @PostMapping("/gate/{parkingId}")
    public String gateEdit(@PathVariable Long parkingId, @Valid Gate gate, BindingResult bindingResult){

        if (!bindingResult.hasErrors()) {
            Parking parking = parkingService.findById(parkingId);
            gate.setParking(parking);
            gateService.saveGate(gate);
        }
        return "redirect:/parking/details/" + gate.getParking().getId();
    }

    @GetMapping("/{parkingId}/new/gate")
    public String getEditinggateId(@PathVariable Long parkingId, Model model) {
        Gate gate = new Gate();
        gate.setParking(parkingService.findById(parkingId));
        model.addAttribute("gate", gate);
        return "parking/gate/edit";
    }
}