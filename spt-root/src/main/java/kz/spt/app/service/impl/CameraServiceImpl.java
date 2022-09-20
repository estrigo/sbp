package kz.spt.app.service.impl;

import kz.spt.app.job.StatusCheckJob;
import kz.spt.app.repository.CameraTabRepository;
import kz.spt.lib.model.Camera;
import kz.spt.app.repository.CameraRepository;
import kz.spt.app.service.CameraService;
import kz.spt.lib.model.CameraTab;
import kz.spt.lib.model.Parking;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class CameraServiceImpl implements CameraService {

    private CameraRepository cameraRepository;

    private CameraTabRepository cameraTabRepository;

    public CameraServiceImpl(CameraRepository cameraRepository, CameraTabRepository cameraTabRepository) {
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
        if (cameraTabOptional.isPresent()) {
            return cameraTabOptional.get();
        }
        return null;
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
        return cameraRepository.findEnabledWithoutTabCameras().stream()
                .filter(distinctByKey(Camera::getIp))
                .collect(Collectors.toList());
    }

    @Override
    public List<Camera> cameraListByTabId(Long cameraTabId) {
        List<Camera> allCameras = cameraRepository.findEnabledWithTabCameras(cameraTabId);
        return allCameras.stream()
                .filter(distinctByKey(Camera::getIp))
                .collect(Collectors.toList());
    }

    @Override
    public List<CameraTab> cameraTabList() {
        return cameraTabRepository.findAll();
    }

    @Override
    public Camera getCameraById(Long id) {
        Camera camera = cameraRepository.getOne(id);
        if (camera.getDetectorId() == null || StringUtils.isEmpty(camera.getDetectorId())) {
            camera.setDetectorId(camera.getId().toString());
        }
        return camera;
    }

    @Override
    public void saveCamera(Camera camera, Boolean updateGlobalGatedtos) {
        cameraRepository.save(camera);
        if (updateGlobalGatedtos) {
            StatusCheckJob.emptyGlobalGateDtos();
        }
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

    @Override
    public void enableSnapshot(Long cameraId) {
        Camera camera = cameraRepository.getOne(cameraId);
        camera.setSnapshotEnabled(camera.getSnapshotEnabled() == null ? true : !camera.getSnapshotEnabled());
        cameraRepository.save(camera);
    }

    @Override
    public Optional<Camera> findCameraByIpAndParking(String ip, Parking parking) {
        return cameraRepository.findCameraByIpAndGate_Parking(ip, parking);
    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }
}
