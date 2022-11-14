package kz.spt.app.thread;

import com.intelligt.modbus.jlibmodbus.Modbus;
import com.intelligt.modbus.jlibmodbus.exception.ModbusIOException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusNumberException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusProtocolException;
import com.intelligt.modbus.jlibmodbus.master.ModbusMaster;
import com.intelligt.modbus.jlibmodbus.master.ModbusMasterFactory;
import com.intelligt.modbus.jlibmodbus.slave.ModbusSlaveFactory;
import com.intelligt.modbus.jlibmodbus.slave.ModbusSlaveTCP;
import com.intelligt.modbus.jlibmodbus.tcp.TcpParameters;
import com.intelligt.modbus.jlibmodbus.utils.ModbusSlaveTcpObserver;
import com.intelligt.modbus.jlibmodbus.utils.TcpClientInfo;
import kz.spt.app.job.CarSimulateJob;
import kz.spt.app.job.StatusCheckJob;
import kz.spt.app.model.dto.BarrierStatusDto;
import kz.spt.app.model.dto.GateStatusDto;
import kz.spt.app.service.impl.BarrierServiceImpl;
import lombok.extern.java.Log;
import org.snmp4j.log.LogLevel;

import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.*;

@Log
public class ModbusProtocolThread extends Thread  {

    private final BarrierStatusDto barrierStatusDto;
    private ModbusMaster modbusMaster = null;
    private Map<Integer, Boolean> inputValues = new HashMap<>();
    private boolean[] outputValues = new boolean[8];
    private Map<Integer, Boolean> writeValues = new HashMap<>();

    private Map<Integer, Long> closeTimeouts = new HashMap<>();
    private Boolean running = true;

    public ModbusProtocolThread(BarrierStatusDto barrierStatusDto) {
        this.barrierStatusDto  = barrierStatusDto;
        getConnectedInstance(barrierStatusDto.ip);
    }

    public Boolean getOutputValue(Integer register){
        return outputValues[register];
    }

    public Boolean getInputValue(Integer register){
        return inputValues.get(register);
    }


    public void setWriteValue(Integer register, Boolean value){
        writeValues.put(register, value);
    }

