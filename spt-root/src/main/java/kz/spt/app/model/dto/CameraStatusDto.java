package kz.spt.app.model.dto;

import kz.spt.lib.model.dto.CarEventDto;
import lombok.Data;

import java.time.LocalTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Data
public class CameraStatusDto {

    public Long id;
    public String ip;
    public int timeout = 1;
    public boolean enabled = true;
    public Long gateId;
    public String login;
    public String password;
    public String snapshotUrl;
    public String carmenIp;
    public String carmenLogin;
    public String carmenPassword;
    public Boolean snapshotEnabled;

    public Map<String, Object> properties = new HashMap<>();
    public CarEventDto carEventDto = null;
    public LocalTime startTime;
    public LocalTime endTime;
    public Date updatedTime;
    public String updatedTimeBy;
}
