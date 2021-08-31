package kz.spt.api.model;

import org.springframework.data.jpa.domain.Specification;

import java.util.Date;

public class EventLogSpecification {

    public static Specification<EventLog> between(Date dateFrom, Date dateTo) {
        return (root, query, builder) -> builder.between(root.get(EventLog_.created), dateFrom, dateTo);
    }

    public static Specification<EventLog> equalPlateNumber(String plateNumber) {
        return (root, query, builder) -> builder.equal(root.get(EventLog_.plateNumber), plateNumber);
    }

    public static Specification<EventLog> orderById() {
        return (root, query, builder) -> {
            query.orderBy(builder.desc(root.get(EventLog_.id)));
            return builder.isNotNull(root.get(EventLog_.id));
        };
    }
}
