package kz.spt.whitelistplugin.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import kz.spt.whitelistplugin.model.Whitelist;
import kz.spt.whitelistplugin.model.WhitelistGroups;
import kz.spt.whitelistplugin.service.WhitelistCategoryService;
import kz.spt.whitelistplugin.service.WhitelistGroupsService;
import kz.spt.whitelistplugin.service.WhitelistService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.validation.Valid;

@Controller
@RequestMapping("/whitelist")
public class WhitelistController {

    private WhitelistService whitelistService;
    private WhitelistGroupsService whitelistGroupsService;
    private WhitelistCategoryService whitelistCategoryService;

    public WhitelistController(WhitelistService whitelistService, WhitelistGroupsService whitelistGroupsService, WhitelistCategoryService whitelistCategoryService){
        this.whitelistService = whitelistService;
        this.whitelistGroupsService = whitelistGroupsService;
        this.whitelistCategoryService = whitelistCategoryService;
    }

    @GetMapping("/list")
    public String showAllWhitelist(Model model) throws JsonProcessingException {
        model.addAttribute("whitelist", whitelistService.listAllWhitelist());
        model.addAttribute("whitelistGroups", whitelistGroupsService.listAllWhitelistGroups());
        model.addAttribute("categories", whitelistCategoryService.listAllCategories());
        return "whitelist/list";
    }

    @GetMapping("/add")
    public String showFormAddCar(Model model) throws JsonProcessingException {
        model.addAttribute("whitelist", new Whitelist());
        model.addAttribute("groupList", whitelistGroupsService.listAllWhitelistGroups());
        model.addAttribute("categoryList", whitelistCategoryService.listAllCategories());
        return "whitelist/add";
    }

    @PostMapping("/add")
    public String processRequestAddCar(Model model, @Valid Whitelist whitelist, BindingResult bindingResult, @AuthenticationPrincipal UserDetails currentUser) throws Exception {
        if(whitelist.getPlatenumber()==null || whitelist.getPlatenumber().length() < 3  || whitelist.getPlatenumber().length() > 16){
            ObjectError error = new ObjectError("invalidPlateNumber", "Invalid plate number");
            bindingResult.addError(error);
        }
        if(whitelist.getCategoryId() == null){
            ObjectError error = new ObjectError("selectCategory", "Please select category");
            bindingResult.addError(error);
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("groupList", whitelistGroupsService.listAllWhitelistGroups());
            model.addAttribute("categoryList", whitelistCategoryService.listAllCategories());
            return "whitelist/add";
        } else {
            whitelistService.saveWhitelist(whitelist, currentUser);
            return "redirect:/whitelist/list";
        }
    }

    @GetMapping("/groups/add")
    public String showFormAddGroup(Model model) {
        model.addAttribute("whitelistGroup", new WhitelistGroups());
        model.addAttribute("categoryList", whitelistCategoryService.listAllCategories());
        return "whitelist/groups/add";
    }

    @PostMapping("/groups/add")
    public String processRequestAddGroup(Model model, @Valid WhitelistGroups whitelistGroups, BindingResult bindingResult, @AuthenticationPrincipal UserDetails currentUser) throws Exception {
        if(whitelistGroups.getCategoryId() == null){
            ObjectError error = new ObjectError("selectCategory", "Please select category");
            bindingResult.addError(error);
        }
        if(whitelistGroups.getName() == null || "".equals(whitelistGroups.getName())){
            ObjectError error = new ObjectError("emptyGroupName", "Please fill group name");
            bindingResult.addError(error);
        }
        if(whitelistGroups.getPlateNumbers() == null || whitelistGroups.getPlateNumbers().size() == 0){
            ObjectError error = new ObjectError("emptyCarList", "Please fill car plate numbers");
            bindingResult.addError(error);
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("categoryList", whitelistCategoryService.listAllCategories());
            return "whitelist/add";
        } else {
            whitelistGroupsService.saveWhitelistGroup(whitelistGroups, currentUser.getUsername());
            return "redirect:/whitelist/list";
        }
    }

    @GetMapping("/edit/{id}")
    public String showFormEditWhiteList(Model model, @PathVariable Long id) throws JsonProcessingException {
        model.addAttribute("whitelist", whitelistService.prepareById(id));
        model.addAttribute("groupList", whitelistGroupsService.listAllWhitelistGroups());
        model.addAttribute("categoryList", whitelistCategoryService.listAllCategories());
        return "whitelist/edit";
    }

    @GetMapping("/delete/{id}")
    public String deleteWhiteList(@PathVariable Long id) {
        whitelistService.deleteById(id);
        return "redirect:/whitelist/list";
    }

    @PostMapping("/edit/{id}")
    public String processRequestEditWhitelist(@PathVariable Long id, @Valid Whitelist whitelist,
                                        BindingResult bindingResult, @AuthenticationPrincipal UserDetails currentUser) throws Exception {
        if(whitelist.getPlatenumber()==null || whitelist.getPlatenumber().length() < 3 || whitelist.getPlatenumber().length() > 16){
            ObjectError error = new ObjectError("invalidPlateNumber", "Invalid plate number");
            bindingResult.addError(error);
        }
        if(whitelist.getCategoryId() == null){
            ObjectError error = new ObjectError("selectCategory", "Please select category");
            bindingResult.addError(error);
        }
        if (bindingResult.hasErrors()) {
            return "redirect:/whitelist/edit/" + id;
        } else {
            whitelistService.saveWhitelist(whitelist, currentUser);
            return "redirect:/whitelist/list";
        }
    }

    @GetMapping("/group/delete/{id}")
    public String deleteWhiteListGroup(@PathVariable Long id) {
        whitelistGroupsService.deleteById(id);
        return "redirect:/whitelist/list";
    }

    @GetMapping("/group/edit/{id}")
    public String showFormEditWhiteListGroup(Model model, @PathVariable Long id) {
        model.addAttribute("whitelistGroup", whitelistGroupsService.prepareById(id));
        model.addAttribute("categoryList", whitelistCategoryService.listAllCategories());
        return "whitelist/groups/edit";
    }

    @PostMapping("/group/edit/{id}")
    public String processRequestEditWhitelist(Model model, @PathVariable Long id, @Valid WhitelistGroups whitelistGroups,
                                              BindingResult bindingResult, @AuthenticationPrincipal UserDetails currentUser) throws Exception {
        if(whitelistGroups.getCategoryId() == null){
            ObjectError error = new ObjectError("selectCategory", "Please select category");
            bindingResult.addError(error);
        }
        if(whitelistGroups.getName() == null || "".equals(whitelistGroups.getName())){
            ObjectError error = new ObjectError("emptyGroupName", "Please fill group name");
            bindingResult.addError(error);
        }
        if(whitelistGroups.getPlateNumbers() == null || whitelistGroups.getPlateNumbers().size() == 0){
            ObjectError error = new ObjectError("emptyCarList", "Please fill car plate numbers");
            bindingResult.addError(error);
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("categoryList", whitelistCategoryService.listAllCategories());
            return "redirect:/whitelist/group/edit/" + id;
        } else {
            whitelistGroupsService.saveWhitelistGroup(whitelistGroups, currentUser.getUsername());
            return "redirect:/whitelist/list";
        }
    }
}