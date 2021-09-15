package kz.spt.app.service.impl;

import kz.spt.app.service.BarrierService;
import kz.spt.app.service.CameraService;
import kz.spt.app.service.ControllerService;
import kz.spt.app.service.GateService;
import kz.spt.lib.model.Camera;
import kz.spt.lib.model.Gate;
import kz.spt.lib.model.Parking;
import kz.spt.app.repository.ParkingRepository;
import kz.spt.lib.service.ParkingService;
import org.springframework.stereotype.Service;

@Service
public class ParkingServiceImpl implements ParkingService {

    private ParkingRepository parkingRepository;
    private GateService gateService;
    private BarrierService barrierService;
    private ControllerService controllerService;
    private CameraService cameraService;

    public ParkingServiceImpl(ParkingRepository parkingRepository, GateService gateService, BarrierService barrierService,
                              ControllerService controllerService, CameraService cameraService){
        this.parkingRepository = parkingRepository;
        this.gateService = gateService;
        this.barrierService = barrierService;
        this.controllerService = controllerService;
        this.cameraService = cameraService;
    }

    @Override
    public Iterable<Parking> listAllParking() {
        return parkingRepository.findAll();
    }

    @Override
    public Parking saveParking(Parking parking) {
        return parkingRepository.save(parking);
    }

    @Override
    public Parking findById(Long id) {
        return parkingRepository.getOne(id);
    }

    @Override
    public void deleteById(Long id) {
        Parking parking = findById(id);
        if(parking.getGateList() != null){
            for(Gate gate : parking.getGateList()){
                if(gate.getBarrier() != null){
                    barrierService.deleteBarrier(gate.getBarrier());
                }
                if(gate.getController() != null){
                    controllerService.deleteController(gate.getController());
                }
                if(gate.getCameraList() != null){
                    for(Camera camera : gate.getCameraList()){
                        cameraService.deleteCamera(camera);
                    }
                }
                gateService.deleteGate(gate);
            }
        }
        parkingRepository.delete(parking);
    }
}
