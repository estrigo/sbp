package kz.spt.app.service;

import kz.spt.lib.model.Reasons;

import java.util.List;

public interface ReasonsService {
    List<Reasons> findAllReasons();
    void addNewReason(String reason);
    void deleteReasonById(Long id);
}
