package kz.spt.lib.service;


import kz.spt.lib.model.Customer;

import java.util.List;

public interface CustomerService {

    List<Customer> listAllCustomers();

    void saveCustomer(Customer customer);



}
