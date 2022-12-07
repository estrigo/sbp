package kz.spt.app.service;

import kz.spt.lib.model.Gate;
import kz.spt.lib.model.dto.GateDto;

import java.util.List;

public interface GateService {

    Iterable<Gate> listAllGates();

    Iterable<Gate> listGatesByType(Gate.GateType type);

    Gate getById(Long id);

    void saveGate(Gate gate);

    void deleteGate(Gate gate);

    void deleteGateWithCamAndBar(Gate gate);

    Iterable<Gate> listAllGatesWithDependents();

    List<GateDto> getGateByParkingId(Long id);
}
