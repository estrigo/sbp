package kz.spt.app.controller;

import kz.spt.app.repository.CarStateRepository;
import kz.spt.app.service.*;
import kz.spt.lib.model.*;
import kz.spt.lib.service.CarStateService;
import kz.spt.lib.service.EmergencySignalService;
import kz.spt.lib.service.ParkingService;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.validation.Valid;

@org.springframework.stereotype.Controller
@Log
@RequestMapping("/parking")
public class ParkingController {

    private ParkingService parkingService;
    private CameraService cameraService;
    private GateService gateService;
    private BarrierService barrierService;
    private ControllerService controllerService;
    private CarStateRepository carStateRepository;
    private CarStateService carStateService;

    private EmergencySignalService emergencySignalService;

    @Value("${carmen.live.enabled}")
    Boolean carmenLiveEnabled;


    public ParkingController(ParkingService parkingService, CameraService cameraService, GateService gateService,
                             BarrierService barrierService, ControllerService controllerService,
                             CarStateRepository carStateRepository, CarStateService carStateService,
                             EmergencySignalService emergencySignalService) {
        this.parkingService = parkingService;
        this.cameraService = cameraService;
        this.gateService = gateService;
        this.barrierService = barrierService;
        this.controllerService = controllerService;
        this.carStateRepository = carStateRepository;
        this.carStateService = carStateService;
        this.emergencySignalService = emergencySignalService;
    }

    @GetMapping("/list")
    public String showAllParking(Model model) {
        model.addAttribute("parkings", parkingService.listAllParking());
        model.addAttribute("permanentOpenEnabled", barrierService.getPermanentOpenEnabled());
        model.addAttribute("emergencySignalConfig", emergencySignalService.getConfigured());
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
    public String showFormEditParking(Model model, @PathVariable Long id) {
        model.addAttribute("parking", parkingService.findById(id));
        return "parking/edit";
    }

    @GetMapping("/delete/{id}")
    public String deleteParking(Model model, @PathVariable Long id) throws Exception {
        parkingService.deleteById(id);
        return "redirect:/parking/list";
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
        if (parking != null) {
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
        model.addAttribute("carmenLiveEnabled", carmenLiveEnabled);
        return "parking/camera/edit";
    }

    @PostMapping("/camera/{gateId}")
    public String cameraEdit(@PathVariable Long gateId, @Valid Camera camera, BindingResult bindingResult) {

        if (!bindingResult.hasErrors()) {
            Gate gate = gateService.getById(gateId);
            camera.setGate(gate);
            cameraService.saveCamera(camera, true);
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

    @GetMapping("/gate/{gateId}/delete")
    public String deleteGate(@PathVariable Long gateId) {
        Gate gate = gateService.getById(gateId);
        Long parkingId = gate.getParking().getId();
        gateService.deleteGateWithCamAndBar(gate);
        return "redirect:/parking/details/" + parkingId;
    }

    @GetMapping("/barrier/{barrierId}")
    public String getEditingBarrierId(Model model, @PathVariable Long barrierId) {
        model.addAttribute("barrier", barrierService.getBarrierById(barrierId));
        return "parking/barrier/edit";
    }

    @PostMapping("/barrier/{gateId}")
    public String barrierEdit(@PathVariable Long gateId, @Valid Barrier barrier, BindingResult bindingResult) {

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
    public String gateEdit(@PathVariable Long parkingId, @Valid Gate gate, BindingResult bindingResult) {

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

    @GetMapping("/cars")
    public String showCurrentStatus(Model model) {
        model.addAttribute("parkingCars", parkingService.listAllParkingCars());
        return "cars-in-parkings/list";
    }

    @GetMapping("/cars/details/{id}")
    public String showCarsInParking(Model model, @PathVariable Long id) {
        model.addAttribute("parkingCars", parkingService.carsInParking(id));
        return "cars-in-parkings/cars/list";
    }

    @GetMapping("/cars/calibration/{id}")
    public String calibration(@PathVariable Long id, Model model){
        Camera camera = cameraService.getCameraById(id);
        model.addAttribute("isSnapshotEnable", camera.getSnapshotEnabled());
        model.addAttribute("camera" ,cameraService.getCameraById(id));
        return "parking/camera/calibration";
    }

    @GetMapping("/cameras/remove/{id}")
    public String removeCamera(@PathVariable Long id) {
        log.info("camera: " + id);
        Camera camera = cameraService.getCameraById(id);
        if (camera != null) {
            cameraService.deleteCamera(camera);
        }
        return "redirect:/parking/list";
    }

    @GetMapping("/barrier/remove/{barrierId}")
    public String removeBarrier(@PathVariable Long barrierId) {
        Barrier barrier = barrierService.getBarrierById(barrierId);
        if (barrier != null) {
            carStateService.UpdateAndRemoveByBarrier(barrier);
        }
        return "redirect:/parking/list";
    }
}