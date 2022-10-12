package kz.spt.app.service.impl;

import kz.spt.app.repository.CustomerRepository;
import kz.spt.lib.model.Customer;
import kz.spt.lib.model.dto.SendMailDto;
import kz.spt.lib.service.MailService;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.activation.DataSource;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(noRollbackFor = Exception.class)
public class MailServiceImpl implements MailService {
    private final JavaMailSender javaMailSender;
    private final CustomerRepository customerRepository;

    @Override
    public void sendMail(SendMailDto model) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo("support@parqour.com");
        msg.setSubject(model.getSubject());
        msg.setText(model.getMessage());

        //javaMailSender.send(msg);
    }

    public void sendEmailWithFile(String fileName, ByteArrayResource resource) {
        String recipients = customerRepository.getAllByMailReceiverIsTrue().stream()
                .map(n -> String.valueOf(n.getEmail()))
                .collect(Collectors.joining(","));
        String subject = "Реестр платежей";
        MimeMessage message = javaMailSender.createMimeMessage();
//        File file = new File(fileName);
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(InternetAddress.parse(recipients));
            helper.setSubject(subject);
            helper.setText("");
            helper.addAttachment(fileName, resource);
        } catch (Exception ex) {
            System.out.println("Error" + ex.getMessage());
        }
        javaMailSender.send(message);
    }
}
