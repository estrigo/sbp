package kz.spt.app.model.dto;

import kz.spt.lib.model.Barrier;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BarrierStatusDto {

    public Long id;
    public Long gateId;
    public Barrier.BarrierType type;
    public Barrier.SensorsType sensorsType;
    public String ip;
    public String password;
    public Integer snmpVersion;
    public String openOid;
    public String closeOid;
    public String modbusDeviceVersion;
    public Integer modbusOpenRegister;
    public Integer modbusCloseRegister;
    public Boolean dontSendZero = false;
    public Integer loopModbusRegister;
    public Integer photoElementModbusRegister;
    public boolean statusCheck;
    public boolean impulseSignal;
    public Integer impulseDelay;

    private String openStatusOid;
    private Integer openStatusDefault;
    private String closeStatusOid;
    private Integer closeStatusDefault;

    public static BarrierStatusDto fromBarrier(Barrier barrier) {
        BarrierStatusDto barrierStatusDto = new BarrierStatusDto();
        barrierStatusDto.id = barrier.getId();
        barrierStatusDto.gateId = barrier.getGate().getId();
        barrierStatusDto.type = barrier.getBarrierType();
        barrierStatusDto.ip = barrier.getIp();
        barrierStatusDto.password = barrier.getPassword();
        barrierStatusDto.snmpVersion = barrier.getSnmpVersion();
        barrierStatusDto.openOid = barrier.getOpenOid();
        barrierStatusDto.closeOid = barrier.getCloseOid();
        barrierStatusDto.sensorsType = barrier.getSensorsType();
        barrierStatusDto.modbusDeviceVersion = barrier.getModbusDeviceVersion();
        barrierStatusDto.modbusOpenRegister = barrier.getModbusOpenRegister();
        barrierStatusDto.modbusCloseRegister = barrier.getModbusCloseRegister();
        barrierStatusDto.dontSendZero = barrier.getDontSendZero() != null ? barrier.getDontSendZero() : false;
        barrierStatusDto.loopModbusRegister = barrier.getLoopModbusRegister();
        barrierStatusDto.photoElementModbusRegister = barrier.getPhotoElementModbusRegister();
        barrierStatusDto.statusCheck = barrier.isStatusCheck();
        barrierStatusDto.impulseSignal = barrier.isImpulseSignal();
        barrierStatusDto.impulseDelay = barrier.getImpulseDelay();
        barrierStatusDto.openStatusOid = barrier.getOpenStatusOid();
        barrierStatusDto.openStatusDefault = barrier.getOpenStatusDefault();
        barrierStatusDto.closeStatusOid = barrier.getCloseStatusOid();
        barrierStatusDto.closeStatusDefault = barrier.getCloseStatusDefault();

        return barrierStatusDto;
    }
}
