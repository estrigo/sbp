package kz.spt.app.job;

import kz.spt.lib.model.dto.SnapshotThreadDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
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

    //@Scheduled(cron = "0 0 * * * *")
    @Scheduled(fixedDelay = 1000)
    public void run() {
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
