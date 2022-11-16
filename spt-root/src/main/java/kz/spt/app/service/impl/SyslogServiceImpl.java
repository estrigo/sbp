package kz.spt.app.service.impl;


import kz.spt.app.repository.SyslogRepository;
import kz.spt.lib.enums.SyslogTypeEnum;
import kz.spt.lib.model.Syslog;
import kz.spt.lib.service.SyslogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Slf4j
@Service
@Transactional(noRollbackFor = Exception.class)
public class SyslogServiceImpl implements SyslogService {

    private SyslogRepository syslogRepository;

    public SyslogServiceImpl(SyslogRepository syslogRepository) {
        this.syslogRepository = syslogRepository;
    }

    @Override
    public void createSyslog(String uuid, Date createdDate, String descriptionRu,
                             String descriptionEn, SyslogTypeEnum syslogTypeEnum, String message) {
        Syslog syslog = new Syslog();

        syslog.setSyslogType(syslogTypeEnum);
        syslog.setCreated(createdDate);
        syslog.setDescriptionRu(descriptionRu);
        syslog.setDescriptionEn(descriptionEn);
        syslog.setUUID(uuid);
        syslog.setMessage(message);

        syslogRepository.save(syslog);
    }

}
