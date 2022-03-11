package kz.spt.app.service.impl;

import com.intelligt.modbus.jlibmodbus.Modbus;
import com.intelligt.modbus.jlibmodbus.exception.ModbusIOException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusNumberException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusProtocolException;
import com.intelligt.modbus.jlibmodbus.master.ModbusMaster;
import com.intelligt.modbus.jlibmodbus.master.ModbusMasterFactory;
import com.intelligt.modbus.jlibmodbus.tcp.TcpParameters;
import kz.spt.app.job.CarSimulateJob;
import kz.spt.app.job.StatusCheckJob;
import kz.spt.app.model.dto.BarrierStatusDto;
import kz.spt.app.model.dto.CameraStatusDto;
import kz.spt.app.model.dto.GateStatusDto;
import kz.spt.app.model.dto.SensorStatusDto;
import kz.spt.app.repository.BarrierRepository;
import kz.spt.app.service.BarrierService;
import kz.spt.app.snmp.SNMPManager;
import kz.spt.lib.model.Barrier;
import kz.spt.lib.model.Camera;
import kz.spt.lib.model.Gate;
import kz.spt.lib.model.dto.jetson.JetsonResponse;
import kz.spt.lib.service.EventLogService;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetAddress;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

@Service
@Log
public class BarrierServiceImpl implements BarrierService {

