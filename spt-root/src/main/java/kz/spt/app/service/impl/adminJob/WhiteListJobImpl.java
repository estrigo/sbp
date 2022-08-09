package kz.spt.app.service.impl.adminJob;

import kz.spt.app.repository.PropertyRepository;
import kz.spt.lib.model.Property;
import kz.spt.lib.service.AdminService;
import kz.spt.lib.service.WhiteListJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

@Log
@Service
@RequiredArgsConstructor
public class WhiteListJobImpl implements WhiteListJob {

    private final PropertyRepository propertyRepository;
    private ScheduledFuture future;
    private final TaskScheduler scheduler;
    private final AdminService adminService;

    private final static String CRON_WHL = "cron_white_list";

    @Bean
    private void doStart() {
        start();
    }


    private void start() {
        future = scheduler.schedule(this::synchronizeWhl,
                triggerContext -> {
                    String cron = cronConfig();
                    if (cron != null) {
                        CronTrigger trigger = new CronTrigger(cron);
                        return trigger.nextExecutionTime(triggerContext);
                    }
                    return null;
                });

    }

    private void stop() {
        if (future != null) {
            future.cancel(false);
        }
    }

    private String cronConfig() {
        Optional<Property> property = propertyRepository.findFirstByKey(CRON_WHL);
        if (property.isPresent() && !BooleanUtils.toBoolean(property.get().getDisabled())) {
            return property.get().getValue();
        }
        return null;
    }


    public void synchronizeWhl() {
        try {
            log.info("Start job synchronization  white list! " + LocalDateTime.now());
            adminService.synchronizeWhl();
        } catch (Exception e) {
            log.warning(e.getMessage());
        }

    }


    public ResponseEntity<?> startWhiteListJob() {
        Optional<Property> property = propertyRepository.findFirstByKey(CRON_WHL);
        if (property.isPresent()) {
            property.get().setDisabled(false);
            propertyRepository.save(property.get());
            start();
        }
        return adminService.getBasicResponse();

    }

    public ResponseEntity<?> stopWhiteListJob() {
        Optional<Property> property = propertyRepository.findFirstByKey(CRON_WHL);
        if (property.isPresent()) {
            property.get().setDisabled(true);
            propertyRepository.save(property.get());
            stop();
        }
        return adminService.getBasicResponse();
    }

}
