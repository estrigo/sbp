package kz.spt.app.model.dto;

import kz.spt.lib.model.Barrier;

public class BarrierStatusDto {

    public Long id;
    public Barrier.BarrierType type;
    public String ip;
    public String password;
    public Integer snmpVersion;
    public String openOid;
    public String closeOid;
}
