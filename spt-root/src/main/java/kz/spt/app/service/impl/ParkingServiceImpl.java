package kz.spt.app.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.spt.app.service.BarrierService;
import kz.spt.app.service.CameraService;
import kz.spt.app.service.ControllerService;
import kz.spt.app.service.GateService;
import kz.spt.lib.extension.PluginRegister;
import kz.spt.lib.model.*;
import kz.spt.app.repository.ParkingRepository;
import kz.spt.lib.model.dto.ParkingCarsDTO;
import kz.spt.lib.model.dto.ParkingDto;
import kz.spt.lib.service.CarStateService;
import kz.spt.lib.service.CarsService;
import kz.spt.lib.service.ParkingService;
import kz.spt.lib.service.PluginService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static kz.spt.lib.utils.StaticValues.*;

@Service
@Transactional(noRollbackFor = Exception.class)
public class ParkingServiceImpl implements ParkingService {

    private ParkingRepository parkingRepository;
    private GateService gateService;
    private BarrierService barrierService;
    private ControllerService controllerService;
    private CameraService cameraService;
    private CarStateService carStateService;
    private CarsService carsService;
    private final PluginService pluginService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${parking.has.access.lcd}")
    Boolean hasAccessLcd;

    public ParkingServiceImpl(ParkingRepository parkingRepository, GateService gateService, BarrierService barrierService,
                              ControllerService controllerService, CameraService cameraService, CarStateService carStateService,
                              CarsService carsService, PluginService pluginService) {
        this.parkingRepository = parkingRepository;
        this.gateService = gateService;
        this.barrierService = barrierService;
        this.controllerService = controllerService;
        this.cameraService = cameraService;
        this.carStateService = carStateService;
        this.carsService = carsService;
        this.pluginService = pluginService;

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
        Optional<Parking> optionalParking = parkingRepository.findById(id);
        return optionalParking.isPresent() ? optionalParking.get() : null;
    }

    @Override
    public Parking findByType(Parking.ParkingType type) {
        List<Parking> prepaidParkings = parkingRepository.findParkingByParkingType(type);
        return prepaidParkings != null &&  prepaidParkings.size() > 0 ? prepaidParkings.get(0) : null;
    }

    @Override
    @Transactional
    public void deleteById(Long id) throws Exception{
        Parking parking = findById(id);
        carStateService.deleteParkingFromCarStates(parking);
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


        PluginRegister whitelistPluginRegister = pluginService.getPluginRegister(whitelistPlugin);
        if (whitelistPluginRegister != null) {
            ObjectNode command = objectMapper.createObjectNode();
            command.put("command", "deleteWhitelists");
            command.put("car_number", "000XXX00");
            command.put("parkingId", parking.getId());
            JsonNode whitelistPluginResult = whitelistPluginRegister.execute(command);
        }

        PluginRegister ratePluginRegister = pluginService.getPluginRegister(ratePlugin);
        if (ratePluginRegister != null) {
            ObjectNode command = objectMapper.createObjectNode();
            command.put("command", "deleteParkingRate");
            command.put("parkingId", parking.getId());
            JsonNode ratePluginResult = ratePluginRegister.execute(command);
        }

        PluginRegister abonomentPluginRegister = pluginService.getPluginRegister(abonementPlugin);
        if (abonomentPluginRegister != null) {
            ObjectNode command = objectMapper.createObjectNode();
            command.put("command", "deleteParkingAbonoments");
            command.put("parkingId", parking.getId());
            JsonNode abonomentPluginResult = abonomentPluginRegister.execute(command);
        }
//        CarState
//

        parkingRepository.delete(parking);
    }

    @Override
    public List<ParkingCarsDTO> listAllParkingCars() {
        Iterable<CarState> carStates = carStateService.getAllNotLeft();
        List<Parking> parkings = (List<Parking>) listAllParking();

        List<ParkingCarsDTO> carsInParkings = new ArrayList<>(parkings.size());

        List<Cars> resultCars = new ArrayList<>(parkings.size());
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

    @Override
    public Iterable<Parking> listWhitelistParkings() {
        return parkingRepository.whitelistParkings();
    }

    @Override
    public Iterable<Parking> listPaymentParkings() {
        return parkingRepository.paymentParkings();
    }

    @Override
    public List<ParkingDto> getParkings() {
        return ParkingDto.fromParking(parkingRepository.findAll());
    }

    @Override
    public Boolean isLcd() {
        return hasAccessLcd;
    }
}
