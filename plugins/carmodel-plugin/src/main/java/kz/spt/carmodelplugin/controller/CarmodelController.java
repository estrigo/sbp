package kz.spt.carmodelplugin.controller;

import kz.spt.carmodelplugin.service.CarmodelService;
import kz.spt.carmodelplugin.viewmodel.CarmodelDto;
import kz.spt.lib.model.CarModel;
import lombok.extern.java.Log;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.util.StringUtils;

import javax.validation.Valid;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Log
@Controller
@RequestMapping("/carmodel")
public class CarmodelController {

    private CarmodelService carmodelService;

    private final String dateformat = "yyyy-MM-dd'T'HH:mm";

    public CarmodelController(CarmodelService carmodelService) {
        this.carmodelService = carmodelService;
    }

    @GetMapping("/list")
    public String getCarmodelList(Model model, @AuthenticationPrincipal UserDetails currentUser) {
        CarmodelDto CarmodelDto = null;
        if (!model.containsAttribute("CarmodelDto")) {
            SimpleDateFormat format = new SimpleDateFormat(dateformat);
            Calendar calendar = Calendar.getInstance();
            Date dateTo = calendar.getTime();
            calendar.add(Calendar.MINUTE, 1);
            calendar.add(Calendar.DAY_OF_MONTH, -1);
            Date dateFrom = calendar.getTime();
            model.addAttribute("CarmodelDto", CarmodelDto.builder()
                    .dateFromString(format.format(dateFrom))
                    .dateToString(format.format(dateTo))
                    .build());
        }
        model.addAttribute("canEdit", currentUser.getAuthorities().stream().anyMatch(m-> Arrays.asList("ROLE_ADMIN","ROLE_OPERATOR").contains(m.getAuthority())));
        return "carmodel/list";
    }


    @GetMapping("/configure/car")
    public String configureOfCarModel(Model model, @AuthenticationPrincipal UserDetails currentUser,
                                      @PageableDefault(size = 10,
                                              sort = {"model", "type"}) Pageable pageable) {
        model.addAttribute("carModels", carmodelService.findAllUsersPageable(pageable));
        model.addAttribute("canEdit", currentUser.getAuthorities().stream().anyMatch(m-> Arrays.asList("ROLE_ADMIN","ROLE_OPERATOR").contains(m.getAuthority())));
        return "carmodel/configure";
    }


    @GetMapping("/configure/car/edit/{carModelId}")
    public String editCarModel(Model model, @PathVariable Integer carModelId, @AuthenticationPrincipal UserDetails currentUser) {
        model.addAttribute("carModelOne", carmodelService.getCarModelById(carModelId));
        return "/carmodel/editByOne";
    }

    @GetMapping("/configure/car/delete/{id}")
    public String deleteUser(Model model, @PathVariable("id") int id, @AuthenticationPrincipal UserDetails currentUser) {
        carmodelService.deleteCarModel(id);
        model.addAttribute("carModels", carmodelService.findAll());
        model.addAttribute("canEdit", currentUser.getAuthorities().stream().anyMatch(m-> Arrays.asList("ROLE_ADMIN","ROLE_OPERATOR").contains(m.getAuthority())));
        return "carmodel/configure";
    }

    @PostMapping("/list")
    public String postCarmodelList(Model model, @Valid @ModelAttribute("CarmodelDto")CarmodelDto carmodelDto,
                         @AuthenticationPrincipal UserDetails currentUser) throws ParseException {
        if(carmodelDto != null) {
            model.addAttribute("carmodelDto", carmodelDto);
        }
        model.addAttribute("canEdit", currentUser.getAuthorities().stream().anyMatch(m-> Arrays.asList("ROLE_ADMIN").contains(m.getAuthority())));
        return "carmodel/list";
    }

    @PostMapping("/editType")
    public String editPlateNumber(@RequestParam String plateNumber, @RequestParam String dimension){
        log.info("editPlateNumber started, plateNumber: " + plateNumber + ", dimensoin: " + dimension);
        carmodelService.editDimensionOfCar(plateNumber, dimension);
        return "redirect:/carmodel/list";
    }

    @PostMapping("/configure/car/create")
    public String createCarModel(@ModelAttribute("carmodel") @Valid CarModel carModel,
                                 Model model,
                                 BindingResult bindingResult,
                                 @AuthenticationPrincipal UserDetails currentUser) {
        Locale locale = LocaleContextHolder.getLocale();
        String language = "en";
        if (locale.toString().equals("ru")) {
            language = "ru-RU";
        }

        ResourceBundle bundle = ResourceBundle.getBundle("messages", Locale.forLanguageTag(language));

        if (StringUtils.isEmpty(carModel.getModel())) {
            ObjectError error = new ObjectError("modelIsNull", bundle.getString("carmodel.modelIsNull"));
            bindingResult.addError(error);
        } else {
            CarModel carModel1FromDB = carmodelService.findByModel(carModel.getModel());
            if (carModel1FromDB != null) {
                ObjectError error = new ObjectError("alreadyRegisteredMessage", bundle.getString("carmodel.alreadyRegisteredMessage"));
                bindingResult.addError(error);
            }
        }
        if (StringUtils.isEmpty(String.valueOf(carModel.getType()))) {
            ObjectError error = new ObjectError("typeIsNull", bundle.getString("carmodel.typeIsNull"));
            bindingResult.addError(error);
        }
        if (bindingResult.hasErrors()) {
            return "redirect:/carmodel/configure/car/add";}
        else {
            carmodelService.saveCarModel(carModel, currentUser);
            return "redirect:/carmodel/configure/car";
        }
    }

    @GetMapping("/configure/car/add")
    public String addCarModel(Model model, @AuthenticationPrincipal UserDetails currentUser) {
        model.addAttribute("carModel", new CarModel());
        return "/carmodel/create";
    }

    @PostMapping("/configure/car/update/{id}")
    public String updateCarModel(@PathVariable("id") int id,
                             @ModelAttribute("carModel") @Valid CarModel carModel,
                                 Model model,
                                 @AuthenticationPrincipal UserDetails currentUser) {
        carmodelService.updateCarModel(id, carModel, currentUser);

        return "redirect:/carmodel/configure/car";
    }


}
