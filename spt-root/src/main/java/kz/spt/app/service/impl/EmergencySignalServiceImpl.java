package kz.spt.app.service.impl;

import kz.spt.app.job.StatusCheckJob;
import kz.spt.lib.model.dto.EmergencySignalConfigDto;
import kz.spt.app.repository.EmergencySignalRepository;
import kz.spt.lib.service.EmergencySignalService;
import kz.spt.lib.model.EmergencySignalConfig;
import org.pf4j.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.CriteriaBuilder;
import java.util.List;

@Service
@Transactional(noRollbackFor = Exception.class)
public class EmergencySignalServiceImpl implements EmergencySignalService {

    private EmergencySignalRepository emergencySignalRepository;

    @Value("${barrier.permanent.open.enabled:false}")
    Boolean permanentOpenEnabled;

    public EmergencySignalServiceImpl(EmergencySignalRepository emergencySignalRepository){
        this.emergencySignalRepository = emergencySignalRepository;
    }

    @Bean
    public void updateEmergencySignal(){
        if(permanentOpenEnabled){
            fillPermanentStatusDevice();
        }
    }

    public void fillPermanentStatusDevice(){
        List<EmergencySignalConfig> configs = emergencySignalRepository.findAll();
        for(EmergencySignalConfig config: configs){
            if(StringUtils.isNotNullOrEmpty(config.getIp()) && config.getModbusRegister() != null && config.getModbusSosActiveValue() != null){
                StatusCheckJob.emergencySignalConfigDto = EmergencySignalConfigDto.fromEmergencySignalConfig(config);
            } else {
                StatusCheckJob.emergencySignalConfigDto = null;
            }
        }
    }

    @Override
    public EmergencySignalConfigDto getConfigured() {
        return StatusCheckJob.emergencySignalConfigDto;
    }

    @Override
    public void save(String ip, Integer modbusRegister, Integer defaultActiveSignal) {
        List<EmergencySignalConfig> emergencySignalConfigs = emergencySignalRepository.findAll();
        EmergencySignalConfig config = new EmergencySignalConfig();
        if(emergencySignalConfigs.size() > 0){
            EmergencySignalConfig oldConfig = emergencySignalConfigs.get(0);
            if(!oldConfig.getIp().equals(ip)){
                emergencySignalRepository.deleteById(oldConfig.getIp());
            } else {
                config = oldConfig;
            }
        }
        config.setIp(ip);
        config.setModbusRegister(modbusRegister);
        config.setModbusSosActiveValue(defaultActiveSignal);
        emergencySignalRepository.save(config);
        fillPermanentStatusDevice();
    }

    @Override
    public void remove(String ip) {
        emergencySignalRepository.deleteById(ip);
        fillPermanentStatusDevice();
    }
}
