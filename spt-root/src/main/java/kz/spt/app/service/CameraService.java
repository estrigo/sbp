package kz.spt.app.service;

import kz.spt.lib.model.Camera;

import java.util.List;

public interface CameraService {

    List<Camera> findCameraByIp(String ip);

    Camera findCameraById(Long id);

    Camera findCameraByDetectorId(String detectorId);

    List<Camera> cameraList();

    Camera getCameraById(Long id);

    void saveCamera(Camera camera);

    void deleteCamera(Camera camera);
}
