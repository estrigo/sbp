package kz.spt.app.job;

import kz.spt.lib.model.dto.SnapshotThreadDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

@Log
@Component
@EnableScheduling
@RequiredArgsConstructor
public class CameraSnapshotJob {
    private final Executor snapshotTaskExecutor;
    public static Map<String, SnapshotThreadDto> threads = new ConcurrentHashMap<>();

    //@Scheduled(cron = "0 0 * * * *")
    @Scheduled(fixedDelay = 5000)
    public void run() {
        threads.forEach((key,m)->{
            if(!m.isActive()){
                snapshotTaskExecutor.execute(m.getThread());
                m.setActive(true);
            }
        });
    }
}
