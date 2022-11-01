package kz.spt.lib.service;

import kz.spt.lib.bootstrap.datatable.Page;
import kz.spt.lib.bootstrap.datatable.PagingRequest;
import kz.spt.lib.model.Blacklist;
import kz.spt.lib.model.dto.BlacklistDto;

import java.util.Optional;

public interface BlacklistService {
    Optional<Blacklist> findByPlate(String plateNumber);
    Page<BlacklistDto> getAll(PagingRequest pagingRequest);
    void save(BlacklistDto model);
    void delete(Long id);
}
