package kz.spt.app.service;

import kz.spt.lib.model.Gate;

public interface GateService {

    Iterable<Gate> listAllGates();

    Gate getById(Long id);

    void saveGate(Gate gate);

    void deleteGate(Gate gate);
}
