package kz.spt.app.rest;

import kz.spt.lib.bootstrap.datatable.Page;
import kz.spt.lib.bootstrap.datatable.PagingRequest;
import kz.spt.lib.model.Customer;
import kz.spt.lib.model.dto.CustomerExcelDto;
import kz.spt.lib.service.CustomerService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/rest/customers")
public class CustomerRestController {

    private CustomerService customerService;

    public CustomerRestController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    public Page<Customer> list(@RequestBody PagingRequest pagingRequest) {
        return customerService.getCustomer(pagingRequest);
    }

    @GetMapping("/excel")
    public List<CustomerExcelDto> list(@RequestParam String searchText) {
        return customerService.getCustomerExcel(searchText);
    }

    @GetMapping("/customerExist/{phoneNum}")
    public Integer customerExistMessage(@PathVariable String phoneNum) throws Exception {
        Integer result = 0;
        String replacement = "7";
        phoneNum = phoneNum.replace("+7",replacement);
        phoneNum = phoneNum.replace("+","");
        if (phoneNum.substring(0,1).equals("8")){
            phoneNum = replacement + phoneNum.substring(1);
        }else if(phoneNum.substring(0,1).equals("+")){
            phoneNum = phoneNum.replace("+","");
        }

        List<Customer> customers =  customerService.getCustomerIfAnyExist(phoneNum);
        if (customers.size()>0) {
            result = 1;
        }
        else {
            result = 0;
        }
        return result;
    }
}
