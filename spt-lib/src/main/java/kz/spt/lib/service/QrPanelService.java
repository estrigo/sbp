package kz.spt.lib.service;

import kz.spt.lib.model.Gate;

public interface QrPanelService {
    void display(Gate gate, String car_number);
    void clear(Gate gate);
}
