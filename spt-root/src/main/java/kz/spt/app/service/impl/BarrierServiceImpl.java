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
    public void deleteBarrier(Barrier barrier) {
        barrierRepository.delete(barrier);
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

    @Override
    public Boolean checkCarPassed(Barrier barrier, Map<String, Object> properties) throws IOException, ParseException, InterruptedException {
        if(!disableOpen){
            if(barrier.getLoopOid() !=null && barrier.getLoopPassword() != null && barrier.getLoopType()!= null && barrier.getLoopSnmpVersion()!= null && barrier.getLoopDefaultValue()!= null){
                SNMPManager loopClient = new SNMPManager("udp:" + barrier.getLoopIp()+ "/161", barrier.getLoopPassword(), barrier.getLoopSnmpVersion());
                loopClient.start();

                boolean carDetected = false;
                long currMillis = System.currentTimeMillis();
                while(!carDetected && System.currentTimeMillis() - currMillis < 10000){ // если больше 10 сек не появлялся значить не заехала машина
                    Thread.sleep(1000);
                    if(!barrier.getLoopDefaultValue().toString().equals(loopClient.getCurrentValue(barrier.getLoopOid()))){
                        carDetected = true;
                    }
                }
                loopClient.close();
                return carDetected;
            } else
                return true;
        }
        return true;
    }

    private Boolean openSnmp(Barrier barrier, Map<String, Object> properties) throws IOException, ParseException, InterruptedException {
        Boolean result = true;
        if(!disableOpen){
            SNMPManager barrierClient = new SNMPManager("udp:" + barrier.getIp() + "/161", barrier.getPassword(), barrier.getSnmpVersion());
            barrierClient.start();
            if(!"1".equals(barrierClient.getCurrentValue(barrier.getOpenOid()))){
                Boolean isOpenValueChanged = barrierClient.changeValue(barrier.getOpenOid(), 1);
                if(!isOpenValueChanged){
                    for(int i=0; i < 3; i++){
                        isOpenValueChanged = barrierClient.changeValue(barrier.getOpenOid(), 1);
                    }
                    if(!isOpenValueChanged){
                        result = false;
                        eventLogService.createEventLog(Barrier.class.getSimpleName(), barrier.getId(), properties, "Контроллер шлагбаума " + (barrier.getGate().getGateType().equals(Gate.GateType.IN) ? "въезда" : (barrier.getGate().getGateType().equals(Gate.GateType.IN) ? "выезда":"въезда/выезда")) +" не получилась перенести на значение 1 чтобы открыть " + (properties.get("carNumber")  != null ? "для номер авто " + properties.get("carNumber") : ""));
                    }
                } else {
                    Thread.sleep(1000);
                    if(!"0".equals(barrierClient.getCurrentValue(barrier.getOpenOid()))){
                        Boolean isReturnValueChanged = barrierClient.changeValue(barrier.getOpenOid(), 0);
                        if(!isReturnValueChanged) {
                            for (int i = 0; i < 3; i++) {
                                isReturnValueChanged = barrierClient.changeValue(barrier.getOpenOid(), 0);
                            }
                            if (!isReturnValueChanged) {
                                eventLogService.createEventLog(Barrier.class.getSimpleName(), barrier.getId(), properties, "Контроллер шлагбаума " + (barrier.getGate().getGateType().equals(Gate.GateType.IN) ? "въезда" : (barrier.getGate().getGateType().equals(Gate.GateType.IN) ? "выезда" : "въезда/выезда")) + " не получилась перенести на значение 0 " + (properties.get("carNumber")  != null ? "для номер авто " + properties.get("carNumber") : ""));
                            }
                        }
                    }
                }
            }
            barrierClient.close();
        }
        return result;
    }

    private Boolean closeSnmp(Barrier barrier, Map<String, Object> properties) throws IOException, ParseException, InterruptedException {
        Boolean result = true;

        if(!disableOpen){
            boolean checkNoObstruction = checkNoObstruction(barrier);

            if(checkNoObstruction){
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
            } else {
                eventLogService.createEventLog(Barrier.class.getSimpleName(), barrier.getId(), properties, "В течение 20 секунд не возможно закрыть шлагбаум " + (barrier.getGate().getGateType().equals(Gate.GateType.IN) ? "въезда" : (barrier.getGate().getGateType().equals(Gate.GateType.IN) ? "выезда" : "въезда/выезда")) + " " + (properties.get("carNumber")  != null ? " после проезда автомобиля с номером " + properties.get("carNumber") : ""));
                result = false;
            }
        }
        return result;
    }

    private Boolean checkNoObstruction(Barrier barrier) throws InterruptedException, IOException, ParseException {
        Boolean carLeftAfterDetect = false;
        Long currMillis = System.currentTimeMillis();
        SNMPManager loopClient = new SNMPManager("udp:" + barrier.getLoopIp()+ "/161", barrier.getLoopPassword(), barrier.getLoopSnmpVersion());
        loopClient.start();

        while(!carLeftAfterDetect && System.currentTimeMillis() - currMillis < 20000){ // если больше 20 сек не уехала машина значить что то случилась
            Thread.sleep(1000);
            if(barrier.getLoopDefaultValue().toString().equals(loopClient.getCurrentValue(barrier.getLoopOid()))){
                carLeftAfterDetect = true;
            }
        }
        loopClient.close();
        return carLeftAfterDetect;
    }
}