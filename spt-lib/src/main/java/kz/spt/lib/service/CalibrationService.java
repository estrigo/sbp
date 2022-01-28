package kz.spt.lib.service;

import kz.spt.lib.model.dto.CalibrationDto;

import java.util.Optional;

public interface CalibrationService {
    void save(Long cameraId, String json);
    Optional<CalibrationDto> findByCamera(Long cameraId);
    Optional<CalibrationDto> findByIp(String ip);
}
