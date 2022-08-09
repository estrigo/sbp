package kz.spt.lib.service;

import kz.spt.lib.model.dto.adminPlace.AdminCommandDto;
import kz.spt.lib.model.dto.adminPlace.GenericWhlEvent;
import org.springframework.http.ResponseEntity;

public interface AdminService {
    String getProperty(String key);


    ResponseEntity<?> updateProperty(AdminCommandDto commandDto);

    void whlProcess (GenericWhlEvent<?> whlEvent);

    ResponseEntity<?> getBasicResponse();

    void synchronizeWhl() throws Exception;
}
