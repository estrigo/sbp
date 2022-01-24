package kz.spt.app.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Log
@Component
@EnableScheduling
@RequiredArgsConstructor
public class CameraSnapshotJob {
    public static Map<String, Thread> threads = new ConcurrentHashMap<>();

    //@Scheduled(cron = "0 0 * * * *")
    @Scheduled(fixedDelay = 5000)
    public void run() {
        threads.forEach((key,thread)->{
            if(!thread.isAlive()){
                thread.start();
            }
        });
    }
}
