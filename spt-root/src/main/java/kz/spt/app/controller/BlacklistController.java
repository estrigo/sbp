package kz.spt.app.controller;

import kz.spt.app.service.BlacklistService;
import kz.spt.lib.model.CarState;
import kz.spt.lib.model.dto.BlacklistDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

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

    @PostMapping("/save")
    public String addBlacklist(@ModelAttribute BlacklistDto model){
        blacklistService.save(model);
        return "redirect:/blacklist/list";
    }

    @GetMapping("/delete")
    public String delete(@RequestParam Long id){
        blacklistService.delete(id);
        return "redirect:/blacklist/list";
    }
}
