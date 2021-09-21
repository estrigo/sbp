package kz.spt.app.controller;


import kz.spt.lib.model.Customer;
import kz.spt.lib.service.CustomerService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

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

}
