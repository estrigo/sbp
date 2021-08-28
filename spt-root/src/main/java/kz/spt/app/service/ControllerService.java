package kz.spt.app.service;


import kz.spt.api.model.Controller;

public interface ControllerService {

    Controller getControllerById(Long id);

    void saveController(Controller controller);
}
