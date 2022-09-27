package kz.spt.app.service.impl;

import kz.spt.app.repository.CarStateRepository;
import kz.spt.app.repository.GateRepository;
import kz.spt.lib.model.Gate;
import kz.spt.lib.service.TabloService;
import lombok.extern.java.Log;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;

@Log
@Service
public class TableServiceImpl implements TabloService {

    private final GateRepository gateRepository;
    private final CarStateRepository carStateRepository;

    public TableServiceImpl(GateRepository gateRepository, CarStateRepository carStateRepository) {
        this.gateRepository = gateRepository;
        this.carStateRepository = carStateRepository;
    }

    @Override
    public void updateOnOut(Gate gate) {
        try {
            Gate gateIN = gateRepository.findFirstByTabloIpAndGateType(gate.getTabloIp(), Gate.GateType.IN);
            if (gateIN != null) {
                Integer totalCarIn = carStateRepository.getCarCountInGate(gateIN);
                int number = gateIN.getParkingSpaceNumber() - totalCarIn;
                updateTablo(gateIN, number);
            }
        }catch (Exception ex) {
            log.log(Level.SEVERE,"Error on table update on car out", ex);
        }
    }

    @Override
    public void updateOnIn(Gate gate) {
        try {
        Integer totalCarIn = carStateRepository.getCarCountInGate(gate);
        int number = gate.getParkingSpaceNumber() - totalCarIn;
        updateTablo(gate, number);
        }catch (Exception ex) {
            log.log(Level.SEVERE,"Error on table update on car in", ex);
        }
    }

    private String updateTablo(Gate gate, int number) {
        try {
            StringBuilder address = new StringBuilder();
            address.append("http://");
            address.append(gate.getTabloIp());
            address.append("/" + number);
            log.info("Tablo display: " + number);
            var restTemplate = new RestTemplate();
            URI uri = new URI(address.toString());
            ResponseEntity<String> result = restTemplate.getForEntity(uri, String.class);
            return result.getBody();
        } catch (Exception ex) {
            log.log(Level.SEVERE,"Error while update tablo on " + gate.getTabloIp());
            log.log(Level.SEVERE,ex.getMessage());
        }
        return null;
    }
}