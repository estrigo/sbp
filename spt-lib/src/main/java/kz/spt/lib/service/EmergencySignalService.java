package kz.spt.lib.service;

import kz.spt.lib.model.dto.EmergencySignalConfigDto;

public interface EmergencySignalService {

    public EmergencySignalConfigDto getConfigured();

    public void save(String ip, Integer modbusRegister, Integer defaultActiveSignal);

    public void remove(String ip);
}
