package kz.spt.app.service.impl;

import kz.spt.lib.model.Controller;
import kz.spt.app.repository.ControllerRepository;
import kz.spt.app.service.ControllerService;
import lombok.extern.java.Log;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Log
@Transactional(noRollbackFor = Exception.class)
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

    @Override
    public void deleteController(Controller controller) {
        controllerRepository.delete(controller);
    }
}