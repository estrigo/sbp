package kz.spt.app.service.impl;

import kz.spt.app.repository.CustomerRepository;
import kz.spt.lib.model.Cars;
import kz.spt.lib.model.Customer;
import kz.spt.lib.service.CarsService;
import kz.spt.lib.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.IContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CustomerServiceImpl implements CustomerService {

    @Autowired
    private CarsService carsService;

    @Autowired
    private CustomerRepository customerRepository;

    @Override
    public List<Customer> listAllCustomers() {
        return customerRepository.findAll();
    }

    @Override
    public void saveCustomer(Customer customer) {
        Set<String> plateNumbers = customer.getPlateNumbers().stream().collect(Collectors.toSet());
        customer.setCars(new ArrayList<Cars>());
        for (String plateNumber : plateNumbers) {
            customer.getCars().add(carsService.createCar(plateNumber));
        }
        customerRepository.save(customer);
    }
}
