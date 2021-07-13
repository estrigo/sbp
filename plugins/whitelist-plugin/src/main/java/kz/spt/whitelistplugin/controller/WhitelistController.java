package kz.spt.whitelistplugin.controller;

import kz.spt.whitelistplugin.model.Whitelist;
import kz.spt.whitelistplugin.service.WhitelistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.validation.Valid;

@Controller
@RequestMapping("/whitelist")
public class WhitelistController {

    private WhitelistService whitelistService;

    public WhitelistController(WhitelistService whitelistService){
        this.whitelistService = whitelistService;
    }

    @GetMapping("/list")
    public String showAllWhitelist(Model model) {
        model.addAttribute("whitelist", whitelistService.listAllWhitelist());
        return "whitelist/list";
    }

    @GetMapping("/add")
    public String showFormAddCar(Model model) {
        model.addAttribute("whitelist", new Whitelist());
        return "whitelist/add";
    }

    @PostMapping("/add")
    public String processRequestAddCar(Model model, @Valid Whitelist whitelist, BindingResult bindingResult) throws Exception {
        if (bindingResult.hasErrors()) {
            for(ObjectError objectError : bindingResult.getAllErrors()){
                System.out.println(objectError.getObjectName() + " " + objectError.getCode() + " " + objectError.getDefaultMessage());
            }
            return "whitelist/add";
        } else {
            whitelistService.saveWhitelist(whitelist);
            return "redirect:/whitelist";
        }
    }
}
