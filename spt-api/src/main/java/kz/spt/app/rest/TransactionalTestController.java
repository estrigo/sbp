package kz.spt.app.rest;

import kz.spt.lib.model.Cars;
import kz.spt.lib.model.Customer;
import kz.spt.lib.service.AbonomentService;
import kz.spt.lib.service.CarsService;
import kz.spt.lib.service.CustomerService;
import kz.spt.lib.service.PluginService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/transactional/test")
public class TransactionalTestController {

    private final AbonomentService abonomentService;
    private final CarsService carsService;
    private final CustomerService customerService;
    private final PluginService pluginService;

    @SneakyThrows
    @Transactional
    @GetMapping
    public ResponseEntity<String> test() {
        Cars cars = new Cars();
        cars.setPlatenumber("ABC");
        Customer customer = new Customer();
        customer.setFirstName("Eugenio");
        customer.setPlateNumbers(List.of("ABCD"));


        pluginService.changeBalance("255AEU02", BigDecimal.valueOf(333L));
        carsService.saveCars(cars);
        abonomentService.deleteAbonoment(12L);
        if (false) {
            throw new RuntimeException();
        }
        customerService.saveCustomer(customer);
        return ResponseEntity.ok("Hello");
    }

}
