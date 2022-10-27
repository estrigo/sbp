package kz.spt.billingplugin.model;

import kz.spt.lib.model.Cars_;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.Date;

public class TransactionSpecification {

    public static Specification<Transaction> lessDate(Date date) {
        return (root, query, builder) -> builder.lessThanOrEqualTo(root.get(Transaction_.date), date);
    }

    public static Specification<Transaction> greaterDate(Date date) {
        return (root, query, builder) -> builder.greaterThanOrEqualTo(root.get(Transaction_.date), date);
    }

    public static Specification<Transaction> likePlateNumber(String plateNumber) {
        return (root, query, builder) -> builder.like(root.get(Transaction_.CAR).get(Cars_.PLATENUMBER), "%" + plateNumber + "%");
    }

    public static Specification<Transaction> equalAmount(Integer amount) {
        return (root, query, builder) -> builder.equal(root.get(Transaction_.amount), BigDecimal.valueOf(amount));
    }

    public static Specification<Transaction> orderById() {
        return (root, query, builder) -> {
            query.orderBy(builder.desc(root.get(Transaction_.id)));
            return builder.isNotNull(root.get(Transaction_.id));
        };
    }
}
