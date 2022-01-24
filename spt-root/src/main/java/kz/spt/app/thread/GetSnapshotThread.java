package kz.spt.app.thread;

import kz.spt.app.job.CameraSnapshotJob;
import kz.spt.lib.model.Camera;
import kz.spt.lib.service.ArmService;
import kz.spt.lib.service.EventLogService;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;

@Log
public class GetSnapshotThread extends Thread {
    private ArmService armService;
    private EventLogService eventLogService;

    private Long cameraId;
    private String ip;
    private String login;
    private String password;
    private String url;

    public GetSnapshotThread(String name,
                             Long cameraId,
                             String ip,
                             String login,
                             String password,
                             String url,
                             ArmService armService,
                             EventLogService eventLogService) {
        super(name);
        this.cameraId = cameraId;
        this.ip = ip;
        this.login = login;
        this.password = password;
        this.url = url;
        this.armService = armService;
        this.eventLogService = eventLogService;
    }

    @SneakyThrows
    public void run() {
        log.info("Running task:" + getName() + "," + "task id:" + getId() + ", thread group:" + getThreadGroup().getName());

        while (!Thread.currentThread().isInterrupted()) {
            try {
                String base64 = armService.snapshot(ip, login, password, url);
                eventLogService.sendSocketMessage(EventLogService.ArmEventType.Photo, EventLogService.EventType.Success, cameraId, "", base64, "");
            } catch (Exception ex) {
                log.warning("Error task:" + getName() + "," +
                        "task id:" + getId() + "," +
                        "message:" + ex.getMessage());
            }
            //Thread.currentThread().sleep(1000);
        }
    }
}
