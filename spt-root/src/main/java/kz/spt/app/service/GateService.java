package kz.spt.app.service;

import kz.spt.api.model.Gate;

public interface GateService {

    Iterable<Gate> listAllGates();

    Gate getById(Long id);

    void saveGate(Gate gate);
}
