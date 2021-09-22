package kz.spt.app.controller;


import kz.spt.lib.model.Customer;
import kz.spt.lib.model.Parking;
import kz.spt.lib.service.CustomerService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.validation.Valid;
import java.util.List;

@Controller
@RequestMapping("/customers")
public class CustomerController {


    private CustomerService customerService;

    public CustomerController(CustomerService customerService)
    {
        this.customerService = customerService;
    }


    @GetMapping("/list")
    public String showAllCustomers(Model model) {

        model.addAttribute("customers", customerService.listAllCustomers());
        return "customers/list";
    }

   @GetMapping("/add")
    public String showFormAddCustomers(Model model) {
        model.addAttribute("customer", new Customer());
        return "customers/add";
    }

    @PostMapping("/add")
    public String processRequestAddParking(Model model, @Valid Customer customer, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "customers/add";
        } else {
            customerService.saveCustomer(customer);
            return "redirect:/customers/list";
        }
    }
}
