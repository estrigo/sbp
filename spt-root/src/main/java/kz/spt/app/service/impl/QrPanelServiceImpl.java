package kz.spt.app.service.impl;

import kz.spt.app.repository.GateRepository;
import kz.spt.lib.model.Gate;
import kz.spt.lib.service.QrPanelService;

import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.logging.Level;

@Log
@Service
public class QrPanelServiceImpl implements QrPanelService {

    @Value("${kaspi.qr-link}")
    String kaspiQrLink;

    private final GateRepository gateRepository;

    public QrPanelServiceImpl(GateRepository gateRepository) {
        this.gateRepository = gateRepository;
    }

    @Override
    public void display(Gate gate, String car_number) {
        if (gate != null && gate.getGateType().equals(Gate.GateType.OUT) && gate.getQrPanelIp() != null && !gate.getQrPanelIp().isEmpty()) {
            try {

                String qrUrl = this.generateUrl(gate, car_number);
                var restTemplate = new RestTemplate();
                StringBuilder address = new StringBuilder();
                address.append("http://");
                address.append(gate.getQrPanelIp());
                address.append("/qr=" + qrUrl);
                log.info("QRPanel display: " + address.toString());

                URI uri = new URI(address.toString());
                restTemplate.getForEntity(uri, String.class);
            } catch (Exception ex) {
                this.clear(gate);
            }
        }
    }

    @Override
    public void clear(Gate gate) {
        if (gate != null && gate.getGateType().equals(Gate.GateType.OUT) && gate.getQrPanelIp() != null && !gate.getQrPanelIp().isEmpty()) {
            var restTemplate = new RestTemplate();
            StringBuilder address = new StringBuilder();
            address.append("http://");
            address.append(gate.getQrPanelIp());
            address.append("/img=");
            log.info("QRPanel clearing");
            try {
            restTemplate.getForEntity(address.toString(), String.class);
            } catch (Exception ex) {
                log.log(Level.SEVERE,"Error while QRPanel clearing for gate");
            }
        }
    }

    @Override
    public String generateUrl(Gate gate, String car_number) {

        if (gate == null) {
            Gate gateOut = gateRepository.findFirstByGateTypeAndQrPanelIpNotNull(Gate.GateType.OUT);
            if (gateOut!=null)
                gate = gateOut;

        }
        if (gate.getParking().getKaspiServiceName() != null) {
            String serviceName = gate.getParking().getKaspiServiceName();

            String serviceId = gate.getParking().getKaspiServiceId();
            String plateParam = gate.getParking().getKaspiPlateParam();
            String qrUrl = String.format(kaspiQrLink, serviceName, serviceId, plateParam);
            return qrUrl + car_number;
        }

        return null;
    }


}
