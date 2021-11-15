package kz.spt.lib.model;

import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.Date;

public class EventLogSpecification {

    public static Specification<EventLog> lessDate(Date date) {
        return (root, query, builder) -> builder.lessThanOrEqualTo(root.get(EventLog_.created), date);
    }

    public static Specification<EventLog> greaterDate(Date date) {
        return (root, query, builder) -> builder.greaterThanOrEqualTo(root.get(EventLog_.created), date);
    }

    public static Specification<EventLog> likePlateNumber(String plateNumber) {
        return (root, query, builder) -> builder.like(root.get(EventLog_.plateNumber), "%" + plateNumber + "%");
    }

    public static Specification<EventLog> likeDescription(String description) {
        return (root, query, builder) -> builder.like(root.get(EventLog_.description), "%" + description + "%");
    }

    public static Specification<EventLog> orderById() {
        return (root, query, builder) -> {
            query.orderBy(builder.desc(root.get(EventLog_.id)));
            return builder.isNotNull(root.get(EventLog_.id));
        };
    }
}
