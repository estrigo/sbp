package kz.spt.lib.service;


import kz.spt.lib.bootstrap.datatable.Page;
import kz.spt.lib.bootstrap.datatable.PagingRequest;
import kz.spt.lib.model.Customer;

import java.util.List;

public interface CustomerService {

    List<Customer> listAllCustomers();

    void saveCustomer(Customer customer);

    Page<Customer> getCustomer(PagingRequest pagingRequest);

    Customer findById(Long id);

    void deleteCustomer(Customer customer);

    List<Customer> getCustomerIfAnyExist(String phoneNumber);
}
