package crm.controller;

import crm.model.Cars;
import crm.service.CarsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.validation.Valid;


@Controller
@RequestMapping("/cars")
public class CarsController {

    private CarsService carsService;

    public CarsController(CarsService carsService){
        this.carsService = carsService;
    }

    /**
     * /cars/list
     * <p>
     * Shows psginated cars
     *
     * @param model model to add attributes to
     * @return cars/list
     */
    @GetMapping("/list")
    public String showAllCars(Model model) {
        model.addAttribute("cars", carsService.listAllCars());
        return "cars/list";
    }

    @GetMapping("/add")
    public String showFormAddCar(Model model) {
        model.addAttribute("car", new Cars());
        return "cars/add";
    }

    @PostMapping("/add")
    public String processRequestAddCar(Model model, @Valid Cars car, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "cars/add";
        } else {
            if(carsService.findByPlatenumber(car.getPlatenumber()) != null){
                model.addAttribute("alreadyRegisteredPlatenumber",
                        "car.alreadyRegisteredPlateNumber");
                model.addAttribute("car", new Cars());
                return "cars/add";
            } else {
                carsService.saveCars(car);
                return "redirect:/cars/list";
            }

        }
    }

    @GetMapping("/edit/{id}")
    public String showFormEditCar(Model model, @PathVariable Long id) {
        model.addAttribute("car", carsService.findById(id));
        return "cars/edit";
    }

    @PostMapping("/edit/{id}")
    public String processRequestEditCar(@PathVariable Long id, @Valid Cars car,
                                             BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "redirect:/cars/edit/" + id;
        } else {
            carsService.saveCars(car);
            return "redirect:/cars/list";
        }
    }
}
