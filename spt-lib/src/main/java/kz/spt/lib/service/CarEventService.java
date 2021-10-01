package kz.spt.app.service;

import kz.spt.app.model.dto.CarEventDto;

public interface CarEventService {

    void saveCarEvent(CarEventDto eventDto) throws Exception;
}
