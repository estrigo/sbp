package kz.spt.app.service.impl;

import kz.spt.api.model.Barrier;
import kz.spt.api.model.Controller;
import kz.spt.api.model.Gate;
import kz.spt.api.service.EventLogService;
import kz.spt.app.repository.BarrierRepository;
import kz.spt.app.repository.ControllerRepository;
import kz.spt.app.service.BarrierService;
import kz.spt.app.service.ControllerService;
import kz.spt.app.snmp.SNMPManager;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.ParseException;
import java.util.Map;

@Service
@Log
public class ControllerServiceImpl implements ControllerService {

    private final ControllerRepository controllerRepository;

    public ControllerServiceImpl(ControllerRepository controllerRepository){
        this.controllerRepository = controllerRepository;
    }

    @Override
    public Controller getControllerById(Long id) {
        return controllerRepository.getOne(id);
    }

    @Override
    public void saveController(Controller controller) {
        controllerRepository.save(controller);
    }
}