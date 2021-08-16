package kz.spt.app.service;


import kz.spt.api.model.Barrier;

public interface BarrierService {

    Barrier getBarrierById(Long id);

    void saveBarrier(Barrier camera);
}
