package kz.spt.lib.service;

import kz.spt.lib.model.dto.CarEventDto;
import kz.spt.lib.model.dto.temp.CarTempReqBodyJsonDto;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

public interface CarEventService {

    void saveCarEvent(CarEventDto eventDto) throws Exception;

    void handleTempCarEvent(MultipartFile file, String json) throws Exception;
}
