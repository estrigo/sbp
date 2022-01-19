package kz.spt.app.controller;

import kz.spt.app.service.BlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

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

    @GetMapping("/delete")
    public String delete(@RequestParam Long id){
        blacklistService.delete(id);
        return "redirect:/blacklist/list";
    }
}
