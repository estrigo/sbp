package kz.spt.lib.service;

import kz.spt.lib.model.dto.CarEventDto;

public interface CarEventService {

    void saveCarEvent(CarEventDto eventDto) throws Exception;
}
