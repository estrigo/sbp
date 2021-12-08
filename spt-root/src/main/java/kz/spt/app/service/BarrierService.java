package kz.spt.app.service;


import kz.spt.app.model.dto.BarrierStatusDto;
import kz.spt.app.model.dto.SensorStatusDto;
import kz.spt.lib.model.Barrier;
import kz.spt.lib.model.Gate;

import java.io.IOException;
import java.text.ParseException;
import java.util.Map;

public interface BarrierService {

    Barrier getBarrierById(Long id);

    void saveBarrier(Barrier barrier);

    void deleteBarrier(Barrier barrier);

    Boolean openBarrier(Barrier barrier, Map<String, Object> properties) throws IOException, ParseException, InterruptedException;

    Boolean closeBarrier(Barrier barrier, Map<String, Object> properties) throws IOException, ParseException, InterruptedException;

    Boolean checkCarPassed(Barrier barrier, Map<String, Object> properties) throws IOException, ParseException, InterruptedException;

    int getSensorStatus(SensorStatusDto sensor) throws IOException, ParseException;

    Boolean openBarrier(Gate.GateType gateType, String carNumber, BarrierStatusDto barrier) throws IOException, ParseException, InterruptedException;

    Boolean closeBarrier(Gate.GateType gateType, String carNumber, BarrierStatusDto barrier) throws IOException, ParseException, InterruptedException;

}
