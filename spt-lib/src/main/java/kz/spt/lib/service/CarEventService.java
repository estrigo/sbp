package kz.spt.lib.service;

import kz.spt.lib.model.dto.CarEventDto;
import kz.spt.lib.model.dto.temp.CarTempEventDto;

public interface CarEventService {

    void saveCarEvent(CarEventDto eventDto) throws Exception;

    void handleTempCarEvent(CarTempEventDto carTempEventDto) throws Exception;
}
