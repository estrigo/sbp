package kz.spt.app.service.impl;

import kz.spt.lib.model.dto.SendMailDto;
import kz.spt.lib.service.MailService;
import lombok.RequiredArgsConstructor;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MailServiceImpl implements MailService {
    private final JavaMailSender javaMailSender;

    @Override
    public void sendMail(SendMailDto model) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo("support@parqour.com");
        msg.setSubject(model.getSubject());
        msg.setText(model.getMessage());

        javaMailSender.send(msg);
    }
}
