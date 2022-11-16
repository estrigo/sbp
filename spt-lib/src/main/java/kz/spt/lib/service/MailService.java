package kz.spt.lib.service;

import kz.spt.lib.model.dto.SendMailDto;
import org.springframework.core.io.ByteArrayResource;

import java.util.UUID;

public interface MailService {
    void sendMail(SendMailDto model);
    void sendEmailWithFile(String fileName, String subjectName, ByteArrayResource resource, UUID uuid);
}
