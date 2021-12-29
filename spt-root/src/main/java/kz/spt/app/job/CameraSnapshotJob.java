package kz.spt.app.job;

import kz.spt.app.service.CameraService;
import kz.spt.lib.service.ArmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Log
@Component
@EnableScheduling
@RequiredArgsConstructor
public class CameraSnapshotJob {
    private final CameraService cameraService;
    private final ArmService armService;

    @Scheduled(fixedDelay = 3000)
    public void clean() {
        cameraService.cameraList().stream()
                .filter(m -> !StringUtils.isEmpty(m.getLogin()) && !StringUtils.isEmpty(m.getPassword()) && !StringUtils.isEmpty(m.getSnapshotUrl()))
                .forEach(m -> {
                    try{
                        armService.snapshot(m);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                });
    }
}
