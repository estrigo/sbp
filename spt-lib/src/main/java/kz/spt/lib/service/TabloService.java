package kz.spt.lib.service;

import kz.spt.lib.model.Gate;

public interface TabloService {

    void updateOnOut(Gate gate);
    void updateOnIn(Gate gate);
}
