package kz.spt.app.service;


import kz.spt.lib.model.Controller;

public interface ControllerService {

    Controller getControllerById(Long id);

    void saveController(Controller controller);

    void deleteController(Controller controller);
}
