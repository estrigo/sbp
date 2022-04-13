package kz.spt.app.service.impl;

import kz.spt.app.job.StatusCheckJob;
import kz.spt.lib.model.Camera;
import kz.spt.app.repository.CameraRepository;
import kz.spt.app.service.CameraService;
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
    public Camera findCameraById(Long id) {
        return cameraRepository.findCameraById(id);
    }

    @Override
    public List<Camera> cameraList() {
        return cameraRepository.findEnabledCameras();
    }

    @Override
    public Camera getCameraById(Long id) {
        return cameraRepository.getOne(id);
    }

    @Override
    public void saveCamera(Camera camera) {
        cameraRepository.save(camera);
        StatusCheckJob.emptyGlobalGateDtos();
    }

    @Override
    public void deleteCamera(Camera camera) {
        cameraRepository.delete(camera);
        StatusCheckJob.emptyGlobalGateDtos();
    }
}
