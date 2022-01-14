package kz.spt.app.service;

import kz.spt.lib.model.dto.BlacklistDto;

import java.util.List;

public interface BlacklistService {
    List<BlacklistDto> list();
    void save(BlacklistDto model);
}
