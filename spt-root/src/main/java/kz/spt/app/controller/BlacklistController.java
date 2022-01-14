package kz.spt.app.controller;

import kz.spt.app.service.BlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/blacklist")
@RequiredArgsConstructor
public class BlacklistController {
    private final BlacklistService blacklistService;

    @GetMapping("/list")
    public String list(Model model){
        model.addAttribute("blacklists", blacklistService.list());
        return "blacklist/list";
    }
}
