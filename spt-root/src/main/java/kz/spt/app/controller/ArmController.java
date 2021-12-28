package kz.spt.app.controller;

import kz.spt.app.service.CameraService;
import kz.spt.lib.model.Camera;
import lombok.val;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Collections;


@Controller
@RequestMapping("/arm")
public class ArmController {

    private CameraService cameraService;


    public ArmController(CameraService cameraService){
        this.cameraService = cameraService;
    }

    @GetMapping("/realtime")
    public String getCamersForRealtime(Model model) {
        model.addAttribute("cameras", cameraService.cameraList());
        return "arm/realtime";
    }

    @PostMapping("/snapshot")
    public byte[] getSnapshot(@RequestBody Camera camera){
        var restTemplate = new RestTemplateBuilder()
                .basicAuthentication(camera.getLogin(),camera.getPassword()).build();
        val headers = new HttpHeaders();
        headers.add("Content-Type", "image/jpeg");
        StringBuilder url = new StringBuilder();
        url.append("http://");
        url.append(camera.getIp());
        url.append("/cgi-bin/snapshot.cgi");
        val result = restTemplate.exchange(url.toString(),
                HttpMethod.GET, new HttpEntity<>(headers), byte[].class,
                Collections.singletonMap("channel", "1")).getBody();

        return result;
    }
}
