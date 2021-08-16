package kz.spt.app.service.impl;

import kz.spt.api.model.Gate;
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
}
