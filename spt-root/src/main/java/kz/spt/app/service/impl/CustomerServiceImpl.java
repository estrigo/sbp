package kz.spt.app.service.impl;

import kz.spt.app.repository.CustomerRepository;
import kz.spt.lib.bootstrap.datatable.*;
import kz.spt.lib.model.*;
import kz.spt.lib.model.dto.BlacklistDto;
import kz.spt.lib.model.dto.CustomerExcelDto;
import kz.spt.lib.service.CarsService;
import kz.spt.lib.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.thymeleaf.util.StringUtils;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.springframework.data.jpa.domain.AbstractPersistable_.id;

@Service
@Transactional(noRollbackFor = Exception.class)
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
        if (ObjectUtils.isEmpty(customer.getEmail())) {
            customer.setEmail(null);
        }
        customer.setCars(new ArrayList<Cars>());
        Customer oldCustomer = customerRepository.getCustomerWithCar(customer.getId());
        if (oldCustomer != null) {
            for (Cars car : oldCustomer.getCars()) {
                if (!plateNumbers.contains(car.getPlatenumber())) {
                    car.setCustomer(null);
                }
            }
            for (String plateNumber : plateNumbers) {
                boolean isContains = false;
                for (Cars car : oldCustomer.getCars()) {
                    if (car.getPlatenumber().equals(plateNumber)) {
                        isContains = true;
                    }
                }

                if (!isContains) {
                    Cars newCar = carsService.createCar(plateNumber);
                    newCar.setCustomer(customer);
                    oldCustomer.getCars().add(newCar);
                }
            }
            oldCustomer.setPhoneNumber(customer.getPhoneNumber());
            oldCustomer.setEmail(customer.getEmail());
            oldCustomer.setMailReceiver(customer.getMailReceiver());
            customerRepository.save(oldCustomer);
        } else {
            for (String plateNumber : plateNumbers) {
                Cars car = carsService.createCar(plateNumber);
                car.setCustomer(customer);
                customer.getCars().add(car);
                customerRepository.save(customer);
            }
        }
    }

    @Override
    public Page<Customer> getCustomer(PagingRequest pagingRequest) {

        String searchValue = pagingRequest.getCustomFilters().get("searchText");
        Specification<Customer> specification = getCustomerSpecification(searchValue);
        org.springframework.data.domain.Page<Customer> filteredCustomers = listByFilters(specification, pagingRequest);
        return getPage(filteredCustomers, pagingRequest);
    }

    private org.springframework.data.domain.Page<Customer> listByFilters(Specification<Customer> CustomerSpecification, PagingRequest pagingRequest) {
        Order order = pagingRequest.getOrder().get(0);

        int columnIndex = order.getColumn();
        Column column = pagingRequest.getColumns().get(columnIndex);
        String columnName = column.getData();
        Direction dir = order.getDir();

        Sort sort = null;
        if ("firstName".equals(columnName)) {
            if (Direction.desc.equals(dir)) {
                sort = Sort.by("firstName").descending();
            } else {
                sort = Sort.by("firstName").ascending();
            }
        } else if ("lastName".equals(columnName)) {
            if (Direction.desc.equals(dir)) {
                sort = Sort.by("lastName").descending();
            } else {
                sort = Sort.by("lastName").ascending();
            }
        } else if ("phoneNumber".equals(columnName)) {
            if (Direction.desc.equals(dir)) {
                sort = Sort.by("phoneNumber").descending();
            } else {
                sort = Sort.by("phoneNumber").ascending();
            }
        } else if ("email".equals(columnName)) {
            if (Direction.desc.equals(dir)) {
                sort = Sort.by("email").descending();
            } else {
                sort = Sort.by("email").ascending();
            }
        }  else {
            if (Direction.asc.equals(dir)) {
                sort = Sort.by("id").ascending();
            } else {
                sort = Sort.by("id").descending();
            }
        }

        Pageable rows = PageRequest.of(pagingRequest.getStart() / pagingRequest.getLength(), pagingRequest.getLength(), sort);
        if (CustomerSpecification != null) {
            return customerRepository.findAll(CustomerSpecification, rows);
        } else {
            return customerRepository.findAll(rows);
        }
    }

    @Override
    public Customer findById(Long id) {
        Optional<Customer> customer = customerRepository.findById(id);
        return customer.orElse(null);

    }

    @Override
    public void deleteCustomer(Customer customer) {
        List<Cars> cars = customer.getCars();
        for (Cars car : cars) {
            car.setCustomer(null);
            carsService.saveCars(car);
        }
        customerRepository.delete(customer);
    }

    private Page<Customer> getPage(org.springframework.data.domain.Page<Customer> customers, PagingRequest pagingRequest) {
        long count = customers.getTotalElements();

        Page<Customer> page = new Page<>(customers.toList());
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
                || (customers.getPhoneNumber() != null && customers.getPhoneNumber().toLowerCase().contains(value.toLowerCase()))
                || (!customers.getCars().isEmpty() && customers.getCars().stream().anyMatch(cars -> cars.getPlatenumber().contains(value.toUpperCase())));
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

    public List<Customer> getCustomerIfAnyExist(String phoneNumber){
        try{
            List<Customer> customers = customerRepository.getCustomerIfAnyExist(phoneNumber);
            return customers;
        }catch (Exception e){
            return new ArrayList<>();
        }
    }

    @Override
    public List<CustomerExcelDto> getCustomerExcel(String searchText) {
        Specification<Customer> specification = getCustomerSpecification(searchText);
        List<Customer> filteredCustomers = listByFiltersExcel(specification);
        return CustomerExcelDto.fromCustomers(filteredCustomers);
    }

    private Specification<Customer> getCustomerSpecification(String searchValue) {
        Specification<Customer> specification = null;

        if (!StringUtils.isEmpty(searchValue)) {
            specification = CustomerSpecification.likePhoneNumber(searchValue)
                    .or(CustomerSpecification.likePlateNumber(searchValue))
                    .or(CustomerSpecification.likeEmail(searchValue))
                    .or(CustomerSpecification.likeFirstName(searchValue))
                    .or(CustomerSpecification.likeLastName(searchValue));
        }
        return specification;
    }

    private List<Customer> listByFiltersExcel(Specification<Customer> CustomerSpecification) {

        Sort sort = Sort.by("id").descending();

        if (CustomerSpecification != null) {
            return customerRepository.findAll(CustomerSpecification, sort);
        } else {
            return customerRepository.findAll(sort);
        }
    }
}