    private Boolean disableOpen;
    private final BarrierRepository barrierRepository;
    private final EventLogService eventLogService;
    private final String BARRIER_ON = "1";
    private final String BARRIER_OFF = "0";

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
                        if (sensor.defaultValue == null || sensor.defaultValue == 0) {
                            carDetected = Integer.valueOf(carDetectedString);
                        } else if (sensor.defaultValue == 1) {
                            carDetected = Integer.valueOf(carDetectedString) == 0 ? 1 : 0; // бывает фотоэлементы которые работают наоборот вместе 0 выдают 1, а вместе 1 выдают 0;
                        }
                    }
                    client.close();
                    return carDetected;
                } else {
                    return -1;
                }
            } else if(Barrier.BarrierType.MODBUS.equals(sensor.type)) {
                int result = -1;
                try {
                    TcpParameters tcpParameters = new TcpParameters();

                    //tcp parameters have already set by default as in example
                    tcpParameters.setHost(InetAddress.getByName(sensor.ip));
                    tcpParameters.setKeepAlive(true);
                    tcpParameters.setPort(Modbus.TCP_PORT);

                    ModbusMaster m = ModbusMasterFactory.createModbusMasterTCP(tcpParameters);

                    int slaveId = 1;
                    int offset = sensor.modbusRegister-1;

                    try {
                        if (!m.isConnected()) {
                            m.connect();
                        }
                        boolean[] changedValue = m.readCoils(slaveId, offset, 1);
                        if(changedValue != null && changedValue.length > 0){
                            result = changedValue[0] ? 0 : 1;
                        }
                    } catch (ModbusProtocolException e) {
                        e.printStackTrace();
                    } catch (ModbusNumberException e) {
                        e.printStackTrace();
                    } catch (ModbusIOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            m.disconnect();
                        } catch (ModbusIOException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return result;
            } else {
                return -1;
            }
        } else { // for test
            if("loop".equals(sensor.sensorName) && CarSimulateJob.magneticLoopMap.containsKey(sensor.barrierId)){
                if (sensor.defaultValue == null || sensor.defaultValue == 0) {
                    return CarSimulateJob.magneticLoopMap.get(sensor.barrierId) ? 0 : 1;
                } else if (sensor.defaultValue == 1) {
                    return CarSimulateJob.magneticLoopMap.get(sensor.barrierId) ? 1 : 0;
                }
            }
            if("photoElement".equals(sensor.sensorName) && CarSimulateJob.photoElementLoopMap.containsKey(sensor.barrierId)){
                if (sensor.defaultValue == null || sensor.defaultValue == 0) {
                    return CarSimulateJob.photoElementLoopMap.get(sensor.barrierId) ? 0 : 1;
                } else if (sensor.defaultValue == 1) {
                    return CarSimulateJob.photoElementLoopMap.get(sensor.barrierId) ? 1 : 0;
                }
            }
            return -1;
        }
    }

    @Override
    public Boolean openBarrier(Barrier barrier, Map<String, Object> properties) throws IOException, ParseException, InterruptedException {
        if(!disableOpen) { //  ignore in development
            GateStatusDto gate = new GateStatusDto();
            gate.gateType = barrier.getGate().getGateType();
            gate.gateName = barrier.getGate().getName();
            barrier.getGate().getCameraList().forEach(camera->{
                if(Camera.CameraType.FRONT.equals(camera.getCameraType())){
                    if(gate.frontCamera == null){
                        gate.frontCamera = new CameraStatusDto();
                        gate.frontCamera.id = camera.getId();
                        gate.frontCamera.ip = camera.getIp();
                    } else {
                        gate.frontCamera2 = new CameraStatusDto();
                        gate.frontCamera2.id = camera.getId();
                        gate.frontCamera2.ip = camera.getIp();
                    }
                }
                if(Camera.CameraType.BACK.equals(camera.getCameraType())){
                    gate.backCamera = new CameraStatusDto();
                    gate.backCamera.id = camera.getId();
                }
            });

            if(barrier.getBarrierType() == null){
                eventLogService.createEventLog(Barrier.class.getSimpleName(), barrier.getId(), null, "Для отправки сигнала на шлагбаум нужно настроит тип (SNMP, MODBUS) для " + (Gate.GateType.IN.equals(gate.gateType) ? "въезда" : (Gate.GateType.OUT.equals(gate.gateType) ? "выезда" : "въезда/выезда")) + " " + gate.gateName + " чтобы открыть" + ((String) properties.get("carNumber") != null ? " для номер авто " + (String) properties.get("carNumber") : ""), "To send a signal to the barrier, you need to configure the type (SNMP, MODBUS) for " + (Gate.GateType.IN.equals(gate.gateType) ? "enter" : (Gate.GateType.OUT.equals(gate.gateType) ? "exit" : "enter/exit")) + " " + gate.gateName + " to open" + ((String) properties.get("carNumber") != null ? " for car number " + (String) properties.get("carNumber") : ""));
                return false;
            } else if (Barrier.BarrierType.SNMP.equals(barrier.getBarrierType())) {
                return snmpChangeValue(gate, (String) properties.get("carNumber"), BarrierStatusDto.fromBarrier(barrier), Command.Open);
            } else if (Barrier.BarrierType.MODBUS.equals(barrier.getBarrierType())) {
                return modbusChangeValue(gate, (String) properties.get("carNumber"), BarrierStatusDto.fromBarrier(barrier), Command.Open);
            } else if (Barrier.BarrierType.JETSON.equals(barrier.getBarrierType())) {
                return jetsonChangeValue(gate, (String) properties.get("carNumber"), BarrierStatusDto.fromBarrier(barrier), Command.Open);
            }
        }
        return true;
    }

    @Override
    public Boolean closeBarrier(Barrier barrier, Map<String, Object> properties) throws IOException, ParseException, InterruptedException {
        if(!disableOpen) { //  ignore in development
            GateStatusDto gate = new GateStatusDto();
            gate.gateType = barrier.getGate().getGateType();
            gate.gateName = barrier.getGate().getName();

            if(barrier.getBarrierType() == null) {
                eventLogService.createEventLog(Barrier.class.getSimpleName(), barrier.getId(), null, "Для отправки сигнала на шлагбаум нужно настроит тип (SNMP, MODBUS) для " + (Gate.GateType.IN.equals(gate.gateType) ? "въезда" : (Gate.GateType.OUT.equals(gate.gateType) ? "выезда" : "въезда/выезда")) + " " + gate.gateName + " чтобы закрыть" + ((String) properties.get("carNumber") != null ? " для номер авто " + (String) properties.get("carNumber") : ""), "To send a signal to the barrier, you need to configure the type (SNMP, MODBUS) for " + (Gate.GateType.IN.equals(gate.gateType) ? "enter" : (Gate.GateType.OUT.equals(gate.gateType) ? "exit" : "enter/exit")) + " " + gate.gateName + " to close" + ((String) properties.get("carNumber") != null ? " for car number " + (String) properties.get("carNumber") : ""));
                return false;
            } else if (Barrier.BarrierType.SNMP.equals(barrier.getBarrierType())) {
                return snmpChangeValue(gate, (String) properties.get("carNumber"), BarrierStatusDto.fromBarrier(barrier), Command.Close);
            } else if (Barrier.BarrierType.MODBUS.equals(barrier.getBarrierType())) {
                return modbusChangeValue(gate, (String) properties.get("carNumber"), BarrierStatusDto.fromBarrier(barrier), Command.Close);
            }
        }
        return true;
    }

    public Boolean openBarrier(GateStatusDto gate, String carNumber, BarrierStatusDto barrier) throws IOException, ParseException, InterruptedException {
        if(!disableOpen) { //  ignore in development
            if(barrier.type == null){
                eventLogService.createEventLog(Barrier.class.getSimpleName(), barrier.id, null, "Для отправки сигнала на шлагбаум нужно настроит тип (SNMP, MODBUS) для " + (Gate.GateType.IN.equals(gate.gateType) ? "въезда" : (Gate.GateType.OUT.equals(gate.gateType) ? "выезда" : "въезда/выезда")) + " " + gate.gateName + " чтобы открыть" + (carNumber != null ? " для номер авто " + carNumber : ""), "To send a signal to the barrier, you need to configure the type (SNMP, MODBUS) for " + (Gate.GateType.IN.equals(gate.gateType) ? "enter" : (Gate.GateType.OUT.equals(gate.gateType) ? "exit" : "enter/exit")) + " " + gate.gateName + " to open" + (carNumber != null ? " for car number " + carNumber : ""));
                return false;
            } else if (Barrier.BarrierType.SNMP.equals(barrier.type)) {
                return snmpChangeValue(gate, carNumber, barrier, Command.Open);
            } else if (Barrier.BarrierType.MODBUS.equals(barrier.type)) {
                return modbusChangeValue(gate, carNumber, barrier, Command.Open);
            } else if (Barrier.BarrierType.JETSON.equals(barrier.type)) {
                return jetsonChangeValue(gate, carNumber, barrier, Command.Open);
            }
        }
        return true;
    }

    public Boolean closeBarrier(GateStatusDto gate, String carNumber, BarrierStatusDto barrier) throws IOException, ParseException, InterruptedException {
        if(!disableOpen) {
            if(barrier.type == null) {
                eventLogService.createEventLog(Barrier.class.getSimpleName(), barrier.id, null, "Для отправки сигнала на шлагбаум нужно настроит тип (SNMP, MODBUS) для " + (Gate.GateType.IN.equals(gate.gateType) ? "въезда" : (Gate.GateType.OUT.equals(gate.gateType) ? "выезда" : "въезда/выезда")) + " " + gate.gateName + " чтобы закрыть" + (carNumber != null ? " для номер авто " + carNumber : ""), "To send a signal to the barrier, you need to configure the type (SNMP, MODBUS) for " + (Gate.GateType.IN.equals(gate.gateType) ? "enter" : (Gate.GateType.OUT.equals(gate.gateType) ? "exit" : "enter/exit")) + " " + gate.gateName + " to close" + (carNumber != null ? " for car number " + carNumber : ""));
                return false;
            } else  if (Barrier.BarrierType.SNMP.equals(barrier.type)) {
                return snmpChangeValue(gate, carNumber, barrier, Command.Close);
            } else if (Barrier.BarrierType.MODBUS.equals(barrier.type)) {
                return modbusChangeValue(gate, carNumber, barrier, Command.Close);
            }
        }
        return true;
    }

    private Boolean snmpChangeValue(GateStatusDto gate, String carNumber, BarrierStatusDto barrier, Command command) throws IOException, InterruptedException, ParseException {
        Boolean result = true;

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
                        eventLogService.createEventLog(Barrier.class.getSimpleName(), barrier.id, null, "Контроллер шлагбаума " + (Gate.GateType.IN.equals(gate.gateType) ? "въезда" : (Gate.GateType.OUT.equals(gate.gateType) ? "выезда" : "въезда/выезда")) + " " + gate.gateName + " не получилась перенести на значение 0 для остановки удержания открытия" + (carNumber != null ? " для номер авто " + carNumber : ""), "Controller gate " + (Gate.GateType.IN.equals(gate.gateType) ? "enter" : (Gate.GateType.OUT.equals(gate.gateType) ? "exit" : "enter/exit")) + " " + gate.gateName + " couldn't change to 0 for terminating opening process" + (carNumber != null ? " for car number " + carNumber : ""));
                    }
                }
            }
        }

        String oid = Command.Open.equals(command) ? barrier.openOid : barrier.closeOid;
        String currentValue = barrierClient.getCurrentValue(oid);

        if (BARRIER_OFF.equals(currentValue)) {
            Boolean isOpenValueChanged = barrierClient.changeValue(oid, Integer.valueOf(BARRIER_ON));
            log.info("snmp isOpenValueChanged: " + isOpenValueChanged);
            if (!isOpenValueChanged) {
                for (int i = 0; i < 3; i++) {
                    isOpenValueChanged = barrierClient.changeValue(oid, Integer.valueOf(BARRIER_ON));
                    if (isOpenValueChanged) {
                        break;
                    }
                }
                if (!isOpenValueChanged) {
                    result = false;
                    eventLogService.createEventLog(Barrier.class.getSimpleName(), barrier.id, null, "Контроллер шлагбаума " + (Gate.GateType.IN.equals(gate.gateType) ? "въезда" : (Gate.GateType.OUT.equals(gate.gateType) ? "выезда" : "въезда/выезда")) + " " + gate.gateName + " не получилась перенести на значение 1 чтобы открыть" + (carNumber != null ? " для номер авто " + carNumber : ""), "Controller for gate " + (Gate.GateType.IN.equals(gate.gateType) ? "enter" : (Gate.GateType.OUT.equals(gate.gateType) ? "exit" : "enter/exit")) + " " + gate.gateName + " couldn't change to 1 for opening " + (carNumber != null ? " for car number " + carNumber : ""));
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
                            eventLogService.createEventLog(Barrier.class.getSimpleName(), barrier.id, null, "Контроллер шлагбаума " + (Gate.GateType.IN.equals(gate.gateType) ? "въезда" : (Gate.GateType.OUT.equals(gate.gateType) ? "выезда" : "въезда/выезда")) + " " + gate.gateName + " не получилась перенести на значение 0 для остановки удержания закрытия " + (carNumber != null ? " для номер авто " + carNumber : ""), "Controller gate " + (Gate.GateType.IN.equals(gate.gateType) ? "enter" : (Gate.GateType.OUT.equals(gate.gateType) ? "exit" : "enter/exit")) + " " + gate.gateName + " couldn't change to 0 for terminating opening process " + (carNumber != null ? " for car number " + carNumber : ""));
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
                    eventLogService.createEventLog(Barrier.class.getSimpleName(), barrier.id, null, "Контроллер шлагбаума " + (Gate.GateType.IN.equals(gate.gateType) ? "въезда" : (Gate.GateType.OUT.equals(gate.gateType) ? "выезда" : "въезда/выезда")) + " " + gate.gateName + " не получилась перенести на значение 0 для остановки удержания закрытия " + (carNumber != null ? " для номер авто " + carNumber : ""), "Controller gate " + (Gate.GateType.IN.equals(gate.gateType) ? "enter" : (Gate.GateType.OUT.equals(gate.gateType) ? "exit" : "enter/exit")) + " " + gate.gateName + " couldn't change to 0 for terminating opening process " + (carNumber != null ? " for car number " + carNumber : ""));
                }
            }
        }
        barrierClient.close();

        return result;
    }

    private Boolean modbusChangeValue(GateStatusDto gate, String carNumber, BarrierStatusDto barrier, Command command) {
        Boolean result = true;

        try {
            TcpParameters tcpParameters = new TcpParameters();

            //tcp parameters have already set by default as in example
            tcpParameters.setHost(InetAddress.getByName(barrier.ip));
            tcpParameters.setKeepAlive(true);
            tcpParameters.setPort(Modbus.TCP_PORT);

            ModbusMaster m = ModbusMasterFactory.createModbusMasterTCP(tcpParameters);

            int slaveId = 1;
            int offset = Command.Open.equals(command) ? barrier.modbusOpenRegister-1 : barrier.modbusCloseRegister-1;

            try {
                // since 1.2.8
                if (!m.isConnected()) {
                    m.connect();
                }

                Boolean isOpenValueChanged = false;
                m.writeSingleCoil(slaveId, offset, true);
                boolean[] changedValue = m.readCoils(slaveId, offset, 1);
                if(changedValue != null && changedValue.length > 0 && changedValue[0]){
                    isOpenValueChanged = true;
                }
                log.info("modbus isOpenValueChanged: " + isOpenValueChanged);
                if (!isOpenValueChanged) {
                    for (int i = 0; i < 3; i++) {
                        m.writeSingleCoil(slaveId, offset, true);
                        if(changedValue != null && changedValue.length > 0 && changedValue[0]){
                            isOpenValueChanged = true;
                            break;
                        }
                    }
                    if (!isOpenValueChanged) {
                        result = false;
                        eventLogService.createEventLog(Barrier.class.getSimpleName(), barrier.id, null, "Контроллер шлагбаума " + (Gate.GateType.IN.equals(gate.gateType) ? "въезда" : (Gate.GateType.OUT.equals(gate.gateType) ? "выезда" : "въезда/выезда")) + " " + gate.gateName + " не получилась перенести на значение true чтобы открыть" + (carNumber != null ? " для номер авто " + carNumber : ""), "Controller for gate " + (Gate.GateType.IN.equals(gate.gateType) ? "enter" : (Gate.GateType.OUT.equals(gate.gateType) ? "exit" : "enter/exit")) + " " + gate.gateName + " couldn't change to 1 for opening " + (carNumber != null ? " for car number " + carNumber : ""));
                    }
                } else {
                    Thread.sleep(500);
                    boolean[] currentValue = m.readCoils(slaveId, offset, 1);
                    if(currentValue != null && currentValue.length > 0 && currentValue[0]){
                        m.writeSingleCoil(slaveId, offset, false);
                        currentValue = m.readCoils(slaveId, offset, 1);
                        Boolean isReturnValueChanged = currentValue != null && currentValue.length > 0 && !currentValue[0];
                        if(!isReturnValueChanged){
                            for (int i = 0; i < 3; i++) {
                                m.writeSingleCoil(slaveId, offset, false);
                                isReturnValueChanged = currentValue != null && currentValue.length > 0 && !currentValue[0];
                                if (isReturnValueChanged) {
                                    break;
                                }
                            }
                            if (!isReturnValueChanged) {
                                eventLogService.createEventLog(Barrier.class.getSimpleName(), barrier.id, null, "Контроллер шлагбаума " + (Gate.GateType.IN.equals(gate.gateType) ? "въезда" : (Gate.GateType.OUT.equals(gate.gateType) ? "выезда" : "въезда/выезда")) + " " + gate.gateName + " не получилась перенести на значение 0 для остановки удержания закрытия " + (carNumber != null ? " для номер авто " + carNumber : ""), "Controller gate " + (Gate.GateType.IN.equals(gate.gateType) ? "enter" : (Gate.GateType.OUT.equals(gate.gateType) ? "exit" : "enter/exit")) + " " + gate.gateName + " couldn't change to 0 for terminating opening process " + (carNumber != null ? " for car number " + carNumber : ""));
                            }
                        }
                    }
                }
            } catch (ModbusProtocolException e) {
                e.printStackTrace();
            } catch (ModbusNumberException e) {
                e.printStackTrace();
            } catch (ModbusIOException e) {
                e.printStackTrace();
            } finally {
                try {
                    m.disconnect();
                } catch (ModbusIOException e) {
                    e.printStackTrace();
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    private Boolean jetsonChangeValue(GateStatusDto gate, String carNumber, BarrierStatusDto barrier, Command command) {
        String cameraIp = gate.frontCamera != null ? gate.frontCamera.ip :
                gate.frontCamera2 != null ? gate.frontCamera2.ip :
                        gate.backCamera != null ? gate.backCamera.ip : "";

        if (cameraIp.isEmpty()) {
            eventLogService.createEventLog(Barrier.class.getSimpleName(), barrier.id, null,
                    "Контроллер шлагбаума " + (Gate.GateType.IN.equals(gate.gateType) ? "въезда" : (Gate.GateType.OUT.equals(gate.gateType) ? "выезда" : "въезда/выезда")) + " " + gate.gateName + " не получилась определить камеру чтобы открыть" + (carNumber != null ? " для номер авто " + carNumber : ""),
                    "Controller for gate " + (Gate.GateType.IN.equals(gate.gateType) ? "enter" : (Gate.GateType.OUT.equals(gate.gateType) ? "exit" : "enter/exit")) + " " + gate.gateName + " couldn't find camera for opening " + (carNumber != null ? " for car number " + carNumber : ""));
            return false;
        }

        var restTemplate = new RestTemplateBuilder().build();
        var response = restTemplate.getForObject("http://" + barrier.ip + ":5000" + "/handle?ip_address=" + cameraIp, JetsonResponse.class);

        log.info(response.toString());
        return response.getSuccess();
    }

    private enum Command {
        Open, Close
    }
}