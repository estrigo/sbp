package kz.spt.app.service;

import kz.spt.api.model.Camera;

import java.util.List;

public interface CameraService {

    Camera findCameraByIp(String ip);

    List<Camera> cameraList();
}
