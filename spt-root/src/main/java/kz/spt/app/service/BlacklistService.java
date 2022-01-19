package kz.spt.app.service;

import kz.spt.lib.model.Blacklist;
import kz.spt.lib.model.dto.BlacklistDto;

import java.util.List;
import java.util.Optional;

public interface BlacklistService {
    Optional<Blacklist> findByPlate(String plateNumber);
    List<BlacklistDto> list();
    void save(BlacklistDto model);
    void delete(Long id);
}
