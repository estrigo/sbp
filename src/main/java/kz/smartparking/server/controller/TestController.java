package kz.smartparking.server.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Log
@RequiredArgsConstructor
public class TestController {

    @GetMapping(path = "/test", produces = "application/json")
    @ResponseBody
    public String getSitesBySiteId() {
        return "test success";
    }
}
