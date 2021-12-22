package kz.spt.app.service.impl;

import kz.spt.app.job.CarSimulateJob;
import kz.spt.app.job.StatusCheckJob;
import kz.spt.app.model.dto.BarrierStatusDto;
import kz.spt.app.model.dto.SensorStatusDto;
import kz.spt.lib.model.Barrier;
import kz.spt.lib.model.Gate;
import kz.spt.lib.service.EventLogService;
import kz.spt.app.repository.BarrierRepository;
import kz.spt.app.service.BarrierService;
import kz.spt.app.snmp.SNMPManager;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.util.StringUtils;

import java.io.IOException;
import java.text.ParseException;
import java.util.Map;

@Service
@Log
public class BarrierServiceImpl implements BarrierService {

    private Boolean disableOpen;
    private final BarrierRepository barrierRepository;
    private final EventLogService eventLogService;
    private final String SENSOR_ON = "1";
    private final String SENSOR_OFF = "0";

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
        StatusCheckJob.emptyGlobalGateDtos();
    }

    @Override
    public void deleteBarrier(Barrier barrier) {
        barrierRepository.delete(barrier);
        StatusCheckJob.emptyGlobalGateDtos();
    }

    @Override
    public int getSensorStatus(SensorStatusDto sensor) throws IOException, ParseException {
        if(!disableOpen){
            if(Barrier.BarrierType.SNMP.equals(sensor.type)){
                if(sensor.oid !=null && sensor.password != null && sensor.ip!= null && sensor.snmpVersion!= null){
                    SNMPManager client = new SNMPManager("udp:" + sensor.ip+ "/161", sensor.password, sensor.snmpVersion);
                    client.start();
                    int carDetected = Integer.valueOf(client.getCurrentValue(sensor.oid));
                    client.close();
                    return carDetected;
                } else {
                    return -1;
                }
            } else {
                return -1;
            }
        } else { // for test
            if("loop".equals(sensor.sensorName) && CarSimulateJob.magneticLoopMap.containsKey(sensor.barrierId)){
                return CarSimulateJob.magneticLoopMap.get(sensor.barrierId) ? 1 : 0;
            }
            if("photoElement".equals(sensor.sensorName) && CarSimulateJob.photoElementLoopMap.containsKey(sensor.barrierId)){
                return CarSimulateJob.photoElementLoopMap.get(sensor.barrierId) ? 1 : 0;
            }
            return -1;
        }
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
            SNMPManager barrierClient = new SNMPManager("udp:" + barrier.getIp() + "/161", barrier.getPassword(), barrier.getSnmpVersion());
            barrierClient.start();
            String currentValue = barrierClient.getCurrentValue(barrier.getOpenOid());
            log.info("currentValue of barrier open channel: " + currentValue);
            if(!"1".equals(currentValue)){
                Boolean isOpenValueChanged = barrierClient.changeValue(barrier.getOpenOid(), 1);
                log.info("isOpenValueChanged: " + isOpenValueChanged);
                if(!isOpenValueChanged){
                    for(int i=0; i < 3; i++){
                        isOpenValueChanged = barrierClient.changeValue(barrier.getOpenOid(), 1);
                        log.info("isOpenValueChanged 2: " + isOpenValueChanged);
                        if(isOpenValueChanged){
                            break;
                        }
                    }
                    if(!isOpenValueChanged){
                        result = false;
                        properties.put("type", EventLogService.EventType.Error);
                        eventLogService.createEventLog(Barrier.class.getSimpleName(), barrier.getId(), properties, "Контроллер шлагбаума " + (barrier.getGate().getGateType().equals(Gate.GateType.IN) ? "въезда" : (barrier.getGate().getGateType().equals(Gate.GateType.IN) ? "выезда":"въезда/выезда")) +" не получилась перенести на значение 1 чтобы открыть " + (properties.get("carNumber")  != null ? "для номер авто " + properties.get("carNumber") : ""));
                    }
                } else {
                    Thread.sleep(1000);
                    String currentValue2 = barrierClient.getCurrentValue(barrier.getOpenOid());
                    log.info("currentValue2: " + currentValue2);
                    if(!"0".equals(currentValue2)){
                        Boolean isReturnValueChanged = barrierClient.changeValue(barrier.getOpenOid(), 0);
                        log.info("isReturnValueChanged: " + isReturnValueChanged);
                        if(!isReturnValueChanged) {
                            for (int i = 0; i < 3; i++) {
                                isReturnValueChanged = barrierClient.changeValue(barrier.getOpenOid(), 0);
                                log.info("isReturnValueChanged: " + isReturnValueChanged);
                                if(isReturnValueChanged){
                                    break;
                                }
                            }
                            if (!isReturnValueChanged) {
                                properties.put("type", EventLogService.EventType.Error);
                                eventLogService.createEventLog(Barrier.class.getSimpleName(), barrier.getId(), properties, "Контроллер шлагбаума " + (barrier.getGate().getGateType().equals(Gate.GateType.IN) ? "въезда" : (barrier.getGate().getGateType().equals(Gate.GateType.IN) ? "выезда" : "въезда/выезда")) + " не получилась перенести на значение 0 " + (properties.get("carNumber")  != null ? "для номер авто " + properties.get("carNumber") : ""));
                            }
                        }
                    }
                }
            } else {
                log.info("barrier connection started");
            }
            barrierClient.close();
        }
        return result;
    }

    private Boolean closeSnmp(Barrier barrier, Map<String, Object> properties) throws IOException, ParseException, InterruptedException {
        Boolean result = true;
        log.info("method closeSnmp started");

        log.info("disableOpen: "  +  disableOpen);
        if(!disableOpen){
            SNMPManager client = new SNMPManager("udp:" + barrier.getIp() + "/161", barrier.getPassword(), barrier.getSnmpVersion());
            client.start();

            String test = client.getCurrentValue(barrier.getCloseOid());
            if(!"1".equals(test)){
                Boolean isCloseValueChanged =client.changeValue(barrier.getCloseOid(), 1);
                log.info("isCloseValueChanged: " + isCloseValueChanged);
                if(!isCloseValueChanged){
                    for(int i=0; i<3; i++){
                        isCloseValueChanged =client.changeValue(barrier.getCloseOid(), 1);
                        log.info("isCloseValueChanged 2: " + isCloseValueChanged);
                        if(isCloseValueChanged){
                            break;
                        }
                    }
                    if(!isCloseValueChanged){
                        result = false;
                        properties.put("type", EventLogService.EventType.Error);
                        eventLogService.createEventLog(Barrier.class.getSimpleName(), barrier.getId(), properties, "Контроллер шлагбаума " + (barrier.getGate().getGateType().equals(Gate.GateType.IN) ? "въезда" : (barrier.getGate().getGateType().equals(Gate.GateType.IN) ? "выезда":"въезда/выезда")) +" не получилась перенести на значение 1 чтобы закрыть " + (properties.get("carNumber")  != null ? "для номер авто " + properties.get("carNumber") : ""));
                    }
                } else {
                    Thread.sleep(1000);
                    if(!"0".equals(client.getCurrentValue(barrier.getCloseOid()))){
                        Boolean isReturnValueChanged =client.changeValue(barrier.getCloseOid(), 0);
                        log.info("isReturnValueChanged: " + isReturnValueChanged);
                        if(!isReturnValueChanged) {
                            for (int i = 0; i < 3; i++) {
                                isReturnValueChanged = client.changeValue(barrier.getCloseOid(), 0);
                                log.info("isReturnValueChanged 2: " + isReturnValueChanged);
                                if(isReturnValueChanged){
                                    break;
                                }
                            }
                            if (!isReturnValueChanged) {
                                properties.put("type", EventLogService.EventType.Error);
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

    public Boolean openBarrier(Gate.GateType gateType, String carNumber, BarrierStatusDto barrier) throws IOException, ParseException, InterruptedException {
        if(Barrier.BarrierType.SNMP.equals(barrier.type)){
            return snmpChangeValue(gateType, carNumber, barrier, barrier.openOid);
        }
        return true;
    }

    public Boolean closeBarrier(Gate.GateType gateType, String carNumber, BarrierStatusDto barrier) throws IOException, ParseException, InterruptedException {
        if(Barrier.BarrierType.SNMP.equals(barrier.type)){
            return snmpChangeValue(gateType, carNumber, barrier, barrier.closeOid);
        }
        return true;
    }

    private Boolean snmpChangeValue(Gate.GateType gateType, String carNumber, BarrierStatusDto barrier, String oid) throws IOException, InterruptedException, ParseException {
        Boolean result = true;
        if(!disableOpen) {
            SNMPManager barrierClient = new SNMPManager("udp:" + barrier.ip + "/161", barrier.password, barrier.snmpVersion);
            barrierClient.start();
            String currentValue = barrierClient.getCurrentValue(barrier.openOid);
            if (SENSOR_OFF.equals(currentValue)) {
                Boolean isOpenValueChanged = barrierClient.changeValue(oid, Integer.valueOf(SENSOR_ON));
                if (!isOpenValueChanged) {
                    for (int i = 0; i < 3; i++) {
                        isOpenValueChanged = barrierClient.changeValue(oid, Integer.valueOf(SENSOR_ON));
                        if (isOpenValueChanged) {
                            break;
                        }
                    }
                    if (!isOpenValueChanged) {
                        result = false;
                        eventLogService.createEventLog(Barrier.class.getSimpleName(), barrier.id, null, "Контроллер шлагбаума " + (Gate.GateType.IN.equals(gateType) ? "въезда" : (Gate.GateType.OUT.equals(gateType) ? "выезда" : "въезда/выезда")) + " не получилась перенести на значение 1 чтобы открыть " + (carNumber != null ? "для номер авто " + carNumber : ""));
                    }
                } else {
                    Thread.sleep(1000);
                    String currentValue2 = barrierClient.getCurrentValue(oid);
                    if (SENSOR_ON.equals(currentValue2)) {
                        Boolean isReturnValueChanged = barrierClient.changeValue(oid, Integer.valueOf(SENSOR_OFF));
                        if (!isReturnValueChanged) {
                            for (int i = 0; i < 3; i++) {
                                isReturnValueChanged = barrierClient.changeValue(oid, Integer.valueOf(SENSOR_OFF));
                                if (isReturnValueChanged) {
                                    break;
                                }
                            }
                            if (!isReturnValueChanged) {
                                eventLogService.createEventLog(Barrier.class.getSimpleName(), barrier.id, null, "Контроллер шлагбаума " + (Gate.GateType.IN.equals(gateType) ? "въезда" : (Gate.GateType.OUT.equals(gateType) ? "выезда" : "въезда/выезда")) + " не получилась перенести на значение 0 " + (carNumber != null ? "для номер авто " + carNumber : ""));
                            }
                        }
                    }
                }
            }
            barrierClient.close();
        }
        return result;
    }
}