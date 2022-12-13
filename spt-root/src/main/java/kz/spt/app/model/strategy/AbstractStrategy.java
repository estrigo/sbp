package kz.spt.app.model.strategy;

import kz.spt.app.model.dto.GateStatusDto;

import java.io.Serializable;

public abstract class AbstractStrategy implements Serializable {
    public Long gateId;
    public boolean isWaitLoop = false;
    public boolean isWaitPhel = false;
    public GateStatusDto.SensorStatus status = GateStatusDto.SensorStatus.Quit;

    public abstract void success();

    public abstract void carEvent() throws Exception;

    public abstract void error();
}
