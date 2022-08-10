package kz.spt.billingplugin.model;

import org.springframework.data.jpa.domain.Specification;

import java.util.Date;

public class BalanceDebtLogSpecification {

    public static Specification<BalanceDebtLog> lessDate(Date date) {
        return (root, query, builder) -> builder.lessThanOrEqualTo(root.get(BalanceDebtLog_.CREATED), date);
    }

    public static Specification<BalanceDebtLog> greaterDate(Date date) {
        return (root, query, builder) -> builder.greaterThanOrEqualTo(root.get(BalanceDebtLog_.CREATED), date);
    }
}
