package kz.spt.app.rest;

import kz.spt.lib.model.Cars;
import kz.spt.lib.model.Customer;
import kz.spt.lib.service.CarsService;
import kz.spt.lib.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/transactional/test")
public class TransactionalTestController {

    private final CarsService carsService;
    private final CustomerService customerService;

    @Transactional
    @GetMapping
    public ResponseEntity<String> test() {
        Cars cars = new Cars();
        cars.setPlatenumber("ABC");
        Customer customer = new Customer();
        customer.setFirstName("Eugenio");
        customer.setPlateNumbers(List.of("ABCD"));

        carsService.saveCars(cars);
        if (true) {
            throw new RuntimeException();
        }
        customerService.saveCustomer(customer);
        return ResponseEntity.ok("Hello");
    }

}
