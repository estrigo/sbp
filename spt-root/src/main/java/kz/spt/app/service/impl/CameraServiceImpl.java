package kz.spt.app.service.impl;

import kz.spt.api.model.Camera;
import kz.spt.app.repository.CameraRepository;
import kz.spt.app.service.CameraService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CameraServiceImpl implements CameraService {

    @Autowired
    private CameraRepository cameraRepository;

    @Override
    public Camera findCameraByIp(String ip) {
        return cameraRepository.findCameraByIp(ip);
    }
}
