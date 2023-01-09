package kz.spt.lib.model.dto;

import kz.spt.lib.model.EmergencySignalConfig;

public class EmergencySignalConfigDto {

    public String ip;
    public Integer modbusRegister;
    public Integer defaultValue = 0;

    public Boolean allFieldsFilled = true;

    public Boolean active = false;

    public static EmergencySignalConfigDto fromEmergencySignalConfig(EmergencySignalConfig config){
        EmergencySignalConfigDto dto = new EmergencySignalConfigDto();
        dto.active = false;
        dto.ip = config.getIp();
        dto.modbusRegister = config.getModbusRegister();
        dto.defaultValue = config.getModbusSosActiveValue();
        dto.allFieldsFilled = true;
        return dto;
    }

}
