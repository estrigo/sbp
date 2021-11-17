package kz.spt.lib.model;

import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.Date;

public class CarStateSpecification {

    public static Specification<CarState> lessDate(Date date) {
        return (root, query, builder) -> builder.lessThanOrEqualTo(root.get(CarState_.inTimestamp), date);
    }

    public static Specification<CarState> greaterDate(Date date) {
        return (root, query, builder) -> builder.greaterThanOrEqualTo(root.get(CarState_.inTimestamp), date);
    }

    public static Specification<CarState> likePlateNumber(String plateNumber) {
        return (root, query, builder) -> builder.like(root.get(CarState_.carNumber), "%" + plateNumber + "%");
    }

    public static Specification<CarState> equalAmount(BigDecimal amount) {
        return (root, query, builder) -> builder.equal(root.get(CarState_.amount), amount);
    }

    public static Specification<CarState> equalInGateId(Long inGateId) {
        return (root, query, builder) -> builder.equal(root.get(CarState_.inGate).get(Gate_.id), inGateId);
    }

    public static Specification<CarState> equalOutGateId(Long outGateId) {
        return (root, query, builder) -> builder.equal(root.get(CarState_.outGate).get(Gate_.id), outGateId);
    }


    public static Specification<CarState> orderById() {
        return (root, query, builder) -> {
            query.orderBy(builder.desc(root.get(CarState_.id)));
            return builder.isNotNull(root.get(CarState_.id));
        };
    }
}
