package kz.spt.app.service;

import kz.spt.app.entity.dto.CarEventDto;

public interface CarEventService {

    public void saveCarEvent(CarEventDto eventDto) throws Exception;
}
