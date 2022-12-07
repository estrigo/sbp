package kz.spt.app.service.impl;

import kz.spt.app.job.StatusCheckJob;
import kz.spt.app.repository.CarStateRepository;
import kz.spt.app.service.BarrierService;
import kz.spt.app.service.CameraService;
import kz.spt.app.service.ControllerService;
import kz.spt.lib.model.Camera;
import kz.spt.lib.model.Gate;
import kz.spt.app.repository.GateRepository;
import kz.spt.app.service.GateService;
import lombok.extern.slf4j.Slf4j;
import kz.spt.lib.model.dto.GateDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Slf4j
@Service
@Transactional(noRollbackFor = Exception.class)
public class GateServiceImpl implements GateService {

    private final GateRepository gateRepository;
    private final ControllerService controllerService;
    private final CameraService cameraService;
    private final BarrierService barrierService;
    private final CarStateRepository carStateRepository;

    public GateServiceImpl(GateRepository gateRepository, BarrierService barrierService,
                           ControllerService controllerService, CameraService cameraService,
                           CarStateRepository carStateRepository) {
        this.gateRepository = gateRepository;
        this.controllerService = controllerService;
        this.cameraService = cameraService;
        this.barrierService = barrierService;
        this.carStateRepository = carStateRepository;
    }

    @Override
    public Iterable<Gate> listAllGates() {
        return gateRepository.findAll();
    }

    @Override
    public Iterable<Gate> listGatesByType(Gate.GateType type) {
        return gateRepository.findByGateType(type);
    }

    @Override
    public Gate getById(Long id) {
        return gateRepository.getOne(id);
    }

    @Override
    public void saveGate(Gate gate) {
        gateRepository.save(gate);
        StatusCheckJob.emptyGlobalGateDtos();
    }

    @Override
    public void deleteGate(Gate gate) {
        gateRepository.delete(gate);
        StatusCheckJob.emptyGlobalGateDtos();
    }

    @Override
    @Transactional
    public void deleteGateWithCamAndBar(Gate gate) {
        if (gate.getGateType().equals(Gate.GateType.IN)) {
            carStateRepository.removeGateInFromCarStates(gate.getId());
        } else if (gate.getGateType().equals(Gate.GateType.OUT)) {
            carStateRepository.removeGateOutFromCarStates(gate.getId());
        }
        if (gate.getController() != null) {
            controllerService.deleteController(gate.getController());
        }
        if (gate.getCameraList() != null) {
            for (Camera camera : gate.getCameraList()) {
                cameraService.deleteCamera(camera);
            }
        }
        if (gate.getBarrier() != null) {
            barrierService.deleteBarrier(gate.getBarrier());
        }
        deleteGate(gate);
    }

    @Override
    public Iterable<Gate> listAllGatesWithDependents() {
        return gateRepository.findByGatesByDependents();
    }

    @Override
    public List<GateDto> getGateByParkingId(Long id) {
        List<Gate> gates = gateRepository.findByParking_Id(id);
        return GateDto.fromGates(gates);
    }
}
