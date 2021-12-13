package kz.spt.whitelistplugin.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import kz.spt.whitelistplugin.model.Whitelist;
import kz.spt.whitelistplugin.model.WhitelistGroups;
import kz.spt.whitelistplugin.service.RootServicesGetterService;
import kz.spt.whitelistplugin.service.WhitelistGroupsService;
import kz.spt.whitelistplugin.service.WhitelistService;
import lombok.extern.java.Log;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

@Log
@Controller
@RequestMapping("/whitelist")
public class WhitelistController {

    private WhitelistService whitelistService;
    private WhitelistGroupsService whitelistGroupsService;
    private RootServicesGetterService rootServicesGetterService;

    public WhitelistController(WhitelistService whitelistService, WhitelistGroupsService whitelistGroupsService,
                               RootServicesGetterService rootServicesGetterService){
        this.whitelistService = whitelistService;
        this.whitelistGroupsService = whitelistGroupsService;
        this.rootServicesGetterService = rootServicesGetterService;
    }

    @GetMapping("/list")
    public String showAllWhitelist(Model model) throws JsonProcessingException {
        model.addAttribute("whitelist", whitelistService.listAllWhitelist());
        model.addAttribute("whitelistGroups", whitelistGroupsService.listAllWhitelistGroups());
        return "whitelist/list";
    }

    @GetMapping("/add")
    public String showFormAddCar(Model model) throws JsonProcessingException {
        model.addAttribute("whitelist", new Whitelist());
        model.addAttribute("groupList", whitelistGroupsService.listAllWhitelistGroups());
        model.addAttribute("parkingList", rootServicesGetterService.getParkingService().listWhitelistParkings());
        return "whitelist/add";
    }

    @PostMapping("/add")
    public String processRequestAddWhitelist(Model model, @Valid Whitelist whitelist, BindingResult bindingResult, @AuthenticationPrincipal UserDetails currentUser) throws Exception {
        if(whitelist.getPlatenumber()==null || whitelist.getPlatenumber().length() < 3  || whitelist.getPlatenumber().length() > 16){
            ObjectError error = new ObjectError("invalidPlateNumber", "Не правильный гос. номер");
            bindingResult.addError(error);
        }
        if(whitelist.getPlatenumber() != null && whitelistService.findByPlatenumber(whitelist.getPlatenumber(), whitelist.getParkingId())!=null){
            ObjectError error = new ObjectError("plateNumberExist", "Запись с таким гос. номером уже существует в текущем паркинге");
            bindingResult.addError(error);
        }
        if(Whitelist.Type.PERIOD.equals(whitelist.getType()) && whitelist.getAccessEndString() == null){
            ObjectError error = new ObjectError("fillDateEndIsRequired", "Заполнение даты окончания обязательно");
            bindingResult.addError(error);
        }
        if(Whitelist.Type.CUSTOM.equals(whitelist.getType()) && (whitelist.getCustomJson() == null || whitelist.getCustomJson().length() < 4)){
            ObjectError error = new ObjectError("selectHourAndDays", "Выбор дней и часов обязательно");
            bindingResult.addError(error);
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("groupList", whitelistGroupsService.listAllWhitelistGroups());
            model.addAttribute("parkingList", rootServicesGetterService.getParkingService().listWhitelistParkings());
            return "whitelist/add";
        } else {
            whitelistService.saveWhitelist(whitelist, currentUser);
            return "redirect:/whitelist/list";
        }
    }

    @GetMapping("/groups/add")
    public String showFormAddGroup(Model model) {
        model.addAttribute("whitelistGroups", new WhitelistGroups());
        model.addAttribute("parkingList", rootServicesGetterService.getParkingService().listWhitelistParkings());
        return "whitelist/groups/add";
    }

