package kz.spt.lib.model;

import org.springframework.data.jpa.domain.Specification;

import java.util.Date;
import java.util.List;

public class EventLogSpecification {

    public static Specification<EventLog> lessDate(Date date) {
        return (root, query, builder) -> builder.lessThanOrEqualTo(root.get(EventLog_.created), date);
    }

    public static Specification<EventLog> greaterDate(Date date) {
        return (root, query, builder) -> builder.greaterThanOrEqualTo(root.get(EventLog_.created), date);
    }

    public static Specification<EventLog> likePlateNumber(String plateNumber) {
        return (root, query, builder) -> builder.like(root.get(EventLog_.CAR).get(Cars_.PLATENUMBER), "%" + plateNumber + "%");
    }

    public static Specification<EventLog> equalGateId(Long gateId){
        return (root, query, builder) -> builder.and(builder.equal(root.get(EventLog_.objectClass), "Gate"), builder.equal(root.get(EventLog_.objectId), gateId));
    }

    public static Specification<EventLog> equalType(EventLog.EventType eventType) {
        return (root, query, builder) -> builder.equal(root.get(EventLog_.eventType), eventType);
    }

    public static Specification<EventLog> inEventType(EventLog.EventType... eventType) {
        return (root, query, builder) -> builder.and(root.get(EventLog_.eventType).in(eventType));
    }
}
