package kz.spt.app.service.impl;

import kz.spt.app.repository.CalibrationRepository;
import kz.spt.app.service.CameraService;
import kz.spt.lib.model.Calibration;
import kz.spt.lib.model.Camera;
import kz.spt.lib.model.dto.CalibrationDto;
import kz.spt.lib.service.CalibrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CalibrationServiceImpl implements CalibrationService {
    private final CalibrationRepository calibrationRepository;
    private final CameraService cameraService;

    @Override
    public void save(Long cameraId, String json) {
        Camera camera = cameraService.getCameraById(cameraId);
        Calibration calibration = calibrationRepository.findByCamera(cameraId)
                .map(m->{
                    m.setIp(camera.getIp());
                    m.setJson(json);
                    return m;
                })
                .orElse(Calibration.builder()
                        .cameraId(camera.getId())
                        .ip(camera.getIp())
                        .json(json)
                        .build());
        calibrationRepository.save(calibration);
    }

    @Override
    public Optional<CalibrationDto> findByCamera(Long cameraId) {
        return calibrationRepository.findByCamera(cameraId)
                .map(m->CalibrationDto.builder()
                        .id(m.getId())
                        .cameraId(m.getCameraId())
                        .ip(m.getIp())
                        .json(m.getJson())
                        .build());
    }

    @Override
    public Optional<CalibrationDto> findByIp(String ip) {
        return calibrationRepository.findByIp(ip)
                .map(m->CalibrationDto.builder()
                        .id(m.getId())
                        .cameraId(m.getCameraId())
                        .ip(m.getIp())
                        .json(m.getJson())
                        .build());
    }
}
