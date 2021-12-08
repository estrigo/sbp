package kz.spt.app.service;

import kz.spt.lib.model.Gate;

public interface GateService {

    Iterable<Gate> listAllGates();

    Iterable<Gate> listGatesByType(Gate.GateType type);

    Gate getById(Long id);

    void saveGate(Gate gate);

    void deleteGate(Gate gate);

    Iterable<Gate> listAllGatesWithDependents();
}
