package kz.spt.billingplugin.model;

import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.Date;

public class PaymentSpecification {
    public static Specification<Payment> lessDate(Date date) {
        return (root, query, builder) -> builder.lessThanOrEqualTo(root.get(Payment_.created), date);
    }

    public static Specification<Payment> greaterDate(Date date) {
        return (root, query, builder) -> builder.greaterThanOrEqualTo(root.get(Payment_.created), date);
    }

    public static Specification<Payment> likePlateNumber(String plateNumber) {
        return (root, query, builder) -> builder.like(root.get(Payment_.carNumber), "%" + plateNumber + "%");
    }

    public static Specification<Payment> equalAmount(BigDecimal amount) {
        if (!amount.equals(BigDecimal.ZERO)) {
            return (root, query, builder) -> builder.equal(root.get(Payment_.price), amount);
        } else {
            return (root, query, builder) -> {
                builder.equal(root.get(Payment_.price), amount);
                return builder.or(builder.isNull(root.get(Payment_.price)));
            };
        }
    }

    public static Specification<Payment> equalProvider(Long id) {
        return (root, query, builder) -> builder.equal(root.get(Payment_.provider).get(PaymentProvider_.id), id);
    }

    public static Specification<Payment> equalTransaction(String transaction) {
        return (root, query, builder) -> builder.like(root.get(Payment_.transaction), "%" + transaction + "%");
    }
}
