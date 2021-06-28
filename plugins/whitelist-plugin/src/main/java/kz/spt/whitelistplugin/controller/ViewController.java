package kz.spt.whitelistplugin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/white-list")
public class ViewController {

    @GetMapping("/list")
    public String showAllContracts(Model model) {

        return "white-list/test";
    }
}
