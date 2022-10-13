package kz.spt.app.service.impl;

import kz.spt.app.job.StatusCheckJob;
import kz.spt.lib.model.Gate;
import kz.spt.app.repository.GateRepository;
import kz.spt.app.service.GateService;
import kz.spt.lib.model.dto.GateDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(noRollbackFor = Exception.class)
public class GateServiceImpl implements GateService {

    private GateRepository gateRepository;

    public GateServiceImpl(GateRepository gateRepository){
        this.gateRepository = gateRepository;
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
    public Iterable<Gate> listAllGatesWithDependents() {
        return gateRepository.findByGatesByDependents();
    }

    @Override
    public List<GateDto> getGateByParkingId(Long id) {
        List<Gate> gates = gateRepository.findByParking_Id(id);
        return GateDto.fromGates(gates);
    }
}
