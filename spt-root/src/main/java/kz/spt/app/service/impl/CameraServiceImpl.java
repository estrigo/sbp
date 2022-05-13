package kz.spt.app.service.impl;

import kz.spt.app.job.StatusCheckJob;
import kz.spt.app.repository.CameraTabRepository;
import kz.spt.lib.model.Camera;
import kz.spt.app.repository.CameraRepository;
import kz.spt.app.service.CameraService;
import kz.spt.lib.model.CameraTab;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CameraServiceImpl implements CameraService {

    private CameraRepository cameraRepository;

    private CameraTabRepository cameraTabRepository;

    public CameraServiceImpl(CameraRepository cameraRepository, CameraTabRepository cameraTabRepository){
        this.cameraRepository = cameraRepository;
        this.cameraTabRepository = cameraTabRepository;
    }

    @Override
    public List<Camera> findCameraByIp(String ip) {
        return cameraRepository.findCameraByIp(ip);
    }

    @Override
    public CameraTab findCameraTabByIdOrReturnNull(Long id) {
        Optional<CameraTab> cameraTabOptional = cameraTabRepository.findById(id);
        if(cameraTabOptional.isPresent()){
            return cameraTabOptional.get();
        }
        return null;
    }

    @Override
    public Camera findCameraByDetectorId(String detectorId) {
        return cameraRepository.findCameraByDetectorId(detectorId);
    }

    @Override
    public Camera findCameraByDetectorId(String detectorId) {
        return cameraRepository.findCameraByDetectorId(detectorId);
    }

    @Override
    public List<Camera> cameraList() {
        return cameraRepository.findEnabledCameras();
    }

    @Override
    public List<Camera> cameraListWithoutTab() {
        return cameraRepository.findEnabledWithoutTabCameras();
    }

    @Override
    public List<Camera> cameraListByTabId(Long cameraTabId) {
        return cameraRepository.findEnabledWithTabCameras(cameraTabId);
    }

    @Override
    public List<CameraTab> cameraTabList() {
        return cameraTabRepository.findAll();
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
    public void saveCameraTab(CameraTab cameraTab) {
        cameraTabRepository.save(cameraTab);
    }

    @Override
    public void deleteCamera(Camera camera) {
        camera.setCameraTab(null);
        cameraRepository.delete(camera);
        StatusCheckJob.emptyGlobalGateDtos();
    }

    @Override
    public void deleteCameraTab(CameraTab cameraTab) {
        cameraTabRepository.delete(cameraTab);
    }
}
