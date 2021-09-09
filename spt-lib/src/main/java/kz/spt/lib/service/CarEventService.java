package kz.spt.app.service;

import kz.spt.app.entity.dto.CarEventDto;

public interface CarEventService {

    void saveCarEvent(CarEventDto eventDto) throws Exception;
}
