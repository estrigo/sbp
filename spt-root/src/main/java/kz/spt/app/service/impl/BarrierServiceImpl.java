package kz.spt.app.service.impl;

import com.intelligt.modbus.jlibmodbus.Modbus;
import com.intelligt.modbus.jlibmodbus.master.ModbusMaster;
import com.intelligt.modbus.jlibmodbus.master.ModbusMasterRTU;
import com.intelligt.modbus.jlibmodbus.serial.SerialParameters;
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

import java.io.IOException;
import java.text.ParseException;
import java.util.Map;

@Service
@Log
public class BarrierServiceImpl implements BarrierService {

    private Boolean disableOpen;
    private final BarrierRepository barrierRepository;
    private final EventLogService eventLogService;
    private final String BARRIER_ON = "1";
    private final String BARRIER_OFF = "0";

    private enum Command {
        Open, Close
    }

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
                    String carDetectedString = client.getCurrentValue(sensor.oid);
                    int carDetected = -1;
                    if(carDetectedString != null){
                        carDetected = Integer.valueOf(carDetectedString);
                    }
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
                return CarSimulateJob.magneticLoopMap.get(sensor.barrierId) ? 0 : 1;
            }
            if("photoElement".equals(sensor.sensorName) && CarSimulateJob.photoElementLoopMap.containsKey(sensor.barrierId)){
                return CarSimulateJob.photoElementLoopMap.get(sensor.barrierId) ? 0 : 1;
            }
            return -1;
        }
    }

    @Override
    public Boolean openBarrier(Barrier barrier, Map<String, Object> properties) throws IOException, ParseException, InterruptedException {
        if(Barrier.BarrierType.SNMP.equals(barrier.getBarrierType())){
            return snmpChangeValue(barrier.getGate().getGateType(), (String) properties.get("carNumber"), BarrierStatusDto.fromBarrier(barrier), Command.Open);
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
            return snmpChangeValue(barrier.getGate().getGateType(), (String) properties.get("carNumber"), BarrierStatusDto.fromBarrier(barrier), Command.Close);
        } else if(Barrier.BarrierType.MODBUS.equals(barrier.getBarrierType())) {
            // TODO: close modbus type
            return false;
        } else {
            throw new RuntimeException("Unable close barrier: unknown barrier type");
        }
    }

    public Boolean openBarrier(Gate.GateType gateType, String carNumber, BarrierStatusDto barrier) throws IOException, ParseException, InterruptedException {
        if(Barrier.BarrierType.SNMP.equals(barrier.type)){
            return snmpChangeValue(gateType, carNumber, barrier, Command.Open);
        } else if(Barrier.BarrierType.MODBUS.equals(barrier.type)) {
            return modbusChangeValue(gateType, carNumber, barrier, Command.Open);
        }
        return true;
    }

    public Boolean closeBarrier(Gate.GateType gateType, String carNumber, BarrierStatusDto barrier) throws IOException, ParseException, InterruptedException {
        if(Barrier.BarrierType.SNMP.equals(barrier.type)){
            return snmpChangeValue(gateType, carNumber, barrier, Command.Close);
        }
        return true;
    }

    private Boolean snmpChangeValue(Gate.GateType gateType, String carNumber, BarrierStatusDto barrier, Command command) throws IOException, InterruptedException, ParseException {
        Boolean result = true;
        if(!disableOpen) {
            SNMPManager barrierClient = new SNMPManager("udp:" + barrier.ip + "/161", barrier.password, barrier.snmpVersion);
            barrierClient.start();

            if(Command.Close.equals(command) && Barrier.SensorsType.MANUAL.equals(barrier.sensorsType)){
                String openValue = barrierClient.getCurrentValue(barrier.openOid);
                if (BARRIER_ON.equals(openValue)) {
                    Boolean changed = barrierClient.changeValue(barrier.openOid, Integer.valueOf(BARRIER_OFF));
                    if (!changed) {
                        for (int i = 0; i < 3; i++) {
                            changed = barrierClient.changeValue(barrier.openOid, Integer.valueOf(BARRIER_OFF));
                            if (changed) {
                                break;
                            }
                        }
                        if (!changed) {
                            result = false;
                            eventLogService.createEventLog(Barrier.class.getSimpleName(), barrier.id, null, "Контроллер шлагбаума " + (Gate.GateType.IN.equals(gateType) ? "въезда" : (Gate.GateType.OUT.equals(gateType) ? "выезда" : "въезда/выезда")) + " не получилась перенести на значение 0 для остановки удержания открытия" + (carNumber != null ? " для номер авто " + carNumber : ""), "Controller gate " + (Gate.GateType.IN.equals(gateType) ? "enter" : (Gate.GateType.OUT.equals(gateType) ? "enter" : "enter/exit")) + " couldn't change to 0 for terminating opening process" + (carNumber != null ? " for car number " + carNumber : ""));
                        }
                    }
                }
            }

            String oid = Command.Open.equals(command) ? barrier.openOid : barrier.closeOid;
            String currentValue = barrierClient.getCurrentValue(oid);

            if (BARRIER_OFF.equals(currentValue)) {
                Boolean isOpenValueChanged = barrierClient.changeValue(oid, Integer.valueOf(BARRIER_ON));
                log.info(" isOpenValueChanged: " + isOpenValueChanged);
                if (!isOpenValueChanged) {
                    for (int i = 0; i < 3; i++) {
                        isOpenValueChanged = barrierClient.changeValue(oid, Integer.valueOf(BARRIER_ON));
                        if (isOpenValueChanged) {
                            break;
                        }
                    }
                    if (!isOpenValueChanged) {
                        result = false;
                        eventLogService.createEventLog(Barrier.class.getSimpleName(), barrier.id, null, "Контроллер шлагбаума " + (Gate.GateType.IN.equals(gateType) ? "въезда" : (Gate.GateType.OUT.equals(gateType) ? "выезда" : "въезда/выезда")) + " не получилась перенести на значение 1 чтобы открыть" + (carNumber != null ? " для номер авто " + carNumber : ""), "Controller for gate " + (Gate.GateType.IN.equals(gateType) ? "enter" : (Gate.GateType.OUT.equals(gateType) ? "exit" : "enter/exit")) + " couldn't change to 1 for opening " + (carNumber != null ? " for car number " + carNumber : ""));
                    }
                }
                if((Command.Close.equals(command) || Barrier.SensorsType.AUTOMATIC.equals(barrier.sensorsType)) && isOpenValueChanged) {
                    Thread.sleep(500);
                    String currentValue2 = barrierClient.getCurrentValue(oid);
                    if (BARRIER_ON.equals(currentValue2)) {
                        Boolean isReturnValueChanged = barrierClient.changeValue(oid, Integer.valueOf(BARRIER_OFF));
                        if (!isReturnValueChanged) {
                            for (int i = 0; i < 3; i++) {
                                isReturnValueChanged = barrierClient.changeValue(oid, Integer.valueOf(BARRIER_OFF));
                                if (isReturnValueChanged) {
                                    break;
                                }
                            }
                            if (!isReturnValueChanged) {
                                eventLogService.createEventLog(Barrier.class.getSimpleName(), barrier.id, null, "Контроллер шлагбаума " + (Gate.GateType.IN.equals(gateType) ? "въезда" : (Gate.GateType.OUT.equals(gateType) ? "выезда" : "въезда/выезда")) + " не получилась перенести на значение 0 для остановки удержания закрытия " + (carNumber != null ? " для номер авто " + carNumber : ""), "Controller gate " + (Gate.GateType.IN.equals(gateType) ? "enter" : (Gate.GateType.OUT.equals(gateType) ? "enter" : "enter/exit")) + " couldn't change to 0 for terminating opening process " + (carNumber != null ? " for car number " + carNumber : ""));
                            }
                        }
                    }
                }
            } else if(Command.Close.equals(command) || Barrier.SensorsType.AUTOMATIC.equals(barrier.sensorsType)) {
                Boolean isReturnValueChanged = barrierClient.changeValue(oid, Integer.valueOf(BARRIER_OFF));
                if (!isReturnValueChanged) {
                    for (int i = 0; i < 3; i++) {
                        isReturnValueChanged = barrierClient.changeValue(oid, Integer.valueOf(BARRIER_OFF));
                        if (isReturnValueChanged) {
                            break;
                        }
                    }
                    if (!isReturnValueChanged) {
                        eventLogService.createEventLog(Barrier.class.getSimpleName(), barrier.id, null, "Контроллер шлагбаума " + (Gate.GateType.IN.equals(gateType) ? "въезда" : (Gate.GateType.OUT.equals(gateType) ? "выезда" : "въезда/выезда")) + " не получилась перенести на значение 0 для остановки удержания закрытия " + (carNumber != null ? " для номер авто " + carNumber : ""), "Controller gate " + (Gate.GateType.IN.equals(gateType) ? "enter" : (Gate.GateType.OUT.equals(gateType) ? "enter" : "enter/exit")) + " couldn't change to 0 for terminating opening process " + (carNumber != null ? " for car number " + carNumber : ""));
                    }
                }
            }
            barrierClient.close();
        }
        return result;
    }

    private Boolean modbusChangeValue(Gate.GateType gateType, String carNumber, BarrierStatusDto barrier, Command command) {
        Boolean result = true;
        if(!disableOpen) {
            SerialParameters parameters = new SerialParameters();
//            parameters.set
//            ModbusMaster modbusMaster = new ModbusMasterRTU(parameters);
//            modbusMaster.connect();
        }
        return true;
    }
}