package kz.spt.lib.service;

import kz.spt.lib.model.Gate;

import java.net.URISyntaxException;

public interface QrPanelService {
    void display(Gate gate, String car_number) ;
    void clear(Gate gate);
    String generateUrl(Gate gate, String car_number);
}
