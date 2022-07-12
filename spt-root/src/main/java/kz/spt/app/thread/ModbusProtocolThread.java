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
    private Map<Integer, Boolean> inputValues = new HashMap<>();
    private boolean[] outputValues = new boolean[8];
    private Map<Integer, Boolean> writeValues = new HashMap<>();
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

                writeValues.forEach((key, m) -> {
                    if(m){
                        new_values[0] += (int) Math.pow(2, key);
                    }
                });

                try {
                    modbusMaster.writeMultipleRegisters(slaveId, offset, new_values);
                    calculateChangedRegisters(new_values[0]);
                    Thread.sleep(100);
                } catch (ModbusProtocolException | ModbusNumberException | ModbusIOException | InterruptedException e) {
                    log.info(barrierStatusDto.ip + " write Registers : " + new_values[0] + " error. message: " + e.getMessage());
                }

                try {
                    outputValues = modbusMaster.readDiscreteInputs(slaveId, offset, 8);
                    Thread.sleep(100);
                } catch (ModbusProtocolException | ModbusNumberException | ModbusIOException | InterruptedException e) {
                    log.info(barrierStatusDto.ip + " read Registers error. message: " + e.getMessage());
                }
            } else{
                try {
                    modbusMaster.connect();
                } catch (Exception e) {
                    log.info(barrierStatusDto.ip + " modbus connect error: " + e.getMessage());
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
        modbusMaster.setResponseTimeout(5000); // 2 seconds timeout
        Modbus.setAutoIncrementTransactionId(true);
    }

    private void calculateChangedRegisters(int total){
        int value = 16;
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
}
