package kz.spt.app.model.dto;

import kz.spt.lib.model.dto.CarEventDto;

import java.util.HashMap;
import java.util.Map;

public class CameraStatusDto {

    public Long id;
    public String ip;
    public int timeout = 1;
    public boolean enabled = true;
    public Long gateId;
    public String login;
    public String password;
    public String snapshotUrl;

    public Map<String, Object> properties = new HashMap<>();
    public CarEventDto carEventDto = null;
}
