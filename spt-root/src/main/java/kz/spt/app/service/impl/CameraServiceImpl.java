package kz.spt.app.service.impl;

import kz.spt.api.model.Camera;
import kz.spt.app.repository.CameraRepository;
import kz.spt.app.service.CameraService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CameraServiceImpl implements CameraService {

    private CameraRepository cameraRepository;

    public CameraServiceImpl(CameraRepository cameraRepository){
        this.cameraRepository = cameraRepository;
    }

    @Override
    public Camera findCameraByIp(String ip) {
        return cameraRepository.findCameraByIp(ip);
    }

    @Override
    public List<Camera> cameraList() {
        return cameraRepository.findAll();
    }

    @Override
    public Camera getCameraById(Long id) {
        return cameraRepository.getOne(id);
    }

    @Override
    public void saveCamera(Camera camera) {
        cameraRepository.save(camera);
    }
}
