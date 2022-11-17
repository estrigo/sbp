package kz.spt.carmodelplugin.controller;

import kz.spt.carmodelplugin.service.CarDimensionsService;
import kz.spt.carmodelplugin.service.CarModelFileServices;
import kz.spt.carmodelplugin.service.CarModelServicePl;
import kz.spt.carmodelplugin.viewmodel.CarmodelDto;
import kz.spt.lib.model.CarModel;
import kz.spt.lib.model.Dimensions;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.thymeleaf.util.StringUtils;

import javax.validation.Valid;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Log
@Controller
@RequestMapping("/carmodel")
public class CarmodelController {

    private CarModelServicePl carModelServicePl;
    private CarDimensionsService carDimensionsService;
    private final String dateformat = "yyyy-MM-dd'T'HH:mm";
    private  CarModelFileServices carModelFileServices;

    public CarmodelController(CarModelServicePl carModelServicePl,
                              CarDimensionsService carDimensionsService,
                              CarModelFileServices carModelFileServices)
    {
        this.carModelServicePl = carModelServicePl;
        this.carDimensionsService = carDimensionsService;
        this.carModelFileServices = carModelFileServices;
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

    @PostMapping("/configure/list")
    public String uploadMultipartFile(@RequestParam("uploadfile") MultipartFile file,
                                      Model model, RedirectAttributes redirectAttributes,
                                      @Valid Dimensions selectedDimension,
                                      @AuthenticationPrincipal UserDetails currentUser) {
        Locale locale = LocaleContextHolder.getLocale();
        String language = "en";
        if (locale.toString().equals("ru")) {
            language = "ru-RU";
        }
        ResourceBundle bundle = ResourceBundle.getBundle("messages", Locale.forLanguageTag(language));
        try {
            carModelFileServices.store(file, selectedDimension, currentUser);
            return "redirect:/carmodel/configure/car";
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/carmodel/configure/car";
        }
    }

    @GetMapping("/configure/car")
    public String configureOfCarModel(Model model, @AuthenticationPrincipal UserDetails currentUser,
                                      @PageableDefault(size = 10,
                                              sort = {"model", "type"}) Pageable pageable) {
        model.addAttribute("carModels", carModelServicePl.findAllUsersPageable(pageable));
        model.addAttribute("canEdit", currentUser.getAuthorities().stream().anyMatch(m-> Arrays.asList("ROLE_ADMIN","ROLE_OPERATOR").contains(m.getAuthority())));
        model.addAttribute("dimensionList", carDimensionsService.listDimensions());
        model.addAttribute("dimensions", new Dimensions());
        return "carmodel/configure";
    }


    @GetMapping("/configure/car/edit/{carModelId}")
    public String editCarModel(Model model, @PathVariable Integer carModelId, @AuthenticationPrincipal UserDetails currentUser) {
        model.addAttribute("carModel", carModelServicePl.getCarModelById(carModelId));
        model.addAttribute("dimensions", carDimensionsService.listDimensions());
        return "/carmodel/editByOne";
    }

    @GetMapping("/configure/car/delete/{id}")
    public String deleteCarModel(Model model, @PathVariable("id") int id, @AuthenticationPrincipal UserDetails currentUser) {
        carModelServicePl.deleteCarModel(id);
        model.addAttribute("carModels", carModelServicePl.findAll());
        model.addAttribute("canEdit", currentUser.getAuthorities().stream().anyMatch(m-> Arrays.asList("ROLE_ADMIN","ROLE_OPERATOR").contains(m.getAuthority())));
        return "redirect:/carmodel/configure/car";
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
        carModelServicePl.editDimensionOfCar(plateNumber, dimension);
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
            CarModel carModel1FromDB = carModelServicePl.findByModel(carModel.getModel());
            if (carModel1FromDB != null) {
                ObjectError error = new ObjectError("alreadyRegisteredMessage", bundle.getString("carmodel.alreadyRegisteredMessage"));
                bindingResult.addError(error);
            }
        }
        if (StringUtils.isEmpty(String.valueOf(carModel.getDimensions().getId()))) {
            ObjectError error = new ObjectError("typeIsNull", bundle.getString("carmodel.typeIsNull"));
            bindingResult.addError(error);
        }
        if (bindingResult.hasErrors()) {
            return "redirect:/carmodel/configure/car/add";}
        else {
            carModelServicePl.saveCarModel(carModel, currentUser);
            return "redirect:/carmodel/configure/car";
        }
    }

    @GetMapping("/configure/car/add")
    public String addCarModel(Model model, @AuthenticationPrincipal UserDetails currentUser) {
        model.addAttribute("carModel", new CarModel());
        model.addAttribute("dimensions", carDimensionsService.listDimensions());
        return "/carmodel/create";
    }

    @GetMapping("/configure/carDimensions/add")
    public String addCarDimensions(Model model, @AuthenticationPrincipal UserDetails currentUser) {
        model.addAttribute("dimensions", new Dimensions());
        model.addAttribute("dimensionslist", carDimensionsService.listDimensions());
        return "/carmodel/createDimensions";
    }
    @PostMapping("/configure/car/dimensions/create")
    public String createCarDimensions(@ModelAttribute("dimensions") @Valid Dimensions dimensions,
                                      Model model,
                                      BindingResult bindingResult,
                                      @AuthenticationPrincipal UserDetails currentUser) {
        carDimensionsService.saveCarDimensions(dimensions, currentUser);

        return "redirect:/carmodel/configure/carDimensions/add";
    }

    @PostMapping("/configure/car/update/{id}")
    public String updateCarModel(@PathVariable("id") int id,
                                 @ModelAttribute("carModel") @Valid CarModel carModel,
                                 Model model,
                                 @AuthenticationPrincipal UserDetails currentUser) {
        Dimensions dimensions = carDimensionsService.getById(carModel.getDimensions().getId());
        carModel.setDimensions(dimensions);
        carModelServicePl.updateCarModel(id, carModel, currentUser);

        return "redirect:/carmodel/configure/car";
    }
}