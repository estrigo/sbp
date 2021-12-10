package kz.spt.app.model.dto;

import kz.spt.lib.model.Barrier;

public class SensorStatusDto {

    public Long barrierId;
    public String ip;
    public String password;
    public String oid;
    public Integer snmpVersion;
    public Barrier.BarrierType type;
    public String sensorName; // For test only
}
