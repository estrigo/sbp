package kz.spt.app.service.impl;

import kz.spt.lib.model.Barrier;
import kz.spt.lib.model.Gate;
import kz.spt.lib.service.EventLogService;
import kz.spt.app.repository.BarrierRepository;
import kz.spt.app.service.BarrierService;
import kz.spt.app.snmp.SNMPManager;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.ParseException;
import java.util.Map;

@Service
@Log
public class BarrierServiceImpl implements BarrierService {

    private Boolean disableOpen;
    private final BarrierRepository barrierRepository;
    private final EventLogService eventLogService;

    public BarrierServiceImpl(@Value("${barrier.open.disabled}") Boolean disableOpen, BarrierRepository barrierRepository, EventLogService eventLogService){
        this.disableOpen = disableOpen;
        this.barrierRepository = barrierRepository;
        this.eventLogService = eventLogService;
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
    public Boolean openBarrier(Barrier barrier, Map<String, Object> properties) throws IOException, ParseException, InterruptedException {
        if(Barrier.BarrierType.SNMP.equals(barrier.getBarrierType())){
            return openSnmp(barrier, properties);
        } else if(Barrier.BarrierType.MODBUS.equals(barrier.getBarrierType())) {
            // TODO: open modbus type
            return false;
        } else {
            throw new RuntimeException("Unable open barrier: unknown barrier type");
        }
    }

    @Override
    public Boolean closeBarrier(Barrier barrier, Map<String, Object> properties) throws IOException, ParseException, InterruptedException {
        if(Barrier.BarrierType.SNMP.equals(barrier.getBarrierType())){
            return closeSnmp(barrier, properties);
        } else if(Barrier.BarrierType.MODBUS.equals(barrier.getBarrierType())) {
            // TODO: close modbus type
            return false;
        } else {
            throw new RuntimeException("Unable close barrier: unknown barrier type");
        }
    }

    private Boolean openSnmp(Barrier barrier, Map<String, Object> properties) throws IOException, ParseException, InterruptedException {
        Boolean result = true;
        if(!disableOpen){
            SNMPManager client = new SNMPManager("udp:" + barrier.getIp() + "/161", barrier.getPassword(), barrier.getSnmpVersion());
            client.start();
            if(!"1".equals(client.getCurrentValue(barrier.getOpenOid()))){
                Boolean isOpenValueChanged =client.changeValue(barrier.getOpenOid(), 1);
                if(!isOpenValueChanged){
                    for(int i=0; i<3; i++){
                        isOpenValueChanged =client.changeValue(barrier.getOpenOid(), 1);
                    }
                    if(!isOpenValueChanged){
                        result = false;
                        eventLogService.createEventLog(Barrier.class.getSimpleName(), barrier.getId(), properties, "Контроллер шлагбаума " + (barrier.getGate().getGateType().equals(Gate.GateType.IN) ? "въезда" : (barrier.getGate().getGateType().equals(Gate.GateType.IN) ? "выезда":"въезда/выезда")) +" не получилась перенести на значение 1 чтобы открыть " + (properties.get("carNumber")  != null ? "для номер авто " + properties.get("carNumber") : ""));
                    }
                } else {
                    Thread.sleep(1000);
                    if(!"0".equals(client.getCurrentValue(barrier.getOpenOid()))){
                        Boolean isReturnValueChanged =client.changeValue(barrier.getOpenOid(), 0);
                        if(!isReturnValueChanged) {
                            for (int i = 0; i < 3; i++) {
                                isReturnValueChanged = client.changeValue(barrier.getOpenOid(), 0);
                            }
                            if (!isReturnValueChanged) {
                                eventLogService.createEventLog(Barrier.class.getSimpleName(), barrier.getId(), properties, "Контроллер шлагбаума " + (barrier.getGate().getGateType().equals(Gate.GateType.IN) ? "въезда" : (barrier.getGate().getGateType().equals(Gate.GateType.IN) ? "выезда" : "въезда/выезда")) + " не получилась перенести на значение 0 " + (properties.get("carNumber")  != null ? "для номер авто " + properties.get("carNumber") : ""));
                            }
                        }
                    }
                }
            }
            client.close();
        }
        return result;
    }

    private Boolean closeSnmp(Barrier barrier, Map<String, Object> properties) throws IOException, ParseException, InterruptedException {
        Boolean result = true;
        if(!disableOpen){
            SNMPManager client = new SNMPManager("udp:" + barrier.getIp() + "/161", barrier.getPassword(), barrier.getSnmpVersion());
            client.start();
            if(!"1".equals(client.getCurrentValue(barrier.getCloseOid()))){
                Boolean isCloseValueChanged =client.changeValue(barrier.getCloseOid(), 1);
                if(!isCloseValueChanged){
                    for(int i=0; i<3; i++){
                        isCloseValueChanged =client.changeValue(barrier.getCloseOid(), 1);
                    }
                    if(!isCloseValueChanged){
                        result = false;
                        eventLogService.createEventLog(Barrier.class.getSimpleName(), barrier.getId(), properties, "Контроллер шлагбаума " + (barrier.getGate().getGateType().equals(Gate.GateType.IN) ? "въезда" : (barrier.getGate().getGateType().equals(Gate.GateType.IN) ? "выезда":"въезда/выезда")) +" не получилась перенести на значение 1 чтобы закрыть " + (properties.get("carNumber")  != null ? "для номер авто " + properties.get("carNumber") : ""));
                    }
                } else {
                    Thread.sleep(1000);
                    if(!"0".equals(client.getCurrentValue(barrier.getCloseOid()))){
                        Boolean isReturnValueChanged =client.changeValue(barrier.getCloseOid(), 0);
                        if(!isReturnValueChanged) {
                            for (int i = 0; i < 3; i++) {
                                isReturnValueChanged = client.changeValue(barrier.getCloseOid(), 0);
                            }
                            if (!isReturnValueChanged) {
                                eventLogService.createEventLog(Barrier.class.getSimpleName(), barrier.getId(), properties, "Контроллер шлагбаума " + (barrier.getGate().getGateType().equals(Gate.GateType.IN) ? "въезда" : (barrier.getGate().getGateType().equals(Gate.GateType.IN) ? "выезда" : "въезда/выезда")) + " не получилась перенести на значение 0 " + (properties.get("carNumber")  != null ? "для номер авто " + properties.get("carNumber") : ""));
                            }
                        }
                    }
                }
            }
            client.close();
        }
        return result;
    }
}