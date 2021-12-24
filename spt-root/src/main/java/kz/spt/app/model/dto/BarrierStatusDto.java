package kz.spt.app.model.dto;

import kz.spt.lib.model.Barrier;

public class BarrierStatusDto {

    public Long id;
    public Barrier.BarrierType type;
    public Barrier.SensorsType sensorsType;
    public String ip;
    public String password;
    public Integer snmpVersion;
    public String openOid;
    public String closeOid;

    public static BarrierStatusDto fromBarrier(Barrier barrier){
        BarrierStatusDto barrierStatusDto = new BarrierStatusDto();
        barrierStatusDto.id = barrier.getId();
        barrierStatusDto.type = barrier.getBarrierType();
        barrierStatusDto.ip = barrier.getIp();
        barrierStatusDto.password = barrier.getPassword();
        barrierStatusDto.snmpVersion = barrier.getSnmpVersion();
        barrierStatusDto.openOid = barrier.getOpenOid();
        barrierStatusDto.closeOid = barrier.getCloseOid();
        barrierStatusDto.sensorsType = barrier.getSensorsType();
        return barrierStatusDto;
    }
}
