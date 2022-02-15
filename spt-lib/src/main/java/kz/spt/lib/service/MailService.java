package kz.spt.lib.service;

import kz.spt.lib.model.dto.SendMailDto;

public interface MailService {
    void sendMail(SendMailDto model);
}
