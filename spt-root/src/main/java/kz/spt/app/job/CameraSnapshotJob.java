package kz.spt.app.job;

import kz.spt.app.service.CameraService;
import kz.spt.app.thread.GetSnapshotThread;
import kz.spt.lib.model.dto.SnapshotThreadDto;
import kz.spt.lib.service.ArmService;
import kz.spt.lib.service.CarImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Log
@Component
@EnableScheduling
@RequiredArgsConstructor
public class CameraSnapshotJob {
    public static Map<String, SnapshotThreadDto> threads = new ConcurrentHashMap<>();
    private final ThreadPoolTaskExecutor snapshotTaskExecutor;
    private final CameraService cameraService;
    private final ArmService armService;
    private final CarImageService carImageService;

    @Value("${images.file.path}")
    private String imagePath;

    //@Scheduled(cron = "0 0 * * * *")
    //@Scheduled(fixedDelay = 5000)
    public void run() {



/*

        cameraService.cameraList().forEach(camera -> {
            String name = "snapshot-camera-" + camera.getId().toString();
            if (StringUtils.isEmpty(camera.getSnapshotUrl())) {
                if (threads.containsKey(name)) {
                    threads.get(name).getThread().interrupt();
                    threads.remove(name);
                    log.info("Ending task:" + name);
                }
                return;
            }

            if (CameraSnapshotJob.threads.containsKey(name)) {
                return;
            }
            CameraSnapshotJob.threads.put(name, SnapshotThreadDto.builder()
                    .isActive(false)
                    .thread(new GetSnapshotThread(name,
                            camera.getId(),
                            camera.getIp(),
                            camera.getLogin(),
                            camera.getPassword(),
                            camera.getSnapshotUrl(),
                            armService,
                            carImageService))
                    .build());
        });

        threads.forEach((key, m) -> {
            if (!m.isActive()) {
                try {
                    snapshotTaskExecutor.execute(m.getThread());
                    m.setActive(true);
                } catch (TaskRejectedException e) {
                    snapshotTaskExecutor.initialize();
                }
            }
        });*/
    }

    //@Scheduled(cron = "0 1 1 * * ?")
    public void clean() {
        cameraService.cameraList().forEach(camera -> {
            if (StringUtils.isEmpty(camera.getSnapshotUrl())) return;

            String cameraPath = imagePath + "/" + camera.getIp();
            try {
                /*Calendar lastY = Calendar.getInstance();
                lastY.setTime(new Date());
                lastY.add(Calendar.YEAR, -1);
                File lastYear = new File(cameraPath + "/" + lastY.get(Calendar.YEAR));
                if (lastYear.exists()) {
                    FileUtils.deleteDirectory(lastYear);
                }

                Calendar lastM = Calendar.getInstance();
                lastM.setTime(new Date());
                lastM.add(Calendar.MONTH, -1);
                File lastMonth = new File(cameraPath + "/" + lastM.get(Calendar.YEAR) + "/" + (lastM.get(Calendar.MONTH) + 1));
                if (lastMonth.exists()) {
                    FileUtils.deleteDirectory(lastMonth);
                }*/

                Calendar lastW = Calendar.getInstance();
                lastW.setTime(new Date());
                for (int i = 1; i <= 7; i++) {
                    lastW.add(Calendar.DATE, -1 * i);
                    File lastWeek = new File(cameraPath + "/" + lastW.get(Calendar.YEAR) + "/" + (lastW.get(Calendar.MONTH) + 1) + "/" + lastW.get(Calendar.DATE) + "/");
                    if (lastWeek.exists()) {
                        FileUtils.deleteDirectory(lastWeek);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
