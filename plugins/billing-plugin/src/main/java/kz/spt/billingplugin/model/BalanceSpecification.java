package kz.spt.billingplugin.model;

import org.springframework.data.jpa.domain.Specification;

public class BalanceSpecification {

    public static Specification<Balance> likePlateNumber(String plateNumber) {
        return (root, query, builder) -> builder.like(root.get(Balance_.plateNumber), "%" + plateNumber + "%");
    }
}
