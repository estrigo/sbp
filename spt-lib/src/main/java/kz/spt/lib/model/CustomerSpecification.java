package kz.spt.lib.model;

import org.springframework.data.jpa.domain.Specification;

public class CustomerSpecification {

    public static Specification<Customer> likePhoneNumber(String searchText) {
        return (root, query, builder) -> builder.like(root.get(Customer_.PHONE_NUMBER), "%" + searchText.toUpperCase() + "%");
    }

    public static Specification<Customer> likeEmail(String searchText) {
        return (root, query, builder) -> builder.like(root.get(Customer_.EMAIL), "%" + searchText.toUpperCase() + "%");
    }

    public static Specification<Customer> likeFirstName(String searchText) {
        return (root, query, builder) -> builder.like(root.get(Customer_.FIRST_NAME), "%" + searchText.toUpperCase() + "%");
    }

    public static Specification<Customer> likeLastName(String searchText) {
        return (root, query, builder) -> builder.like(root.get(Customer_.LAST_NAME), "%" + searchText.toUpperCase() + "%");
    }

    public static Specification<Customer> likePlateNumber(String searchText) {
        return (root, query, builder) -> builder.like(root.join(Customer_.CARS).get(Cars_.PLATENUMBER), "%" + searchText.toUpperCase() + "%");
    }
}
