package kz.spt.app.service.impl;

import kz.spt.app.repository.CustomerRepository;
import kz.spt.lib.bootstrap.datatable.*;
import kz.spt.lib.model.Cars;
import kz.spt.lib.model.Customer;
import kz.spt.lib.service.CarsService;
import kz.spt.lib.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.util.StringUtils;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class CustomerServiceImpl implements CustomerService {

    @Autowired
    private CarsService carsService;

    @Autowired
    private CustomerRepository customerRepository;

    private static final Comparator<Customer> EMPTY_COMPARATOR = (e1, e2) -> 0;


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

    @Override
    public Page<Customer> getCustomer(PagingRequest pagingRequest) {
        List<Customer> customers = customerRepository.findAll();
        return getPage(customers, pagingRequest);
    }

    private Page<Customer> getPage(List<Customer> customers, PagingRequest pagingRequest) {
        List<Customer> filtered = customers.stream()
                .sorted(sortCustomers(pagingRequest))
                .filter(filterCustomers(pagingRequest))
                .skip(pagingRequest.getStart())
                .limit(pagingRequest.getLength())
                .collect(Collectors.toList());

        long count = customers.stream()
                .filter(filterCustomers(pagingRequest))
                .count();

        Page<Customer> page = new Page<>(filtered);
        page.setRecordsFiltered((int) count);
        page.setRecordsTotal((int) count);
        page.setDraw(pagingRequest.getDraw());

        return page;
    }

    private Predicate<Customer> filterCustomers(PagingRequest pagingRequest) {
        if (pagingRequest.getSearch() == null || StringUtils.isEmpty(pagingRequest.getSearch()
                .getValue())) {
            return customers -> true;
        }
        String value = pagingRequest.getSearch().getValue();

        return customers -> (customers.getFirstName() != null && customers.getFirstName().toLowerCase().contains(value.toLowerCase()))
                || (customers.getLastName() != null && customers.getLastName().toLowerCase().contains(value.toLowerCase()))
                || (customers.getPhoneNumber() != null && customers.getPhoneNumber().toLowerCase().contains(value.toLowerCase()));
    }

    private Comparator<Customer> sortCustomers(PagingRequest pagingRequest) {
        if (pagingRequest.getOrder() == null) {
            return EMPTY_COMPARATOR;
        }

        try {
            Order order = pagingRequest.getOrder()
                    .get(0);

            int columnIndex = order.getColumn();
            Column column = pagingRequest.getColumns()
                    .get(columnIndex);

            Comparator<Customer> comparator = CustomerComporators.getComparator(column.getData(), order.getDir());
            return Objects.requireNonNullElse(comparator, EMPTY_COMPARATOR);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return EMPTY_COMPARATOR;
    }
}
