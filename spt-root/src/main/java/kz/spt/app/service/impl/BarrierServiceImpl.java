package kz.spt.app.service.impl;

import kz.spt.api.model.Barrier;
import kz.spt.app.repository.BarrierRepository;
import kz.spt.app.service.BarrierService;
import kz.spt.app.snmp.SNMPManager;
import lombok.extern.java.Log;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.ParseException;

@Service
@Log
public class BarrierServiceImpl implements BarrierService {

    private final BarrierRepository barrierRepository;

    public BarrierServiceImpl(BarrierRepository barrierRepository){
        this.barrierRepository = barrierRepository;
    }

    @Override
    public Barrier getBarrierById(Long id) {
        return barrierRepository.getOne(id);
    }

    @Override
    public void saveBarrier(Barrier barrier) {
        barrierRepository.save(barrier);
    }

    @Override
    public void openBarrier(Barrier barrier) throws IOException, ParseException {
        if(Barrier.BarrierType.SNMP.equals(barrier.getBarrierType())){
            openSnmp(barrier);
        } else if(Barrier.BarrierType.MODBUS.equals(barrier.getBarrierType())) {
            // TODO: open modbus type
        } else {
            throw new RuntimeException("Unknown barrier type");
        }
    }

    private void openSnmp(Barrier barrier) throws IOException, ParseException {
        SNMPManager client = new SNMPManager("udp:" + barrier.getIp() + "/161", barrier.getPassword());
        client.start();
        Boolean isValueChanged =client.open(barrier.getOpenOid());
        if(isValueChanged != null){
            // TODO: retry value change;
        }
        client.close();
    }
}
