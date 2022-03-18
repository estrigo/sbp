package kz.spt.app.thread;

import kz.spt.lib.model.EventLog;
import kz.spt.lib.service.ArmService;
import kz.spt.lib.service.CarImageService;
import kz.spt.lib.service.EventLogService;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;
import org.springframework.scheduling.annotation.Async;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Date;
import java.util.concurrent.Future;

@Log
public class GetSnapshotThread extends Thread {
    private ArmService armService;
    private CarImageService carImageService;

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
                             CarImageService carImageService) {
        super(name);
        this.cameraId = cameraId;
        this.ip = ip;
        this.login = login;
        this.password = password;
        this.url = url;
        this.armService = armService;
        this.carImageService = carImageService;
    }

    @SneakyThrows
    public void run() {
        log.info("Running task:" + getName() + "," + "task id:" + getId() + ", thread group:" + getThreadGroup().getName() + ", parent:" + getThreadGroup().getParent().getName());

        while (!Thread.currentThread().isInterrupted()) {
            try {
                Future<byte[]> future = armService.getSnapshot(ip, login, password, url);
                while (true) {
                    if (future.isDone()) {
                        carImageService.saveSnapshot(future.get(), ip);
                        break;
                    }
                }
                Thread.sleep(1000);
            } catch (Exception ex) {
                log.warning("Error task:" + getName() + "," +
                        "task id:" + getId() + "," +
                        "message:" + ex.getMessage());
            }
        }
    }
}
