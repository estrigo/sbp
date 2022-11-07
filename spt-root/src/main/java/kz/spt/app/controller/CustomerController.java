package kz.spt.app.controller;

import kz.spt.lib.model.Customer;
import kz.spt.lib.service.CustomerService;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.validation.Valid;
import java.util.Locale;
import java.util.ResourceBundle;

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
            String phoneNum = customer.getPhoneNumber();
            phoneNum = phoneNum.replace("+","");
            if (phoneNum.substring(0,1).equals("8")){
                phoneNum = "7" + phoneNum.substring(1);
            }
            customer.setPhoneNumber(phoneNum);
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
        Locale locale = LocaleContextHolder.getLocale();
        String language = "en";
        if (locale.toString().equals("ru")) {
            language = "ru-RU";
        }

        ResourceBundle bundle = ResourceBundle.getBundle("messages", Locale.forLanguageTag(language));
        if (bindingResult.hasErrors()) {
            return "customers/add";
        } else {
            if (customer.getFirstName() == null || "".equals(customer.getFirstName())) {
                ObjectError error = new ObjectError("emptyFirstName", bundle.getString("user.firstNameIsNull"));
                bindingResult.addError(error);
            }
            if (customer.getLastName() == null || "".equals(customer.getLastName())) {
                ObjectError error = new ObjectError("emptyLastName", bundle.getString("user.lastNameIsNull"));
                bindingResult.addError(error);
            }
            if (customer.getPhoneNumber() == null || "".equals(customer.getPhoneNumber())) {
                ObjectError error = new ObjectError("emptyPhoneNumber", bundle.getString("customer.emptyPhoneNumber"));
                bindingResult.addError(error);
            }
            if (customer.getPlateNumbers() == null || customer.getPlateNumbers().size() == 0) {
                ObjectError error = new ObjectError("emptyCarList", "Please, fill car plate numbers");
                bindingResult.addError(error);
            }

            if (!bindingResult.hasErrors()) {
                String phoneNum = customer.getPhoneNumber();
                phoneNum = phoneNum.replace("+","");
                if (phoneNum.substring(0,1).equals("8")){
                    phoneNum = "7" + phoneNum.substring(1);
                }
                customer.setPhoneNumber(phoneNum);
                customerService.saveCustomer(customer);
                return "redirect:/customers/list";
            }
            return "customers/add";
        }
    }
}