    @PostMapping("/groups/add")
    public String processRequestAddGroup(Model model, @Valid WhitelistGroups whitelistGroups, BindingResult bindingResult, @AuthenticationPrincipal UserDetails currentUser) throws Exception {

        if(whitelistGroups.getName() == null || "".equals(whitelistGroups.getName())){
            ObjectError error = new ObjectError("emptyGroupName", "Пожалуйста заполните название группы");
            bindingResult.addError(error);
        }
        if(whitelistGroups.getPlateNumbers() == null || whitelistGroups.getPlateNumbers().size() == 0){
            ObjectError error = new ObjectError("emptyCarList", "Пожалуйста заполните гос. номеры");
            bindingResult.addError(error);
        } else {
            List<String> plateNumbers = new ArrayList<>();
            for (String plateNumber : whitelistGroups.getPlateNumbers()) {
                plateNumbers.add(plateNumber.toUpperCase());
            }
            whitelistGroups.setPlateNumbers(plateNumbers);

            if(!whitelistGroups.getForceUpdate()){
                List<String> platenumbers = whitelistService.getExistingPlatenumbers(whitelistGroups.getPlateNumbers(), whitelistGroups.getParkingId());
                if(platenumbers.size() > 0){
                    StringBuilder text = null;
                    for(String platenumber:platenumbers){
                        if(text == null){
                            text = new StringBuilder(platenumber);
                        } else {
                            text.append("," + platenumber);
                        }
                    }
                    ObjectError error = new ObjectError("someCarsExist", "Для следующих номеров уже существует записи в текущем паркинге: " + text);
                    bindingResult.addError(error);
                }
            }
        }
        if(Whitelist.Type.PERIOD.equals(whitelistGroups.getType()) && whitelistGroups.getAccessEndString() == null){
            ObjectError error = new ObjectError("fillDateEndIsRequired", "Заполнение даты окончания обязательно");
            bindingResult.addError(error);
        }
        if(Whitelist.Type.CUSTOM.equals(whitelistGroups.getType()) && (whitelistGroups.getCustomJson() == null || whitelistGroups.getCustomJson().length() < 4)){
            ObjectError error = new ObjectError("selectHourAndDays", "Выбор дней и часов обязательно");
            bindingResult.addError(error);
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("whitelistGroups", whitelistGroups);
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
        if(whitelist.getPlatenumber()!=null){
            Whitelist existingWhiteList = whitelistService.findByPlatenumber(whitelist.getPlatenumber(), whitelist.getParkingId());
            if(existingWhiteList!= null && !id.equals(existingWhiteList.getId())){
                ObjectError error = new ObjectError("plateNumberExist", "Запись с гос. номером уже существует в текущем паркинге");
                bindingResult.addError(error);
            }
        }
        if(Whitelist.Type.PERIOD.equals(whitelist.getType()) && whitelist.getAccessEndString() == null){
            ObjectError error = new ObjectError("fillDateEndIsRequired", "Заполнение даты окончания обязательно");
            bindingResult.addError(error);
        }
        if(Whitelist.Type.CUSTOM.equals(whitelist.getType()) && (whitelist.getCustomJson() == null || whitelist.getCustomJson().length() < 4)){
            ObjectError error = new ObjectError("selectHourAndDays", "Выбор дней и часов обязательно");
            bindingResult.addError(error);
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("groupList", whitelistGroupsService.listAllWhitelistGroups());
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
        model.addAttribute("whitelistGroups", whitelistGroupsService.prepareById(id));
        model.addAttribute("parkingList", rootServicesGetterService.getParkingService().listWhitelistParkings());
        return "whitelist/groups/edit";
    }

    @PostMapping("/group/edit/{id}")
    public String processRequestEditWhitelist(Model model, @PathVariable Long id, @Valid WhitelistGroups whitelistGroups,
                                              BindingResult bindingResult, @AuthenticationPrincipal UserDetails currentUser) throws Exception {
        if(whitelistGroups.getName() == null || "".equals(whitelistGroups.getName())){
            ObjectError error = new ObjectError("emptyGroupName", "Пожалуйста заполните название группы");
            bindingResult.addError(error);
        }
        if(whitelistGroups.getPlateNumbers() == null || whitelistGroups.getPlateNumbers().size() == 0){
            ObjectError error = new ObjectError("emptyCarList", "Пожалуйста заполните гос. номеры");
            bindingResult.addError(error);
        } else {
            List<String> plateNumbers = new ArrayList<>();
            for (String plateNumber : whitelistGroups.getPlateNumbers()) {
                plateNumbers.add(plateNumber.toUpperCase());
            }
            whitelistGroups.setPlateNumbers(plateNumbers);

            if(!whitelistGroups.getForceUpdate()) {
                List<String> platenumbers = whitelistService.getExistingPlatenumbers(whitelistGroups.getPlateNumbers(), whitelistGroups.getParkingId(), whitelistGroups.getId());
                if (platenumbers.size() > 0) {
                    StringBuilder text = null;
                    for (String platenumber : platenumbers) {
                        if (text == null) {
                            text = new StringBuilder(platenumber);
                        } else {
                            text.append("," + platenumber);
                        }
                    }
                    ObjectError error = new ObjectError("someCarsExist", "Для следующих номеров уже существует записи в текущем паркинге: " + text);
                    bindingResult.addError(error);
                }
            }
        }
        if(Whitelist.Type.PERIOD.equals(whitelistGroups.getType()) && whitelistGroups.getAccessEndString() == null){
            ObjectError error = new ObjectError("fillDateEndIsRequired", "Заполнение даты окончания обязательно");
            bindingResult.addError(error);
        }
        if(Whitelist.Type.CUSTOM.equals(whitelistGroups.getType()) && (whitelistGroups.getCustomJson() == null || whitelistGroups.getCustomJson().length() < 4)){
            ObjectError error = new ObjectError("selectHourAndDays", "Выбор дней и часов обязательно");
            bindingResult.addError(error);
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("whitelistGroups", whitelistGroups);
            model.addAttribute("parkingList", rootServicesGetterService.getParkingService().listWhitelistParkings());
            return "whitelist/groups/edit";
        } else {
            whitelistGroupsService.saveWhitelistGroup(whitelistGroups, currentUser.getUsername());
            return "redirect:/whitelist/list";
        }
    }
}