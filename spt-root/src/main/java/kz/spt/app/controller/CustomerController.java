package kz.spt.app.controller;

import kz.spt.lib.model.Customer;
import kz.spt.lib.service.CustomerService;
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
@RequestMapping("/customers")
public class CustomerController {


    private CustomerService customerService;

    public CustomerController(CustomerService customerService) {
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

    @GetMapping("/edit/{id}")
    public String showFormEditCar(Model model, @PathVariable Long id) {
        model.addAttribute("customer", customerService.findById(id));
        return "customers/edit";
    }

    @PostMapping("/edit/{id}")
    public String processRequestEditCar(@PathVariable Long id, @Valid Customer customer,
                                        BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "redirect:/customers/edit/" + id;
        } else {
            customerService.saveCustomer(customer);
            return "redirect:/customers/list";
        }
    }

    @GetMapping("/delete/{id}")
    public String deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(customerService.findById(id));
        return "redirect:/customers/list";
    }


    @PostMapping("/add")
    public String processRequestAddParking(Model model, @Valid Customer customer, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "customers/add";
        } else {
            if (customer.getFirstName() == null || "".equals(customer.getFirstName())) {
                ObjectError error = new ObjectError("emptyFirstName", "Please fill first name");
                bindingResult.addError(error);
            }
            if (customer.getLastName() == null || "".equals(customer.getLastName())) {
                ObjectError error = new ObjectError("emptyLastName", "Please fill last name");
                bindingResult.addError(error);
            }
            if (customer.getPhoneNumber() == null || "".equals(customer.getPhoneNumber())) {
                ObjectError error = new ObjectError("emptyPhoneNumber", "Please fill phone number");
                bindingResult.addError(error);
            }
            if (customer.getPlateNumbers() == null || customer.getPlateNumbers().size() == 0) {
                ObjectError error = new ObjectError("emptyCarList", "Please fill car plate numbers");
                bindingResult.addError(error);
            }

            if (!bindingResult.hasErrors()) {
                customerService.saveCustomer(customer);
                return "redirect:/customers/list";
            }
            return "customers/add";
        }
    }


}
