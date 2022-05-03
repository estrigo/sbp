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
import kz.spt.app.model.dto.GateStatusDto;
import kz.spt.app.model.dto.SensorStatusDto;
import kz.spt.app.repository.BarrierRepository;
import kz.spt.app.service.BarrierService;
import kz.spt.app.snmp.SNMPManager;
import kz.spt.lib.model.Barrier;
import kz.spt.lib.model.Gate;
import kz.spt.lib.model.dto.jetson.JetsonResponse;
import kz.spt.lib.service.EventLogService;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Log
public class BarrierServiceImpl implements BarrierService {

    public static Map<String, ModbusMaster> modbusMasterMap = new ConcurrentHashMap<>();
    private final BarrierRepository barrierRepository;
    private final EventLogService eventLogService;
    private final String BARRIER_ON = "1";
    private final String BARRIER_OFF = "0";
    private Boolean disableOpen;

    public BarrierServiceImpl(@Value("${barrier.open.disabled}") Boolean disableOpen, BarrierRepository barrierRepository, EventLogService eventLogService) {
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
    public int getSensorStatus(SensorStatusDto sensor) throws IOException, ParseException, ModbusIOException, ModbusProtocolException, ModbusNumberException {
        if (!disableOpen && (sensor.gateNotControlBarrier == null || !sensor.gateNotControlBarrier)) {
            if (Barrier.BarrierType.SNMP.equals(sensor.type)) {
                if (sensor.oid != null && sensor.password != null && sensor.ip != null && sensor.snmpVersion != null) {
                    SNMPManager client = new SNMPManager("udp:" + sensor.ip + "/161", sensor.password, sensor.snmpVersion);
                    client.start();
                    String carDetectedString = client.getCurrentValue(sensor.oid);
                    int carDetected = -1;
                    if (carDetectedString != null) {
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
            } else if (Barrier.BarrierType.MODBUS.equals(sensor.type)) {
                int result = -1;

                ModbusMaster m;
                m = modbusMasterMap.get(sensor.barrierIp);

                int slaveId = 1;

                if (!m.isConnected()) {
                    m.connect();
                }

                if (sensor.modbusDeviceVersion != null && "210-301".equals(sensor.modbusDeviceVersion)) {
                    int offset = 51;
                    int sensor_value = sensor.modbusRegister - 1;
                    int quantity = 1;

                    int[] values = m.readHoldingRegisters(slaveId, offset, quantity);
                    for (int value : values) {
                        result = Integer.toBinaryString(value).charAt(sensor_value) == '1' ? 1 : 0;
                    }
                } else {
                    int offset = sensor.modbusRegister - 1;
                    boolean[] changedValue = m.readCoils(slaveId, offset, 1);
                    if (changedValue != null && changedValue.length > 0) {
                        result = changedValue[0] ? 1 : 0;
                    }
                }
                if (sensor.modbusDeviceVersion != null && "icpdas".equals(sensor.modbusDeviceVersion)) {
                    m.disconnect();
                }
                return result;
            } else if (Barrier.BarrierType.JETSON.equals(sensor.type)) {
                var response = new RestTemplateBuilder().build().getForObject("http://" + sensor.ip + ":9001" + "/sensor_status?pin=" + sensor.oid, JetsonResponse.class);
                log.info(response.toString());
                return response.getState() == 0 ? 1 : 0;
            } else {
                return -1;
            }
        } else { // for test
            if ("loop".equals(sensor.sensorName) && CarSimulateJob.magneticLoopMap.containsKey(sensor.barrierId)) {
                if (sensor.defaultValue == null || sensor.defaultValue == 0) {
                    return CarSimulateJob.magneticLoopMap.get(sensor.barrierId) ? 0 : 1;
                } else if (sensor.defaultValue == 1) {
                    return CarSimulateJob.magneticLoopMap.get(sensor.barrierId) ? 1 : 0;
                }
            }
            if ("photoElement".equals(sensor.sensorName) && CarSimulateJob.photoElementLoopMap.containsKey(sensor.barrierId)) {
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
    public Boolean openBarrier(Barrier barrier, Map<String, Object> properties) throws IOException, ParseException, InterruptedException, ModbusProtocolException, ModbusNumberException, ModbusIOException {
        if (!disableOpen && (barrier.getGate().getNotControlBarrier() == null || !barrier.getGate().getNotControlBarrier())) { //  ignore in development
            GateStatusDto gate = new GateStatusDto();
            gate.gateType = barrier.getGate().getGateType();
            gate.gateName = barrier.getGate().getName();

            if (barrier.getBarrierType() == null) {
                eventLogService.createEventLog(Barrier.class.getSimpleName(), barrier.getId(), null, "Для отправки сигнала на шлагбаум нужно настроит тип (SNMP, MODBUS) для " + (Gate.GateType.IN.equals(gate.gateType) ? "въезда" : (Gate.GateType.OUT.equals(gate.gateType) ? "выезда" : "въезда/выезда")) + " " + gate.gateName + " чтобы открыть" + ((String) properties.get("carNumber") != null ? " для номер авто " + (String) properties.get("carNumber") : ""), "To send a signal to the barrier, you need to configure the type (SNMP, MODBUS) for " + (Gate.GateType.IN.equals(gate.gateType) ? "enter" : (Gate.GateType.OUT.equals(gate.gateType) ? "exit" : "enter/exit")) + " " + gate.gateName + " to open" + ((String) properties.get("carNumber") != null ? " for car number " + (String) properties.get("carNumber") : ""));
                return false;
            } else if (Barrier.BarrierType.SNMP.equals(barrier.getBarrierType())) {
                return snmpChangeValue(gate, (String) properties.get("carNumber"), BarrierStatusDto.fromBarrier(barrier), Command.Open);
            } else if (Barrier.BarrierType.MODBUS.equals(barrier.getBarrierType())) {
                return modbusChangeValue(gate, (String) properties.get("carNumber"), BarrierStatusDto.fromBarrier(barrier), Command.Open);
            } else if (Barrier.BarrierType.JETSON.equals(barrier.getBarrierType())) {
                return jetsonChangeValue(BarrierStatusDto.fromBarrier(barrier), Command.Open);
            }
        }
        return true;
    }

    @Override
    public Boolean closeBarrier(Barrier barrier, Map<String, Object> properties) throws IOException, ParseException, InterruptedException, ModbusProtocolException, ModbusNumberException, ModbusIOException {
        if (!disableOpen && (barrier.getGate().getNotControlBarrier() == null || !barrier.getGate().getNotControlBarrier())) { //  ignore in development
            GateStatusDto gate = new GateStatusDto();
            gate.gateType = barrier.getGate().getGateType();
            gate.gateName = barrier.getGate().getName();

            if (barrier.getBarrierType() == null) {
                eventLogService.createEventLog(Barrier.class.getSimpleName(), barrier.getId(), null, "Для отправки сигнала на шлагбаум нужно настроит тип (SNMP, MODBUS) для " + (Gate.GateType.IN.equals(gate.gateType) ? "въезда" : (Gate.GateType.OUT.equals(gate.gateType) ? "выезда" : "въезда/выезда")) + " " + gate.gateName + " чтобы закрыть" + ((String) properties.get("carNumber") != null ? " для номер авто " + (String) properties.get("carNumber") : ""), "To send a signal to the barrier, you need to configure the type (SNMP, MODBUS) for " + (Gate.GateType.IN.equals(gate.gateType) ? "enter" : (Gate.GateType.OUT.equals(gate.gateType) ? "exit" : "enter/exit")) + " " + gate.gateName + " to close" + ((String) properties.get("carNumber") != null ? " for car number " + (String) properties.get("carNumber") : ""));
                return false;
            } else if (Barrier.BarrierType.SNMP.equals(barrier.getBarrierType())) {
                return snmpChangeValue(gate, (String) properties.get("carNumber"), BarrierStatusDto.fromBarrier(barrier), Command.Close);
            } else if (Barrier.BarrierType.MODBUS.equals(barrier.getBarrierType())) {
                return modbusChangeValue(gate, (String) properties.get("carNumber"), BarrierStatusDto.fromBarrier(barrier), Command.Close);
            } else if (Barrier.BarrierType.JETSON.equals(barrier.getBarrierType())) {
                return jetsonChangeValue(BarrierStatusDto.fromBarrier(barrier), Command.Close);
            }
        }
        return true;
    }

    public Boolean openBarrier(GateStatusDto gate, String carNumber, BarrierStatusDto barrier) throws IOException, ParseException, InterruptedException, ModbusProtocolException, ModbusNumberException, ModbusIOException {
        if (!disableOpen && (gate.notControlBarrier == null || !gate.notControlBarrier)) { //  ignore in development
            if (barrier.type == null) {
                eventLogService.createEventLog(Barrier.class.getSimpleName(), barrier.id, null, "Для отправки сигнала на шлагбаум нужно настроит тип (SNMP, MODBUS) для " + (Gate.GateType.IN.equals(gate.gateType) ? "въезда" : (Gate.GateType.OUT.equals(gate.gateType) ? "выезда" : "въезда/выезда")) + " " + gate.gateName + " чтобы открыть" + (carNumber != null ? " для номер авто " + carNumber : ""), "To send a signal to the barrier, you need to configure the type (SNMP, MODBUS) for " + (Gate.GateType.IN.equals(gate.gateType) ? "enter" : (Gate.GateType.OUT.equals(gate.gateType) ? "exit" : "enter/exit")) + " " + gate.gateName + " to open" + (carNumber != null ? " for car number " + carNumber : ""));
                return false;
            } else if (Barrier.BarrierType.SNMP.equals(barrier.type)) {
                return snmpChangeValue(gate, carNumber, barrier, Command.Open);
            } else if (Barrier.BarrierType.MODBUS.equals(barrier.type)) {
                return modbusChangeValue(gate, carNumber, barrier, Command.Open);
            } else if (Barrier.BarrierType.JETSON.equals(barrier.type)) {
                return jetsonChangeValue(barrier, Command.Open);
            }
        }
        return true;
    }

    public Boolean closeBarrier(GateStatusDto gate, String carNumber, BarrierStatusDto barrier) throws IOException, ParseException, InterruptedException, ModbusProtocolException, ModbusNumberException, ModbusIOException {
        if (!disableOpen && (gate.notControlBarrier == null || !gate.notControlBarrier)) {
            if (barrier.type == null) {
                eventLogService.createEventLog(Barrier.class.getSimpleName(), barrier.id, null, "Для отправки сигнала на шлагбаум нужно настроит тип (SNMP, MODBUS) для " + (Gate.GateType.IN.equals(gate.gateType) ? "въезда" : (Gate.GateType.OUT.equals(gate.gateType) ? "выезда" : "въезда/выезда")) + " " + gate.gateName + " чтобы закрыть" + (carNumber != null ? " для номер авто " + carNumber : ""), "To send a signal to the barrier, you need to configure the type (SNMP, MODBUS) for " + (Gate.GateType.IN.equals(gate.gateType) ? "enter" : (Gate.GateType.OUT.equals(gate.gateType) ? "exit" : "enter/exit")) + " " + gate.gateName + " to close" + (carNumber != null ? " for car number " + carNumber : ""));
                return false;
            } else if (Barrier.BarrierType.SNMP.equals(barrier.type)) {
                return snmpChangeValue(gate, carNumber, barrier, Command.Close);
            } else if (Barrier.BarrierType.MODBUS.equals(barrier.type)) {
                return modbusChangeValue(gate, carNumber, barrier, Command.Close);
            } else if (Barrier.BarrierType.JETSON.equals(barrier.type)) {
                return jetsonChangeValue(barrier, Command.Close);
            }
        }
        return true;
    }

    @Override
    public void addGlobalModbusMaster(Barrier barrier) throws ModbusIOException, UnknownHostException {
        if (!disableOpen && (barrier.getGate().getNotControlBarrier() == null || !barrier.getGate().getNotControlBarrier()) && !modbusMasterMap.containsKey(barrier.getIp())) {
            log.info("connecting global " + barrier.getIp());
            modbusMasterMap.put(barrier.getIp(), getConnectedInstance(barrier.getIp()));
        }
    }

    private ModbusMaster getConnectedInstance(String ip) throws ModbusIOException, UnknownHostException {
        TcpParameters tcpParameters = new TcpParameters();
        tcpParameters.setHost(InetAddress.getByName(ip));
        tcpParameters.setKeepAlive(true);
        tcpParameters.setPort(Modbus.TCP_PORT);

        ModbusMaster m = ModbusMasterFactory.createModbusMasterTCP(tcpParameters);
        m.setResponseTimeout(5000); // 5 seconds timeout

        log.info("Connecting barrier.getIp(): " + ip);

        if (!m.isConnected()) {
            try {
                m.connect();
            } catch (Exception e) {
                log.info("retry connect ip: " + ip + " error: " + e.getMessage());
                modbusRetryConnect(m);
            }
        }
        return m;
    }

    private Boolean snmpChangeValue(GateStatusDto gate, String carNumber, BarrierStatusDto barrier, Command command) throws IOException, InterruptedException, ParseException {
        Boolean result = true;

        SNMPManager barrierClient = new SNMPManager("udp:" + barrier.ip + "/161", barrier.password, barrier.snmpVersion);
        barrierClient.start();

        if (Command.Close.equals(command) && Barrier.SensorsType.MANUAL.equals(barrier.sensorsType)) {
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
            if ((Command.Close.equals(command) || Barrier.SensorsType.AUTOMATIC.equals(barrier.sensorsType)) && isOpenValueChanged) {
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
        } else if (Command.Close.equals(command) || Barrier.SensorsType.AUTOMATIC.equals(barrier.sensorsType)) {
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

    private Boolean modbusChangeValue(GateStatusDto gate, String carNumber, BarrierStatusDto barrier, Command command) throws ModbusIOException, ModbusProtocolException, ModbusNumberException, InterruptedException, UnknownHostException {
        Boolean result = true;

        ModbusMaster m;
        if (!modbusMasterMap.containsKey(barrier.ip) || modbusMasterMap.get(barrier.ip) == null) {
            modbusMasterMap.put(barrier.ip, getConnectedInstance(barrier.ip));
        }
        m = modbusMasterMap.get(barrier.ip);

        int slaveId = 1;
        // since 1.2.8
        if (!m.isConnected()) {
            log.info("barrier.ip: " + barrier.ip + " !m.isConnected()");
            try {
                m.connect();
            } catch (Exception e) {
                log.info("retry connect error: " + e.getMessage());
                modbusRetryConnect(m);
            }
        }
        Boolean isOpenValueChanged = false;

        int offset = Command.Open.equals(command) ? barrier.modbusOpenRegister - 1 : barrier.modbusCloseRegister - 1;
        if (barrier.modbusDeviceVersion != null && "210-301".equals(barrier.modbusDeviceVersion)) {
            offset = 470;
            int new_value = Command.Open.equals(command) ? (int) Math.pow(2, barrier.modbusOpenRegister - 1) : (int) Math.pow(2, barrier.modbusCloseRegister - 1);
            int quantity = 1;

            int[] new_values = new int[1];
            new_values[0] = new_value;
            m.writeMultipleRegisters(slaveId, offset, new_values);
            // at next string we receive ten registers from a slave with id of 1 at offset of 0.
            int[] registerValues = m.readHoldingRegisters(slaveId, offset, quantity);
            for (int value : registerValues) {
                if (value == new_value) {
                    isOpenValueChanged = true;
                }
            }
            log.info("modbus isOpenValueChanged: " + isOpenValueChanged);
            if (!isOpenValueChanged) {
                for (int i = 0; i < 3; i++) {
                    m.writeMultipleRegisters(slaveId, offset, new_values);
                    registerValues = m.readHoldingRegisters(slaveId, offset, quantity);
                    for (int value : registerValues) {
                        if (value == new_value) {
                            isOpenValueChanged = true;
                        }
                    }
                    if (isOpenValueChanged) {
                        break;
                    }
                }
                if (!isOpenValueChanged) {
                    result = false;
                    eventLogService.createEventLog(Barrier.class.getSimpleName(), barrier.id, null, "Контроллер шлагбаума " + (Gate.GateType.IN.equals(gate.gateType) ? "въезда" : (Gate.GateType.OUT.equals(gate.gateType) ? "выезда" : "въезда/выезда")) + " " + gate.gateName + " не получилась перенести на значение true чтобы открыть" + (carNumber != null ? " для номер авто " + carNumber : ""), "Controller for gate " + (Gate.GateType.IN.equals(gate.gateType) ? "enter" : (Gate.GateType.OUT.equals(gate.gateType) ? "exit" : "enter/exit")) + " " + gate.gateName + " couldn't change to 1 for opening " + (carNumber != null ? " for car number " + carNumber : ""));
                }
            } else {
                Thread.sleep(500);
                registerValues = m.readHoldingRegisters(slaveId, offset, quantity);
                Boolean valueKeepHolding = false;
                for (int value : registerValues) {
                    if (value == new_value) {
                        valueKeepHolding = true;
                    }
                }
                if (valueKeepHolding) {
                    new_values[0] = 0; // turn off all holdings
                    m.writeMultipleRegisters(slaveId, offset, new_values);
                    registerValues = m.readHoldingRegisters(slaveId, offset, quantity);
                    Boolean isReturnValueChanged = false;
                    for (int value : registerValues) {
                        isReturnValueChanged = value == 0;
                    }
                    if (!isReturnValueChanged) {
                        for (int i = 0; i < 3; i++) {
                            m.writeMultipleRegisters(slaveId, offset, new_values);
                            registerValues = m.readHoldingRegisters(slaveId, offset, quantity);
                            for (int value : registerValues) {
                                isReturnValueChanged = value == 0;
                            }
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
        } else {
            try {
                m.writeSingleCoil(slaveId, offset, true);
            } catch (Exception e) {
                log.info("retry write error: " + e.getMessage());
                boolean retryResult = modbusRetryWrite(m, slaveId, offset, true);
                if(!retryResult){
                    log.info("modbus isOpenValueChanged: " + retryResult);
                    return retryResult;
                }
            }
            boolean[] changedValue;
            try {
                changedValue = m.readCoils(slaveId, offset, 1);
            } catch (Exception e) {
                changedValue = modbusRetryRead(m, slaveId, offset, 1);
            }
            if (changedValue != null && changedValue.length > 0 && changedValue[0]) {
                isOpenValueChanged = true;
            }
            log.info("modbus isOpenValueChanged: " + isOpenValueChanged);
            if (!isOpenValueChanged) {
                for (int i = 0; i < 3; i++) {
                    try {
                        m.writeSingleCoil(slaveId, offset, true);
                    } catch (Exception e) {
                        log.info("retry error: " + e.getMessage());
                        modbusRetryWrite(m, slaveId, offset, true);
                    }
                    try {
                        changedValue = m.readCoils(slaveId, offset, 1);
                    } catch (Exception e) {
                        changedValue = modbusRetryRead(m, slaveId, offset, 1);
                    }
                    if (changedValue != null && changedValue.length > 0 && changedValue[0]) {
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
                boolean[] currentValue = null;
                try {
                    currentValue = m.readCoils(slaveId, offset, 1);
                } catch (Exception e) {
                    currentValue = modbusRetryRead(m, slaveId, offset, 1);
                }
                if (currentValue != null && currentValue.length > 0 && currentValue[0]) {
                    try {
                        m.writeSingleCoil(slaveId, offset, false);
                    } catch (Exception e) {
                        log.info("retry error: " + e.getMessage());
                        modbusRetryWrite(m, slaveId, offset, false);
                    }
                    try {
                        currentValue = m.readCoils(slaveId, offset, 1);
                    } catch (Exception e) {
                        currentValue = modbusRetryRead(m, slaveId, offset, 1);
                    }
                    Boolean isReturnValueChanged = currentValue != null && currentValue.length > 0 && !currentValue[0];
                    if (!isReturnValueChanged) {
                        for (int i = 0; i < 3; i++) {
                            try {
                                m.writeSingleCoil(slaveId, offset, false);
                            } catch (Exception e) {
                                log.info("retry error: " + e.getMessage());
                                modbusRetryWrite(m, slaveId, offset, false);
                            }
                            try {
                                currentValue = m.readCoils(slaveId, offset, 1);
                            } catch (Exception e) {
                                currentValue = modbusRetryRead(m, slaveId, offset, 1);
                            }
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
        }
        if (barrier.modbusDeviceVersion != null && "icpdas".equals(barrier.modbusDeviceVersion)) {
            m.disconnect();
        }
        return result;
    }

    private Boolean jetsonChangeValue(BarrierStatusDto barrier, Command command) {
        String pin = Command.Open.equals(command) ? barrier.openOid : barrier.closeOid;
        var response = new RestTemplateBuilder().build().getForObject("http://" + barrier.ip + ":9001" + "/gate_action?pin=" + pin, JetsonResponse.class);
        log.info(response.toString());
        return response.getSuccess();
    }

    private boolean[] modbusRetryRead(ModbusMaster m, int slaveId, int offset, int value) {
        Boolean read = false;
        int retryCount = 0;
        boolean[] results = null;
        while (!read && retryCount < 3) {
            try {
                retryCount++;
                log.info("modbus read retry count: " + retryCount);
                m.connect();
                results = m.readCoils(slaveId, offset, value);
                read = true;
            } catch (Exception e) {
                log.info("modbus read retry error: " + e.getMessage());
            }
        }
        return results;
    }

    private Boolean modbusRetryWrite(ModbusMaster m, int slaveId, int offset, boolean value) {
        Boolean wrote = false;
        int retryCount = 0;
        while (!wrote && retryCount < 3) {
            try {
                retryCount++;
                log.info("modbus write retry count: " + retryCount);
                m.connect();
                m.writeSingleCoil(slaveId, offset, value);
                wrote = true;
            } catch (Exception e) {
                log.info("modbus retry error: " + e.getMessage());
            }
        }
        return wrote;
    }

    private Boolean modbusRetryConnect(ModbusMaster m) {
        Boolean connected = false;
        int retryCount = 0;
        while (!connected && retryCount < 3) {
            try {
                retryCount++;
                log.info("modbus connect retry count: " + retryCount);
                m.connect();
                connected = true;
            } catch (Exception e) {
                log.info("modbus connect error: " + e.getMessage());
            }
        }
        return connected;
    }

    private enum Command {
        Open, Close
    }
}