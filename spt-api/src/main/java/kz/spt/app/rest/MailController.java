package kz.spt.app.rest;

import kz.spt.lib.model.dto.SendMailDto;
import kz.spt.lib.service.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/mail")
@RequiredArgsConstructor
public class MailController {
    private final MailService mailService;

    @PostMapping("/send")
    public void sendMail(@RequestBody SendMailDto sendMail){
        mailService.sendMail(sendMail);
    }
}
