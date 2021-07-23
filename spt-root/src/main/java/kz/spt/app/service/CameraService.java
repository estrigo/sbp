package kz.spt.app.service;

import kz.spt.api.model.Camera;

public interface CameraService {

    Camera findCameraByIp(String ip);
}
