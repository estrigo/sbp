package kz.spt.app.service.impl;

import kz.spt.lib.model.Gate;
import kz.spt.app.repository.GateRepository;
import kz.spt.app.service.GateService;
import org.springframework.stereotype.Service;

@Service
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
    public Gate getById(Long id) {
        return gateRepository.getOne(id);
    }

    @Override
    public void saveGate(Gate gate) {
        gateRepository.save(gate);
    }

    @Override
    public void deleteGate(Gate gate) {
        gateRepository.delete(gate);
    }
}
