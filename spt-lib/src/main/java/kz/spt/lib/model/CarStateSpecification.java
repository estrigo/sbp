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

    public static Specification<CarState> lessEndDate(Date date) {
        return (root, query, builder) -> builder.lessThanOrEqualTo(root.get(CarState_.outTimestamp), date);
    }

    public static Specification<CarState> greaterEndDate(Date date) {
        return (root, query, builder) -> builder.greaterThanOrEqualTo(root.get(CarState_.outTimestamp), date);
    }

    public static Specification<CarState> likePlateNumber(String plateNumber) {
        return (root, query, builder) -> builder.like(root.get(CarState_.carNumber), "%" + plateNumber + "%");
    }

    public static Specification<CarState> inTimestampIsNull() {
        return (root, query, builder) -> builder.isNull(root.get(CarState_.inTimestamp));
    }

    public static Specification<CarState> equalAmount(BigDecimal amount) {
        if (!amount.equals(BigDecimal.ZERO)) {
            return (root, query, builder) -> builder.equal(root.get(CarState_.amount), amount);
        } else {
            return (root, query, builder) -> {
                builder.equal(root.get(CarState_.amount), amount);
                return builder.or(builder.isNull(root.get(CarState_.amount)));
            };
        }
    }

    public static Specification<CarState> equalInGateId(Long inGateId) {
        return (root, query, builder) -> builder.equal(root.get(CarState_.inGate).get(Gate_.id), inGateId);
    }

    public static Specification<CarState> equalOutGateId(Long outGateId) {
        return (root, query, builder) -> builder.equal(root.get(CarState_.outGate).get(Gate_.id), outGateId);
    }

    public static Specification<CarState> emptyOutGateTime() {
        return (root, query, builder) -> builder.isNull(root.get(CarState_.outTimestamp));
    }

    public static Specification<CarState> orderById() {
        return (root, query, builder) -> {
            query.orderBy(builder.desc(root.get(CarState_.id)));
            return builder.isNotNull(root.get(CarState_.id));
        };
    }
}