    public void run() {
        while (modbusMaster != null && running){
            if(modbusMaster.isConnected()){
                int slaveId = 1;
                int offset = 0;
                if (barrierStatusDto.modbusDeviceVersion != null && "210-301".equals(barrierStatusDto.modbusDeviceVersion)) {
                    offset = 470;
                }
                final int[] new_values = {0};
                final boolean[] new_boolean_values = new boolean[8];

                writeValues.forEach((key, m) -> {
                    if("icpdas".equals(barrierStatusDto.modbusDeviceVersion)){
                        new_boolean_values[key] = m;
                        if("10.66.83.11".equals(barrierStatusDto.ip)){
                            if(m){
                                log.info("setting close value = " + System.currentTimeMillis() + " to key: " + barrierStatusDto.modbusCloseRegister);
                                closeTimeouts.put(barrierStatusDto.modbusCloseRegister-1, System.currentTimeMillis());
                            }
                        }
                    } else {
                        if(m) {
                            new_values[0] += (int) Math.pow(2, key);
                        }
                    }
                });

                if(closeTimeouts != null){
                    if("10.66.83.11".equals(barrierStatusDto.ip)){
                        List<Integer> timeoutsToRemove = new ArrayList<>();
                        closeTimeouts.forEach((key, m) -> {
                            if(System.currentTimeMillis() - m > 5000){
                                try {
                                    modbusMaster.writeSingleCoil(slaveId, key, true);
                                    Thread.sleep(200);
                                    modbusMaster.writeSingleCoil(slaveId, key, false);
                                    timeoutsToRemove.add(key);
                                } catch (ModbusProtocolException | ModbusNumberException | ModbusIOException e) {
                                    e.printStackTrace();
                                } catch (InterruptedException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        });
                        for(Integer val: timeoutsToRemove){
                            closeTimeouts.remove(val);
                        }
                    }
                }

                try {
                    if("icpdas".equals(barrierStatusDto.modbusDeviceVersion)){
                        modbusMaster.writeMultipleCoils(slaveId, offset, new_boolean_values);
                        calculateChangedRegisters(new_boolean_values);
                    } else {
                        modbusMaster.writeMultipleRegisters(slaveId, offset, new_values);
                        calculateChangedRegisters(new_values[0]);
                    }
                    Thread.sleep(100);
                } catch (ModbusProtocolException | ModbusNumberException | ModbusIOException | InterruptedException e) {
                    log.info(barrierStatusDto.ip + " write Registers : " + new_values[0] + " error. message: " + e.getMessage());
                    try {
                        modbusMaster.disconnect();
                        modbusMaster.connect();
                    } catch (ModbusIOException mie) {
                        log.info(barrierStatusDto.ip + " disconnect/connect error. message: " + mie.getMessage());
                    }

                    if(!StatusCheckJob.checkIpExist(barrierStatusDto.ip)){
                        GateStatusDto.removeThread(barrierStatusDto.ip);
                        break;
                    }
                }

                try {
                    outputValues = modbusMaster.readDiscreteInputs(slaveId, offset, 8);
                             //getTestValues(barrierStatusDto); Only for testing
                    Thread.sleep(100);
                } catch (ModbusProtocolException | ModbusNumberException | ModbusIOException | InterruptedException e) {
                    log.info(barrierStatusDto.ip + " read Registers error. message: " + e.getMessage());
                    try {
                        modbusMaster.disconnect();
                        modbusMaster.connect();
                    } catch (ModbusIOException mie) {
                        log.info(barrierStatusDto.ip + " disconnect/connect error. message: " + mie.getMessage());
                    }

                    if(!StatusCheckJob.checkIpExist(barrierStatusDto.ip)){
                        GateStatusDto.removeThread(barrierStatusDto.ip);
                        break;
                    }
                }
            } else{
                try {
                    log.info("Reconnecting to: " + barrierStatusDto.ip);
                    modbusMaster.connect();
                } catch (Exception e) {
                    log.info(barrierStatusDto.ip + " modbus connect error: " + e.getMessage());

                    if(!StatusCheckJob.checkIpExist(barrierStatusDto.ip)){
                        GateStatusDto.removeThread(barrierStatusDto.ip);
                        break;
                    }
                }
            }
        }
    }

    private void getConnectedInstance(String ip) {
        TcpParameters tcpParameters = new TcpParameters();
        try {
            tcpParameters.setHost(InetAddress.getByName(ip));
        } catch (UnknownHostException e) {
            log.info("Barrier ip " + ip +" UnknownHostException : " + e.getMessage());
        }
        tcpParameters.setKeepAlive(true);
        tcpParameters.setPort(Modbus.TCP_PORT);

        modbusMaster = ModbusMasterFactory.createModbusMasterTCP(tcpParameters);
        modbusMaster.setResponseTimeout(3000); // 3 seconds timeout
        Modbus.setAutoIncrementTransactionId(true);
    }

    private void calculateChangedRegisters(int total){
        int value = 18;
        if(total > 0){
            while (total > 0 && value >= 0){
                if(total >= (int) Math.pow(2, value)){
                    total -= (int) Math.pow(2, value);
                    inputValues.put(value, true);
                } else if(inputValues.containsKey(value)) {
                    inputValues.put(value, false);
                }
                value--;
            }
        } else {
            inputValues.forEach((key, m) -> {
                inputValues.put(key, false);
            });
        }
    }

    private void calculateChangedRegisters(boolean[] changedValues){
        for(int i=0; i < changedValues.length; i++){
            inputValues.put(i, changedValues[i]);
        }
    }

    private boolean[] getTestValues(BarrierStatusDto barrierStatusDto){

        boolean loopValue = CarSimulateJob.magneticLoopMap.containsKey(barrierStatusDto.id)  ? CarSimulateJob.magneticLoopMap.get(barrierStatusDto.id) : false;
        boolean pheValue = CarSimulateJob.photoElementLoopMap.containsKey(barrierStatusDto.id) ? CarSimulateJob.photoElementLoopMap.get(barrierStatusDto.id) : false;

        boolean[] values = new boolean[8];
        for (int i = 0; i<values.length; i++){
            if(barrierStatusDto.loopModbusRegister-1 == i){
                values[i] = loopValue;
            } else if(barrierStatusDto.photoElementModbusRegister-1 == i){
                values[i] = pheValue;
            } else {
                values[i] = false;
            }
        }

        return values;
    }
}
