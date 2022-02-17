package kz.spt.app.thread;

import kz.spt.lib.service.ArmService;
import kz.spt.lib.service.EventLogService;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;
import org.springframework.scheduling.annotation.Async;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.Future;

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
    @Async("SnapshotTaskScheduler")
    public void run() {
        log.info("Running task:" + getName() + "," + "task id:" + getId() + ", thread group:" + getThreadGroup().getName() + ", parent:" + getThreadGroup().getParent().getName());

        while (!Thread.currentThread().isInterrupted()) {
            try {
                Future<byte[]> future = armService.getSnapshot(ip, login, password, url);
                while (true) {
                    if (future.isDone()) {
                        String base64 = StringUtils.newStringUtf8(Base64.encodeBase64(future.get(), false));
                        eventLogService.sendSocketMessage(EventLogService.ArmEventType.Photo, EventLogService.EventType.Success, cameraId, "", base64, "");
                        break;
                    }
                }
            } catch (Exception ex) {
                log.warning("Error task:" + getName() + "," +
                        "task id:" + getId() + "," +
                        "message:" + ex.getMessage());
            }
            //Thread.currentThread().sleep(1000);
        }
    }
}
