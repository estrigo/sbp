package kz.spt.app.service;

import kz.spt.lib.model.Camera;
import kz.spt.lib.model.CameraTab;

import java.util.List;

public interface CameraService {

    List<Camera> findCameraByIp(String ip);

    CameraTab findCameraTabByIdOrReturnNull(Long id);

    Camera findCameraByDetectorId(String detectorId);

    Camera findCameraByDetectorId(String detectorId);

    List<Camera> cameraList();

    List<Camera> cameraListWithoutTab();

    List<Camera> cameraListByTabId(Long cameraTabId);

    List<CameraTab> cameraTabList();

    Camera getCameraById(Long id);

    void saveCamera(Camera camera);

    void saveCameraTab(CameraTab cameraTab);

    void deleteCamera(Camera camera);

    void deleteCameraTab(CameraTab cameraTab);
}
