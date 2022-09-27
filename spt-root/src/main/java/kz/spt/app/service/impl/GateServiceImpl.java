package kz.spt.app.service.impl;

import kz.spt.app.job.StatusCheckJob;
import kz.spt.app.service.BarrierService;
import kz.spt.app.service.CameraService;
import kz.spt.app.service.ControllerService;
import kz.spt.lib.model.Camera;
import kz.spt.lib.model.Gate;
import kz.spt.app.repository.GateRepository;
import kz.spt.app.service.GateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(noRollbackFor = Exception.class)
public class GateServiceImpl implements GateService {

    private final GateRepository gateRepository;
    private final ControllerService controllerService;
    private final CameraService cameraService;
    private final BarrierService barrierService;

    public GateServiceImpl(GateRepository gateRepository, BarrierService barrierService,
                           ControllerService controllerService, CameraService cameraService) {
        this.gateRepository = gateRepository;
        this.controllerService = controllerService;
        this.cameraService = cameraService;
        this.barrierService = barrierService;
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
}
