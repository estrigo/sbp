package kz.spt.app.service.impl;

import kz.spt.api.model.Barrier;
import kz.spt.app.repository.BarrierRepository;
import kz.spt.app.service.BarrierService;
import org.springframework.stereotype.Service;

@Service
public class BarrierServiceImpl implements BarrierService {

    private final BarrierRepository barrierRepository;

    public BarrierServiceImpl(BarrierRepository barrierRepository){
        this.barrierRepository = barrierRepository;
    }

    @Override
    public Barrier getBarrierById(Long id) {
        return barrierRepository.getOne(id);
    }

    @Override
    public void saveBarrier(Barrier barrier) {
        barrierRepository.save(barrier);
    }
}
