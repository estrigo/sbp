package kz.spt.qrpanel.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/test")
public class TestController {

    @GetMapping("/list")
    public String showTestList(Model model) {
        return "test/list";
    }

    @GetMapping("/list2")
    public String showTest1List(Model model) {
        return "test/list2";
    }
}
