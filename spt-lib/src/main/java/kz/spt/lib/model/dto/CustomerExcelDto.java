package kz.spt.lib.model.dto;

import kz.spt.lib.model.Cars;
import kz.spt.lib.model.Customer;

import java.util.ArrayList;
import java.util.List;

public class CustomerExcelDto {
    private static String dateFormat = "dd.MM.yyyy HH:mm:ss";

    public String firstname;
    public String lastname;
    public String phone;
    public String cars;
    public String email;

    public static CustomerExcelDto fromCustomer(Customer customer) {

        CustomerExcelDto dto = new CustomerExcelDto();
        dto.firstname = customer.getFirstName();
        dto.lastname = customer.getLastName();
        dto.phone = customer.getPhoneNumber();
        dto.email = customer.getEmail();
        List<Cars> cars = customer.getCars();
        if (!cars.isEmpty()) {
            StringBuilder carStringBuilder = new StringBuilder("");
            for (Cars car: cars){
                carStringBuilder.append(carStringBuilder.length() > 0 ? ", " : "");
                carStringBuilder.append(car.getPlatenumber());
            }
            dto.cars = carStringBuilder.toString();
        }
        return dto;
    }

    public static List<CustomerExcelDto> fromCustomers(List<Customer> customers) {
        List<CustomerExcelDto> customerDtos = new ArrayList<>(customers.size());
        for (Customer customer : customers) {
            customerDtos.add(fromCustomer(customer));
        }
        return customerDtos;
    }
}
