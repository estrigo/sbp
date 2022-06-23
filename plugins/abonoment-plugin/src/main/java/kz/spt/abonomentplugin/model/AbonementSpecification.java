package kz.spt.abonomentplugin.model;

import kz.spt.lib.model.Cars_;
import org.springframework.data.jpa.domain.Specification;
import java.util.Date;

public class AbonementSpecification {

    public static Specification<Abonement> lessCreateDate(Date date) {
        return (root, query, builder) -> builder.lessThanOrEqualTo(root.get(Abonement_.created), date);
    }

    public static Specification<Abonement> greaterCreateDate(Date date) {
        return (root, query, builder) -> builder.greaterThanOrEqualTo(root.get(Abonement_.created), date);
    }

    public static Specification<Abonement> likePlateNumber(String plateNumber) {
        return (root, query, builder) -> builder.like(root.get(Abonement_.CAR).get(Cars_.PLATENUMBER), "%" + plateNumber + "%");
    }

    public static Specification<Abonement> equalAbonementType(String abonementType) {
        return (root, query, builder) -> builder.equal(root.get(Abonement_.paidType), abonementType);
    }
}
