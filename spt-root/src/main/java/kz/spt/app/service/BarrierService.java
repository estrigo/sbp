package kz.spt.app.service;


import com.intelligt.modbus.jlibmodbus.exception.ModbusIOException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusNumberException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusProtocolException;
import kz.spt.app.model.dto.BarrierStatusDto;
import kz.spt.app.model.dto.GateStatusDto;
import kz.spt.app.model.dto.SensorStatusDto;
import kz.spt.app.model.strategy.AbstractStrategy;
import kz.spt.lib.model.Barrier;

import java.io.IOException;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

public interface BarrierService {

    Barrier getBarrierById(Long id);

    void saveBarrier(Barrier barrier);

    void deleteBarrier(Barrier barrier);

    Boolean openBarrier(Barrier barrier, Map<String, Object> properties) throws IOException, ParseException, InterruptedException, ModbusProtocolException, ModbusNumberException, ModbusIOException;

    Boolean openPermanentBarrier(Barrier barrier) throws ModbusProtocolException, IOException, ModbusNumberException, InterruptedException, ModbusIOException, ParseException;

    Boolean closeBarrier(Barrier barrier, Map<String, Object> properties) throws IOException, ParseException, InterruptedException, ModbusProtocolException, ModbusNumberException, ModbusIOException;

    Boolean closePermanentBarrier(Barrier barrier, Map<String, Object> properties) throws IOException, ParseException, InterruptedException, ModbusProtocolException, ModbusNumberException, ModbusIOException;

    int getSensorStatus(SensorStatusDto sensor) throws IOException, ParseException, ModbusIOException, ModbusProtocolException, ModbusNumberException;

    Boolean openBarrier(GateStatusDto gate, String carNumber, BarrierStatusDto barrier) throws IOException, ParseException, InterruptedException, ModbusProtocolException, ModbusNumberException, ModbusIOException;

    Boolean closeBarrier(GateStatusDto gate, String carNumber, BarrierStatusDto barrier) throws IOException, ParseException, InterruptedException, ModbusProtocolException, ModbusNumberException, ModbusIOException;

    Boolean getBarrierStatus(Barrier barrier, Map<String, Object> properties) throws IOException, ParseException, InterruptedException, ModbusProtocolException, ModbusNumberException, ModbusIOException;

    List<Long> getBarrierOpenCameraIdsList();
}
