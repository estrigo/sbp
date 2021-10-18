package kz.spt.whitelistplugin.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import kz.spt.lib.service.ParkingService;
import kz.spt.whitelistplugin.model.AbstractWhitelist;
import kz.spt.whitelistplugin.model.Whitelist;
import kz.spt.whitelistplugin.model.WhitelistGroups;
import kz.spt.whitelistplugin.service.RootServicesGetterService;
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
    private RootServicesGetterService rootServicesGetterService;

    public WhitelistController(WhitelistService whitelistService, WhitelistGroupsService whitelistGroupsService,
                               WhitelistCategoryService whitelistCategoryService, RootServicesGetterService rootServicesGetterService){
        this.whitelistService = whitelistService;
        this.whitelistGroupsService = whitelistGroupsService;
        this.whitelistCategoryService = whitelistCategoryService;
        this.rootServicesGetterService = rootServicesGetterService;
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
        model.addAttribute("parkingList", rootServicesGetterService.getParkingService().listWhitelistParkings());
        return "whitelist/add";
    }

    @PostMapping("/add")
    public String processRequestAddCar(Model model, @Valid Whitelist whitelist, BindingResult bindingResult, @AuthenticationPrincipal UserDetails currentUser) throws Exception {
        if(whitelist.getPlatenumber()==null || whitelist.getPlatenumber().length() < 3  || whitelist.getPlatenumber().length() > 16){
            ObjectError error = new ObjectError("invalidPlateNumber", "Не правильный гос. номер");
            bindingResult.addError(error);
        }
        if(whitelist.getCategoryId() == null){
            ObjectError error = new ObjectError("selectCategory", "Пожалуйста выберите категорию");
            bindingResult.addError(error);
        }
        if(Whitelist.Type.PERIOD.equals(whitelist.getType()) && whitelist.getAccess_end() == null){
            ObjectError error = new ObjectError("selectCategory", "Заполнение даты окончания обязательно");
            bindingResult.addError(error);
        }
        if(Whitelist.Type.CUSTOM.equals(whitelist.getType()) && (whitelist.getCustomJson() == null || whitelist.getCustomJson().length() < 4)){
            ObjectError error = new ObjectError("selectCategory", "Выбор дней и часов обязательно");
            bindingResult.addError(error);
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("groupList", whitelistGroupsService.listAllWhitelistGroups());
            model.addAttribute("categoryList", whitelistCategoryService.listAllCategories());
            model.addAttribute("parkingList", rootServicesGetterService.getParkingService().listWhitelistParkings());
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
        model.addAttribute("parkingList", rootServicesGetterService.getParkingService().listWhitelistParkings());
        return "whitelist/groups/add";
    }

    @PostMapping("/groups/add")
    public String processRequestAddGroup(Model model, @Valid WhitelistGroups whitelistGroups, BindingResult bindingResult, @AuthenticationPrincipal UserDetails currentUser) throws Exception {
        if(whitelistGroups.getCategoryId() == null){
            ObjectError error = new ObjectError("selectCategory", "Пожалуйста выберите категорию");
            bindingResult.addError(error);
        }
        if(whitelistGroups.getName() == null || "".equals(whitelistGroups.getName())){
            ObjectError error = new ObjectError("emptyGroupName", "Пожалуйста заполните название группы");
            bindingResult.addError(error);
        }
        if(whitelistGroups.getPlateNumbers() == null || whitelistGroups.getPlateNumbers().size() == 0){
            ObjectError error = new ObjectError("emptyCarList", "Пожалуйста заполните гос. номеры");
            bindingResult.addError(error);
        }
        if(Whitelist.Type.PERIOD.equals(whitelistGroups.getType()) && whitelistGroups.getAccess_end() == null){
            ObjectError error = new ObjectError("selectCategory", "Заполнение даты окончания обязательно");
            bindingResult.addError(error);
        }
        if(Whitelist.Type.CUSTOM.equals(whitelistGroups.getType()) && (whitelistGroups.getCustomJson() == null || whitelistGroups.getCustomJson().length() < 4)){
            ObjectError error = new ObjectError("selectCategory", "Выбор дней и часов обязательно");
            bindingResult.addError(error);
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("categoryList", whitelistCategoryService.listAllCategories());
            model.addAttribute("parkingList", rootServicesGetterService.getParkingService().listWhitelistParkings());
            return "whitelist/groups/add";
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
        model.addAttribute("parkingList", rootServicesGetterService.getParkingService().listWhitelistParkings());
        return "whitelist/edit";
    }

    @GetMapping("/delete/{id}")
    public String deleteWhiteList(@PathVariable Long id) {
        whitelistService.deleteById(id);
        return "redirect:/whitelist/list";
    }

    @PostMapping("/edit/{id}")
    public String processRequestEditWhitelist(Model model, @PathVariable Long id, @Valid Whitelist whitelist,
                                        BindingResult bindingResult, @AuthenticationPrincipal UserDetails currentUser) throws Exception {
        if(whitelist.getPlatenumber()==null || whitelist.getPlatenumber().length() < 3 || whitelist.getPlatenumber().length() > 16){
            ObjectError error = new ObjectError("invalidPlateNumber", "Не правильный гос. номер");
            bindingResult.addError(error);
        }
        if(whitelist.getCategoryId() == null){
            ObjectError error = new ObjectError("selectCategory", "Пожалуйста выберите категорию");
            bindingResult.addError(error);
        }
        if(Whitelist.Type.PERIOD.equals(whitelist.getType()) && whitelist.getAccess_end() == null){
            ObjectError error = new ObjectError("selectCategory", "Заполнение даты окончания обязательно");
            bindingResult.addError(error);
        }
        if(Whitelist.Type.CUSTOM.equals(whitelist.getType()) && (whitelist.getCustomJson() == null || whitelist.getCustomJson().length() < 4)){
            ObjectError error = new ObjectError("selectCategory", "Выбор дней и часов обязательно");
            bindingResult.addError(error);
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("groupList", whitelistGroupsService.listAllWhitelistGroups());
            model.addAttribute("categoryList", whitelistCategoryService.listAllCategories());
            model.addAttribute("parkingList", rootServicesGetterService.getParkingService().listWhitelistParkings());
            return "whitelist/edit";
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
        model.addAttribute("parkingList", rootServicesGetterService.getParkingService().listWhitelistParkings());
        return "whitelist/groups/edit";
    }

    @PostMapping("/group/edit/{id}")
    public String processRequestEditWhitelist(Model model, @PathVariable Long id, @Valid WhitelistGroups whitelistGroups,
                                              BindingResult bindingResult, @AuthenticationPrincipal UserDetails currentUser) throws Exception {
        if(whitelistGroups.getCategoryId() == null){
            ObjectError error = new ObjectError("selectCategory", "Пожалуйста выберите категорию");
            bindingResult.addError(error);
        }
        if(whitelistGroups.getName() == null || "".equals(whitelistGroups.getName())){
            ObjectError error = new ObjectError("emptyGroupName", "Пожалуйста заполните название группы");
            bindingResult.addError(error);
        }
        if(whitelistGroups.getPlateNumbers() == null || whitelistGroups.getPlateNumbers().size() == 0){
            ObjectError error = new ObjectError("emptyCarList", "Пожалуйста заполните гос. номеры");
            bindingResult.addError(error);
        }
        if(Whitelist.Type.PERIOD.equals(whitelistGroups.getType()) && whitelistGroups.getAccess_end() == null){
            ObjectError error = new ObjectError("selectCategory", "Заполнение даты окончания обязательно");
            bindingResult.addError(error);
        }
        if(Whitelist.Type.CUSTOM.equals(whitelistGroups.getType()) && (whitelistGroups.getCustomJson() == null || whitelistGroups.getCustomJson().length() < 4)){
            ObjectError error = new ObjectError("selectCategory", "Выбор дней и часов обязательно");
            bindingResult.addError(error);
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("whitelistGroup", whitelistGroupsService.prepareById(id));
            model.addAttribute("categoryList", whitelistCategoryService.listAllCategories());
            model.addAttribute("parkingList", rootServicesGetterService.getParkingService().listWhitelistParkings());
            return "whitelist/groups/edit";
        } else {
            whitelistGroupsService.saveWhitelistGroup(whitelistGroups, currentUser.getUsername());
            return "redirect:/whitelist/list";
        }
    }
}