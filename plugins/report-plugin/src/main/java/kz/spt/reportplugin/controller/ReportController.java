package kz.spt.reportplugin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/report")
@RequiredArgsConstructor
public class ReportController {

    @GetMapping("/journal")
    public String journal() {
        return "report/journal";
    }

    @GetMapping("/manualOpen")
    public String manualOpen() {
        return "report/manualOpen";
    }

/*    @GetMapping("/sum")
    public String sum() {
        return "report/sum";
    }*/
}
