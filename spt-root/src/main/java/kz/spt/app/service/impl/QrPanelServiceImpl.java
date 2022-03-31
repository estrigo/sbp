package kz.spt.app.service.impl;

import kz.spt.lib.model.Gate;
import kz.spt.lib.service.QrPanelService;

import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Files;
import java.nio.file.Paths;
@Log
@Service
public class QrPanelServiceImpl implements QrPanelService {

    @Value("${kaspi.qr-link}")
    String kaspiQrLink;

    @Override
    public void display(Gate gate, String car_number) {
        if (gate != null && gate.getGateType().equals(Gate.GateType.OUT) && gate.getQrPanelIp()!=null && !gate.getQrPanelIp().isEmpty()) {
            String serviceName = gate.getParking().getKaspiServiceName();
            String serviceId = gate.getParking().getKaspiServiceId();
            String plateParam = gate.getParking().getKaspiPlateParam();
            String qrUrl = String.format(kaspiQrLink, serviceName, serviceId, plateParam);
            var restTemplate = new RestTemplate();
            StringBuilder address = new StringBuilder();
            address.append("http://");
            address.append(gate.getQrPanelIp());
            address.append("/qr="+qrUrl+car_number);
            log.info("QRPanel display: " + address.toString());
            restTemplate.getForEntity(address.toString(),String.class);
        }
    }

    @Override
    public void clear(Gate gate) {
        if (gate != null && gate.getGateType().equals(Gate.GateType.OUT) && gate.getQrPanelIp()!=null && !gate.getQrPanelIp().isEmpty()) {
            var restTemplate = new RestTemplate();
            StringBuilder address = new StringBuilder();
            address.append("http://");
            address.append(gate.getQrPanelIp());
            address.append("/img=");
            log.info("QRPanel clearing");
            restTemplate.getForEntity(address.toString(),String.class);
        }
    }


}
