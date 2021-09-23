package kz.spt.app.service.impl;

import kz.spt.app.service.BarrierService;
import kz.spt.app.service.CameraService;
import kz.spt.app.service.ControllerService;
import kz.spt.app.service.GateService;
import kz.spt.lib.model.*;
import kz.spt.app.repository.ParkingRepository;
import kz.spt.lib.model.dto.ParkingCarsDTO;
import kz.spt.lib.service.CarStateService;
import kz.spt.lib.service.CarsService;
import kz.spt.lib.service.ParkingService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ParkingServiceImpl implements ParkingService {

    private ParkingRepository parkingRepository;
    private GateService gateService;
    private BarrierService barrierService;
    private ControllerService controllerService;
    private CameraService cameraService;
    private CarStateService carStateService;
    private CarsService carsService;

    public ParkingServiceImpl(ParkingRepository parkingRepository, GateService gateService, BarrierService barrierService,
                              ControllerService controllerService, CameraService cameraService, CarStateService carStateService, CarsService carsService) {
        this.parkingRepository = parkingRepository;
        this.gateService = gateService;
        this.barrierService = barrierService;
        this.controllerService = controllerService;
        this.cameraService = cameraService;
        this.carStateService = carStateService;
        this.carsService = carsService;

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
        if (parking.getGateList() != null) {
            for (Gate gate : parking.getGateList()) {
                if (gate.getBarrier() != null) {
                    barrierService.deleteBarrier(gate.getBarrier());
                }
                if (gate.getController() != null) {
                    controllerService.deleteController(gate.getController());
                }
                if (gate.getCameraList() != null) {
                    for (Camera camera : gate.getCameraList()) {
                        cameraService.deleteCamera(camera);
                    }
                }
                gateService.deleteGate(gate);
            }
        }
        parkingRepository.delete(parking);
    }

    @Override
    public List<ParkingCarsDTO> listAllParkingCars() {
        Iterable<CarState> carStates = carStateService.getAllNotLeft();
        List<Parking> parkings = (List<Parking>) listAllParking();

        List<ParkingCarsDTO> carsInParkings = new ArrayList<>();

        List<Cars> resultCars = new ArrayList<>();
        for (Parking parking : parkings) {
            ParkingCarsDTO parkingCarsDTO = new ParkingCarsDTO();
            parkingCarsDTO.setParking(parking);
            for (CarState carState : carStates) {
                if (carState.getParking() != null && carState.getParking().getId().equals(parking.getId())) {
                    resultCars.add(carsService.findByPlatenumber(carState.getCarNumber()));
                }
            }
            parkingCarsDTO.setCarsList(resultCars);
            carsInParkings.add(parkingCarsDTO);
        }
        return carsInParkings;
    }

    @Override
    public ParkingCarsDTO carsInParking(Long parkingId) {
        List<ParkingCarsDTO> listCarsInParkings = listAllParkingCars();
        for (ParkingCarsDTO parkingCarsDTO : listCarsInParkings) {
            if (parkingCarsDTO.getParking().getId().equals(parkingId)) {
                return parkingCarsDTO;
            }
        }
        return null;
    }
}
