package kz.spt.whitelistplugin.controller;

import kz.spt.whitelistplugin.service.WhitelistService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/whitelist")
public class WhitelistController {

    private WhitelistService whitelistService;

    public WhitelistController(WhitelistService whitelistService){
        this.whitelistService = whitelistService;
    }

    @GetMapping()
    public String showAllWhitelist(Model model) {
        model.addAttribute("whitelist", whitelistService.listAllWhitelist());
        return "whitelist";
    }


}
