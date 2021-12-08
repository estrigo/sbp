package kz.spt.app.model.dto;

import kz.spt.lib.model.dto.CarEventDto;

import java.util.HashMap;
import java.util.Map;

public class CameraStatusDto {

    public Long id;

    public Map<String, Object> properties = new HashMap<>();
    public CarEventDto carEventDto = null;
}
