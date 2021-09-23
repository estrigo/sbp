package kz.spt.lib.bootstrap.datatable;

import kz.spt.lib.model.Cars;
import kz.spt.lib.model.Customer;
import kz.spt.lib.model.User;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class CustomerComporators {
    @EqualsAndHashCode
    @AllArgsConstructor
    @Getter
    static class Key {
        String name;
        Direction dir;
    }

    static Map<CustomerComporators.Key, Comparator<Customer>> map = new HashMap<>();

    static {
        map.put(new CustomerComporators.Key("firstName", Direction.asc), Comparator.comparing(Customer::getFirstName));
        map.put(new CustomerComporators.Key("firstName", Direction.desc), Comparator.comparing(Customer::getFirstName).reversed());

        map.put(new CustomerComporators.Key("lastName", Direction.asc), Comparator.comparing(Customer::getLastName));
        map.put(new CustomerComporators.Key("lastName", Direction.desc), Comparator.comparing(Customer::getLastName).reversed());

        map.put(new CustomerComporators.Key("phoneNumber", Direction.asc), Comparator.comparing(Customer::getPhoneNumber));
        map.put(new CustomerComporators.Key("phoneNumber", Direction.desc), Comparator.comparing(Customer::getPhoneNumber).reversed());

        map.put(new CustomerComporators.Key("cars", Direction.asc), Comparator.comparing((Customer u)->u.getCars().toString()));
        map.put(new CustomerComporators.Key("cars", Direction.desc), Comparator.comparing((Customer u) ->u.getCars().toString()).reversed());
    }

    public static Comparator<Customer> getComparator(String name, Direction dir) {
        return map.get(new CustomerComporators.Key(name, dir));
    }

    private CustomerComporators() {
    }
}
