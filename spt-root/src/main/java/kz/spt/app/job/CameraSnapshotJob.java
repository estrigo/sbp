package kz.spt.app.job;

import kz.spt.app.service.CameraService;
import kz.spt.app.thread.GetSnapshotThread;
import kz.spt.lib.model.dto.SnapshotThreadDto;
import kz.spt.lib.service.ArmService;
import kz.spt.lib.service.CarImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Log
@Component
@EnableScheduling
@RequiredArgsConstructor
public class CameraSnapshotJob {
    private final ThreadPoolTaskExecutor snapshotTaskExecutor;
    public static Map<String, SnapshotThreadDto> threads = new ConcurrentHashMap<>();

    private final CameraService cameraService;
    private final ArmService armService;
    private final CarImageService carImageService;

    //@Scheduled(cron = "0 0 * * * *")
    @Scheduled(fixedDelay = 5000)
    public void run() {
        cameraService.cameraList().forEach(camera->{
            if (StringUtils.isEmpty(camera.getSnapshotUrl())) return;

            String name = "snapshot-camera-" + camera.getId().toString();
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

        threads.forEach((key,m)->{
            if(!m.isActive()){
               try{
                   snapshotTaskExecutor.execute(m.getThread());
                   m.setActive(true);
               }catch (TaskRejectedException e){
                   snapshotTaskExecutor.initialize();
               }
            }
        });
    }
}
