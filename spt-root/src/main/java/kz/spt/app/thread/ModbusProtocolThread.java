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
import kz.spt.app.model.dto.BarrierStatusDto;
import kz.spt.app.service.impl.BarrierServiceImpl;
import lombok.extern.java.Log;
import org.snmp4j.log.LogLevel;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Observer;

@Log
public class ModbusProtocolThread extends Thread  {

    private final BarrierStatusDto barrierStatusDto;
    private ModbusMaster modbusMaster = null;
    private Map<Integer, Boolean> readValues = new HashMap<>();
    private Map<Integer, Boolean> writeValues = new HashMap<>();
    private int maxReadValue;
    private Boolean running = true;

    public ModbusProtocolThread(BarrierStatusDto barrierStatusDto) {
        this.barrierStatusDto  = barrierStatusDto;
        getConnectedInstance(barrierStatusDto.ip);
    }

    public void addModbusRegisters(BarrierStatusDto barrierStatusDto){
        if(barrierStatusDto.modbusCloseRegister != null){
            writeValues.put(barrierStatusDto.modbusCloseRegister-1, false);
            readValues.put(barrierStatusDto.modbusCloseRegister-1, false);
        }
        if(barrierStatusDto.modbusOpenRegister != null){
            writeValues.put(barrierStatusDto.modbusOpenRegister-1, false);
            readValues.put(barrierStatusDto.modbusOpenRegister-1, false);
        }
        if(barrierStatusDto.loopModbusRegister != null){
            readValues.put(barrierStatusDto.loopModbusRegister-1, false);
        }
        if(barrierStatusDto.photoElementModbusRegister != null){
            readValues.put(barrierStatusDto.photoElementModbusRegister-1, false);
        }
        findMaxReadValue();
    }

    public Boolean getReadValue(Integer register){
        return readValues.get(register);
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

                writeValues.forEach((key, m) -> {
                    if(m){
                        new_values[0] += (int) Math.pow(2, key);
                    }
                });

                try {
                    modbusMaster.writeMultipleRegisters(slaveId, offset, new_values);
//                   int[] values = modbusMaster.readHoldingRegisters(slaveId, offset, 1);

                    calculateChangedRegisters(new_values[0], this.maxReadValue);
                    Thread.sleep(250);
                } catch (ModbusProtocolException | ModbusNumberException | ModbusIOException | InterruptedException e) {
                    e.printStackTrace();
                }
            } else{
                log.info(barrierStatusDto.ip + " modbus is not connected");
                try {
                    modbusMaster.connect();
                } catch (Exception e) {
                    log.info(barrierStatusDto.ip + " modbus connect error: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    private void getConnectedInstance(String ip) {
        TcpParameters tcpParameters = new TcpParameters();
        try {
            tcpParameters.setHost(InetAddress.getByName(ip));
        } catch (UnknownHostException e) {
            log.info("UnknownHostException : " + e.getMessage());
        }
        tcpParameters.setKeepAlive(true);
        tcpParameters.setPort(Modbus.TCP_PORT);

        modbusMaster = ModbusMasterFactory.createModbusMasterTCP(tcpParameters);
        modbusMaster.setResponseTimeout(5000); // 2 seconds timeout
        Modbus.setAutoIncrementTransactionId(true);
    }

    private void calculateChangedRegisters(int total, int maxReadValue){
        int value = maxReadValue;
        if(total > 0){
            while (total > 0 && value >= 0){
                if(total >= (int) Math.pow(2, value)){
                    total -= (int) Math.pow(2, value);
                    readValues.put(value, true);
                } else if(readValues.containsKey(value)) {
                    readValues.put(value, false);
                }
                value--;
            }
        } else {
            readValues.forEach((key, m) -> {
                readValues.put(key, false);
            });
        }
    }

    private void findMaxReadValue(){
        readValues.forEach((key, m) -> {
            if(key > maxReadValue){
                maxReadValue = key;
            }
        });
    }

    public void stopModbus(){
        try {
            running = false;
            modbusMaster.disconnect();
        } catch (ModbusIOException e) {
            e.printStackTrace();
        }
    }
}
