package kz.spt.lib.service;

import kz.spt.lib.enums.SyslogTypeEnum;

import java.util.Date;

public interface SyslogService {
    void createSyslog(String uuid, Date createdDate, String descriptionRu,
                      String descriptionEn, SyslogTypeEnum syslogTypeEnum, String message);
}
