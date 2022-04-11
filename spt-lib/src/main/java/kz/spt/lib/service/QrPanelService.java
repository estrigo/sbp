package kz.spt.lib.service;

import kz.spt.lib.model.Gate;

import java.net.URISyntaxException;

public interface QrPanelService {
    void display(Gate gate, String car_number) throws URISyntaxException;
    void clear(Gate gate);
}
