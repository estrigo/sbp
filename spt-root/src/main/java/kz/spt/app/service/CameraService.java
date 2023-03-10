package kz.spt.app.service;

import kz.spt.lib.model.Camera;
import kz.spt.lib.model.CameraTab;
import kz.spt.lib.model.Parking;

import java.util.List;
import java.util.Optional;

public interface CameraService {

    List<Camera> findCameraByIp(String ip);

    CameraTab findCameraTabByIdOrReturnNull(Long id);

    Camera findCameraByDetectorId(String detectorId);

    List<Camera> findCameraIdsAndSnapshotEnabled();

    List<Camera> cameraList();

    List<Camera> cameraListWithoutTab();

    List<Camera> cameraListByTabId(Long cameraTabId);

    List<CameraTab> cameraTabList();

    Camera getCameraById(Long id);

    void saveCamera(Camera camera, Boolean updateGlobalGatedtos);

    void saveCameraTab(CameraTab cameraTab);

    void deleteCamera(Camera camera);

    void deleteCameraTab(CameraTab cameraTab);

    void enableSnapshot(Long cameraId);

    void enableSnapshot(Long cameraId, Boolean isStreamOn);

    Optional<Camera> findCameraByIpAndParking(String ip, Parking parking);
}
